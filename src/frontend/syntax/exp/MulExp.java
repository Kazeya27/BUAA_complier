package frontend.syntax.exp;

import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class MulExp extends SyntaxUnit{
    private final MulExp mulExp;
    private final Token operation;
    private final UnaryExp unaryExp;

    public MulExp(String type, List<SyntaxUnit> units, MulExp mulExp, Token operation, UnaryExp unaryExp) {
        super(type, units);
        this.mulExp = mulExp;
        this.operation = operation;
        this.unaryExp = unaryExp;
    }

    //MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp }
    //MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
    public static MulExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "MulExp";
        List<SyntaxUnit> units = new ArrayList<>();
        MulExp mulExp = null;
        Token operation = null;
        UnaryExp unaryExp = UnaryExp.parse();
        units.add(unaryExp);
        while (tokenLocator.getTokenType(0).equals(Type.MULT) ||
                tokenLocator.getTokenType(0).equals(Type.DIV) ||
                tokenLocator.getTokenType(0).equals(Type.MOD)) {
            mulExp = new MulExp(syntax,units,mulExp,operation,unaryExp);
            units = new ArrayList<>();
            units.add(mulExp);
            operation = tokenLocator.goAhead();
            units.add(operation);
            unaryExp = UnaryExp.parse();
            units.add(unaryExp);
        }
        return new MulExp(syntax, units, mulExp, operation, unaryExp);
    }

    public int getValue() throws ComplexExp {
        int val = unaryExp.getValue();
        if (operation != null) {
            if (operation.getType() == Type.MULT) {
                val = val * mulExp.getValue();
            }
            else if (operation.getType() == Type.DIV) {
                val = mulExp.getValue() / val;
            }
            else {
                val = mulExp.getValue() % val;
            }
        }
        return val;
    }

    public boolean errorSolve() {
        boolean flag = true;
        if (unaryExp != null)
            flag = unaryExp.errorSolve();
        if (mulExp != null)
            flag = mulExp.errorSolve();
        return flag;
    }

    public int getDim() {
        if (operation != null) {
            return 0;
        }
        return unaryExp.getDim();
    }

    public String icode() {
        try {
            return Integer.toString(getValue());
        } catch (Exception ignored) {
        }

        String baseExp;
        if (operation != null) {
            MidCode.Operation op = null;
            switch (operation.getType()) {
                case MULT:
                    op = MidCode.Operation.MUL;
                    break;
                case DIV:
                    op = MidCode.Operation.DIV;
                    break;
                case MOD:
                    op = MidCode.Operation.MOD;
                    break;
                default:
                    System.err.println("something ERROR");
                    break;
            }
            String operand1 = mulExp.icode();
            String operand2 = unaryExp.icode();
            if (operand2.contains("[")) {
                operand2 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand2,"*OPERAND1*");
            }
            if (operand1.contains("[")) {
                operand1 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand1,"*OPERAND1*");
            }
            baseExp = Intermediate.getInstance().addCode(op,operand1,operand2,"*TEMP*");
        }
        else {
            String operand2 = unaryExp.icode();
            baseExp = operand2;
        }
        return baseExp;
    }

}
