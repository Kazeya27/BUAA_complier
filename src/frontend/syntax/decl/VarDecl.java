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

public class VarDecl extends SyntaxUnit{
    private final List<VarDef> varDefs;
    private final Token semi;

    public VarDecl(String type, List<SyntaxUnit> units, List<VarDef> varDefs, Token semi) {
        super(type, units);
        this.varDefs = varDefs;
        this.semi = semi;
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    public static VarDecl parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "VarDecl";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead()); // int
        List<VarDef> varDefs = new ArrayList<>();
        VarDef varDef = VarDef.parse();
        units.add(varDef);
        varDefs.add(varDef);
        while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
            units.add(tokenLocator.goAhead());    // ,
            varDef = VarDef.parse();
            units.add(varDef);
            varDefs.add(varDef);
        }
        Token semi = null;
        try {
            semi = tokenLocator.goAhead(Type.SEMICN);
            units.add(semi); //;
        } catch (SymbolLack e) {
            ErrorList.getInstance().add(new Error(ErrorType.MSSM, tokenLocator.getLastLine()));
        }

        return new VarDecl(syntax, units, varDefs, semi);
    }

    public void errorSolve() throws SymbolLack {
        for (VarDef varDef:varDefs) {
            varDef.errorSolve();
        }
    }

    public void icode() {
        for (VarDef varDef: varDefs) {
            varDef.icode();
        }
    }
}
