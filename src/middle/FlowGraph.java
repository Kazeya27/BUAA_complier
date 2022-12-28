package middle;

import frontend.error.Symbol;
import frontend.error.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FlowGraph {
    private static FlowGraph instance;
    private HashMap<BasicBlock, List<BasicBlock>> nextBlocks = new HashMap<>();
    private HashMap<BasicBlock, List<BasicBlock>> prevBlocks = new HashMap<>();
    private HashMap<Integer,BasicBlock> bid2Block = new HashMap<>();
    private ArrayList<BasicBlock> blocks = new ArrayList<>();
    private HashMap<String,HashSet<Integer>> defPoints = new HashMap<>();
    private HashMap<Integer,MidCode> codeMap = new HashMap<>();
    private final SymbolTable globalTable;

    private FlowGraph(){
        globalTable = SymbolTable.getGlobal();
    }

    public static FlowGraph getInstance() {
        if (instance == null)
            instance = new FlowGraph();
        return instance;
    }

    private void getDefPoints(Intermediate icodes) {
        int index = 0;
        for (MidCode icode:icodes.codes) {
            String defVar = icode.getDefVar();
            if (defVar != null && !defVar.startsWith("*")) {
                icode.setDefIndex(index);
                codeMap.put(index++,icode);
                if (!defPoints.containsKey(defVar)) {
                    defPoints.put(defVar,new HashSet<>());
                }
                else {
                    defPoints.get(defVar).add(icode.getDefIndex());
                }
            }
        }
    }

    private void divideBlocks(Intermediate icodes) {
        int curBid = 0;
        HashMap<MidCode, Integer> belong = new HashMap<>();
        for (MidCode icode:icodes.codes) {
            if (icode.isLabel()) {
                belong.put(icode,++curBid);
            }
            else if (icode.isJump()) {
                belong.put(icode,curBid++);
            }
            else {
                belong.put(icode,curBid);
            }
        }
        curBid = -1;
        BasicBlock curBlock = null;
        ArrayList<BasicBlock> tmpBlocks = new ArrayList<>();
        for (MidCode icode: icodes.codes) {
            if (belong.get(icode) != curBid) {
                curBid = belong.get(icode);
                curBlock = new BasicBlock(curBid);
                bid2Block.put(curBid,curBlock);
                tmpBlocks.add(curBlock);
            }
            curBlock.addCode(icode);
        }
        for (BasicBlock block:tmpBlocks) {
            if (!block.isEmpty()) {
                blocks.add(block);
            }
        }
        if (!blocks.get(0).getICode(0).getOperation().equals(MidCode.Operation.FUNC)) {
            blocks.remove(0);
        }
    }

    private void constructFlow() {
        HashMap<String, BasicBlock> nameMap = new HashMap<>();
        for (BasicBlock block:blocks) {
            // dddif empty
            if (block.getICode(0).isLabel()) {
                nameMap.put(block.getICode(0).getOperand1(),block);
            }
        }

        for (int i = 0;i<blocks.size();i++) {
            BasicBlock block = blocks.get(i);
            BasicBlock nextBlock = null;
            if (i < blocks.size()-1) {
                nextBlock = blocks.get(i+1);
            }
            if (!block.isEmpty()) {
                MidCode exit = block.getLastICode();
                if (exit.getOperation().equals(MidCode.Operation.JUMP)) {
                    // jump的后继块是jump指令的目标标签
                    nextBlock = nameMap.get(exit.getOperand1());
                    link(block,nextBlock);
                }
                else if (exit.getOperation().equals(MidCode.Operation.BEQ) || exit.getOperation().equals(MidCode.Operation.BNE)) {
                    // beq 和 bne 在不满足条件时，后继就是下一个基本块
                    if (nextBlock != null) {
                        link(block,nextBlock);
                    }
                    // 同时，后继也是跳转的目标标签
                    nextBlock = nameMap.get(exit.getResult());
                    link(block,nextBlock);
                }
                else if (nextBlock == null || exit.getOperation().equals(MidCode.Operation.RETURN)) {
                    // 函数返回是流的终点
                    block.addNextBlock(-1);
                }
                else if (exit.getOperation().equals(MidCode.Operation.CALL)) {
                    block.addNextBlock(-1);
                    nameMap.get(exit.getOperand1()).addPrevBlock(0);
                }
                else {
                    link(block,nextBlock);
                }
            }
        }
    }

    private void link(BasicBlock block,BasicBlock nextBlock) {
        block.addNextBlock(nextBlock.id);
        nextBlock.addPrevBlock(block.id);
        addNextBlock(block,nextBlock);
        addPrevBlock(nextBlock,block);
    }

    private void addPrevBlock(BasicBlock key,BasicBlock prevBlock) {
        if (!prevBlocks.containsKey(key)) {
            prevBlocks.put(key,new ArrayList<>());
        }
        prevBlocks.get(key).add(prevBlock);
    }

    private void addNextBlock(BasicBlock key,BasicBlock nextBlock) {
        if (!nextBlocks.containsKey(key)) {
            nextBlocks.put(key,new ArrayList<>());
        }
        nextBlocks.get(key).add(nextBlock);
    }

    private void fresh(Intermediate ir) {
        nextBlocks = new HashMap<>();
        prevBlocks = new HashMap<>();
        bid2Block = new HashMap<>();
        blocks = new ArrayList<>();
        defPoints = new HashMap<>();
        codeMap = new HashMap<>();
        for (MidCode icode:ir.codes) {
            icode.setDefIndex(-1);
        }
    }

    private boolean isChangedDU(HashSet<String> old, HashSet<String> n) {
        HashSet<String> oldMinusNew = new HashSet<>(old);
        oldMinusNew.removeAll(n);
        HashSet<String> newMinusOld = new HashSet<>(n);
        newMinusOld.removeAll(old);
        return !(oldMinusNew.isEmpty() && newMinusOld.isEmpty());
    }

    private boolean isChangedGK(HashSet<Integer> old, HashSet<Integer> n) {
        HashSet<Integer> oldMinusNew = new HashSet<>(old);
        oldMinusNew.removeAll(n);
        HashSet<Integer> newMinusOld = new HashSet<>(n);
        newMinusOld.removeAll(old);
        return !(oldMinusNew.isEmpty() && newMinusOld.isEmpty());
    }

    private void addDefUse() {
        for (BasicBlock block:blocks) {
            block.generateDefUse();
        }
    }

    private void addGenKill() {
        for (BasicBlock block:blocks) {
            block.generateGenKill();
        }
    }

    private void getLive() {
        boolean isChanged = false;
        do {
            isChanged = false;
            // 逆向活跃变量分析
            for (int i = blocks.size()-1;i>=0;i--) {
                BasicBlock block = blocks.get(i);
                // out = ∪后继in
                List<BasicBlock> nexts = nextBlocks.get(block);
                // 等于Null表示下一个块是exit
                if (nexts != null) {
                    for (BasicBlock nextBlock:nexts) {
                        block.addLiveOut(nextBlock.getLiveIn());
                    }
                }
                // in = use ∪ (out - def)
                HashSet<String> in = new HashSet<>(block.getUse());
                HashSet<String> out = new HashSet<>(block.getLiveOut());
                out.removeAll(block.getDef());
                in.addAll(out);
                if (isChangedDU(block.getLiveIn(),in)) {
                    isChanged = true;
                    block.setLiveIn(in);
                }
            }
        } while(isChanged);
    }

    private void getReach() {
        boolean isChanged = false;
        do {
            isChanged = false;
            // 正向可达定义分析
            for (BasicBlock block:blocks) {
                // in = 前继∪out
                List<BasicBlock> prevs = prevBlocks.get(block);
                if (prevs != null) {
                    for (BasicBlock prevBlock:prevs) {
                        block.addReachIn(prevBlock.getReachOut());
                    }
                }
                // out = gen ∪ (in - kill)
                HashSet<Integer> out = new HashSet<>(block.getGen());
                HashSet<Integer> in = new HashSet<>(block.getReachIn());
                in.removeAll(block.getKill());
                out.addAll(in);
                if (isChangedGK(block.getReachOut(),out)) {
                    isChanged = true;
                    block.setReachOut(out);
                }
            }
        } while (isChanged);
    }

    private void deadCodeEliminate() {
        for (BasicBlock block:blocks) {
            block.deadCodeElimination();
        }
    }

    private void constantPropagation() {
        for (BasicBlock block:blocks) {
            block.constantPropagation();
        }
    }

    private void optimizePre(Intermediate ir) {
        getDefPoints(ir);
        divideBlocks(ir);
        constructFlow();
        addDefUse();
        getLive();
        addGenKill();
        getReach();
    }

    private void constantMerge(MidCode icode) {
        if (icode.isDigit(icode.getOperand1()) && icode.isDigit(icode.getOperand2())) {
            int ans;
            int opd1 = Integer.parseInt(icode.getOperand1());
            int opd2 = Integer.parseInt(icode.getOperand2());
            switch (icode.getOperation()) {
                case ADD:
                    ans = opd1 + opd2;
                    break;
                case SUB:
                    ans = opd1 - opd2;
                    break;
                case MUL:
                    ans = opd1 * opd2;
                    break;
                case MOD:
                    ans = opd1 % opd2;
                    break;
                case DIV:
                    ans = opd1 / opd2;
                    break;
                default:
                    return;
            }
            icode.setOperation(MidCode.Operation.ASSIGN);
            icode.setOperand1(icode.getResult());
            icode.setOperand2(String.valueOf(ans));
        }
    }

    //       add a b 0, multi a c 1
    //       multi a 1 c, add a 0 b
    private void uselessUnary(MidCode icode) {
        switch (icode.getOperation()) {
            case ADD:
                if (icode.getOperand1().equals("0")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand1(icode.getResult());
                }
                else if (icode.getOperand2().equals("0")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand2(icode.getOperand1());
                    icode.setOperand1(icode.getResult());
                }
                break;
            case SUB:
                if (icode.getOperand2().equals("0")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand2(icode.getOperand1());
                    icode.setOperand1(icode.getResult());
                }
                break;
            case MUL:
                if (icode.getOperand1().equals("0")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand1(icode.getResult());
                    icode.setOperand2("0");
                }
                else if (icode.getOperand2().equals("0")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand1(icode.getResult());
                }
                else if (icode.getOperand1().equals("1")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand1(icode.getResult());
                }
                else if (icode.getOperand2().equals("1")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand2(icode.getOperand1());
                    icode.setOperand1(icode.getResult());
                }
                break;
            case DIV:
                if (icode.getOperand2().equals("1")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand2(icode.getOperand1());
                    icode.setOperand1(icode.getResult());
                }
                else if (icode.getOperand1().equals("0")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand2("0");
                    icode.setOperand1(icode.getResult());
                }
                break;
            case MOD:
                if (icode.getOperand2().equals("1")) {
                    icode.setOperation(MidCode.Operation.ASSIGN);
                    icode.setOperand2("0");
                    icode.setOperand1(icode.getResult());
                }
                break;
            default:
                return;
        }
    }



    private void peephole(Intermediate ir) {
        for (MidCode icode:ir.codes) {
            constantMerge(icode);
            uselessUnary(icode);
        }
        // indirectAssign(ir);
    }

    public void optimize(Intermediate ir) {
        for (int i = 0;i<20;i++) {
            fresh(ir);
            peephole(ir);
            optimizePre(ir);
            deadCodeEliminate();
            constantPropagation();
        }
    }

    public HashMap<String, HashSet<Integer>> getDefPoints() {
        return defPoints;
    }

    public HashMap<Integer, MidCode> getCodeMap() {
        return codeMap;
    }

    public HashMap<BasicBlock, List<BasicBlock>> getNextBlocks() {
        return nextBlocks;
    }

    public HashMap<BasicBlock, List<BasicBlock>> getPrevBlocks() {
        return prevBlocks;
    }

    public ArrayList<BasicBlock> getBlocks() {
        return blocks;
    }

    public boolean isGlobalVar(String name) {
        Symbol symbol = globalTable.getSym(name.split("@")[0]);
        // 还要判断MidName是否相同。
        return symbol != null && symbol.getMidName().equals(name.split("\\[")[0]);
    }
}

