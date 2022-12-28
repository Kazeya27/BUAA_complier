import backend.Translator;
import frontend.error.ErrorList;
import frontend.exceptions.SymbolLack;
import frontend.lexical.Tokenizer;
import frontend.syntax.CompUnit;
import frontend.syntax.SyntaxParser;
import frontend.syntax.SyntaxUnit;
import middle.FlowGraph;
import middle.Intermediate;

import java.io.IOException;

public class Compiler {
    public static boolean opt = true;

    //d e g
    public static void main(String[] args) throws IOException, SymbolLack {
        if (args.length > 0) {
            Tokenizer tokenizer = new Tokenizer(args[1]);
            tokenizer.tokenize();
            // tokenizer.output(args[3]);
            SyntaxUnit.setFileWriter(args[3]);
            SyntaxParser syntaxParser = new SyntaxParser(tokenizer.getTokenList());
            CompUnit unit = syntaxParser.compParse();
            unit.output(false);
            SyntaxUnit.getFileWriter().flush();
            unit.errorSolve();
            ErrorList.getInstance().sort();
            ErrorList.getInstance().output(args[3],true);
        }
        else {
            Tokenizer tokenizer = new Tokenizer("testfile.txt");
            tokenizer.tokenize();
            // tokenizer.output("output.txt",true);
            // SyntaxUnit.setFileWriter("output.txt");
            SyntaxParser syntaxParser = new SyntaxParser(tokenizer.getTokenList());
            CompUnit unit = syntaxParser.compParse();
            // unit.output(false);
            // SyntaxUnit.getFileWriter().flush();
            unit.errorSolve();
            ErrorList.getInstance().sort();
            ErrorList.getInstance().output("error.txt",true);
            Intermediate.getInstance();
            unit.icode();
            Intermediate.getInstance().output("testfile1_19374223_王永瑶_优化前中间代码.txt");

            if (opt) {
                FlowGraph flowGraph = FlowGraph.getInstance();
                flowGraph.optimize(Intermediate.getInstance());
            }
            Intermediate.getInstance().output("testfile1_19374223_王永瑶_优化后中间代码.txt");

            Translator mips = new Translator(Intermediate.getInstance());
            mips.icode2Mips();
            mips.output("mips.txt",false);
        }
    }
}
