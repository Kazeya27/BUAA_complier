package frontend.syntax.stmt.single;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class BreakStmt extends SyntaxUnit{
    public BreakStmt(String type, List<SyntaxUnit> units) {
        super(type, units);
    }

    public static BreakStmt parse() {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "BreakStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead());
        return new BreakStmt(syntax,units);
    }

    public void errorSolve() throws SymbolLack {
        if (!ErrorSolver.isInLoop()) {
            ErrorList.getInstance().add(new Error(ErrorType.WRBC, units.get(0).getLine()));
        }
    }

    public void icode() {
        String loopEnd = Intermediate.getInstance().getLoopEnd();
        Intermediate.getInstance().addCode(MidCode.Operation.JUMP,loopEnd,"*NULL*","*NULL*");
    }

}
