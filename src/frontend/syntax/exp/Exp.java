package frontend.syntax.exp;

import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class Exp extends SyntaxUnit{
    private AddExp addExp;

    public Exp(String type, List<SyntaxUnit> units, AddExp addExp) {
        super(type, units);
        this.addExp = addExp;
    }

    //  Exp → AddExp
    public static Exp parse() throws SymbolLack {
        String syntax = "Exp";
        List<SyntaxUnit> units = new ArrayList<>();
        AddExp addExp = AddExp.parse();
        units.add(addExp);
        return new Exp(syntax,units,addExp);
    }

    public Integer getValue() throws ComplexExp {
        return addExp.getValue();
    }

    public boolean errorSolve() {
        return addExp.errorSolve();
    }

    public int getDim() {
        return addExp.getDim();
    }

    public String icode() {
        try {
            return Integer.toString(getValue());
        } catch (ComplexExp ignored) {
        }
        String ans = addExp.icode();
        if (ans.contains("[") && !ans.contains("&")) {
            ans = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",ans,"*OPERAND1*");
        }
        return ans;
    }

}
