package frontend.syntax.func;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.stmt.multiple.Block;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class MainFuncDef extends SyntaxUnit{
    private final Block block;

    public MainFuncDef(String type, List<SyntaxUnit> units, Block block) {
        super(type, units);
        this.block = block;
    }

    public static MainFuncDef parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "MainFuncDef";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead()); // int
        units.add(tokenLocator.goAhead()); // main
        units.add(tokenLocator.goAhead()); // (
        try {
                units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
            }
        Block block = Block.parse();
        units.add(block);
        return new MainFuncDef(syntax, units, block);
    }

    public void errorSolve() throws SymbolLack {
        ErrorSolver.setCurFuncRtnType(Symbol.DataType.INT);
        SymbolTable symbolTable = new SymbolTable();
        block.errorSolve(symbolTable,"main");
    }

    public void icode() {
        String operand1 = "main";
        String operand2 = "INT";
        Intermediate.getInstance().addCode(MidCode.Operation.FUNC,operand1,operand2,"*NULL*");
        block.icode();
        Intermediate.getInstance().addCode(MidCode.Operation.FUNC_END,"*NULL*","*NULL*","*NULL*");
    }

}
