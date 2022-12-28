package frontend.syntax.decl;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class ConstDecl extends SyntaxUnit {
    private final Token semi;
    private final List<ConstDef> constDefs;

    public ConstDecl(String type, List<SyntaxUnit> units, Token semi, List<ConstDef> constDefs) {
        super(type, units);
        this.semi = semi;
        this.constDefs = constDefs;
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    public static ConstDecl parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "ConstDecl";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead()); // const
        units.add(tokenLocator.goAhead()); // int
        List<ConstDef> constDefs = new ArrayList<>();
        ConstDef constDef = ConstDef.parse();
        units.add(constDef);
        constDefs.add(constDef);
        while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
            units.add(tokenLocator.goAhead());
            constDef = ConstDef.parse();
            units.add(constDef);
            constDefs.add(constDef);
        }
        Token semi = null;
        try {
           semi = tokenLocator.goAhead(Type.SEMICN);
           units.add(semi); //;
        } catch (SymbolLack e) {
            ErrorList.getInstance().add(new Error(ErrorType.MSSM, tokenLocator.getLastLine()));
        }

        return new ConstDecl(syntax, units, semi, constDefs);
    }

    public List<ConstDef> getConstDefs() {
        return constDefs;
    }

    public void errorSolve() throws SymbolLack {
        for (ConstDef constDef:constDefs) {
            constDef.errorSolve();
        }
    }

    public void icode() {
        for (ConstDef constDef: constDefs) {
            constDef.icode();
        }
    }
}
