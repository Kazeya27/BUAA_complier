package frontend.syntax.stmt.multiple;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class MulParser extends SyntaxUnit{
    public enum MulType {
        BRANCH,LOOP,BLOCK
    }

    private MulType mulType;

    public MulParser(String type, List<SyntaxUnit> units, MulType mulType) {
        super(type, units);
        this.mulType = mulType;
    }

    // MulStmt → BrcStmt | LoopStmt | Block
    public static MulParser parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "MulStmt";
        MulType mulType;
        List<SyntaxUnit> units = new ArrayList<>();
        if (tokenLocator.getTokenType(0).equals(Type.IFTK)) {
            mulType = MulType.BRANCH;
            units.add(BrcStmt.parse());
        }

        else if (tokenLocator.getTokenType(0).equals(Type.WHILETK)) {
            mulType = MulType.LOOP;
            units.add(LoopStmt.parse());
        }
        else {
            mulType = MulType.BLOCK;
            units.add(Block.parse());
        }
        return new MulParser(syntax,units, mulType);
    }

    public void errorSolve() throws SymbolLack {
        SyntaxUnit unit = units.get(0);
        if (mulType == MulType.BRANCH) {
            ((BrcStmt) unit).errorSolve();
        }
        else if (mulType == MulType.LOOP) {
            ((LoopStmt) unit).errorSolve();
        }
        else {
            ((Block) unit).errorSolve();
        }
    }

    public void icode() {
        SyntaxUnit unit = units.get(0);
        if (mulType == MulType.BRANCH) {
            ((BrcStmt) unit).icode();
        }
        else if (mulType == MulType.LOOP) {
            ((LoopStmt) unit).icode();
        }
        else {
            ((Block) unit).icode();
        }
    }

}
