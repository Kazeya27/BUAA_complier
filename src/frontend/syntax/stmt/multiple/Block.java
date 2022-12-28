package frontend.syntax.stmt.multiple;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.func.FuncFParam;
import frontend.syntax.func.FuncFParams;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class Block extends SyntaxUnit{
    private final List<BlockItem> blockItems;
    private final Token RBrace;                // 记录g类错误行号

    public Block(String type, List<SyntaxUnit> units, List<BlockItem> blockItems, Token RBrace) {
        super(type, units);
        this.blockItems = blockItems;
        this.RBrace = RBrace;
    }

    // Block → '{' { BlockItem } '}'
    public static Block parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "Block";
        List<SyntaxUnit> units = new ArrayList<>();
        List<BlockItem> blockItems = new ArrayList<>();
        units.add(tokenLocator.goAhead());  // {
        // BlockItem → Decl | Stmt
        while (tokenLocator.getTokenType(0) != Type.RBRACE) {
            BlockItem blockItem = BlockItem.parse();
            units.add(blockItem);
            blockItems.add(blockItem);
        }
        Token RBrace = tokenLocator.goAhead();
        units.add(RBrace); // }
        return new Block(syntax, units, blockItems, RBrace);
    }

    public void errorSolve(SymbolTable params,String funcName) throws SymbolLack {
        ErrorSolver.addDepth((Token) units.get(0),funcName);
        ErrorSolver.addSyms(params);
        for (BlockItem item:blockItems) {
            item.errorSolve();
        }
        if (ErrorSolver.getCurFuncRtnType() == Symbol.DataType.INT) {
            if (blockItems.size() == 0) {
                ErrorList.getInstance().add(new Error(ErrorType.NORT, RBrace.getLine()));
            }
            else {
                BlockItem item = blockItems.get(blockItems.size()-1);
                if (!item.isReturn()) {
                    ErrorList.getInstance().add(new Error(ErrorType.NORT, RBrace.getLine()));
                }
            }
        }
        ErrorSolver.minuDepth();
    }

    public void errorSolve() throws SymbolLack {
        ErrorSolver.addDepth((Token) units.get(0));
        for (BlockItem item:blockItems) {
            item.errorSolve();
        }
        ErrorSolver.minuDepth();
    }

    public void icode(FuncFParams funcFParams) {
        Intermediate.setCurTable(Intermediate.getSubTable((Token) units.get(0)));
        Intermediate.getInstance().addCode(MidCode.Operation.ENTER_BLOCK,"*NULL*","*NULL*","*NULL*",(Token) units.get(0));
        if (funcFParams != null)
            funcFParams.icode();
        for (BlockItem item:blockItems) {
            item.icode();
        }
        Intermediate.getInstance().addCode(MidCode.Operation.EXIT_BLOCK,"*NULL*","*NULL*","*NULL*");
        Intermediate.setWithParent();
    }

    public void icode() {
        Intermediate.setCurTable(Intermediate.getSubTable((Token) units.get(0)));
        Intermediate.getInstance().addCode(MidCode.Operation.ENTER_BLOCK,"*NULL*","*NULL*","*NULL*",(Token) units.get(0));
        for (BlockItem item:blockItems) {
            item.icode();
        }
        Intermediate.getInstance().addCode(MidCode.Operation.EXIT_BLOCK,"*NULL*","*NULL*","*NULL*");
        Intermediate.setWithParent();
    }

}
