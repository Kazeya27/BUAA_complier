package middle;

import java.util.*;

public class BasicBlock {
    public int id;
    private final ArrayList<MidCode> icodes = new ArrayList<>();
    private final ArrayList<Integer> prevBlocks = new ArrayList<>();
    private final ArrayList<Integer> nextBlocks = new ArrayList<>();
    private final HashSet<String> use = new HashSet<>();
    private final HashSet<String> def = new HashSet<>();
    private HashSet<String> liveIn = new HashSet<>();  // 使用def、use，从后往前分析
    private final HashSet<String> liveOut = new HashSet<>();
    private final HashSet<Integer> gen = new HashSet<>();
    private final HashSet<Integer> kill = new HashSet<>();
    private final HashSet<Integer> reachIn = new HashSet<>();
    private HashSet<Integer> reachOut = new HashSet<>();
    private final ArrayList<MidCode> labels = new ArrayList<>();
    private final ArrayList<MidCode> jumps = new ArrayList<>();

    public BasicBlock(int id) {
        this.id = id;
    }

    public void addLiveOut(HashSet<String> midNames) {
        liveOut.addAll(midNames);
    }

    public void addReachIn(HashSet<Integer> indexs) {
        reachIn.addAll(indexs);
    }

    public void addDef(String midName) {
        def.add(midName);
    }

    public void addUse(String midName) {
        use.add(midName);
    }

    public void addCode(MidCode icode) {
        if (icode.isLabel()) {
            addLabel(icode);
        }
        else if (icode.isJump()) {
            addJump(icode);
        }
        icodes.add(icode);
    }

    public void addLabel(MidCode icode) {
        labels.add(icode);
    }

    public void addJump(MidCode icode) {
        jumps.add(icode);
    }

    public void addPrevBlock(int id) {
        prevBlocks.add(id);
    }

    public void addNextBlock(int id) {
        nextBlocks.add(id);
    }

    public ArrayList<MidCode> getIcodes() {
        return icodes;
    }

    public ArrayList<Integer> getPrevBlocks() {
        return prevBlocks;
    }

    public ArrayList<Integer> getNextBlocks() {
        return nextBlocks;
    }

    public MidCode getICode(int index) {
        return icodes.get(index);
    }

    public MidCode getLastICode() {
        return icodes.get(icodes.size()-1);
    }

    public boolean isEmpty() {
        return icodes.isEmpty();
    }

    public boolean isFuncEntry() {
        if (isEmpty()) {
            return false;
        }
        return icodes.get(0).getOperation().equals(MidCode.Operation.FUNC);
    }

    public boolean isFuncEnd() {
        for (MidCode icode:icodes) {
            if (icode.getOperation().equals(MidCode.Operation.FUNC_END)) {
                return true;
            }
        }
        return false;
    }

    public String getFuncName() {
        return icodes.get(0).getOperand1();
    }

    public HashSet<String> getLiveIn() {
        return liveIn;
    }

    public HashSet<String> getUse() {
        return use;
    }

    public HashSet<String> getDef() {
        return def;
    }

    public HashSet<Integer> getGen() {
        return gen;
    }

    public HashSet<Integer> getKill() {
        return kill;
    }

    public HashSet<String> getLiveOut() {
        return liveOut;
    }

    public HashSet<Integer> getReachOut() {
        return reachOut;
    }

    public HashSet<Integer> getReachIn() {
        return reachIn;
    }

    public void setReachOut(HashSet<Integer> reachOut) {
        this.reachOut = reachOut;
    }

    public void setLiveIn(HashSet<String> liveIn) {
        this.liveIn = liveIn;
    }

    // 进行死代码删除的时候，如果一条语句**没有副作用**，而且它的赋值目标(如果有的话)不在$out_S$中，那么这条语句就可以删去
    // 所谓的副作用，其实就是除了"改变赋值目标变量"之外，其他所有的作用。
    // 实现的时候可以认为除了`a = call b`之外的所有有赋值目标的语句都是没有副作用的，
    public void deadCodeElimination() {
        HashSet<String> usefulVar = new HashSet<>(liveOut);
        // 逆序删除
        // a = b + c;
        // d = a + c;
        // 保证a被标记为有用的变量
        for (int i = icodes.size() - 1;i>=0;i--) {
            MidCode icode = icodes.get(i);
            String defVar = icode.getDefVar();
            // 如果修改的变量无用
            if (defVar != null && !usefulVar.contains(defVar)) {
                if (icode.getOperation().equals(MidCode.Operation.GETINT)) {
                    icode.setOperation(MidCode.Operation.INVALID_GI);
                }
                else {
                    icode.setOperation(MidCode.Operation.INVALID);
                }
            }
            else {
                // 如果产生了def，之前的def可能是无用的，需要消除
                // a = b + c; // 该代码为死代码
                // a = c + d;
                // e = a + c;
                if (defVar != null) {
                    usefulVar.remove(defVar);
                }
                usefulVar.addAll(icode.getUseVar());
            }
        }
    }

    public void constantPropagation() {
        localConstantPropagation();
        globalConstantPropagation();
    }

    private void localConstantPropagation() {
        // 值确定的变量
        HashMap<String,String> constants = new HashMap<>();
        for (MidCode icode:icodes) {
            HashMap<String,String> values = new HashMap<>();
            HashSet<String> useVars = icode.getUseVar();
            // 如果代码使用到的变量值确定，就可以传播
            for (String useVar:useVars) {
                if (constants.containsKey(useVar)) {
                    values.put(useVar,constants.get(useVar));
                }
            }
            icode.constantReplace(values);
            // 使用后从哈希表中删除
            String defVar = icode.getDefVar();
            constants.remove(defVar);
            if (defVar != null && icode.isConst()) {
                // 如果传播之后值如果确定，加入哈希表
                constants.put(defVar,icode.getOperand2());
            }
        }
    }

    private void globalConstantPropagation() {
        for (MidCode icode:icodes) {
            // 记录可以进行常量传播的变量
            HashMap<String,String> values = new HashMap<>();
            HashSet<String> useVars = icode.getUseVar();
            for (String useVar:useVars) {
                List<Integer> defPoints = new ArrayList<>();
                for (Integer defPoint:reachIn) {
                    // 从in得到各个变量定义点，如果包含useVar，进行常量传播
                    String defVar = FlowGraph.getInstance().getCodeMap().get(defPoint).getDefVar();
                    if (useVar.equals(defVar)) {
                        defPoints.add(defPoint);
                    }
                }
                if (defPoints.size() != 1)
                    continue;
                HashSet<String> blockDefVars = new HashSet<>();
                // 定义点是否和当前代码在同一个block,同一个block下将在块内常量传播完成
                boolean diffBlock = true;
                for (MidCode icode2:icodes) {
                    if (icode2.equals(icode)) {
                        // 在块内该代码之前定义过，说明在同一个块中
                        if (blockDefVars.contains(useVar)) {
                            diffBlock = false;
                        }
                        break;
                    }
                    // 如果没到icode，且该代码是有效定义点，加入到blockDefVars
                    else if (icode2.isValidDefPoint()) {
                        blockDefVars.add(icode2.getDefVar());
                    }
                }
                if (diffBlock) {
                    MidCode defCode = FlowGraph.getInstance().getCodeMap().get(defPoints.get(0));
                    // 定义点的代码是常量才进行传播
                    if ((defCode.isConst())) {
                        values.put(useVar,defCode.getOperand2());
                    }
                }
            }
            icode.constantReplace(values);
        }
    }

    public void generateDefUse() {
        // 顺序构造def和use集
        for (MidCode icode:icodes) {
            String defVar = icode.getDefVar();
            HashSet<String> useVars = icode.getUseVar();
            // 被定义先于使用
            if (defVar != null && !use.contains(defVar) && !useVars.contains(defVar)) {
                def.add(defVar);
            }
            // 被使用先于定义
            // a = a + b a在use中
            for (String useVar:useVars) {
                if (useVar != null && !def.contains(useVar)) {
                    use.add(useVar);
                }
            }
        }
    }

    // kill[block] = kill[d1] ∪ kill[d2] ∪ kill[d3] ∪ ...
    // gen[block] = gen[dn] ∪ (gen[d(n-1)] - kill[dn]) ∪
    //              (gen[d(n-2)] - kill[d(n-1)] - kill[dn])
    // 后面的gen把前面的gen覆盖了
    public void generateGenKill() {
        // 逆序构造gen和kill集，前面的gen集取决于后面的kill集
        for (int i = icodes.size()-1;i>=0;i--) {
            MidCode icode = icodes.get(i);
            if (icode.getDefIndex() != -1) {
                if (!kill.contains(icode.getDefIndex())) {
                    gen.add(icode.getDefIndex());
                }
                kill.addAll(icode.getKill());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock block = (BasicBlock) o;
        return id == block.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
