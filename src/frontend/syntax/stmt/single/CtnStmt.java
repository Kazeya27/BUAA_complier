package frontend.syntax.stmt.single;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class CtnStmt extends SyntaxUnit{
    public CtnStmt(String type, List<SyntaxUnit> units) {
        super(type, units);
    }

    public static CtnStmt parse() {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "CtnStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead());
        return new CtnStmt(syntax,units);
    }

    public void errorSolve() throws SymbolLack {
        if (!ErrorSolver.isInLoop()) {
            ErrorList.getInstance().add(new Error(ErrorType.WRBC, units.get(0).getLine()));
        }
    }

    public void icode() {
        String loopBegin = Intermediate.getInstance().getLoopBegin();
        Intermediate.getInstance().addCode(MidCode.Operation.JUMP,loopBegin,"*NULL*","*NULL*");
    }

}
