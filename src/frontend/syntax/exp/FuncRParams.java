package frontend.syntax.exp;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.error.SymbolTable;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class FuncRParams extends SyntaxUnit{
    private final List<Exp> exps;

    public FuncRParams(String type, List<SyntaxUnit> units, List<Exp> exps) {
        super(type, units);
        this.exps = exps;
    }

    // FuncRParams → Exp { ',' Exp }
    public static FuncRParams parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "FuncRParams";
        List<SyntaxUnit> units = new ArrayList<>();
        List<Exp> exps = new ArrayList<>();
        Exp exp = Exp.parse();
        units.add(exp);
        exps.add(exp);
        while (tokenLocator.getTokenType(0).equals(Type.COMMA)) {
            units.add(tokenLocator.goAhead()); // ,
            exp = Exp.parse();
            units.add(exp);
            exps.add(exp);
        }
        return new FuncRParams(syntax, units, exps);
    }

    public boolean errorSolve(SymbolTable symbolTable, int line) {
        if (exps.size() != symbolTable.getSymbolMap().size()) {
            ErrorList.getInstance().add(new Error(ErrorType.FACM,line));
            return false;
        }
        List<String> paraNames = symbolTable.getSymbolName();
        for (int i = 0;i<exps.size();i++) {
            Exp exp = exps.get(i);
            if (!exp.errorSolve()) {
                return false;
            }
            int rDim = exp.getDim();
            if (rDim == -2)
                continue;
            int fDim = symbolTable.getSym(paraNames.get(i)).getDim();
            if (rDim != fDim) {
                ErrorList.getInstance().add(new Error(ErrorType.FATM,line));
                return false;
            }
        }
        return true;
    }

    public void icode() {
        int i = 1;
        for (Exp exp:exps) {
            String operand1 = exp.icode();
            if (operand1.contains("&"))
                Intermediate.getInstance().addCode(MidCode.Operation.PUSH_PTR,operand1,String.valueOf(i++),"*NULL*");
            else
                Intermediate.getInstance().addCode(MidCode.Operation.PUSH_VAL,operand1,String.valueOf(i++),"*NULL*");
        }
    }

}
