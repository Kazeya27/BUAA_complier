package frontend.syntax.exp;

import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;

import java.util.ArrayList;
import java.util.List;

public class UnaryOp extends SyntaxUnit{
    public UnaryOp(String type, List<SyntaxUnit> units) {
        super(type, units);
    }

    // UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中
    public static UnaryOp parse() {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "UnaryOp";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead()); // op
        return new UnaryOp(syntax,units);
    }

    public Type getType() {
        return ((Token) units.get(0)).getType();
    }
}
