package frontend.syntax.func;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class FuncFParams extends SyntaxUnit{
    private final List<FuncFParam> funcFParams;

    public FuncFParams(String type, List<SyntaxUnit> units, List<FuncFParam> funcFParams) {
        super(type, units);
        this.funcFParams = funcFParams;
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }
    public static FuncFParams parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "FuncFParams";
        List<SyntaxUnit> units = new ArrayList<>();
        List<FuncFParam> funcFParams = new ArrayList<>();
        FuncFParam funcFParam = FuncFParam.parse();
        units.add(funcFParam);
        funcFParams.add(funcFParam);
        while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
            units.add(tokenLocator.goAhead());
            funcFParam = FuncFParam.parse();
            units.add(funcFParam);
            funcFParams.add(funcFParam);
        }
        return new FuncFParams(syntax, units, funcFParams);
    }

    public SymbolTable errorSolve() {
        SymbolTable symbolTable = new SymbolTable();
        for (FuncFParam funcFParam:funcFParams) {
            funcFParam.errorSolve(symbolTable);
        }
        return symbolTable;
    }

    public void icode() {
        for (FuncFParam funcFParam:funcFParams) {
            funcFParam.icode();
        }
    }

}
