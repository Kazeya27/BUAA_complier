package frontend.syntax.decl;

import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.ConstExp;
import frontend.syntax.exp.Exp;

import java.util.ArrayList;
import java.util.List;

public class InitVal extends SyntaxUnit{
    private final List<InitVal> initVals;
    private final Exp exp;

    public InitVal(String type, List<SyntaxUnit> units, List<InitVal> initVals, Exp exp) {
        super(type, units);
        this.initVals = initVals;
        this.exp = exp;
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public static InitVal parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "InitVal";
        List<SyntaxUnit> units = new ArrayList<>();
        List<InitVal> initVals = new ArrayList<>();
        Exp exp = null;
        if (tokenLocator.getTokenType(0).equals(Type.LBRACE)) {
            units.add(tokenLocator.goAhead()); // {
            if (!tokenLocator.getTokenType(0).equals(Type.RBRACE)) {
                InitVal initVal = InitVal.parse();
                units.add(initVal);
                initVals.add(initVal);
                while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
                    units.add(tokenLocator.goAhead()); // ,
                    initVal = InitVal.parse();
                    units.add(initVal);
                    initVals.add(initVal);
                }
            }
            units.add(tokenLocator.goAhead()); // }
        }
        else {
            exp = Exp.parse();
            units.add(exp);
        }
        return new InitVal(syntax, units, initVals,exp);
    }

    public void errorSolve() throws SymbolLack {
        if (exp != null) {
            exp.errorSolve();
        }
        else {
            for (InitVal initVal:initVals) {
                initVal.errorSolve();
            }
        }
    }

    public int getValue() throws ComplexExp {
        return exp.getValue();
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // a[2][2] = {}/{{1,1},{}}
    public void getValues(List<String> values) {
        if (exp == null) {
            for (InitVal initVal:initVals) {
                initVal.getValues(values);
            }
        }
        else {
            try {
                values.add(Integer.toString(exp.getValue()));
            } catch (ComplexExp ignored) {
            }
        }
    }

    public void getInitValues(List<String> values) {
        if (exp == null) {
            for (InitVal initVal:initVals) {
                initVal.getInitValues(values);
            }
        }
        else {
            values.add(exp.icode());
        }
    }

    public String icode() {
        if (exp != null) {
            return exp.icode();
        }
        return null;
    }

}
