package frontend.syntax.stmt.single;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
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

public class PFStmt extends SyntaxUnit{
    private final int perCnt;
    private final int expCnt;
    private final List<Exp> exps;
    private final Token str;

    public PFStmt(String type, List<SyntaxUnit> units, int perCnt, int expCnt, List<Exp> exps, Token str) {
        super(type, units);
        this.perCnt = perCnt;
        this.expCnt = expCnt;
        this.exps = exps;
        this.str = str;
    }

    public static PFStmt parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "PFStmt";
        List<SyntaxUnit> units = new ArrayList<>();
        List<Exp> exps = new ArrayList<>();
        units.add(tokenLocator.goAhead()); // printf
        units.add(tokenLocator.goAhead()); // (
        Token stringFormat = tokenLocator.goAhead();
        units.add(stringFormat); // StringFormat
        String str = stringFormat.getString();
        int perCnt = 0;
        int expCnt = 0;
        if (str.contains("%d")) {
            perCnt = (str.length()-str.replaceAll("%d","").length())/2;
        }
        while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
            expCnt++;
            units.add(tokenLocator.goAhead()); // ,
            Exp exp = Exp.parse();
            units.add(exp);
            exps.add(exp);
        }
        try {
            units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
        } catch (SymbolLack e) {
            ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
        }
        return new PFStmt(syntax,units,perCnt,expCnt,exps,stringFormat);
    }

    public void errorSolve() throws SymbolLack {
        if (perCnt != expCnt) {
            // System.out.println("print " + perCnt + " " + expCnt);
            ErrorList.getInstance().add(new Error(ErrorType.PFNE, units.get(0).getLine()));
        }
        strSolve();
        for (Exp exp:exps) {
            exp.errorSolve();
        }
    }

    private void strSolve() {
        String s = str.getString().substring(1,str.getString().length()-1);
        // System.out.println(s);
        for (int i = 0;i<s.length();i++) {
            char c = s.charAt(i);
            if (c == '%' ) {
                if (i == s.length()-1 || s.charAt(i+1) != 'd') {
                    ErrorList.getInstance().add(new Error(ErrorType.ILGT,str.getLine()));
                    return;
                }
            }
            else if (!(c == 32 || c == 33 || (40 <= c && c <= 126))) {
                ErrorList.getInstance().add(new Error(ErrorType.ILGT,str.getLine()));
                return;
            }
            else if (c == 92) {
                if (i == s.length()-1 || s.charAt(i+1) != 'n') {
                    ErrorList.getInstance().add(new Error(ErrorType.ILGT,str.getLine()));
                    return;
                }
            }
        }
    }

    public void icode() {
        // str.split("(?<=。)")  在每项最后保留分隔符
        // str.split("(?>=。)")  在每项开头保留分隔符
        // str.split("(?<=。)|(?=。)")  分隔符单独做一项
        String[] subStr = str.getString().substring(1,str.getString().length()-1).split("(?<=%d)|(?=%d)");
        int index = expCnt-1;
        String operand1;
        ArrayList<String> opds = new ArrayList<>();
        for (int i = subStr.length-1;i>=0;i--) {
            String s = subStr[i];
            if (s.equals("%d")) {
                operand1 = exps.get(index--).icode();
                if (operand1.contains("[")) {
                    operand1 = Intermediate.getInstance().addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand1,"*OPERAND1*");
                }
                opds.add(operand1);
            }
            else {
                operand1 = s;
                opds.add(operand1);
            }
        }
        index = opds.size()-1;
        for (int i = 0;i<subStr.length;i++) {
            if (subStr[i].equals("%d")) {
                Intermediate.getInstance().addCode(MidCode.Operation.PRINT_INT,opds.get(index--),"*NULL*","*NULL*");
            }
            else {
                Intermediate.getInstance().addCode(MidCode.Operation.PRINT_STR,opds.get(index--),"*NULL*","*NULL*");
            }
        }
    }

}