package frontend.syntax.stmt.single;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.Exp;
import frontend.syntax.exp.LVal;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class SingleParser extends SyntaxUnit{
    public enum SingleType {
        ASSIGN,EXP,BREAK,CONTINUE,RETURN,GETINT,PRINTF
    }

    private final SingleType singleType;

    public SingleParser(String type, List<SyntaxUnit> units, SingleType singleType) {
        super(type, units);
        this.singleType = singleType;
    }

    // SingleStmt → AssignStmt | ExpStmt | BreakStmt | CtnStmt | RtnStmt | GIStmt | PFStmt
    //              Ident    (/Ident/number/+-!  break   continue  return   Ident    printf
    //               =        [                                              =
    //
    public static SingleParser parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "SingleStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        SingleType singleType;
        if (tokenLocator.getTokenType(0).equals(Type.BREAKTK)) {
            singleType = SingleType.BREAK;
            units.add(BreakStmt.parse());
        }
        else if (tokenLocator.getTokenType(0).equals(Type.CONTINUETK)) {
            singleType = SingleType.CONTINUE;
            units.add(CtnStmt.parse());
        }
        else if (tokenLocator.getTokenType(0).equals(Type.RETURNTK)) {
            singleType = SingleType.RETURN;
            units.add(RtnStmt.parse());
        }
        else if (tokenLocator.getTokenType(0).equals(Type.PRINTFTK)) {
            singleType = SingleType.PRINTF;
            units.add(PFStmt.parse());
        }
        else {
            SyntaxUnit exp = Exp.parse();
            SyntaxUnit lVal = exp.getLVal();
            if (lVal != null) {
                if (tokenLocator.getTokenType(0).equals(Type.ASSIGN)) {
                    if (tokenLocator.getTokenType(1).equals(Type.GETINTTK)) {
                        singleType = SingleType.GETINT;
                        units.add(lVal);              // LVal
                        units.add(tokenLocator.goAhead());  // =
                        units.add(tokenLocator.goAhead());  // getint
                        units.add(tokenLocator.goAhead());  // (
                        try {
                            units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
                        } catch (SymbolLack e) {
                            ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
                        }
                    }
                    else {
                        singleType = SingleType.ASSIGN;
                        units.add(lVal);              // LVal
                        units.add(tokenLocator.goAhead());  // =
                        units.add(Exp.parse()); // Exp
                    }
                }
                else {
                    singleType = SingleType.EXP;
                    units.add(exp);
                }
            }
            else {
                singleType = SingleType.EXP;
                units.add(exp);
            }
        }
        return new SingleParser(syntax,units, singleType);
    }

    public void errorSolve() throws SymbolLack {
        SyntaxUnit unit = units.get(0);
        if (singleType == SingleType.ASSIGN || singleType == SingleType.GETINT) {
            Token ident = ((LVal) unit).getIdent();
            Symbol symbol = ErrorSolver.getSym(ident.getString());
            if (symbol == null) {
                ErrorList.getInstance().add(new Error(ErrorType.NUDF,ident.getLine()));
                return;
            }
            else {
                if (symbol.getKind() == Symbol.Kind.CONST) {
                    ErrorList.getInstance().add(new Error(ErrorType.COTC,ident.getLine()));
                    return;
                }
            }
            if (singleType == SingleType.ASSIGN) {
                ((Exp) units.get(2)).errorSolve();
            }
        }
        else if (singleType == SingleType.EXP) {
            ((Exp) unit).errorSolve();
        }
        else if (singleType == SingleType.BREAK) {
            ((BreakStmt) unit).errorSolve();
        }
        else if (singleType == SingleType.CONTINUE) {
            ((CtnStmt) unit).errorSolve();
        }
        else if (singleType == SingleType.RETURN) {
            ((RtnStmt) unit).errorSolve();
        }
        else if (singleType == SingleType.PRINTF) {
            ((PFStmt) unit).errorSolve();
        }
    }

    public void icode() {
        SyntaxUnit unit = units.get(0);
        if (singleType == SingleType.ASSIGN) {
            String operand1 = ((LVal) unit).icode();
            String operand2 = ((Exp) units.get(2)).icode();
            // 数组还是普通变量，数组通过array_save存（中间变量）
            // 存在'['、']'
            if (operand1.matches(".*\\[.*].*")) {
                String temp = Intermediate.getInstance().addCode(MidCode.Operation.ASSIGN,"*TEMP*",operand2,"*OPERAND1*");
                Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_SAVE,operand1,temp,"*NULL*");
            }
            // 普通变量
            else {
                Intermediate.getInstance().addCode(MidCode.Operation.ASSIGN,operand1,operand2,"*NULL*");
            }
        }
        else if (singleType == SingleType.GETINT) {
            String operand1 = ((LVal) unit).icode();
            if (operand1.matches(".*\\[.*].*")) {
                String temp = Intermediate.getInstance().addCode(MidCode.Operation.GETINT,"*TEMP*","*NULL*","*OPERAND1*");
                Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_SAVE,operand1,temp,"*NULL*");
            }
            else {
                Intermediate.getInstance().addCode(MidCode.Operation.GETINT,operand1,"*NULL*","*NULL*");
            }
        }
        else if (singleType == SingleType.EXP) {
            ((Exp) unit).icode();
        }
        else if (singleType == SingleType.BREAK) {
            ((BreakStmt) unit).icode();
        }
        else if (singleType == SingleType.CONTINUE) {
            ((CtnStmt) unit).icode();
        }
        else if (singleType == SingleType.RETURN) {
            ((RtnStmt) unit).icode();
        }
        else if (singleType == SingleType.PRINTF) {
            ((PFStmt) unit).icode();
        }
    }

    public SingleType getSingleType() {
        return singleType;
    }
}
