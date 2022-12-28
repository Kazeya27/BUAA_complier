package frontend.syntax.exp;

import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.LinkedList;
import java.util.List;

public class ConstExp extends SyntaxUnit{
    private final AddExp addExp;

    public ConstExp(String type, List<SyntaxUnit> units, AddExp addExp) {
        super(type, units);
        this.addExp = addExp;
    }

    public static ConstExp parse() throws SymbolLack {
        String syntax = "ConstExp";
        List<SyntaxUnit> units = new LinkedList<>();
        AddExp addExp = AddExp.parse();
        units.add(addExp);
        return new ConstExp(syntax, units, addExp);
    }

    public void errorSolve() throws SymbolLack {
        addExp.errorSolve();
    }

    public int getValue() throws ComplexExp {
        return addExp.getValue();
    }

    public String icode() {
        return addExp.icode();
    }
}
