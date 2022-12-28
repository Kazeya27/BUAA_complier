package backend;

import backend.instruction.*;
import frontend.error.Symbol;
import frontend.error.SymbolTable;
import frontend.lexical.Token;
import middle.Intermediate;
import middle.MidCode;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

public class Translator {
    private static final String STR_DEF = ":.asciiz ";
    private static final String VAR_DEF = ":.word ";
    private static final boolean BIN_OPT = true;
    private static int DIV_OPT = 0;
    /*
 o     全局栈帧 --/|\->  +----------------------------+---/|\------- $sp + 0
 o                |     |         $ra                |    |           4
 o               -|-    +----------------------------+----|------- $sp + 4
 o                |     |         $sp                |    |           4
 o               -|-    +----------------------------+----|------- $sp + 8
 o CONTEXT_SIZE   |     |       $a0-$a3              |    |           16
 o               -|-    +----------------------------+----|------- $sp + 24
 o                |     |       $s0-$s7 + $fp        |    |           32
 o               -|-    +----------------------------+----|------- $sp + 56
 o                |     |       $t0-$t9              |    |           40
 o               -|-    +----------------------------+----|------- $sp + 96
 o                |     |      保存上下文使用的栈       |    |
 o               \|/    +----------------------------+---\|/------ $sp + 100
 o                      |                            |
 o      func1    -----> +----------------------------+---/|\-----  $sp + 100
 o                      |         ...........        |    |
 o                      +----------------------------+----|------  funcStackSize1
 o                      |         ...........        |    |
 o                      +----------------------------+---\|/------ $sp + funcStackSize1
 o                      |    func1的上下文保存栈        |               contextSize
 o func2(func1调用)----> +----------------------------+---/|\------- $sp + funcStackSize1 + contextSize ---- spOffset
 o                      |         ...........        |    |
 o                      +----------------------------+----|------  funcStackSize2
 o                      |         ...........        |    |
 o                      +----------------------------+---\|/------ $sp + funcStackSize(1+2)
*/
    private static final int CONTEXT_SIZE = 104;
    private static final int RA_OFFSET = 0;
    private static final int SP_OFFSET = RA_OFFSET + 4;         // 4-7
    private static final int AREG_OFFSET = SP_OFFSET + 4;       // 8-23
    private static final int SREG_OFFSET = AREG_OFFSET + 16;    // 24-59
    private static final int TREG_OFFSET = SREG_OFFSET + 36;    // 60-95
    private static final String RA_ADDR = "0($sp)";

    private int spOffset = 0;                                   // 当前函数sp指针的初始位置
    private final Intermediate icodes;                          // 中间代码
    private final ArrayList<Instruction> instrs = new ArrayList<>(); // 生成的指令
    private int index;                                          // 中间代码的索引
    private Register register = new Register();                 // 寄存器模拟

    private SymbolTable curTable;                               // 当前所处的符号表
    private final SymbolTable globalTable;                      // 全局符号表
    private final HashMap<String,SymbolTable> funcTables;       // 函数名到函数符号表的映射

    private final HashMap<String,Integer> funcSizeMap = new HashMap<>();  // 函数名到函数栈大小的映射
    private final ArrayList<Integer> funcSizeStack = new ArrayList<>();   // 当前各函数函数栈大小(链式调用)

    private String curFunc;                                               // 当前函数名
    private boolean firstFunc = true;
    private final ArrayList<Register> registerStack = new ArrayList<>();

    // 记录当前块使用了哪些寄存器，离开的时候释放掉
    private final Stack<HashSet<String>> blockRegs = new Stack<>();
    private HashSet<String> curBlockRegs = new HashSet<>();

    private final static HashSet<MidCode.Operation> binaryOp = new HashSet<MidCode.Operation>() {
        {
            add(MidCode.Operation.ADD);
            add(MidCode.Operation.SUB);
            add(MidCode.Operation.DIV);
            add(MidCode.Operation.MUL);
            add(MidCode.Operation.MOD);
        }
    };

    public Translator(Intermediate icodes) {
        this.icodes = icodes;
        this.index = 0;
        this.funcTables = SymbolTable.getFuncTables();
        this.globalTable = SymbolTable.getGlobal();
        this.curTable = SymbolTable.getGlobal();
        int globalSize = 0;
        for (String name:globalTable.getSymbolName()) {
            globalSize = globalTable.getSym(name).setAddress(globalSize);
        }
        for (Map.Entry<String,SymbolTable> entry:funcTables.entrySet()) {
            funcSizeMap.put(entry.getKey(),entry.getValue().getStackSize());
        }
    }

    // **********************************  核心翻译方法  ************************************************

    private void loadFunc() {
        while (hasNext()) {
            MidCode icode = getIcode();
            addCode(new Comment(icode.toString()));
            MidCode.Operation op = icode.getOperation();
            String opd1 = icode.getOperand1();
            String opd2 = icode.getOperand2();
            String rst = icode.getResult();
            // const opd1 opd2 | var opd1 opd2(null) | assign opd1 opd2
            if (op.equals(MidCode.Operation.ASSIGN) || op.equals(MidCode.Operation.VAR) || op.equals(MidCode.Operation.CONST)) {
                // 不是var op1 null,存入寄存器
                if (!opd2.equals("*NULL*")) {
                    addAssign(opd1,opd2);
                }
            }
            else if (op.equals(MidCode.Operation.GETINT) || op.equals(MidCode.Operation.INVALID_GI)) {
                boolean onlyGet = !op.equals(MidCode.Operation.GETINT);
                getInt(opd1,onlyGet);
            }
            else if (op.equals(MidCode.Operation.PRINT_STR)) {
                printStr(opd1);
            }
            else if (op.equals(MidCode.Operation.PRINT_INT)) {
                printInt(opd1);
            }
            else if (op.equals(MidCode.Operation.PREPARE)) {
                int funcStackSize = funcSizeMap.get(opd1) + CONTEXT_SIZE;
                funcSizeStack.add(funcStackSize);
                spOffset = getCurSp();
                addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,"$sp","$sp",String.valueOf(-funcStackSize)));
            }
            //     add $a0, off($sp),
            //     sw $a0 param_off($sp)
            else if (op.equals(MidCode.Operation.PUSH_PTR)) {
                String dstAddr = 4*Integer.parseInt(opd2) + "($sp)";
                String index = "0";
                if (opd1.contains("[")) {
                    index = opd1.split("\\[")[1];
                    while (index.endsWith("]") || index.endsWith("&"))
                        index = index.substring(0,index.length()-1);
                }
                String arrPtrReg = getPtrReg(opd1,index);
                addCode(new StoreWord(arrPtrReg,dstAddr));
            }
            else if (op.equals(MidCode.Operation.PUSH_VAL)) {
                String dstAddr = 4*Integer.parseInt(opd2) + "($sp)";
                String valReg = loadWhenNotInReg("$a1",opd1);
                addCode(new StoreWord(valReg,dstAddr));
            }
            else if (op.equals(MidCode.Operation.CALL)) {
                Register oldRegister = contextSave(opd1);
                registerStack.add(oldRegister);
                register = new Register();
                addCode(new JumpAndLink(opd1));
                oldRegister = registerStack.remove(registerStack.size()-1);
                contextRecover(opd1,oldRegister);
                register = oldRegister;
                addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,"$sp","$sp",Integer.toString(funcSizeStack.get(funcSizeStack.size()-1))));
                funcSizeStack.remove(funcSizeStack.size()-1);
                spOffset = getCurSp();
            }
            else if (op.equals(MidCode.Operation.RETURN)) {
                if (!opd1.equals("*NULL*")) {
                    load("$v0",opd1);
                }
                if (curFunc.equals("main")) {
                    addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,"$sp","$sp",Integer.toString(funcSizeMap.get("main") + CONTEXT_SIZE)));
                    addCode(new LoadImm("$v0","10"));
                    addCode(new Syscall());
                }
                else {
                    addCode(new JumpReg("$ra"));
                }
            }
            else if (isBinaryOp(op)) {
                addBinary(opd1,opd2,rst,op);
            }
            else if (op.equals(MidCode.Operation.ENTER_BLOCK)) {
                enterBlock(icode.getBlockFlag());
            }
            else if (op.equals(MidCode.Operation.EXIT_BLOCK)) {
                exitBlock();
            }
            else if (op.equals(MidCode.Operation.ARRAY_SAVE) || op.equals(MidCode.Operation.ARRAY_LOAD)) {
                arrSaveOrLoad(op,opd1,opd2);
            }
            else if (op.equals(MidCode.Operation.FUNC)) {
                funcDef(icode);
            }
            else if (op.equals(MidCode.Operation.JUMP)) {
                // saveAllToStack();
                // register = new Register();
                addCode(new Jump(opd1));
            }
            else if (op.equals(MidCode.Operation.LABEL) || op.equals(MidCode.Operation.LOOP_BEGIN)) {
                // saveAllToStack();
                // register = new Register();
                addCode(new Label(opd1 + ":"));
            }
            else if (op.equals(MidCode.Operation.BEQ) || op.equals(MidCode.Operation.BNE)) {
                addBranch(op,opd1,opd2,rst);
            }
            else if (op.equals(MidCode.Operation.SET)) {
                addSet(icode.getLogicOp(),opd1,opd2,rst);
            }
        }
    }

    // **********************************  寄存操作、数据加载保存、查询地址等核心方法  ********************************

    private void freeReg(String regName) {
        if (regName.startsWith("$t")) {
            register.freeReg(register.getRegWithRegName(regName));
            addCode(new Comment("free reg: " + regName));
        }
    }

    private Register contextSave(String funcName) {
        Register oldRegister = register.copy();
        addCode(new StoreWord("$ra",RA_ADDR));
        for (Map.Entry<String, Reg> entry: register.getName2Reg().entrySet()) {
            Reg reg = entry.getValue();
            if (reg.getRegName().charAt(1) == 't') {
                addCode(new StoreWord(reg.getRegName(),TREG_OFFSET + 4*reg.getId() + funcSizeMap.get(funcName) + "($sp)"));
            }
            else if (reg.getRegName().charAt(1) == 's') {
                addCode(new StoreWord(reg.getRegName(),SREG_OFFSET + 4*reg.getId() + funcSizeMap.get(funcName) + "($sp)"));
            }
            else if (reg.getRegName().equals("$fp")) {
                addCode(new StoreWord(reg.getRegName(),SREG_OFFSET + 4*reg.getId() + funcSizeMap.get(funcName) + "($sp)"));
            }
        }
        return oldRegister;
    }

    private void contextRecover(String funcName,Register oldRegister) {
        for (Map.Entry<String, Reg> entry: oldRegister.getName2Reg().entrySet()) {
            Reg reg = entry.getValue();
            if (reg.getRegName().charAt(1) == 't') {
                addCode(new LoadWord(reg.getRegName(),TREG_OFFSET + 4*reg.getId() + funcSizeMap.get(funcName) + "($sp)"));
            }
            else if (reg.getRegName().charAt(1) == 's') {
                addCode(new LoadWord(reg.getRegName(),SREG_OFFSET + 4*reg.getId() + funcSizeMap.get(funcName) + "($sp)"));
            }
            else if (reg.getRegName().equals("$fp")) {
                addCode(new LoadWord(reg.getRegName(),SREG_OFFSET + 4*reg.getId() + funcSizeMap.get(funcName) + "($sp)"));

            }
        }
        addCode(new LoadWord("$ra",RA_ADDR));
    }

    /** 判断是否在寄存器中，是否请求一个寄存器，checkOnly使用需要请求，replace如果寄存器满了，是否要替换 */
    public boolean tryAlloc(String operand,boolean checkOnly,boolean replace) {
        if (register.allocated(operand)) {
            return true;
        }
        if (checkOnly || isGlobalVar(operand) || isImm(operand)) {
            return false;
        }
        String regName;
        if (operand.startsWith("**")) {
            regName = register.allocTReg(operand);
            if (!regName.equals("FULL")) {
                // curBlockRegs.add(regName);
                return true;
            }
        }
        if (!isGlobalVar(operand) && !isImm(operand)) {
            Symbol symbol = curTable.getSym(operand.split("@")[0]);
            if (symbol != null && symbol.isVar()) {
                regName = register.allocSReg(operand);
                if (!regName.equals("FULL")) {
                    curBlockRegs.add(regName);
                    return true;
                }
            }
        }
        if (replace) {
            Reg reg;
            if (operand.startsWith("**")) {
                reg = register.replaceTReg(operand);
            }
            else {
                reg = register.replaceSReg(operand);
            }
            // 替换的寄存器也要加入到curBlockRegs
            curBlockRegs.add(reg.getRegName());
            // 可能已经离开那个变量的作用域了，再去找curTable会空指针(增加blockRegs后是否还需要？)
            if (curTable.hasSym(reg.getName().split("@")[0],true))
                saveToStack(reg.getRegName(),reg.getName());
            return true;
        }
        return false;
    }

    private void saveToStack(String regName,String operand) {
        String addr = getAddr(operand);
        addCode(new StoreWord(regName,addr));
    }

    private String getMemAddr(String operand) {
        if (isDigit(operand)) {
            return operand;
        }
        // 在内存内，全局变量返回标签，局部变量返回栈帧位置
        Symbol symbol;
        if (isGlobalVar(operand)) {
            symbol = globalTable.getSym(operand.split("@")[0]);
            String addr;
            addr = "_" + symbol.getName() + "_";
            String index = "0";
            if (operand.contains("[")) {
                index = operand.split("\\[")[1];
                index = index.substring(0,index.length()-1);
            }
            addr += "+" + index;
            return addr;
        }
        else {
            symbol = curTable.getSym(operand.split("@")[0]);
            return (symbol.getAddress() + spOffset) + "($sp)";
        }
    }

    /** 获取操作数的地址 */
    private String getAddr(String operand) {
        // 在寄存器内，返回寄存器名字
        if (register.allocated(operand)) {
            return register.getRegWithSymbol(operand);
        }
        // 立即数，返回数本身
        if (isDigit(operand)) {
            return operand;
        }
        // 在内存内，全局变量返回标签，局部变量返回栈帧位置
        Symbol symbol;
        if (isGlobalVar(operand)) {
            symbol = globalTable.getSym(operand.split("@")[0]);
            String addr;
            addr = "_" + symbol.getName() + "_";
            String index = "0";
            if (operand.contains("[")) {
                index = operand.split("\\[")[1];
                index = index.substring(0,index.length()-1);
            }
            addr += "+" + index;
            return addr;
        }
        else {
            symbol = curTable.getSym(operand.split("@")[0]);
            return (symbol.getAddress() + spOffset) + "($sp)";
        }
    }

    /** 返回数组指针所在的寄存器，没有就分配到$a1，主要用于指针作为实参 */
    private String getPtrReg(String operand,String index) {
        String arrName = operand.split("@")[0];
        String arrAddr = getAddr(operand.split("&")[0]);
        // ptrReg -> 数组指针加载到该寄存器
        String ptrReg = "$a1";
        Symbol symbol;
        if (isGlobalVar(operand.split("&")[0]))
            symbol = globalTable.getSym(arrName);
        else
            symbol = curTable.getSym(arrName);
        int size2 = symbol.getSize2();
        size2 = (size2 == -1)?0:size2;
        // 如果索引是纯数字，不需要加载
        if (isDigit(index)) {
            int offset = Integer.parseInt(index)*size2*4;
            if (symbol.isPara()) {
                // 参数不在寄存器就在内存
                if (!register.allocated(operand.split("&")[0])) {
                    addCode(new LoadWord(ptrReg,arrAddr));
                    arrAddr = ptrReg;
                }
                addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,ptrReg,arrAddr,Integer.toString(offset)));
            }
            else {
                // 全局变量加载标签到ptrReg -> la $a1, arr+100
                if (arrAddr.contains("+")) {
                    arrAddr = arrAddr.split("\\+")[0] + "+" + offset;
                    addCode(new LoadAddr(ptrReg,arrAddr));
                }
                // 内存内加载$sp指针的位置到寄存器 -> addiu $a1,$sp,100
                else if (arrAddr.endsWith("($sp)")) {
                    offset += symbol.getAddress() + spOffset;
                    addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,ptrReg,"$sp",Integer.toString(offset)));
                }
                // 寄存器内直接加法 -> addiu $a1,$t1,100
                else {
                    offset += symbol.getAddress() + spOffset;
                    addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,ptrReg,arrAddr,Integer.toString(offset)));
                }
            }
        }
        // 索引不是立即数，先加载到寄存器
        else {
            String offsetReg = "$a2";
            String indexReg = loadWhenNotInReg(offsetReg,index);
            // offset = (index<<2)[*size2]，有size2说明是二维数组，需要乘第二个维度的长度
            addCode(new BinaryRegImm(Binary.BinaryInstr.SLL,offsetReg,indexReg,"2"));
            if (size2 > 0)
                addCode(new BinaryRegImm(Binary.BinaryInstr.MUL,offsetReg,offsetReg,String.valueOf(size2)));
            if (symbol.isPara()) {
                if (!register.allocated(operand.split("&")[0])) {
                    addCode(new LoadWord(ptrReg,arrAddr));
                    arrAddr = ptrReg;
                }
                addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,ptrReg,arrAddr,offsetReg));
            }
            else {
                // 和上面类似，但需要把索引所在的寄存器相加
                if (arrAddr.contains("+")) {
                    addCode(new LoadAddr(ptrReg,arrAddr));
                    addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,ptrReg,ptrReg,offsetReg));
                }
                else if (arrAddr.endsWith("($sp)")) {
                    addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,ptrReg,"$sp",Integer.toString(spOffset + symbol.getAddress())));
                    addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,ptrReg,ptrReg,offsetReg));
                }
                else {
                    addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,ptrReg,arrAddr,offsetReg));
                }
            }
        }
        return ptrReg;
    }

    // 主要用于加载到指定寄存器，主要是a寄存器
    /** operand不在寄存器时加载到寄存器，并返回最终所处寄存器 */
    private String loadWhenNotInReg(String reg,String operand) {
        String addr = getAddr(operand);
        // opd2是立即数，先加载再存
        if (isImm(operand)) {
            // 如果是立即数，不用把名字加上，免得后面对应a寄存器改值了以后，用立即数还会用这个寄存器，从而出错
            curBlockRegs.add(reg);
            register.replaceWithoutName(reg);
            // register.allocAReg(operand,reg);
            addCode(new LoadImm(reg,addr));
        }
        else if (register.allocated(operand)) {
            return addr;
        }
        // opd2在内存，先加载再存
        else {
            curBlockRegs.add(reg);
            register.allocAReg(operand,reg);
            addCode(new LoadWord(reg,addr));
        }
        return reg;
    }

    // 加载值目标一定在寄存器，不用请求
    private void load(String reg, String operand) {
        String addr = getAddr(operand);
        // 寄存器写寄存器
        if (tryAlloc(operand,true,false)) {
            addCode(new Move(reg,addr));
        }
        // 加载立即数
        else if (isImm(operand)) {
            addCode(new LoadImm(reg,addr));
        }
        // 从内存加载到寄存器
        else {
            // 全局变量用标签
            addCode(new LoadWord(reg,addr));
        }
    }

    // 存值可能需要寄存器，需要请求
    // getint存值到寄存器或内存
    private void save(String reg, String operand) {
        // $v0写寄存器
        String memAddr = getMemAddr(operand);
        if (tryAlloc(operand,false,false)) {
            String addr = getAddr(operand);
            addCode(new Move(addr,reg));
            if ((memAddr.contains("+") || memAddr.contains("sp")) && (addr.contains("s") || addr.equals("$fp"))) {
                addCode(new StoreWord(addr,memAddr));
            }
        }
        // $v0写内存
        else {
            String addr = getAddr(operand);
            addCode(new StoreWord(reg,addr));
        }
    }

    // **********************************  各指令处理方法  ************************************************

    private void addBranch(MidCode.Operation op, String opd1, String opd2, String label) {
        String reg1 = "$a1";
        String reg2 = "$a2";
        reg1 = loadWhenNotInReg(reg1,opd1);
        reg2 = loadWhenNotInReg(reg2,opd2);
        addCode(new Branch(op,reg1,reg2,label));
    }

    private void enterBlock(Token flag) {
        curTable = curTable.getSubTable(flag);
        blockRegs.add(curBlockRegs);
        curBlockRegs = new HashSet<>();
    }

    private void exitBlock() {
        // 退出block，释放S寄存器之前回存一下
        for (String regName:curBlockRegs) {
            if (regName.contains("s")) {
                Reg reg = register.getRegWithRegName(regName);
                String varName = reg.getName();
                register.freeReg(reg);
                String addr = getAddr(varName);
                addCode(new StoreWord(regName,addr));
            }
        }
        curTable = curTable.getParent();
        register.freeRegs(curBlockRegs);
        curBlockRegs = blockRegs.pop();
    }

    // 加载到寄存器或内存
    private void addAssign(String opd1,String opd2) {
        String memAddr = getMemAddr(opd1);
        boolean inReg1 = tryAlloc(opd1,false,false);
        String addr1 = getAddr(opd1);
        if (inReg1) {
            load(addr1,opd2);
            if ((memAddr.contains("+") || memAddr.contains("sp")) && (addr1.contains("s") || addr1.equals("$fp"))) {
                addCode(new StoreWord(addr1,memAddr));
            }
        }
        // opd1不在寄存器，需要把opd2写内存
        else {
            String tmpReg = "$a1";
            String reg2 = loadWhenNotInReg(tmpReg,opd2);
            addCode(new StoreWord(reg2,addr1));
        }
    }

    public void loadStrCon() {
        addCode(new Label(".data"));
        int i = 0;
        String code;
        for (String strCon: icodes.globalStrings) {
            code = "str" + i++ + STR_DEF + "\"" + strCon + "\"";
            addCode(new Label(code));
        }
    }

    public void loadGlobalVar() {
        MidCode icode = getIcode();
        StringBuilder mipsCode;
        while (!icode.getOperation().equals(MidCode.Operation.FUNC)) {
            mipsCode = new StringBuilder("_" + icode.getOperand1().split("@")[0] + "_" + VAR_DEF);           // 获取数组名
            // ARRAY name size
            if (icode.getOperation().equals(MidCode.Operation.ARRAY)) {
                // ARRAY_SAVE name[0] value
                int size = Integer.parseInt(icode.getOperand2());
                int cnt = 0;
                icode = getIcode();
                while (icode.getOperation().equals(MidCode.Operation.ARRAY_SAVE)) {
                    mipsCode.append(icode.getOperand2()).append(",");
                    icode = getIcode();
                    cnt++;
                }
                while (cnt < size) {
                    mipsCode.append("0,");
                    cnt++;
                }
                mipsCode.deleteCharAt(mipsCode.length()-1);
            }
            // VAR name [val]
            else {
                String val = icode.getOperand2();
                if (val.equals("*NULL*")) {
                    val = "0";
                }
                mipsCode.append(val);
                icode = getIcode();
            }
            addCode(new Label(mipsCode.toString()));
        }
        goBack();
    }

    // 感觉要用的时候再加载也行
    private void loadParaf() {
        MidCode icode = getIcode();
        spOffset = 0;
        while (icode.getOperation().equals(MidCode.Operation.PARAF)) {
            if (!icode.getOperand2().equals("0")) {
                String addr = getAddr(icode.getOperand1());
                String reg = register.allocSReg(icode.getOperand1());
                if (!reg.equals("FULL"))
                    addCode(new LoadWord(reg,addr));
            }
            icode = getIcode();
        }
        goBack();
    }

    private void printStr(String str) {
        addCode(new LoadAddr("$a0",str));
        // 系统调用准备
        addCode(new LoadImm("$v0","4"));
        addCode(new Syscall());
    }

    // 寄存器、立即数、栈帧。  move $a0,$t1, li $a0,1, lw $a0,off($sp)
    private void printInt(String operand) {
        load("$a0",operand);
        // 系统调用准备
        addCode(new LoadImm("$v0","1"));
        addCode(new Syscall());
    }

    // b = getint() --- 寄存器里   a[0] = getint() --- 栈里
    //  move $t1,$v0                save $v0,off($sp)
    private void getInt(String operand,boolean onlyGet) {
        // 系统调用准备
        addCode(new LoadImm("$v0","5"));
        addCode(new Syscall());
        if (!onlyGet)
            save("$v0",operand);
    }

    private void funcDef(MidCode icode) {
        if (firstFunc) {
            addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,"$sp","$sp","-" + (funcSizeMap.get("main") + CONTEXT_SIZE)));
            addCode(new Jump("main"));
            firstFunc = false;
        }
        curFunc = icode.getOperand1();
        addCode(new Label(icode.getOperand1() + ":"));
        spOffset = 0;
        register = new Register();
        icode = getIcode();
        assert icode.getOperation().equals(MidCode.Operation.ENTER_BLOCK);
        enterBlock(icode.getBlockFlag());
        loadParaf();
    }

    private void addSet(MidCode.Operation op, String opd1, String opd2,String rst) {
        String src1 = getAddr(opd1);
        String src2 = getAddr(opd2);
        // 不应该直接加载到a寄存器，因为后续还会使用rst的值，应使用tryAlloc申请t寄存器(rst全是**TEMP**)
        // 删除间接赋值后，不一定是**temp**，需要load
        String memAddr = getAddr(rst);
        tryAlloc(rst,false,true);
        String dst = getAddr(rst);
        if ((dst.contains("$s") || dst.equals("$fp")) && (memAddr.contains("$sp") || memAddr.contains("_"))) {
            addCode(new LoadWord(dst,memAddr));
        }
        // String dst = loadWhenNotInReg("$a0",rst);
        if (isImm(src1) && isImm(src2)) {
            int ans = 0;
            switch (op) {
                case EQL:
                    ans = (Integer.parseInt(src1) == Integer.parseInt(src2))?1:0;
                    break;
                case NEQ:
                    ans = (Integer.parseInt(src1) != Integer.parseInt(src2))?1:0;
                    break;
                case LSS:
                    ans = (Integer.parseInt(src1) < Integer.parseInt(src2))?1:0;
                    break;
                case LEQ:
                    ans = (Integer.parseInt(src1) <= Integer.parseInt(src2))?1:0;
                    break;
                case GEQ:
                    ans = (Integer.parseInt(src1) >= Integer.parseInt(src2))?1:0;
                    break;
                case GRE:
                    ans = (Integer.parseInt(src1) > Integer.parseInt(src2))?1:0;
                    break;
            }
            addCode(new LoadImm(dst,String.valueOf(ans)));
        }
        else if (isImm(src1) || isImm(src2)) {
            if (isImm(src1)) {
                String regSrc2 = "$a3";
                String regSrc1 = loadWhenNotInReg("$a2", src1);
                regSrc2 = loadWhenNotInReg(regSrc2, opd2);
                switch (op) {
                    case EQL:
                        addCode(new SetRegReg(SetInstr.LogicInstr.EQL,dst,regSrc1,regSrc2));
                        break;
                    case NEQ:
                        addCode(new SetRegReg(SetInstr.LogicInstr.NEQ,dst,regSrc1,regSrc2));
                        break;
                    case LSS:
                        addCode(new SetRegReg(SetInstr.LogicInstr.LSS,dst,regSrc1,regSrc2));
                        break;
                    case LEQ:
                        addCode(new SetRegReg(SetInstr.LogicInstr.LEQ,dst,regSrc1,regSrc2));
                        break;
                    case GEQ:
                        addCode(new SetRegReg(SetInstr.LogicInstr.GEQ,dst,regSrc1,regSrc2));
                        break;
                    case GRE:
                        addCode(new SetRegImm(SetInstr.LogicInstr.GRE,dst,regSrc1,regSrc2));
                        break;
                }
            }
            else {
                String regSrc = "$a3";
                String imm = src2;
                regSrc = loadWhenNotInReg(regSrc,opd1);
                switch (op) {
                    case EQL:
                        addCode(new SetRegImm(SetInstr.LogicInstr.EQL,dst,regSrc,imm));
                        break;
                    case NEQ:
                        addCode(new SetRegImm(SetInstr.LogicInstr.NEQ,dst,regSrc,imm));
                        break;
                    case LSS:
                        int tmp = Integer.parseInt(imm);
                        if (tmp > 32767 || tmp < -32768) {
                            imm = loadWhenNotInReg("$a2",imm);
                            addCode(new SetRegReg(SetInstr.LogicInstr.LSS,dst,regSrc,imm));
                        }
                        else {
                            addCode(new SetRegImm(SetInstr.LogicInstr.LSS,dst,regSrc,imm));
                        }
                        break;
                    case LEQ:
                        addCode(new SetRegImm(SetInstr.LogicInstr.LEQ,dst,regSrc,imm));
                        break;
                    case GEQ:
                        addCode(new SetRegImm(SetInstr.LogicInstr.GEQ,dst,regSrc,imm));
                        break;
                    case GRE:
                        addCode(new SetRegImm(SetInstr.LogicInstr.GRE,dst,regSrc,imm));
                        break;
                }
            }
        }
        else {
            String regSrc1 = "$a3";
            String regSrc2 = "$a2";
            regSrc1 = loadWhenNotInReg(regSrc1,opd1);
            regSrc2 = loadWhenNotInReg(regSrc2,opd2);
            switch (op) {
                case EQL:
                    addCode(new SetRegReg(SetInstr.LogicInstr.EQL,dst,regSrc1,regSrc2));
                    break;
                case NEQ:
                    addCode(new SetRegReg(SetInstr.LogicInstr.NEQ,dst,regSrc1,regSrc2));
                    break;
                case LSS:
                    addCode(new SetRegReg(SetInstr.LogicInstr.LSS,dst,regSrc1,regSrc2));
                    break;
                case LEQ:
                    addCode(new SetRegReg(SetInstr.LogicInstr.LEQ,dst,regSrc1,regSrc2));
                    break;
                case GEQ:
                    addCode(new SetRegReg(SetInstr.LogicInstr.GEQ,dst,regSrc1,regSrc2));
                    break;
                case GRE:
                    addCode(new SetRegImm(SetInstr.LogicInstr.GRE,dst,regSrc1,regSrc2));
                    break;
            }
        }
    }

    // **********************************  二元运算方法  ***********************************************
    // todo 降低运算强度
    private void addBinary(String opd1, String opd2, String rst, MidCode.Operation op) {
        // rst全是**TEMP**，加载到t寄存器
        // 删除间接赋值后，不一定是**temp**，需要load
        String memAddr = getAddr(rst);
        tryAlloc(rst,false,true);
        String dst = getAddr(rst);
        // System.out.println(dst);
        if ((dst.contains("$s") || dst.equals("$fp")) && (memAddr.contains("$sp") || memAddr.contains("_"))) {
            addCode(new LoadWord(dst,memAddr));
        }
        String src1 = getAddr(opd1);
        String src2 = getAddr(opd2);
        String reg1 = "$a1";
        if (BIN_OPT && (op.equals(MidCode.Operation.DIV) || op.equals(MidCode.Operation.MOD)) && isImm(src2)) {
            String regSrc = "$a3";
            String regTmp = "$a0";
            regSrc = loadWhenNotInReg(regSrc,opd1);
            // System.out.println(regSrc);
            int d = Integer.parseInt(src2);
            if (op.equals(MidCode.Operation.DIV) && isPower(d)) {
                int pow = log(d);
                String label = "DIV_BRANCH_" + DIV_OPT++;
                addCodeWithoutFree(new Move(dst,regSrc));
                addCodeWithoutFree(new BGTZ(regSrc,label));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.ADD,dst,regSrc,Integer.toString(d-1)));
                addCodeWithoutFree(new Label(label + ":"));
                addCode(new BinaryRegImm(Binary.BinaryInstr.SRA,dst,dst,Integer.toString(pow)));
                return;
            }
            Multiplier multiplier = Optimizer.chooseMultiplier(d,31);
            long m = multiplier.m;
            int sh = multiplier.sh;
            int l = multiplier.l;
            // System.out.println(m + " " + sh + " " + l);
            if (d == 1) {
                addCode(new Move(dst,regSrc));
            }
            else if (d == -1) {
                addCode(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,"$zero",regSrc));
            }
            else if (Math.abs(d) == (1 << l)) {
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRA,regTmp,regSrc,Integer.toString(l-1)));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRL,dst,regTmp,Integer.toString(Optimizer.N - l)));
                addCodeWithoutFree(new BinaryRegReg(Binary.BinaryInstr.ADD,regTmp,regSrc,dst));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRA,dst,regTmp,Integer.toString(l)));
            }
            else if (m < (1L << (Optimizer.N - 1))) {
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.MUL,regTmp,regSrc,Long.toString(m)));
                addCodeWithoutFree(new MoveFromHI(regTmp));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRA,dst,regTmp,Integer.toString(sh)));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRA,regTmp,regSrc,Integer.toString(31)));
                addCodeWithoutFree(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,dst,regTmp));
            }
            else {
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.MUL,regTmp,regSrc,Integer.toString((int) (m - Math.pow(2,Optimizer.N)))));
                addCodeWithoutFree(new MoveFromHI(regTmp));
                addCodeWithoutFree(new BinaryRegReg(Binary.BinaryInstr.ADD,regTmp,regSrc,regTmp));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRA,dst,regTmp,Integer.toString(sh)));
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.SRA,regTmp,regSrc,Integer.toString(31)));
                addCodeWithoutFree(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,dst,regTmp));
            }
            if (d < 0) {
                addCodeWithoutFree(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,"$zero",dst));
            }
            if (op.equals(MidCode.Operation.MOD)) {
                addCodeWithoutFree(new BinaryRegImm(Binary.BinaryInstr.MUL,regTmp,dst,Integer.toString(d)));
                addCodeWithoutFree(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,regSrc,regTmp));
            }
            return;
        }
        if (isImm(src1) && isImm(src2)) {
            int ans = 0;
            switch (op) {
                case ADD:
                    ans = Integer.parseInt(src1) + Integer.parseInt(src2);
                    break;
                case SUB:
                    ans = Integer.parseInt(src1) - Integer.parseInt(src2);
                    break;
                case DIV:
                    ans = Integer.parseInt(src1) / Integer.parseInt(src2);
                    break;
                case MOD:
                    ans = Integer.parseInt(src1) % Integer.parseInt(src2);
                    break;
                case MUL:
                    ans = Integer.parseInt(src1) * Integer.parseInt(src2);
                    break;
            }
            if (register.allocated(rst)) {
                addCode(new LoadImm(dst,String.valueOf(ans)));
            }
            else {
                addCode(new LoadImm(reg1,String.valueOf(ans)));
                addCode(new StoreWord(reg1,dst));
            }
        }
        else if (isImm(src1) || isImm(src2)) {
            if (isImm(src1)) {
                String regSrc2 = "$a3";
                regSrc2 = loadWhenNotInReg(regSrc2,opd2);
                if (op.equals(MidCode.Operation.ADD)) {
                    addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,dst,regSrc2,src1));
                    return;
                }
                if (op.equals(MidCode.Operation.MUL)) {
                    addCode(new BinaryRegImm(Binary.BinaryInstr.MUL,dst,regSrc2,src1));
                    return;
                }
                String regSrc1 = loadWhenNotInReg("$a2",src1);
                switch (op) {
                    case SUB:
                        addCode(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,regSrc1,regSrc2));
                        break;
                    case DIV:
                        addCode(new BinaryRegReg(Binary.BinaryInstr.DIV,dst,regSrc1,regSrc2));
                        break;
                    case MOD:
                        addCode(new BinaryRegReg(Binary.BinaryInstr.MOD,dst,regSrc1,regSrc2));
                        break;
                }
            }
            else {
                String regSrc = "$a3";
                String imm = src2;
                regSrc = loadWhenNotInReg(regSrc,opd1);
                switch (op) {
                    case ADD:
                        addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,dst,regSrc,imm));
                        break;
                    case SUB:
                        addCode(new BinaryRegImm(Binary.BinaryInstr.SUB,dst,regSrc,imm));
                        break;
                    case DIV:
                        addCode(new BinaryRegImm(Binary.BinaryInstr.DIV,dst,regSrc,imm));
                        break;
                    case MOD:
                        addCode(new BinaryRegImm(Binary.BinaryInstr.MOD,dst,regSrc,imm));
                        break;
                    case MUL:
                        addCode(new BinaryRegImm(Binary.BinaryInstr.MUL,dst,regSrc,imm));
                        break;
                }
            }
        }
        else {
            String regSrc1 = "$a3";
            String regSrc2 = "$a2";
            regSrc1 = loadWhenNotInReg(regSrc1,opd1);
            regSrc2 = loadWhenNotInReg(regSrc2,opd2);
            switch (op) {
                case ADD:
                    addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,dst,regSrc1,regSrc2));
                    break;
                case SUB:
                    addCode(new BinaryRegReg(Binary.BinaryInstr.SUB,dst,regSrc1,regSrc2));
                    break;
                case DIV:
                    addCode(new Div(regSrc1,regSrc2));
                    addCode(new MoveFromLO(dst));
                    break;
                case MOD:
                    addCode(new Div(regSrc1,regSrc2));
                    addCode(new MoveFromHI(dst));
                    break;
                case MUL:
                    addCode(new Mult(regSrc1,regSrc2));
                    addCode(new MoveFromLO(dst));
                    break;
            }
        }
    }

    // **********************************  数组操作方法  *************************************

    // ARRAY_SAVE name@loc[index] opd2
    // ARRAY_LOAD opd1 name@loc[index]
    private void arrSaveOrLoad(MidCode.Operation op,String opd1,String opd2) {
        String arr;
        String valAddr;
        String val;
        boolean valInReg;
        if (op.equals(MidCode.Operation.ARRAY_SAVE)) {
            arr = opd1;
            // 存值不需要申请寄存器
            valInReg = tryAlloc(opd2,true,false);
            val = opd2;
            valAddr = getAddr(opd2);
        }
        else {
            arr = opd2;
            // 加载值需要申请
            valInReg = tryAlloc(opd1,false,true);
            val = opd1;
            valAddr = getAddr(opd1);
        }
        String name = arr.split("\\[")[0];
        String tmp = arr.split("\\[")[1];
        String arrName = arr.split("@")[0];
        String index = tmp.substring(0,tmp.length()-1);
        String arrAddr;
        // 判断函数传参是否是指针
        boolean isPtr = isParaf(arrName);
        String indexReg = "$a1";
        /* ************************** 获取数组地址 ********************************************/
        /* ************* 指针参数可能需要先加载内存地址 *************/
        if (isPtr) {
            /* ******** 判断指针地址是否在寄存器 ********/
            // 如果指针地址不在寄存器里，就加载到$a3寄存器
            String baseReg = "$a3";
            if (register.allocated(name)) {
                baseReg = getAddr(name);
            }
            else {
                addCode(new LoadWord(baseReg,getAddr(name)));
            }
            /* ******** 判断指针地址是否在寄存器 ********/

            /* ******** 根据数组下标获得内存地址 ********/
            // 如果数组下标是数字
            if (isDigit(index)) {
                int offset = 4 * Integer.parseInt(index);
                arrAddr = offset + "(" + baseReg + ")";
            }
            // 如果数组下标是变量**T1**或a,b,c
            else {
                // 如果下标在寄存器，乘4放到$a1
                if (register.allocated(index)) {
                    index = getAddr(index);
                    addCode(new BinaryRegImm(Binary.BinaryInstr.SLL,indexReg, index, "2"));
                }
                // 否则，先加载到寄存器，再乘4
                else {
                    index = getAddr(index);
                    addCode(new LoadWord(indexReg,index));
                    addCode(new BinaryRegImm(Binary.BinaryInstr.SLL,indexReg, indexReg, "2"));
                }
                // 地址 = base + index * 4
                addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,indexReg,baseReg,indexReg));
                // lw $t0,0($a1)
                arrAddr = "0(" + indexReg + ")";
            }
            /* ******** 根据数组下标获得内存地址 *******/
        }
        /* ************* 指针参数可能需要先加载内存地址 *************/

        /* ************* 变量直接对内存地址操作即可 ****************/
        else {
            boolean isGlobal = isGlobalVar(name);
            // 如果数组下标是数字
            if (isDigit(index)) {
                int offset = 4 * Integer.parseInt(index);
                if (isGlobal) {
                    arrAddr = "_" + name.split("@")[0] + "_" + "+" + offset + "($zero)";
                }
                // 在内存还要加数组的base，然后通过$sp寄存器获取
                else {
                    Symbol symbol = curTable.getSym(name.split("@")[0]);
                    offset += symbol.getAddress() + spOffset;
                    arrAddr = offset + "($sp)";
                }
            }
            // 数组下标是变量
            else {
                // 如果下标在寄存器，乘4放到$a1
                if (register.allocated(index)) {
                    index = getAddr(index);
                    addCode(new BinaryRegImm(Binary.BinaryInstr.SLL,indexReg, index, "2"));
                }
                // 否则，先加载到寄存器，再乘4
                else {
                    index = getAddr(index);
                    addCode(new LoadWord(indexReg,index));
                    addCode(new BinaryRegImm(Binary.BinaryInstr.SLL,indexReg, indexReg, "2"));
                }
                // 获取数组地址,如果.data段，全局数组lw $a0,array($a1)
                if (isGlobal) {
                    arrAddr = "_" + name.split("@")[0] + "_" + "(" + indexReg + ")";
                }
                // 如果在内存，先把$sp指针存到$a1,再把数组地址加上
                else {
                    arrAddr = "0(" + indexReg + ")";
                    addCode(new BinaryRegReg(Binary.BinaryInstr.ADD,indexReg,indexReg,"$sp"));
                    Symbol symbol = curTable.getSym(name.split("@")[0]);
                    addCode(new BinaryRegImm(Binary.BinaryInstr.ADD,indexReg,indexReg,String.valueOf(symbol.getAddress() + spOffset)));
                }
            }
        }
        /* ************************** 获取数组地址 ********************************************/

        /* ************************** 正式操作数组 ********************************************/
        String tmpReg = "$a2";
        if (op.equals(MidCode.Operation.ARRAY_SAVE)) {
            // sw $t1,100($sp)--变量指针, sw $t1,0($a1)--参数指针
            String valReg = loadWhenNotInReg(tmpReg,val);
            addCode(new StoreWord(valReg,arrAddr));
        }
        else {
            // 加载值到寄存器
            // lw $t1,100($sp)
            if (valInReg) {
                addCode(new LoadWord(valAddr,arrAddr));
            }
            // 加载值到内存
            else {
                addCode(new LoadWord(tmpReg,arrAddr));
                addCode(new StoreWord(tmpReg,arrAddr));
            }
        }
    }

    // **********************************  判断类方法以及指令添加(不太会改)  **********************************************

    public MidCode getIcode() {
        return icodes.getIcode(index++);
    }

    private int getCurSp() {
        int rst = 0;
        for (Integer integer:funcSizeStack) {
            rst += integer;
        }
        return rst;
    }

    private void goBack(int cnt) {
        index -= cnt;
    }

    private void goBack() {
        index--;
    }

    private boolean hasNext() {
        return icodes.hasNext(index);
    }

    private void addCode(Instruction instr) {
        instrs.add(instr);
        if (twoOpInstr(instr) || threeOpInstr(instr)) {
            if (!instr.getReg2().equals(instr.getDst()))
                freeReg(instr.getReg2());
            if (!instr.getReg1().equals(instr.getDst()))
                freeReg(instr.getReg1());
        }
    }

    private void addCodeWithoutFree(Instruction instr) {
        instrs.add(instr);
    }

    private boolean twoOpInstr(Instruction instr) {
        return instr instanceof LoadWord ||
                instr instanceof Move ||  instr instanceof Mult || instr instanceof Div;
    }

    private boolean threeOpInstr(Instruction instr) {
        return instr instanceof Binary || instr instanceof SetInstr;
    }

    public boolean isGlobalVar(String name) {
        Symbol symbol = globalTable.getSym(name.split("@")[0]);
        // 还要判断MidName是否相同。
        return symbol != null && symbol.getMidName().equals(name.split("\\[")[0]);
    }

    private boolean isBinaryOp(MidCode.Operation op) {
        return binaryOp.contains(op);
    }
    
    private boolean isParaf(String name) {
        Symbol symbol = curTable.getSym(name);
        return symbol == null || symbol.isPara();
    }

    private boolean isDigit(String name) {
        return name.matches("[0-9]*") || name.charAt(0) == '-' || name.charAt(0) == '+';
    }

    private boolean isImm(String name) {
        // 不是内存、全局和0寄存器
        return isDigit(name) && !name.endsWith("($sp)") && !name.equals("0") && !name.contains("+");
    }

    private boolean isPower(int num) {
        if (num <= 0)
            return false;
        return (num & (num - 1)) == 0;
    }

    public void icode2Mips() {
        loadStrCon();
        loadGlobalVar();
        addCode(new Label(".text"));
        loadFunc();
    }

    private int log(int num) {
        return (int) Math.floor(Math.log(num) / Math.log(2));
    }

    public void output(String path, boolean isDebug) {
        if (isDebug) {
            for (Instruction code: instrs) {
                System.out.println(code.toString());
            }
        }
        else {
            try (FileWriter fileWriter = new FileWriter(path)) {
                for (Instruction instr:instrs)
                    fileWriter.append(instr.toString()).append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
