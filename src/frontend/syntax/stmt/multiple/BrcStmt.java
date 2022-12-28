package frontend.syntax.stmt.multiple;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.exp.Cond;
import frontend.syntax.stmt.Stmt;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class BrcStmt extends SyntaxUnit{
    private final Cond cond;
    private final List<Stmt> stmts;

    public BrcStmt(String type, List<SyntaxUnit> units, Cond cond, List<Stmt> stmts) {
        super(type, units);
        this.cond = cond;
        this.stmts = stmts;
    }

    // BrcStmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public static BrcStmt parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "BrcStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        units.add(tokenLocator.goAhead());         // if
        units.add(tokenLocator.goAhead());         // (
        Cond cond = Cond.parse();
        units.add(cond); // Cond
        try {
                units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
            }
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt = Stmt.parse();
        stmts.add(stmt);
        units.add(stmt); // Stmt
        while (tokenLocator.getTokenType(0).equals(Type.ELSETK)) {
            units.add(tokenLocator.goAhead());         // else
            stmt = Stmt.parse();
            stmts.add(stmt);
            units.add(stmt); // Stmt
        }
        return new BrcStmt(syntax,units, cond, stmts);
    }

    public void errorSolve() throws SymbolLack {
        for (Stmt stmt:stmts) {
            stmt.errorSolve();
        }
    }

    /*
        else_1:
            orCond1:
                andCond1_1:
                    branch a > b orCond2  # >= > == != <= < 如果直接用于跳转要反向
                andCond1_2:
                    branch 2 > 1 orCond2
            j body_1
            orCond2:
                andCond2_1:
                    branch a == b else_2
                andCond2_2:
                    branch a >= b == c >= d == 1 else_2  # 最后符号才需要反向
            j body_1
            body_1:
                xxxx
                j if_end_1
        else_2:
            orCond3:
                xxxxx if_end_1
            j body_2    # 删除，减少跳转次数
            body_2:
                xxxx
                j if_end_1  # 删除，减少跳转次数
        if_end_1:
     */

    public void icode() {
        // Intermediate.getInstance().addCode(MidCode.Operation.IF_BEGIN);

        if (stmts.size() == 1) {
            String ifEnd = Intermediate.getInstance().getIfEndLabel();
            cond.icode(ifEnd,ifEnd,stmts.get(0));
            Intermediate.getInstance().addCode(MidCode.Operation.LABEL,ifEnd,"*NULL*","*NULL*");
        }
        else {
            String elseLabel = Intermediate.getInstance().getElseLabel();
            String ifEnd = Intermediate.getInstance().getElseLabel();
            cond.icode(ifEnd,elseLabel,stmts.get(0));
            Intermediate.getInstance().addCode(MidCode.Operation.LABEL,elseLabel,"*NULL*","*NULL*");
            stmts.get(1).icode();
            Intermediate.getInstance().addCode(MidCode.Operation.LABEL,ifEnd,"*NULL*","*NULL*");
        }
    }

}
