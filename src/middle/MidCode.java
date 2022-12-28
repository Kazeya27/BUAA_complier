package middle;

import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.lexical.Token;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MidCode {
    // op operand1 operand2 result

    public enum Operation{
        VAR,CONST,ARRAY_SAVE,ARRAY_LOAD,ARRAY,
        GETINT,PRINT_INT,PRINT_STR,
        ASSIGN, ADD, SUB, MUL, DIV, MOD,NOT,
        FUNC,PARAF,FUNC_END,
        PREPARE,PUSH_VAL,CALL,RETURN,PUSH_PTR,
        ENTER_BLOCK,EXIT_BLOCK,
        LABEL,JUMP,BEQ,BNE,LOOP_BEGIN, SET,BRANCH,IF_BEGIN,
        OR,AND,GRE,GEQ,LSS,LEQ, EQL, NEQ,

        INVALID,INVALID_GI
    }

    private Operation operation;
    private String operand1;
    private String operand2;
    private String result;
    private int size;
    private boolean isGlobal;
    private Token blockFlag;
    private Operation logicOp;

    private int defIndex = -1;

    // 存标签
    public MidCode(Operation operation,String operand1) {
        this.operation = operation;
        this.operand1 = operand1;
        operand2 = "*NULL*";
        result = "*NULL*";
    }

    public MidCode(Operation operation, String operand1, Operation logicOp, String operand2, String result) {
        this.operation = operation;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result = result;
        this.logicOp = logicOp;
    }

    public MidCode(Operation operation, String operand1, String operand2, String result, Token blockFlag) {
        this.operation = operation;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result = result;
        this.blockFlag = blockFlag;
    }

    public MidCode(Operation operation, String operand1, String operand2, String result, int size, boolean isGlobal) {
        this.operation = operation;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result = result;
        this.size = size;
        this.isGlobal = isGlobal;
    }

    public MidCode(Operation operation, String operand1, String operand2, String result) {
        this.operation = operation;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result = result;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public void setOperand1(String operand1) {
        this.operand1 = operand1;
    }

    public void setOperand2(String operand2) {
        this.operand2 = operand2;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getOperand1() {
        return operand1;
    }

    public String getOperand2() {
        return operand2;
    }

    public String getResult() {
        return result;
    }

    public int getSize() {
        return size;
    }

    public Token getBlockFlag() {
        return blockFlag;
    }

    public Operation getLogicOp() {
        return logicOp;
    }

    public boolean isValidDefPoint() {
        return defIndex != -1;
    }

    public int getDefIndex() {
        return defIndex;
    }

    public void setDefIndex(int defIndex) {
        this.defIndex = defIndex;
    }

    @Override
    public String toString() {
        String result = this.result;
        String operand1 = this.operand1;
        String operand2 = this.operand2;
        if (result.equals("*NULL*") || result.equals("*OPERAND1*")) {
            result = "";
        }
        if (operand2.equals("*NULL*")) {
            operand2 = "";
        }
        if (operand1.equals("*NULL*")) {
            operand1 = "";
        }
        String rst;
        if (logicOp != null) {
            rst = operation + " " + operand1 + " " + logicOp + " " + operand2 + " " + result;
        }
        else
            rst = operation + " " + operand1 + " " + operand2 + " " + result;
        if (operation.equals(Operation.FUNC)) {
            rst = operand1 + ": " + rst;
        }
        return rst;
    }

    public HashSet<String> getUseVar() {
        HashSet<String> useVars = new HashSet<>();
        switch (operation) {
            // 二元运算，a = b + c，a = (b <= c)
            // 跳转, beq a,b,label
            case ADD:
            case MOD:
            case DIV:
            case MUL:
            case SUB:
            case SET:
            case BNE:
            case BEQ:
                if (isVar(operand1)) {
                    useVars.add(operand1);
                }
                if (isVar(operand2)) {
                    useVars.add(operand2);
                }
                break;
            case ASSIGN:
            case VAR:
                // 单目运算, a = b
                //          a = 1
                if (isVar(operand2)) {
                    useVars.add(operand2);
                }
                break;
            case PUSH_VAL:
            case RETURN:
            case PRINT_INT:
                if (isVar(operand1)) {
                    useVars.add(operand1);
                }
                break;
            case ARRAY_SAVE:
                // a@<1>[i@<4>] = b
                if (isVar(operand2)) {
                    useVars.add(operand2);
                }
                String index = operand1.split("\\[")[1].split("]")[0];
                if (isVar(index)) {
                    useVars.add(index);
                }
                break;
            case ARRAY_LOAD:
                // **T** = a@<3>[i@<4>]
                index = operand2.split("\\[")[1].split("]")[0];
                if (isVar(index)) {
                    useVars.add(index);
                }
                break;
        }
        return useVars;
    }

    public String getDefVar() {
        String ans = null;
        switch (operation) {
            case ADD:
            case MOD:
            case DIV:
            case MUL:
            case SUB:
            case SET:
                if (isVar(result)) {
                    ans = result;
                }
                break;
            case ASSIGN:
            case GETINT:
            case ARRAY_LOAD:
            case VAR:
            case PARAF:
                if (isVar(operand1)) {
                    ans = operand1;
                }
                break;
        }
        return ans;
    }

    public HashSet<Integer> getKill() {
        // kill[dk]包含除dk以外，其他所有对dk的定义点
        HashSet<Integer> kills = new HashSet<>();
        String defVar = getDefVar();
        if (defVar != null) {
            kills.addAll(FlowGraph.getInstance().getDefPoints().get(defVar));
            kills.remove(defIndex);
        }
        return kills;
    }

    public boolean isLabel() {
        return operation.equals(Operation.FUNC) || operation.equals(Operation.LOOP_BEGIN) || operation.equals(Operation.LABEL);
    }

    public boolean isJump() {
        return operation.equals(Operation.JUMP) || operation.equals(Operation.BEQ) || operation.equals(Operation.BNE)
                || operation.equals(Operation.RETURN);
    }

    public boolean isDigit(String name) {
        return name.matches("[0-9]*") || name.charAt(0) == '-' || name.charAt(0) == '+';
    }

    private boolean isVar(String midName) {
        String name = midName.split("@")[0];
        Symbol symbol = null;
        if (midName.split("<").length == 2) {
            String line = midName.split("<")[1].split(">")[0];
            symbol = SymbolTable.getGlobal().getSym(name,Integer.parseInt(line));
        }
        // 全局变量不属于def
        if (symbol != null)
            return false;
        return midName.startsWith("**T") || (!isDigit(name) && !name.equals("*RST*") && !name.equals("*NULL*"));
    }

    public boolean isUnary() {
        return operation.equals(Operation.ADD) || operation.equals(Operation.SUB) ||
                operation.equals(Operation.MUL) || operation.equals(Operation.DIV) ||
                operation.equals(Operation.MOD);
    }

    public boolean isConst() {
        if (operation.equals(Operation.VAR) || operation.equals(Operation.CONST) || operation.equals(Operation.ASSIGN)) {
            return isDigit(operand2);
        }
        return false;
    }

    public void constantReplace(HashMap<String,String> values) {
        if (operation.equals(Operation.ARRAY_SAVE)) {
            String index = operand1.split("\\[")[1].split("]")[0];
            for (Map.Entry<String,String> entry: values.entrySet()) {
                if (operand2.equals(entry.getKey())) {
                    operand2 = entry.getValue();
                }
                if (index.equals(entry.getKey())) {
                    operand1 = operand1.split("(?<=\\[)")[0] + entry.getValue() + operand1.split("(?=])")[1];
                }
            }
        }
        else if (operation.equals(Operation.ARRAY_LOAD)) {
            String index = operand2.split("\\[")[1].split("]")[0];
            for (Map.Entry<String,String> entry: values.entrySet()) {
                if (index.equals(entry.getKey())) {
                    operand2 = operand2.split("(?<=\\[)")[0] + entry.getValue() + operand2.split("(?=])")[1];
                }
            }
        }
        // 防止出现 assign a = a被改成 assign 1 = 1
        else if (operation.equals(Operation.ASSIGN)) {
            for (Map.Entry<String,String> entry: values.entrySet()) {
                if (operand2.equals(entry.getKey())) {
                    operand2 = entry.getValue();
                }
            }
        }
        else {
            for (Map.Entry<String,String> entry: values.entrySet()) {
                if (operand1.equals(entry.getKey())) {
                    operand1 = entry.getValue();
                }
                if (operand2.equals(entry.getKey())) {
                    operand2 = entry.getValue();
                }
            }
        }
    }

}
