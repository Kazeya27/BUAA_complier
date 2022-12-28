package frontend.error;

import frontend.lexical.Token;

public class ErrorSolver {
    private static SymbolTable curTable = SymbolTable.getGlobal();
    private static int curDepth = 0;
    private static int loopDepth = 0;
    private static Symbol.DataType curFuncRtnType;
    public static boolean isGlobal = true;

    public static Symbol getSym(String name) {
        return curTable.getSym(name);
    }

    public static boolean isInLoop() {
        return loopDepth > 0;
    }

    public static boolean outLoop() {
        if (loopDepth == 0)
            return false;
        loopDepth--;
        return true;
    }

    public static void setCurFuncRtnType(Symbol.DataType curFuncRtnType) {
        ErrorSolver.curFuncRtnType = curFuncRtnType;
    }

    public static Symbol.DataType getCurFuncRtnType() {
        return curFuncRtnType;
    }

    public static boolean inLoop() {
        return loopDepth >= 1;
    }

    public static void enterLoop() {
        loopDepth++;
    }

    public static void minuDepth() {
        curDepth--;
        curTable = curTable.getParent();
    }

    public static void addDepth(Token token) {
        curDepth++;
        isGlobal = false;
        curTable = new SymbolTable(token, curTable);
    }

    public static void addDepth(Token token,String funcName) {
        addDepth(token);
        SymbolTable.addFuncTable(funcName,curTable);
    }

    public static boolean hasSym(String name,boolean recursive) {
        return curTable.hasSym(name,recursive);
    }

    public static void addSyms(SymbolTable symbolTable) {
        curTable.addSyms(symbolTable.getSymbolMap(),symbolTable.getSymbolName());
    }

    public static void addSym(Symbol symbol) {
        curTable.addSym(symbol);
    }

    public static SymbolTable getCurTable() {
        return curTable;
    }
}
