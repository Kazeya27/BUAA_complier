package frontend.syntax.exp;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class Number extends SyntaxUnit{
    private final Token intConst;

    public Number(String type, List<SyntaxUnit> units, Token intConst) {
        super(type, units);
        this.intConst = intConst;
    }

    public static Number parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "Number";
        List<SyntaxUnit> units = new ArrayList<>();
        Token intConst = tokenLocator.goAhead(Type.INTCON);
        units.add(intConst); // Number
        return new Number(syntax,units,intConst);
    }

    public int getValue() {
        return Integer.parseInt(intConst.getString());
    }
}
