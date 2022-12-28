package frontend.syntax.exp;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.stmt.Stmt;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class LOrExp extends SyntaxUnit{
    private final LOrExp lOrExp;
    private final Token operation;
    private final LAndExp lAndExp;

    public LOrExp(String type, List<SyntaxUnit> units, LOrExp lOrExp, Token operation, LAndExp lAndExp) {
        super(type, units);
        this.lOrExp = lOrExp;
        this.operation = operation;
        this.lAndExp = lAndExp;
    }

    // LOrExp → LAndExp { '||' LAndExp }
    // LOrExp → LAndExp | LOrExp '||' LAndExp
    public static LOrExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "LOrExp";
        List<SyntaxUnit> units = new ArrayList<>();
        LOrExp lOrExp = null;
        Token operation = null;
        LAndExp lAndExp = LAndExp.parse();
        units.add(lAndExp);
        while (tokenLocator.getTokenType(0).equals(Type.OR)) {
            lOrExp = new LOrExp(syntax,units,lOrExp,operation,lAndExp);
            units = new ArrayList<>();
            units.add(lOrExp);
            operation = tokenLocator.goAhead();
            units.add(operation);  // ||
            lAndExp = LAndExp.parse();
            units.add(lAndExp);
        }
        return new LOrExp(syntax,units,lOrExp,operation, lAndExp);
    }

    public void errorSolve() {
        if (lOrExp != null) {
            lOrExp.errorSolve();
        }
        if (lAndExp != null) {
            lAndExp.errorSolve();
        }
    }

    // LOrExp → LAndExp { '||' LAndExp }
    // LOrExp → LAndExp | LOrExp '||' LAndExp
    public void icode(String endLabel, String nextLabel, Stmt stmt,int depth, int curDepth) {
        String nextOr = nextLabel;
        if (lOrExp != null) {
            lOrExp.icode(endLabel,nextLabel,stmt,depth,curDepth-1);
        }
        if (curDepth == depth) {
            nextOr = nextLabel;
        }
        else {
            nextOr = Intermediate.getInstance().getOrLabel();
        }
        lAndExp.icode(nextOr);
        stmt.icode();
        Intermediate.getInstance().addCode(MidCode.Operation.JUMP,endLabel,"*NULL*","*NULL*");
        if (curDepth != depth)
            Intermediate.getInstance().addCode(MidCode.Operation.LABEL,nextOr,"*NULL*","*NULL*");
    }

    public int getDepth() {
        int ans = 1;
        if (lOrExp != null) {
            ans += lOrExp.getDepth();
        }
        return ans;
    }

}
