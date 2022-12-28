package frontend.syntax.decl;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class DeclParser extends SyntaxUnit{
    private final ConstDecl constDecl;
    private final VarDecl varDecl;

    public DeclParser(String type, List<SyntaxUnit> units, ConstDecl constDecl, VarDecl varDecl) {
        super(type, units);
        this.constDecl = constDecl;
        this.varDecl = varDecl;
    }

    // Decl → ConstDecl | VarDecl
    public static DeclParser parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "Decl";
        List<SyntaxUnit> units = new ArrayList<>();
        ConstDecl constDecl = null;
        VarDecl varDecl = null;
        if (tokenLocator.getTokenType(0).equals(Type.CONSTTK)) {
            constDecl = ConstDecl.parse();
            units.add(constDecl);
        }
        else {
            varDecl = VarDecl.parse();
            units.add(varDecl);
        }
        return new DeclParser(syntax, units, constDecl, varDecl);
    }

    public boolean isConst() {
        return constDecl != null;
    }

    public void errorSolve() throws SymbolLack {
        if (isConst()) {
            constDecl.errorSolve();
        }
        else {
            varDecl.errorSolve();
        }
    }

    public void icode() {
        if (isConst()) {
            constDecl.icode();
        }
        else {
            varDecl.icode();
        }
    }

}
