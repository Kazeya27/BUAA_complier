package frontend.error;

import frontend.lexical.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    private final HashMap<String,Symbol> symbolMap = new HashMap<>();
    private final SymbolTable parent;
    private final List<String> symbolName = new ArrayList<>();
    private static final HashMap<String,SymbolTable> params = new HashMap<>();
    // 直接子表，用于中间代码查找
    private final HashMap<Token,SymbolTable> subTables = new HashMap<>();
    private static final SymbolTable global = new SymbolTable();
    private static final HashMap<String,SymbolTable> funcTables = new HashMap<>();
    private int stackSize;

    public SymbolTable() {
        this.parent = null;
    }

    public SymbolTable(Token token,SymbolTable parent) {
        this.parent = parent;
        parent.addSubTable(token,this);
    }

    public boolean hasSym(Symbol symbol) {
        return symbolMap.containsValue(symbol);
    }

    public boolean isGlobal() {
        return this.equals(global);
    }

    public static SymbolTable getGlobal() {
        return global;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public void addSym(Symbol symbol) {
        symbolMap.putIfAbsent(symbol.getName(), symbol);
        symbolName.add(symbol.getName());
    }

    public static HashMap<String,SymbolTable> getFuncTables() {
        for (SymbolTable table:funcTables.values()) {
            table.stackSize = setTableAddr(table,4);
        }
        return funcTables;
    }

    private static int setTableAddr(SymbolTable table,int address) {
        for (String name:table.getSymbolName()) {
            address = table.getSym(name).setAddress(address);
        }
        for (SymbolTable subTable:table.getSubTables().values()) {
            address = setTableAddr(subTable,address);
        }
        table.stackSize = address;
        return address;
    }

    public HashMap<Token, SymbolTable> getSubTables() {
        return subTables;
    }

    public static void addFuncTable(String funcName, SymbolTable symbolTable) {
        funcTables.put(funcName, symbolTable);
    }

    public void addSubTable(Token token,SymbolTable symbolTable) {
        this.subTables.put(token,symbolTable);
    }

    public SymbolTable getSubTable(Token token) {
        return this.subTables.get(token);
    }

    public List<String> getSymbolName() {
        return symbolName;
    }

    public boolean hasSym(String name, boolean recursive) {
        if (symbolMap.containsKey(name))
            return true;
        if (recursive && parent != null)
            return parent.hasSym(name, true);
        return false;
    }

    // 修复因keySet无序导致地址出错的bug
    public void addSyms(HashMap<String ,Symbol> symbols,List<String>names) {
        symbolMap.putAll(symbols);
        symbolName.addAll(names);
    }

    public HashMap<String, Symbol> getSymbolMap() {
        return symbolMap;
    }

    public Symbol getSym(String name,int line) {
        if (symbolMap.containsKey(name)) {
            Symbol symbol = symbolMap.get(name);
            if (symbol.getLine() <= line)
                return symbol;
        }
        if (parent == null)
            return null;
        else
            return parent.getSym(name,line);
    }

    public Symbol getSym(String name) {
        if (symbolMap.containsKey(name)) {
            return symbolMap.get(name);
        }
        if (parent == null)
            return null;
        else
            return parent.getSym(name);
    }

    public static void addFunc(String name,SymbolTable param) {
        params.put(name,param);
    }

    public static SymbolTable getSymbolTable(String name) {
        return params.get(name);
    }

    public int getStackSize() {
        return stackSize;
    }
}
