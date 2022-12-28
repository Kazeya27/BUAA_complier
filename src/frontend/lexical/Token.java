package frontend.lexical;

import frontend.syntax.SyntaxUnit;

import java.util.Objects;

public class Token extends SyntaxUnit {
    Type type;
    private final String string;
    private final int line;

    public Token(Type type, String string, int line) {
        super(type.name(), null);
        this.string = string;
        this.type = type;
        this.line = line;
    }

    public Type getType() {
        return type;
    }

    public String getString() {
        return string;
    }

    public int getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return line == token.line && type == token.type && string.equals(token.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, string, line);
    }

    @Override
    public String toString() {
        return type.toString() + " " + string + " " + line;
    }
}
