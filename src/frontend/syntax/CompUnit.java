package frontend.syntax;

import frontend.exceptions.SymbolLack;
import frontend.syntax.decl.DeclParser;
import frontend.syntax.func.FuncParser;
import frontend.syntax.func.MainFuncDef;

import java.util.List;

public class CompUnit extends SyntaxUnit{
    private final List<DeclParser> declParsers;
    private final List<FuncParser> funcParsers;
    private final MainFuncDef mainFuncDef;

    public CompUnit(String type, List<SyntaxUnit> units, List<DeclParser> declParsers, List<FuncParser> funcParsers, MainFuncDef mainFuncDef) {
        super(type, units);
        this.declParsers = declParsers;
        this.funcParsers = funcParsers;
        this.mainFuncDef = mainFuncDef;
    }

    public void errorSolve() throws SymbolLack {
        for (DeclParser declParser:declParsers) {
            declParser.errorSolve();
        }
        for (FuncParser funcParser:funcParsers) {
            funcParser.errorSolve();
        }
        mainFuncDef.errorSolve();
    }

    public void icode() {
        for (DeclParser declParser:declParsers) {
            declParser.icode();
        }
        for (FuncParser funcParser:funcParsers) {
            funcParser.icode();
        }
        mainFuncDef.icode();
    }

}
