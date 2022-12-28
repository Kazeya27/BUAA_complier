package frontend.syntax.exp;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class EqExp extends SyntaxUnit{
    private final EqExp eqExp;
    private final Token operation;
    private final RelExp relExp;

    public EqExp(String type, List<SyntaxUnit> units, EqExp eqExp, Token operation, RelExp relExp) {
        super(type, units);
        this.eqExp = eqExp;
        this.operation = operation;
        this.relExp = relExp;
    }

    // EqExp → RelExp { ('==' | '!=') RelExp }
    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    public static EqExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "EqExp";
        List<SyntaxUnit> units = new ArrayList<>();
        EqExp eqExp = null;
        Token operation = null;
        RelExp relExp = RelExp.parse();
        units.add(relExp);
        while (tokenLocator.getTokenType(0).equals(Type.EQL) ||
                tokenLocator.getTokenType(0).equals(Type.NEQ)) {
            eqExp = new EqExp(syntax,units,eqExp,operation,relExp);
            units = new ArrayList<>();
            units.add(eqExp);
            operation = tokenLocator.goAhead();
            units.add(operation);
            relExp = RelExp.parse();
            units.add(relExp);
        }
        return new EqExp(syntax,units, eqExp,operation, relExp);
    }

    public void errorSolve() {
        if (eqExp != null) {
            eqExp.errorSolve();
        }
        if (relExp != null) {
            relExp.errorSolve();
        }
    }

    // EqExp → RelExp { ('==' | '!=') RelExp }
    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    public String icode(String endLabel,boolean opposite) {
        String ans;
        if (eqExp != null && opposite) {
            String cond1 = eqExp.icode(endLabel,false); // 前面的符号都用来算值，最后一个符号判断分支
            String cond2 = relExp.icode();
            MidCode.Operation op = (operation.getType().equals(Type.EQL))? MidCode.Operation.BNE : MidCode.Operation.BEQ;
            ans = Intermediate.getInstance().addCode(op,cond1,cond2,endLabel);
        }
        else if (eqExp != null){
            String operand1 = eqExp.icode(endLabel,false); // 所有符号都用来算值
            String operand2 = relExp.icode();
            MidCode.Operation logic = (operation.getType().equals(Type.EQL))? MidCode.Operation.EQL : MidCode.Operation.NEQ;
            ans = Intermediate.getInstance().addCode(MidCode.Operation.SET,operand1,logic,operand2,"*TEMP*");
        }
        // eqExp == null,如果需要反转，说明只有一个表达式，需要变成 a == 0 跳转
        else if (opposite) {
            String cond1 = relExp.icode();
            ans = Intermediate.getInstance().addCode(MidCode.Operation.BEQ,cond1,"0",endLabel);
        }
        else {
            ans = relExp.icode();
        }
        return ans;
    }
}
