package frontend.syntax.stmt.multiple;

import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.Cond;
import frontend.syntax.stmt.Stmt;
import frontend.error.Error;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class LoopStmt extends SyntaxUnit{
    private final Cond cond;
    private final Stmt stmt;

    public LoopStmt(String type, List<SyntaxUnit> units, Cond cond, Stmt stmt) {
        super(type, units);
        this.cond = cond;
        this.stmt = stmt;
    }

    // LoopStmt → 'while' '(' Cond ')' Stmt
    public static LoopStmt parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "LoopStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead()); // while
        units.add(tokenLocator.goAhead()); // (
        Cond cond = Cond.parse();
        units.add(cond);
        try {
                units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
        } catch (SymbolLack e) {
            ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
        }
        Stmt stmt = Stmt.parse();
        units.add(stmt);
        return new LoopStmt(syntax,units,cond,stmt);
    }

    public void errorSolve() throws SymbolLack {
        ErrorSolver.enterLoop();
        cond.errorSolve();
        stmt.errorSolve();
        ErrorSolver.outLoop();
    }

    /*
        while_begin_1:
            orCond1:
                andCond1_1:
                    branch a > b orCond2  # >= > == != <= < 如果直接用于跳转要反向
                andCond1_2:
                    branch 2 > 1 orCond2
                while_body
                j while_begin_1
            orCond2:
                andCond2_1:
                    branch a == b while_end_1
                andCond2_2:
                    branch a >= b == c >= d == 1 while_end_1  # 最后符号才需要反向
                while_body
                j while_begin_1
        while_end_1:
     */

    public void icode() {
        String beginLabel = Intermediate.getInstance().addCode(MidCode.Operation.LOOP_BEGIN);
        String endLabel = Intermediate.getInstance().getLoopEndLabel();
        Intermediate.getInstance().addLoop(beginLabel,endLabel);
        cond.icode(beginLabel,endLabel,stmt);
        Intermediate.getInstance().addCode(MidCode.Operation.LABEL,endLabel,"*NULL*","*NULL*");
        Intermediate.getInstance().deleteLoop();
    }

}
