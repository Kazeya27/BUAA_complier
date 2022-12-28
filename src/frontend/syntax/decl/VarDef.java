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

public class VarDef extends SyntaxUnit{
    private final Token ident;
    private final List<ConstExp> constExps;
    private final InitVal initVal;

    public VarDef(String type, List<SyntaxUnit> units, Token ident, List<ConstExp> constExps, InitVal initVal) {
        super(type, units);
        this.ident = ident;
        this.constExps = constExps;
        this.initVal = initVal;
    }

    // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    public static VarDef parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "VarDef";
        List<SyntaxUnit> units = new ArrayList<>();
        Token ident = tokenLocator.goAhead();
        units.add(ident); // Indet
        List<ConstExp> constExps = new ArrayList<>();
        InitVal initVal = null;
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
        if (tokenLocator.getTokenType(0).equals(Type.ASSIGN)) {
            units.add(tokenLocator.goAhead()); // =
            initVal = InitVal.parse();
            units.add(initVal);
        }
        return new VarDef(syntax, units,ident, constExps,initVal);
    }

    public int getDim(int dim) throws ComplexExp {
        if (constExps.size() < dim)
            return -1;
        else
            return constExps.get(dim-1).getValue();
    }

    private int getSize() throws ComplexExp {
        int row = getDim(1);
        int col = getDim(2);
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

    public void errorSolve() throws SymbolLack {
        if (ErrorSolver.hasSym(ident.getString(),false)) {
            ErrorList.getInstance().add(new Error(ErrorType.NRDF,ident.getLine()));
            return;
        }
        for (ConstExp exp:constExps) {
            exp.errorSolve();
        }
        if (initVal != null) {
            initVal.errorSolve();
        }
        int row = -1;
        int col = -1;
        try {
            row = getDim(1);
            col = getDim(2);
        } catch (ComplexExp ignored) {
        }
        if (initVal == null) {
            Symbol symbol;
            if (row == -1 && col == -1) {
                symbol = new Symbol(ident.getString(), row, col, Symbol.Kind.VAR, Symbol.DataType.INT,ident.getLine());
            }
            else {
                symbol = new Symbol(ident.getString(), row, col, Symbol.Kind.VAR, Symbol.DataType.ARRAY,ident.getLine());
            }
            ErrorSolver.addSym(symbol);
        }
        else {
            if (row == -1 && col == -1) {
                int val = 0;
                if (ErrorSolver.isGlobal) {
                    try {
                        val = initVal.getValue();
                    } catch (ComplexExp ignored) {
                    }
                }
                Symbol symbol = new Symbol(ident.getString(),row,col, val, Symbol.Kind.VAR, Symbol.DataType.INT,ident.getLine());
                ErrorSolver.addSym(symbol);
            }
            else {
                List<Integer> values = new ArrayList<>();
                if (ErrorSolver.isGlobal) {
                    List<String> tmp = new ArrayList<>();
                    initVal.getValues(tmp);
                    // 全局变量一定可以解析出数字
                    for (String v:tmp) {
                        values.add(Integer.parseInt(v));
                    }
                }
                Symbol symbol = new Symbol(ident.getString(),row,col, values, Symbol.Kind.VAR, Symbol.DataType.ARRAY,ident.getLine());
                ErrorSolver.addSym(symbol);
            }
        }
    }

    public void icode() {
        String operand1 = Intermediate.getMidName(ident.getString(), ident.getLine());
        int size = 0;
        try {
            size = getSize();
        } catch (ComplexExp e) {
            throw new RuntimeException(e);
        }
        String operand2 = "*NULL*";
        // 简单变量直接取值
        // 数组变量需要对initVal特殊处理一下
        if (constExps.size() == 0) {
            if (initVal != null) {
                if (Intermediate.isGlobal) {
                    operand2 = Integer.toString(Intermediate.getInstance().getSym(ident.getString(),ident.getLine()).getValue());
                }
                else {
                    operand2 = initVal.icode();
                }
            }
            Intermediate.getInstance().addCode(MidCode.Operation.VAR,operand1,operand2,"*NULL*",size);
        }
        else {
            // 将数组展平，逐层调用填数组
            Intermediate.getInstance().addCode(MidCode.Operation.ARRAY,operand1,String.valueOf(size/4),"*NULL*");
            if (initVal != null) {
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
                    initVal.getInitValues(values);
                    for (int i = 0;i<values.size();i++) {
                        Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_SAVE,operand1 + "[" + i + "]",
                                values.get(i),"*NULL*");
                    }
                }

            }
        }
    }

}
