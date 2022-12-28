package frontend.syntax.stmt.multiple;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.decl.DeclParser;
import frontend.syntax.stmt.Stmt;

import java.util.ArrayList;
import java.util.List;

public class BlockItem extends SyntaxUnit{
    private final DeclParser declParser;
    private final Stmt stmt;

    public BlockItem(String type, List<SyntaxUnit> units, DeclParser declParser, Stmt stmt) {
        super(type, units);
        this.declParser = declParser;
        this.stmt = stmt;
    }

    // BlockItem → Decl | Stmt
    //          const/int
    public static BlockItem parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "BlockItem";
        List<SyntaxUnit> units = new ArrayList<>();
        DeclParser declParser = null;
        Stmt stmt = null;
        if (tokenLocator.getTokenType(0).equals(Type.INTTK) ||
                tokenLocator.getTokenType(0).equals(Type.CONSTTK)) {
            declParser = DeclParser.parse();
            units.add(declParser);
        }
        else {
            stmt = Stmt.parse();
            units.add(stmt);
        }
        return new BlockItem(syntax, units, declParser, stmt);
    }

    public boolean isReturn() {
        if (stmt == null)
            return false;
        return stmt.isReturn();
    }

    public void errorSolve() throws SymbolLack {
        if (declParser != null) {
            declParser.errorSolve();
        }
        else {
            stmt.errorSolve();
        }
    }

    public void icode() {
        if (declParser != null) {
            declParser.icode();
        }
        else {
            stmt.icode();
        }
    }

}
