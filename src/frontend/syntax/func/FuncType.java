package frontend.syntax.func;

import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class FuncType extends SyntaxUnit{
    private final Token type;

    public FuncType(String type, List<SyntaxUnit> units, Token type1) {
        super(type, units);
        this.type = type1;
    }

    public static FuncType parse() {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "FuncType";
        List<SyntaxUnit> units = new ArrayList<>();
        Token type = tokenLocator.goAhead();
        units.add(type);
        return new FuncType(syntax, units, type);
    }

    public Type getType() {
        return type.getType();
    }
}
