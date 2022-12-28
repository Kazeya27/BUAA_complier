package frontend.syntax;

import frontend.exceptions.SymbolLack;
import frontend.lexical.Token;
import frontend.lexical.Type;
import frontend.syntax.decl.ConstDecl;
import frontend.syntax.decl.ConstDef;
import frontend.syntax.decl.DeclParser;
import frontend.syntax.decl.VarDecl;
import frontend.syntax.func.FuncParser;
import frontend.syntax.func.MainFuncDef;

import java.util.ArrayList;
import java.util.List;

public class SyntaxParser {
    private final TokenLocator tokenLocator;

    public SyntaxParser(List<Token> tokens) {
        this.tokenLocator = TokenLocator.getInstance(tokens);
    }

    public CompUnit compParse() throws SymbolLack {
        // CompUnit → { Decl } { FuncDef } MainFuncDef
        //            const/int  void/int    int
        //            int/Ident   Ident      main
        //            Ident/[/,     (          (
        String syntax = "CompUnit";
        List<SyntaxUnit> units = new ArrayList<>();
        List<DeclParser> declParsers = new ArrayList<>();
        List<FuncParser> funcParsers = new ArrayList<>();
        MainFuncDef mainFuncDef;
        while (tokenLocator.getTokenType(0).equals(Type.CONSTTK) ||
                !tokenLocator.getTokenType(2).equals(Type.LPARENT)) {
            DeclParser declParser = DeclParser.parse();
            units.add(declParser);
            declParsers.add(declParser);
        }
        while (tokenLocator.getTokenType(1).equals(Type.IDENFR)) {
            FuncParser funcParser = FuncParser.parse();
            units.add(funcParser);
            funcParsers.add(funcParser);
        }
        mainFuncDef = MainFuncDef.parse();
        units.add(mainFuncDef);
        return new CompUnit(syntax,units,declParsers,funcParsers,mainFuncDef);
    }
}
