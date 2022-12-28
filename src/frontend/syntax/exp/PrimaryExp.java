package frontend.syntax.exp;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorSolver;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import middle.Intermediate;

import java.util.ArrayList;
import java.util.List;

public class PrimaryExp extends SyntaxUnit{
    private final Exp exp;
    private final LVal lVal;
    private final Number number;
    private String value = null;

    public PrimaryExp(String type, List<SyntaxUnit> units, Exp exp, LVal lVal, Number number) {
        super(type, units);
        this.exp = exp;
        this.lVal = lVal;
        this.number = number;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number
    public static PrimaryExp parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "PrimaryExp";
        List<SyntaxUnit> units = new ArrayList<>();
        Exp exp = null;
        LVal lVal = null;
        Number number = null;
        if (tokenLocator.getTokenType(0).equals(Type.LPARENT)) {
            units.add(tokenLocator.goAhead());          // (
            exp = Exp.parse();
            units.add(exp);   // Exp
            try {
                units.add(tokenLocator.goAhead(Type.RPARENT)); // ')'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSPR, tokenLocator.getLastLine()));
            }
        }
        else if (tokenLocator.getTokenType(0).equals(Type.IDENFR)) {
            lVal = LVal.parse();
            units.add(lVal);
        }
        else {
            number = Number.parse();
            units.add(number);
        }
        return new PrimaryExp(syntax, units,exp,lVal,number);
    }

    public int getValue() throws ComplexExp {
        if (value != null) {
            return Integer.parseInt(value);
        }
        if (exp != null)
            return exp.getValue();
        if (lVal != null) {
            if (ErrorSolver.isGlobal) {
                return lVal.getValue();
            }
            else {
                throw new ComplexExp();
            }
        }
        return number.getValue();
    }

    public boolean errorSolve() {
        if (exp != null) {
            return exp.errorSolve();
        }
        else if (lVal != null) {
            Token ident = lVal.getIdent();
            Symbol symbol = ErrorSolver.getSym(ident.getString());
            if (symbol.isConst()) {
                String value = "";
                List<Exp> exps = lVal.getExps();
                if (exps.size() == 0) {
                    value = String.valueOf(symbol.getValue());
                }
                else if (exps.size() == 1) {
                    try {
                        int index = exps.get(0).getValue();
                        value = String.valueOf(symbol.getValue(index,0));
                    } catch (ComplexExp ignored) {
                    }
                }
                else if (exps.size() == 2) {
                    try {
                        int index1 = exps.get(0).getValue();
                        int index2 = exps.get(1).getValue();
                        value = String.valueOf(symbol.getValue(index1,index2));
                    } catch (ComplexExp ignored) {
                    }
                }
                if (!value.equals("")) {
                    this.value = value;
                }
            }
            return lVal.errorSolve();
        }
        return true;
    }

    public int getDim() {
        if (exp != null) {
            return exp.getDim();
        }
        else if (lVal != null) {
            return lVal.getDim();
        }
        // number
        return 0;
    }

    public String icode() {
        try {
            return Integer.toString(getValue());
        } catch (ComplexExp ignored) {
        }
        if (exp != null) {
            return exp.icode();
        }
        else if (lVal != null) {
            return lVal.icode();
        }
        else {
            return Integer.toString(number.getValue());
        }
    }

}
