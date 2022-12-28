package frontend.syntax.exp;

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
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class LVal extends SyntaxUnit{
    private final Token ident;
    private final List<Exp> exps;

    public LVal(String type, List<SyntaxUnit> units, Token ident, List<Exp> exps) {
        super(type, units);
        this.ident = ident;
        this.exps = exps;
    }

    // LVal → Ident {'[' Exp ']'}
    public static LVal parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "LVal";
        List<SyntaxUnit> units = new ArrayList<>();
        Token ident = tokenLocator.goAhead();
        List<Exp> exps = new ArrayList<>();
        units.add(ident);
        while (tokenLocator.getTokenType(0).equals(Type.LBRACK)) {
            units.add(tokenLocator.goAhead()); // [
            Exp exp = Exp.parse();
            units.add(exp);// Exp
            exps.add(exp);
            try {
                units.add(tokenLocator.goAhead(Type.RBRACK)); // ']'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSBR, tokenLocator.getLastLine()));
            }
        }
        return new LVal(syntax, units, ident, exps);
    }

    public int getSize(int dim) throws ComplexExp {
        if (exps.size() < dim)
            return -1;
        else
            return exps.get(dim-1).getValue();
    }

    public int getValue() {
        String name = ident.getString();
        if (!ErrorSolver.hasSym(name,true)) {
            ErrorList.getInstance().add(new Error(ErrorType.NUDF,ident.getLine()));
            // 未定义变量异常
            return -1;
        }
        Symbol symbol = ErrorSolver.getCurTable().getSym(name);
        int row = -1;
        int col = -1;
        try {
            row = getSize(1);
            col = getSize(2);
        } catch (ComplexExp ignored) {
        }
        if (symbol.getKind() != Symbol.Kind.FUNC) {
            return symbol.getValue(row,col);
        }
        else {
            // 标识符是函数抛出异常
            return -1;
        }
    }

    public boolean errorSolve() {
        if (!ErrorSolver.hasSym(ident.getString(),true)) {
            ErrorList.getInstance().add(new Error(ErrorType.NUDF,ident.getLine()));
            return false;
        }
        for (Exp exp:exps) {
            exp.errorSolve();
        }
        return true;
    }

    public int getDim() {
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
            return symbol.getDim() - exps.size();
        }
    }

    public List<Exp> getExps() {
        return exps;
    }

    public String icode() {
        // 要从符号表取出来
        String var = ident.getString();
        Symbol symbol = Intermediate.getInstance().getSym(var,ident.getLine());
        int size1 = symbol.getSize1();
        int size2 = symbol.getSize2();
        // 作为参数时传值还是传指针
        var = Intermediate.getMidName(ident.getString(),ident.getLine());
        if (exps.size() == 2) {
            // 一定传值
            String d1 = exps.get(0).icode();
            String d2 = exps.get(1).icode();
            String xDim;
            String oneDim; // 数组展平后的下标
            // 或者通过parseInt捕获异常判断
            if (d1.matches("^[0-9]+$")) {
                xDim = String.valueOf(Integer.parseInt(d1)*size2);
            }
            // 通过四元式表示复杂数组下标
            else {
                xDim = Intermediate.getInstance().addCode(MidCode.Operation.MUL,d1,String.valueOf(size2),"*TEMP*");
            }
            try {
                oneDim = String.valueOf(Integer.parseInt(xDim) + Integer.parseInt(d2));
            } catch (NumberFormatException e) {
                // 通过四元式表示复杂数组下标
                oneDim = Intermediate.getInstance().addCode(MidCode.Operation.ADD,xDim,d2,"*TEMP*");
            }
            var += "[" + oneDim + "]";

        }
        else if (exps.size() == 1) {
            // 数组本身是二维，但使用一维，会传指针
            if (size2 != -1) {
                var += "&";
            }
            String oneDim = exps.get(0).icode();
            if (oneDim.matches(".*\\[.*].*")) {
                // 生成变量赋值，并返回该变量
                oneDim = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",oneDim,"*OPERAND1*");
            }
            var += "[" + oneDim + "]";

        }
        else if (size1 != -1){
            // 数组本身是一维，但直接使用指针
            var += "&";
        }
        return var;
    }

    public Token getIdent() {
        return ident;
    }
}
