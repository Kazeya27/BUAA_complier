package frontend.lexical;

import java.util.regex.Pattern;

public enum Type {
    INTCON("[0-9]+"),
    STRCON("\\\"[^\\\"]*\\\""),
    MAINTK("main(?![_A-Za-z0-9])"),
    CONSTTK("const(?![_A-Za-z0-9])"),
    INTTK("int(?![_A-Za-z0-9])"),
    BREAKTK("break(?![_A-Za-z0-9])"),
    CONTINUETK("continue(?![_A-Za-z0-9])"),
    IFTK("if(?![_A-Za-z0-9])"),
    ELSETK("else(?![_A-Za-z0-9])"),
    WHILETK("while(?![_A-Za-z0-9])"),
    GETINTTK("getint(?![_A-Za-z0-9])"),
    PRINTFTK("printf(?![_A-Za-z0-9])"),
    RETURNTK("return(?![_A-Za-z0-9])"),
    VOIDTK("void(?![_A-Za-z0-9])"),
    IDENFR("[_A-Za-z][_A-Za-z0-9]*"),
    PLUS("\\+"),
    MINU("-"),
    MULT("\\*"),
    SINC("//.*"),
    AND("&&"),
    OR("\\|\\|"),
    DIV("/"),
    MOD("%"),
    LEQ("<="),
    LSS("<(?![=])"),
    GEQ(">="),
    GRE(">(?![=])"),
    EQL("=="),
    NEQ("!="),
    NOT("!(?![=])"),
    ASSIGN("=(?![=])"),
    SEMICN(";"),
    COMMA(","),
    LPARENT("\\("),
    RPARENT("\\)"),
    LBRACK("\\["),
    RBRACK("\\]"),
    LBRACE("\\{"),
    RBRACE("\\}");

    private final Pattern pattern;

    Type(String pattern) {
        this.pattern = Pattern.compile("^" + pattern);
    }

    public Pattern getPattern() {
        return pattern;
    }
}