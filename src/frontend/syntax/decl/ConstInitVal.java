package frontend.syntax.decl;

import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.ConstExp;

import java.util.ArrayList;
import java.util.List;

public class ConstInitVal extends SyntaxUnit{
    private final List<ConstInitVal> constInitVals;
    private final ConstExp constExp;

    public ConstInitVal(String type, List<SyntaxUnit> units, List<ConstInitVal> constInitVals, ConstExp constExp) {
        super(type, units);
        this.constInitVals = constInitVals;
        this.constExp = constExp;
    }

    // ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public static ConstInitVal parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "ConstInitVal";
        List<SyntaxUnit> units = new ArrayList<>();
        List<ConstInitVal> constInitVals = new ArrayList<>();
        ConstInitVal constInitVal;
        ConstExp constExp = null;
        if (tokenLocator.getTokenType(0).equals(Type.LBRACE)) {
            units.add(tokenLocator.goAhead()); // {
            constInitVal = ConstInitVal.parse();
            units.add(constInitVal);
            constInitVals.add(constInitVal);
            while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
                units.add(tokenLocator.goAhead()); // ,
                constInitVal = ConstInitVal.parse();
                units.add(constInitVal);
                constInitVals.add(constInitVal);
            }
            units.add(tokenLocator.goAhead()); // }
        }
        else {
            constExp = ConstExp.parse();
            units.add(constExp);
        }
        return new ConstInitVal(syntax, units, constInitVals,constExp);
    }

    public int getValue() throws ComplexExp {
        return constExp.getValue();
    }

    public void errorSolve() throws SymbolLack {
        if (constExp != null) {
            constExp.errorSolve();
        }
        else {
            for (ConstInitVal initVal:constInitVals) {
                initVal.errorSolve();
            }
        }
    }

    public String icode() {
        if (constExp != null) {
            return constExp.icode();
        }
        return null;
    }

    // ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    // a[2][2] = {}/{{1,1},{}}
    public void getValues(List<Integer> values) {
        if (constExp == null) {
            for (ConstInitVal initVal:constInitVals) {
                initVal.getValues(values);
            }
        }
        else {
            try {
                values.add(constExp.getValue());
            } catch (ComplexExp ignored) {
            }
        }
    }

    public void getInitValues(List<String> values) {
        if (constExp == null) {
            for (ConstInitVal initVal:constInitVals) {
                initVal.getInitValues(values);
            }
        }
        else {
            values.add(constExp.icode());
        }
    }

}
