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

public class AddExp extends SyntaxUnit{
    private final AddExp addExp;
    private final Token operation;
    private final MulExp mulExp;

    public AddExp(String type, List<SyntaxUnit> units, AddExp addExp, Token operation, MulExp mulExp) {
        super(type, units);
        this.addExp = addExp;
        this.operation = operation;
        this.mulExp = mulExp;
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    // AddExp → MulExp { ('+' | '-') MulExp }
    public static AddExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "AddExp";
        List<SyntaxUnit> units = new ArrayList<>();
        AddExp addExp = null;
        Token operation = null;
        MulExp mulExp = MulExp.parse();
        units.add(mulExp);
        while (tokenLocator.getTokenType(0).equals(Type.PLUS) ||
                tokenLocator.getTokenType(0).equals(Type.MINU)) {
            addExp = new AddExp(syntax, units, addExp,operation,mulExp);
            units = new ArrayList<>();
            units.add(addExp);
            operation = tokenLocator.goAhead();
            units.add(operation);
            mulExp = MulExp.parse();
            units.add(mulExp);
        }
        return new AddExp(syntax, units, addExp,operation,mulExp);
    }

    public int getValue() throws ComplexExp {
        int val = mulExp.getValue();
        if (operation != null) {
            if (operation.getType() == Type.PLUS) {
                val = val + addExp.getValue();
            }
            else {
                val = addExp.getValue() - val;
            }
        }
        return val;
    }

    public boolean errorSolve() {
        boolean flag = true;
        if (addExp != null)
            flag = addExp.errorSolve();
        if (mulExp != null)
            flag = mulExp.errorSolve();
        return flag;
    }

    public int getDim() {
        // 不涉及数组指针运算
        if (operation != null) {
            return 0;
        }
        return mulExp.getDim();
    }

    public String icode() {
        try {
            return Integer.toString(getValue());
        } catch (ComplexExp ignored) {
        }

        String baseExp;
        if (operation != null) {
            MidCode.Operation op = (operation.getType() == Type.PLUS)? MidCode.Operation.ADD: MidCode.Operation.SUB;
            String operand1 = addExp.icode();
            String operand2 = mulExp.icode();
            if (operand2.contains("[")) {
                operand2 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand2,"*OPERAND1*");
            }
            if (operand1.contains("[")) {
                operand1 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand1,"*OPERAND1*");
            }
            baseExp = Intermediate.getInstance().addCode(op,operand1,operand2,"*TEMP*");
        }
        else {
            String operand2 = mulExp.icode();
            baseExp = operand2;
        }
        return baseExp;
    }

}
