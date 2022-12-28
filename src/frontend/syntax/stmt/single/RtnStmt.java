package frontend.syntax.stmt.single;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.Exp;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class RtnStmt extends SyntaxUnit{
    // RtnStmt → 'return'  [Exp]
    private final Exp exp;

    public RtnStmt(String type, List<SyntaxUnit> units, Exp exp) {
        super(type, units);
        this.exp = exp;
    }

    public static RtnStmt parse() {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "RtnStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead());
        Exp exp = null;
        try {
            exp = Exp.parse();
            units.add(exp);
        } catch (SymbolLack ignored) {

        }
        return new RtnStmt(syntax,units,exp);
    }

    public void errorSolve() throws SymbolLack {
        if (exp != null) {
            exp.errorSolve();
        }
        if (ErrorSolver.getCurFuncRtnType() == Symbol.DataType.VOID) {
            if (exp != null) {
                ErrorList.getInstance().add(new Error(ErrorType.VDRT, units.get(0).getLine()));
            }
        }
    }

    public void icode() {
        String operand1 = "*NULL*";
        if (exp != null)
            operand1 = exp.icode();
        Intermediate.getInstance().addCode(MidCode.Operation.RETURN,operand1,"*NULL*","*NULL*");
    }

}
