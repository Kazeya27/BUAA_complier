package frontend.syntax;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;

import java.util.List;

public class TokenLocator {
    private final List<Token> tokens;
    private int loc;
    private static TokenLocator instance;

    private TokenLocator(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Type getTokenType(int index) {
        return tokens.get(loc + index).getType();
    }

    public Token goAhead() {
        return tokens.get(loc++);
    }

    public int getLastLine() {
        return tokens.get(loc-1).getLine();
    }

    // 解决错误i,j,k
    public Token goAhead(Type type) throws SymbolLack {
        Token token = tokens.get(loc);
        if (token.getType().equals(type)) {
            loc++;
            return token;
        }
        else {
            // System.err.println(token.getLine() + " " + token.getType());
            throw new SymbolLack();
        }
    }

    public static TokenLocator getInstance(List<Token> tokens) {
        if (instance == null) {
            instance = new TokenLocator(tokens);
        }
        return instance;
    }

    public static TokenLocator getInstance() {
        return instance;
    }

}
