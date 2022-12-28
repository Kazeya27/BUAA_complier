package frontend.syntax.exp;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.stmt.Stmt;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class LAndExp extends SyntaxUnit{
    private final LAndExp lAndExp;
    private final Token operation;
    private final EqExp eqExp;

    public LAndExp(String type, List<SyntaxUnit> units, LAndExp lAndExp, Token operation, EqExp eqExp) {
        super(type, units);
        this.lAndExp = lAndExp;
        this.operation = operation;
        this.eqExp = eqExp;
    }

    // LAndExp → EqExp { '&&' EqExp }
    //  LAndExp → EqExp | LAndExp '&&' EqExp
    public static LAndExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "LAndExp";
        List<SyntaxUnit> units = new ArrayList<>();
        LAndExp lAndExp = null;
        Token operation = null;
        EqExp eqExp = EqExp.parse();
        units.add(eqExp);
        while (tokenLocator.getTokenType(0).equals(Type.AND)) {
            lAndExp = new LAndExp(syntax,units,lAndExp,operation,eqExp);
            units = new ArrayList<>();
            units.add(lAndExp);
            operation = tokenLocator.goAhead();
            units.add(operation);  // ||
            eqExp = EqExp.parse();
            units.add(eqExp);
        }
        return new LAndExp(syntax,units,lAndExp,operation,eqExp);
    }

    public void errorSolve() {
        if (lAndExp != null) {
            lAndExp.errorSolve();
        }
        if (eqExp != null) {
            eqExp.errorSolve();
        }
    }

    // LAndExp → EqExp { '&&' EqExp }
    //  LAndExp → EqExp | LAndExp '&&' EqExp
    public void icode(String endLabel) {
        if (lAndExp != null) {
            lAndExp.icode(endLabel);
        }
        Intermediate.getInstance().addCode(MidCode.Operation.AND);
        eqExp.icode(endLabel,true);
    }

}
