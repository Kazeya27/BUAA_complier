package frontend.syntax.stmt;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.error.ErrorType;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.SyntaxUnit;
import frontend.syntax.TokenLocator;
import frontend.syntax.stmt.multiple.MulParser;
import frontend.syntax.stmt.single.SingleParser;

import java.util.ArrayList;
import java.util.List;

public class Stmt extends SyntaxUnit {
    private final SingleParser singleParser;
    private final MulParser mulParser;

    public Stmt(String type, List<SyntaxUnit> units, SingleParser singleParser, MulParser mulParser) {
        super(type, units);
        this.singleParser = singleParser;
        this.mulParser = mulParser;
    }

    // Stmt → ';' | Single ';' | MulStmt
    public static Stmt parse() throws SymbolLack {
        TokenLocator tokenLocator = TokenLocator.getInstance();
        String syntax = "Stmt";
        List<SyntaxUnit> units = new ArrayList<>();
        SingleParser singleParser = null;
        MulParser mulParser = null;
        if (tokenLocator.getTokenType(0).equals(Type.SEMICN))
            units.add(tokenLocator.goAhead()); //;
        else if (tokenLocator.getTokenType(0).equals(Type.IFTK) ||
                tokenLocator.getTokenType(0).equals(Type.WHILETK) ||
                tokenLocator.getTokenType(0).equals(Type.LBRACE)) {
            mulParser = MulParser.parse();
            units.add(mulParser);
        }
        else {
            singleParser = SingleParser.parse();
            units.add(singleParser);
            try {
                units.add(tokenLocator.goAhead(Type.SEMICN)); //;
            } catch (SymbolLack e) {
                ErrorList.getInstance().add(new Error(ErrorType.MSSM, tokenLocator.getLastLine()));
            }
        }
        return new Stmt(syntax,units,singleParser,mulParser);
    }

    public boolean isReturn() {
        if (singleParser == null)
            return false;
        return singleParser.getSingleType() == SingleParser.SingleType.RETURN;
    }

    public void errorSolve() throws SymbolLack {
        if (singleParser != null) {
            singleParser.errorSolve();
        }
        else if (mulParser != null){
            mulParser.errorSolve();
        }
    }

    public void icode() {
        if (singleParser != null) {
            singleParser.icode();
        }
        else if (mulParser != null){
            mulParser.icode();
        }
    }

}
