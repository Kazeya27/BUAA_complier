package frontend.syntax.exp;

import frontend.exceptions.SymbolLack;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.stmt.Stmt;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class Cond extends SyntaxUnit{
    private final LOrExp lOrExp;

    public Cond(String type, List<SyntaxUnit> units, LOrExp lOrExp) {
        super(type, units);
        this.lOrExp = lOrExp;
    }

    // Cond → LOrExp
    public static Cond parse() throws SymbolLack {
        String syntax = "Cond";
        List<SyntaxUnit> units = new ArrayList<>();
        LOrExp lOrExp = LOrExp.parse();
        units.add(lOrExp);
        return new Cond(syntax,units,lOrExp);
    }

    public void errorSolve() {
        lOrExp.errorSolve();
    }

    public void icode(String endLabel,String elseLabel,Stmt stmt) {
        int depth = lOrExp.getDepth();
        lOrExp.icode(endLabel,elseLabel,stmt,depth,depth);
    }

}
