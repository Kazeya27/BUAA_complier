package frontend.syntax.func;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.exceptions.ComplexExp;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.decl.ConstInitVal;
import frontend.syntax.exp.ConstExp;
import middle.Intermediate;
import middle.MidCode;

import java.util.ArrayList;
import java.util.List;

public class FuncFParam extends SyntaxUnit{
    private final Token ident;
    private final List<ConstExp> constExps;
    private final int dim;

    public FuncFParam(String type, List<SyntaxUnit> units, Token ident, List<ConstExp> constExps, int dim) {
        super(type, units);
        this.ident = ident;
        this.constExps = constExps;
        this.dim = dim;
    }

    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    public static FuncFParam parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "FuncFParam";
        List<SyntaxUnit> units = new ArrayList<>();
        Token ident;
        List<ConstExp> constExps = new ArrayList<>();
        int dim = 0;
        units.add(tokenLocator.goAhead());   // int
        ident = tokenLocator.goAhead();
        units.add(ident);   // Ident
        if (tokenLocator.getTokenType(0).equals(Type.LBRACK)) {
            units.add(tokenLocator.goAhead());   // [
            try {
                units.add(tokenLocator.goAhead(Type.RBRACK)); // ']'
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSBR, tokenLocator.getLastLine()));
            }
            dim++;
            while (tokenLocator.getTokenType(0).equals(Type.LBRACK)) {
                units.add(tokenLocator.goAhead());       // [
                ConstExp constExp = ConstExp.parse();
                units.add(constExp); // ConstExp
                constExps.add(constExp);
                try {
                    units.add(tokenLocator.goAhead(Type.RBRACK)); // ']'
                } catch (SymbolLack e) {
                    ErrorList.getInstance().add(new Error(ErrorType.MSBR, tokenLocator.getLastLine()));
                }
                dim++;
            }
        }
        return new FuncFParam(syntax, units, ident, constExps,dim);
    }

    public void errorSolve(SymbolTable symbolTable) {
        for (ConstExp exp:constExps) {
            try {
                exp.errorSolve();
            } catch (SymbolLack ignored) {
            }
        }
        int size1 = (dim > 0)?0:-1;
        int size2 = -1;
        if (dim > 1) {
            try {
                size2 = constExps.get(0).getValue();
            } catch (ComplexExp ignored) {
            }
        }
        Symbol symbol;
        if (size1 == -1) {
            symbol = new Symbol(ident.getString(),Symbol.Kind.PARA, Symbol.DataType.INT,ident.getLine());
        }
        else {
            symbol = new Symbol(ident.getString(),size1,size2, Symbol.Kind.PARA, Symbol.DataType.ARRAY,ident.getLine());
        }
        if (symbolTable.hasSym(ident.getString(),false)) {
            ErrorList.getInstance().add(new Error(ErrorType.NRDF,ident.getLine()));
        }
        else {
            symbolTable.addSym(symbol);
        }
    }

    public void icode() {
        Intermediate.getInstance().addCode(MidCode.Operation.PARAF, Intermediate.getMidName(ident.getString(),ident.getLine()), String.valueOf(dim),"*NULL*");
    }

}
