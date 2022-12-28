package frontend.syntax.decl;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.ConstExp;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class ConstDef extends SyntaxUnit {
    private final Token ident;
    private final List<ConstExp> constExps;
    private final ConstInitVal constInitVal;

    public ConstDef(String type, List<SyntaxUnit> units, Token ident, List<ConstExp> constExps, ConstInitVal constInitVal) {
        super(type, units);
        this.ident = ident;
        this.constExps = constExps;
        this.constInitVal = constInitVal;
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    public static ConstDef parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "ConstDef";
        List<SyntaxUnit> units = new ArrayList<>();
        Token ident = tokenLocator.goAhead();
        List<ConstExp> constExps = new ArrayList<>();
        units.add(ident);
        while (tokenLocator.getTokenType(0).equals(Type.LBRACK)) {
            units.add(tokenLocator.goAhead()); // '['
            ConstExp constExp = ConstExp.parse();
            units.add(constExp); // 'ConstExp'
            constExps.add(constExp);
            try {
                units.add(tokenLocator.goAhead(Type.RBRACK)); // ']'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSBR, tokenLocator.getLastLine()));
            }
        }
        units.add(tokenLocator.goAhead()); // =
        ConstInitVal constInitVal = ConstInitVal.parse();
        units.add(constInitVal);
        return new ConstDef(syntax, units,ident,constExps,constInitVal);
    }

    public Token getIdent() {
        return ident;
    }

    public int getDim(int dim) throws ComplexExp {
        if (constExps.size() < dim)
            return -1;
        else
            return constExps.get(dim-1).getValue();
    }

    public void errorSolve() throws SymbolLack {
        if (ErrorSolver.hasSym(ident.getString(),false)) {
            ErrorList.getInstance().add(new Error(ErrorType.NRDF,ident.getLine()));
            return;
        }
        for (ConstExp constExp:constExps) {
            constExp.errorSolve();
        }
        constInitVal.errorSolve();
        int row = -1;
        int col = -1;
        try {
            row = getDim(1);
            col = getDim(2);
        } catch (ComplexExp ignored) {

        }
        if (row == -1 && col == -1) {
            int initVal = 0;
            try {
                initVal = constInitVal.getValue();
            } catch (ComplexExp ignored) {
            }
            //int initVal = 0;
            //if (ErrorSolver.isGlobal) {
            //    try {
            //        initVal = constInitVal.getValue();
            //    } catch (ComplexExp ignored) {
            //    }
            //}
            Symbol symbol = new Symbol(ident.getString(),row,col, initVal, Symbol.Kind.CONST, Symbol.DataType.INT,ident.getLine());
            ErrorSolver.addSym(symbol);
        }
        else {
            List<Integer> initVal = new ArrayList<>();
            constInitVal.getValues(initVal);
            //if (ErrorSolver.isGlobal) {
            //    constInitVal.getValues(initVal);
            //}
            Symbol symbol = new Symbol(ident.getString(),row,col, initVal, Symbol.Kind.CONST, Symbol.DataType.ARRAY,ident.getLine());
            ErrorSolver.addSym(symbol);
        }
    }

    private int getSize() {
        Symbol symbol = Intermediate.getInstance().getSym(ident.getString(),ident.getLine());
        int row = symbol.getSize1();
        int col = symbol.getSize2();
        int rst;
        if (row == -1) {
            rst = 4;
        }
        else {
            rst = 4*row;
            if (col > 0) {
                rst *= col;
            }
        }
        return rst;
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    public void icode() {
        String operand1 = Intermediate.getMidName(ident.getString(), ident.getLine());
        String operand2;
        int size = getSize();
        // 简单变量直接取值
        // 数组变量需要对initVal特殊处理一下
        if (constExps.size() == 0) {
            if (Intermediate.isGlobal) {
                operand2 = Integer.toString(Intermediate.getInstance().getSym(ident.getString(),ident.getLine()).getValue());
            }
            else {
                operand2 = constInitVal.icode();
            }
            Intermediate.getInstance().addCode(MidCode.Operation.CONST,operand1,operand2,"*NULL*",size);
        }
        else {
            // 将数组展平，逐层调用填数组

            Intermediate.getInstance().addCode(MidCode.Operation.ARRAY,operand1,String.valueOf(size/4),"*NULL*");
            if (Intermediate.isGlobal) {
                List<Integer> values;
                values = Intermediate.getInstance().getSym(ident.getString(),ident.getLine()).getValues();
                for (int i = 0;i<values.size();i++) {
                    Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_SAVE,operand1 + "[" + i + "]",
                            String.valueOf(values.get(i)),"*NULL*");
                }
            }
            else {
                List<String> values = new ArrayList<>();
                constInitVal.getInitValues(values);
                for (int i = 0;i<values.size();i++) {
                    Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_SAVE,operand1 + "[" + i + "]",
                            values.get(i),"*NULL*");
                }
            }

        }
    }
}
