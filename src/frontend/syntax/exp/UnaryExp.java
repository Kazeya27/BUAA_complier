package frontend.syntax.exp;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class UnaryExp extends SyntaxUnit{
    private final UnaryOp unaryOp;
    private final Token ident;
    private final PrimaryExp primaryExp;
    private final FuncRParams funcRParams;
    private final UnaryExp unaryExp;

    public UnaryExp(String type, List<SyntaxUnit> units, UnaryOp unaryOp, Token ident, PrimaryExp primaryExp, FuncRParams funcRParams, UnaryExp unaryExp) {
        super(type, units);
        this.unaryOp = unaryOp;
        this.ident = ident;
        this.primaryExp = primaryExp;
        this.funcRParams = funcRParams;
        this.unaryExp = unaryExp;
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    //          (/Ident/num        Ident                          +-!
    public static UnaryExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "UnaryExp";
        List<SyntaxUnit> units = new ArrayList<>();
        Token ident = null;
        UnaryOp unaryOp = null;
        PrimaryExp primaryExp = null;
        FuncRParams funcRParams = null;
        UnaryExp unaryExp = null;
        if (tokenLocator.getTokenType(0).equals(Type.IDENFR) &&
                tokenLocator.getTokenType(1).equals(Type.LPARENT)) {
            ident = tokenLocator.goAhead();
            units.add(ident); // Identifier
            units.add(tokenLocator.goAhead()); // (
            try {
                funcRParams = FuncRParams.parse();
                units.add(funcRParams);
            } catch (SymbolLack ignored) {

            }
            try {
                units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
            }
        }
        else if (tokenLocator.getTokenType(0).equals(Type.PLUS) ||
                tokenLocator.getTokenType(0).equals(Type.MINU) ||
                tokenLocator.getTokenType(0).equals(Type.NOT)) {
            unaryOp = UnaryOp.parse();
            units.add(unaryOp);
            unaryExp = UnaryExp.parse();
            units.add(unaryExp);
        }
        else {
            primaryExp = PrimaryExp.parse();
            units.add(primaryExp);
        }
        return new UnaryExp(syntax, units,unaryOp,ident,primaryExp,funcRParams,unaryExp);
    }

    public int getValue() throws ComplexExp {
        int val = 0;
        if (unaryOp != null) {
            val = unaryExp.getValue();
            Type op = unaryOp.getType();
            if (op == Type.MINU) {
                val = -val;
            }
            else if (op == Type.NOT) {
                val = (val == 0)?1:0;
            }
        }
        else if (primaryExp != null) {
            val = primaryExp.getValue();
        }
        else if (ident != null) {
            throw new ComplexExp();
        }
        return val;
    }

    public boolean errorSolve() {
        if (primaryExp != null) {
            return primaryExp.errorSolve();
        }
        else if (ident != null) {
            Symbol symbol = ErrorSolver.getSym(ident.getString());
            if (symbol == null || symbol.getKind() != Symbol.Kind.FUNC) {
                ErrorList.getInstance().add(new Error(ErrorType.NUDF, ident.getLine()));
                return false;
            }
            SymbolTable fParas = SymbolTable.getSymbolTable(ident.getString());
            if (funcRParams != null)
                return funcRParams.errorSolve(fParas,ident.getLine());
            else {
                if (fParas.getSymbolMap().size() > 0) {
                    ErrorList.getInstance().add(new Error(ErrorType.FACM, ident.getLine()));
                    return false;
                }
            }
        }
        return true;
    }

    public int getDim() {
        if (primaryExp != null) {
            return primaryExp.getDim();
        }
        else if (unaryExp != null)  {
            return unaryExp.getDim();
        }
        else {
            Symbol symbol = ErrorSolver.getSym(ident.getString());
            if (symbol == null) {
                ErrorList.getInstance().add(new Error(ErrorType.NUDF,ident.getLine()));
                // 抛异常
                return -2;
            }
            if (symbol.getKind() == Symbol.Kind.FUNC) {
                if (symbol.getDataType() == Symbol.DataType.INT)
                    return 0;
                else
                    return -1;
            }
            else {
                // 不是函数调用，抛异常
                return -2;
            }
        }
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    // PrimaryExp → '(' Exp ')' | LVal | Number
    public String icode() {
        try {
            return Integer.toString(getValue());
        } catch (ComplexExp ignored) {
        }
        // 存在函数调用或变量给变量赋值
        if (ident != null) {
            String operand1 = ident.getString();
            Intermediate.getInstance().addCode(MidCode.Operation.PREPARE,operand1,"*NULL*","*NULL*");
            if (funcRParams != null) {
                funcRParams.icode();
            }
            Intermediate.getInstance().addCode(MidCode.Operation.CALL,operand1,"*NULL*","*NULL*");
            return Intermediate.getInstance().addCode(MidCode.Operation.ASSIGN,"*TEMP*","*RST*","*OPERAND1*");
        }
        else if (primaryExp != null){
            return primaryExp.icode();
        }
        else {
            Type type = unaryOp.getType();
            MidCode.Operation op;
            switch (type) {
                case MINU:
                    op = MidCode.Operation.SUB;
                    break;
                case PLUS:
                    op = MidCode.Operation.ADD;
                    break;
                default:
                    op = MidCode.Operation.NOT;
                    break;
            }
            if (op.equals(MidCode.Operation.SUB) || op.equals(MidCode.Operation.ADD)) {
                return Intermediate.getInstance().addCode(op,"0",unaryExp.icode(),"*TEMP*");
            }
            else {
                String operand2 = unaryExp.icode();
                if (operand2.contains("[")) {
                    operand2 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand2,"*OPERAND1*");
                }
                return Intermediate.getInstance().addCode(MidCode.Operation.SET,operand2, MidCode.Operation.EQL,"0","*TEMP*");
            }
        }
    }

}
