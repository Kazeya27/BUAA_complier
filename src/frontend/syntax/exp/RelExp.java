package frontend.syntax.exp;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RelExp extends SyntaxUnit{
    private final RelExp relExp;
    private final Token operation;
    private final AddExp addExp;

    private final HashMap<Type, MidCode.Operation> trans = new HashMap<Type,MidCode.Operation>() {
        {
            put(Type.LSS, MidCode.Operation.LSS);
            put(Type.LEQ, MidCode.Operation.LEQ);
            put(Type.GEQ, MidCode.Operation.GEQ);
            put(Type.GRE, MidCode.Operation.GRE);
        }
    };

    public RelExp(String type, List<SyntaxUnit> units, RelExp relExp, Token operation, AddExp addExp) {
        super(type, units);
        this.relExp = relExp;
        this.operation = operation;
        this.addExp = addExp;
    }

    // RelExp → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    public static RelExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "RelExp";
        List<SyntaxUnit> units = new ArrayList<>();
        RelExp relExp = null;
        Token operation = null;
        AddExp addExp = AddExp.parse();
        units.add(addExp);
        while (tokenLocator.getTokenType(0).equals(Type.GRE) ||
                tokenLocator.getTokenType(0).equals(Type.GEQ) ||
                tokenLocator.getTokenType(0).equals(Type.LSS) ||
                tokenLocator.getTokenType(0).equals(Type.LEQ)) {
            relExp = new RelExp(syntax,units,relExp,operation,addExp);
            units = new ArrayList<>();
            units.add(relExp);
            operation = tokenLocator.goAhead();
            units.add(operation);
            addExp = AddExp.parse();
            units.add(addExp);
        }
        return new RelExp(syntax,units,relExp,operation,addExp);
    }

    public void errorSolve() {
        if (relExp != null) {
            relExp.errorSolve();
        }
        if (addExp != null) {
            addExp.errorSolve();
        }
    }

    // RelExp → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // 只有AddExp，增加 a != 0
    public String icode() {
        String ans;
        if (relExp != null){
            String operand1 = relExp.icode();
            String operand2 = addExp.icode();
            if (operand2.contains("[")) {
                operand2 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand2,"*OPERAND1*");
            }
            MidCode.Operation logic = trans.get(operation.getType());
            ans = Intermediate.getInstance().addCode(MidCode.Operation.SET,operand1,logic,operand2,"*TEMP*");
        }
        else {
            ans = addExp.icode();
            if (ans.contains("[")) {
                ans = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",ans,"*OPERAND1*");
            }
        }
        return ans;
    }

}
