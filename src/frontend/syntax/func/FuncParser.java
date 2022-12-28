package frontend.syntax.func;

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
import frontend.syntax.stmt.multiple.Block;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class FuncParser extends SyntaxUnit{
    private final FuncType funcType;
    private final Token ident;
    private final FuncFParams funcFParams;
    private final Block block;

    public FuncParser(String type, List<SyntaxUnit> units, FuncType funcType, Token ident, FuncFParams funcFParams, Block block) {
        super(type, units);
        this.funcType = funcType;
        this.ident = ident;
        this.funcFParams = funcFParams;
        this.block = block;
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public static FuncParser parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "FuncDef";
        List<SyntaxUnit> units = new ArrayList<>();
        FuncType funcType = FuncType.parse();
        Token ident = tokenLocator.goAhead();
        FuncFParams funcFParams = null;
        Block block;
        units.add(funcType);    // FuncType
        units.add(ident);                // Ident
        units.add(tokenLocator.goAhead());                // (
        if (tokenLocator.getTokenType(0).equals(Type.INTTK)) {
            funcFParams = FuncFParams.parse();
            units.add(funcFParams); // FuncFParams
        }
        try {
                units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
            }
        block = Block.parse();
        units.add(block);
        return new FuncParser(syntax, units, funcType,ident,funcFParams,block);
    }

    public void errorSolve() throws SymbolLack {
        if (ErrorSolver.hasSym(ident.getString(),false)) {
            ErrorList.getInstance().add(new Error(ErrorType.NRDF,ident.getLine()));
            return;
        }
        Symbol.DataType dataType;
        if (funcType.getType() == Type.INTTK) {
            dataType = Symbol.DataType.INT;
        }
        else {
            dataType = Symbol.DataType.VOID;
        }
        Symbol symbol = new Symbol(ident.getString(), Symbol.Kind.FUNC, dataType,ident.getLine());
        ErrorSolver.addSym(symbol);
        SymbolTable params;
        if (funcFParams != null)
            params = funcFParams.errorSolve();
        else
            params = new SymbolTable();
        SymbolTable.addFunc(ident.getString(),params);
        ErrorSolver.setCurFuncRtnType(dataType);
        block.errorSolve(params,ident.getString());
    }

    public void icode() {
        String operand1 = ident.getString();
        String operand2 = (funcType.getType() == Type.VOIDTK)?"VOID":"INT";
        Intermediate.getInstance().addCode(MidCode.Operation.FUNC,operand1,operand2,"*NULL*");
        block.icode(funcFParams);
        Intermediate.getInstance().addCode(MidCode.Operation.RETURN,"*NULL*","*NULL*","*NULL*");
        Intermediate.getInstance().addCode(MidCode.Operation.FUNC_END,"*NULL*","*NULL*","*NULL*");
    }
}
