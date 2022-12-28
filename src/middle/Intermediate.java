package middle;

import frontend.error.Error;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.lexical.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Intermediate {
    public final List<String> globalStrings = new ArrayList<>();       // 记录字符串
    private int strCnt = 0;
    private int tmpCnt = 0;                                             // 记录中间变量个数
    private int labelCnt = 0;                                           // 记录标签个数
    private int loopCnt = 0;                                            // 记录循环个数->生成标签
    private int orCnt = 0;
    private int andCnt = 0;
    private int elseCnt = 0;
    private int ifCnt = 0;
    public final List<MidCode> codes;
    public static boolean isGlobal = true;
    private static Intermediate instance;
    private static SymbolTable curTable;
    private final Stack<String> loopBegins = new Stack<>();
    private final Stack<String> loopEnds = new Stack<>();

    private Intermediate() {
        codes = new ArrayList<>();
        curTable = SymbolTable.getGlobal();
    }

    public String getLoopEnd() {
        return loopEnds.peek();
    }

    public String getLoopBegin() {
        return loopBegins.peek();
    }

    public void deleteLoop() {
        loopBegins.pop();
        loopEnds.pop();
    }

    public void addLoop(String loopBegin,String loopEnd) {
        loopBegins.push(loopBegin);
        loopEnds.push(loopEnd);
    }

    public boolean hasNext(int index) {
        return index < codes.size();
    }

    public static String getMidName(String name,int line) {
        return curTable.getSym(name,line).getMidName();
    }

    public MidCode getIcode(int index) {
        if (index == codes.toArray().length)
            return null;
        return codes.get(index);
    }

    public static SymbolTable getSubTable(Token token) {
        return curTable.getSubTable(token);
    }

    public static void setWithParent() {
        curTable = curTable.getParent();
    }

    public Symbol getSym(String name,int line) {
        return curTable.getSym(name,line);
    }

    public static void setCurTable(SymbolTable curTable) {
        Intermediate.curTable = curTable;
    }

    public static Intermediate getInstance() {
        if (instance == null) {
            instance = new Intermediate();
        }
        return instance;
    }

    public String addCode(MidCode.Operation op) {
        String ans = null;
        if (op.equals(MidCode.Operation.OR)) {
            ans = "orCond_" + orCnt++;
        }
        else if (op.equals(MidCode.Operation.AND)) {
            ans = "andCond_" + andCnt++;
        }
        else if (op.equals(MidCode.Operation.LOOP_BEGIN)) {
            ans = "while_beign_" + loopCnt++;
        }
        else if (op.equals(MidCode.Operation.IF_BEGIN)) {
            ans = "if_begin_" + ifCnt++;
        }
        codes.add(new MidCode(op, ans));
        return ans;
    }

    public String addCode(MidCode.Operation op,String cond1,MidCode.Operation logic,String cond2,String result) {
        if (result.equals("*TEMP*")) {
            result = "**T" + tmpCnt++ + "**";
            curTable.addSym(new Symbol(result, Symbol.Kind.TEMP));
        }
        codes.add(new MidCode(op,cond1,logic,cond2,result));
        return result(op,cond1,cond2,result);
    }

    public String addCode(MidCode.Operation op, String operand1, String operand2, String result) {
        if (op.equals(MidCode.Operation.ENTER_BLOCK)) {
            isGlobal = false;
        }
        if (operand1.equals("*TEMP*")) {
            operand1 = "**T" + tmpCnt++ + "**";
            curTable.addSym(new Symbol(operand1, Symbol.Kind.TEMP));
        }
        if (result.equals("*TEMP*")) {
            result = "**T" + tmpCnt++ + "**";
            curTable.addSym(new Symbol(result, Symbol.Kind.TEMP));
        }
        if (op.equals(MidCode.Operation.PRINT_STR)) {
            operand1 = addGlobalString(operand1);
        }
        if (!op.equals(MidCode.Operation.PRINT_INT)) {
            if (operand2.matches(".*\\[.*].*") && !op.equals(MidCode.Operation.ARRAY_LOAD)) {
                // 使用数组时，要先取到临时变量中
                operand2 = addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand2,"*OPERAND1*");
            }
            // 如果不是ARRAY_SAVE,先存到临时变量，再存到array
            if (operand2.matches(".*\\[.*].*") && !op.equals(MidCode.Operation.ARRAY_SAVE)) {
                if ( op.equals(MidCode.Operation.GETINT) || op.equals(MidCode.Operation.ASSIGN)) {
                    if (op.equals(MidCode.Operation.GETINT)) {
                        operand2 = addCode(MidCode.Operation.GETINT,"*TEMP*","*NULL*","*OPERAND1*");
                    }
                    else {
                        operand2 = addCode(MidCode.Operation.ASSIGN,"*TEMP*",operand1,"*OPERAND1*");
                    }
                    op = MidCode.Operation.ARRAY_SAVE;
                }
            }
        }
        codes.add(new MidCode(op,operand1,operand2,result));
        return result(op,operand1,operand2,result);
    }

    private String addGlobalString(String str) {
        String name = "str" + strCnt++;
        globalStrings.add(str);
        return name;
    }

    public void addCode(MidCode.Operation op, String operand1, String operand2, String result,Token flag) {
        if (op.equals(MidCode.Operation.ENTER_BLOCK)) {
            isGlobal = false;
        }
        codes.add(new MidCode(op,operand1,operand2,result,flag));
    }

    public void addCode(MidCode.Operation op, String operand1, String operand2, String result, int size) {
        if (op.equals(MidCode.Operation.ENTER_BLOCK)) {
            isGlobal = false;
        }
        if (operand2.matches(".*\\[.*].*")) {
            operand2 = addCode(MidCode.Operation.ARRAY_LOAD,"*TEMP*",operand2,"*OPERAND1*");
        }
        codes.add(new MidCode(op,operand1,operand2,result,size,isGlobal));
    }

    public String result(MidCode.Operation op, String operand1, String operand2, String result) {
        String rst = result;
        if (result.equals("LABEL")) {
            rst = "LABLE_" + labelCnt++;
        }
        else if (result.equals("*OPERAND1*")) {
            rst = operand1;
        }
        return rst;
    }

    public String getIfEndLabel() {
        return "if_end_" + ifCnt++;
    }

    public String getElseLabel() {
        return "else_" + elseCnt++;
    }

    public String getOrLabel() {
        return "orCond_" + orCnt++;
    }

    public String getLoopEndLabel() {
        return "while_end_" + (loopCnt-1);
    }

    public void output(String path) {
        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.append("== GlobalString ==").append("\n");
            int i = 0;
            for (String str:globalStrings) {
                fileWriter.append("STR").append(String.valueOf(i++)).append(" ").append(str).append("\n");
            }
            fileWriter.append("== GlobalString ==").append("\n");
            for (MidCode code:codes) {
                if (!code.getOperation().equals(MidCode.Operation.INVALID))
                    fileWriter.append(code.toString()).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
