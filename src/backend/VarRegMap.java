package backend;

import frontend.error.SymbolTable;
import middle.BasicBlock;
import middle.FlowGraph;
import middle.MidCode;

import java.util.*;

public class VarRegMap {
    private ConflictGraph conflictGraph = new ConflictGraph();
    private FlowGraph flowGraph;
    private HashMap<String,Reg> curVar2Reg = new HashMap<>();
    private ArrayList<BasicBlock> blocks;
    private HashMap<String,HashMap<String,Reg>> funcVar2Reg = new HashMap<>();

    public VarRegMap(FlowGraph flowGraph) {
        this.flowGraph = flowGraph;
        blocks = flowGraph.getBlocks();
    }

    private void generateConflictGraph() {
        for (BasicBlock block:blocks) {
            if (block.isFuncEntry()) {
                curVar2Reg = new HashMap<>();
                funcVar2Reg.put(block.getFuncName(),curVar2Reg);
            }
            if (block.isFuncEnd()) {
                color();
            }
            HashSet<String> activeOut = new HashSet<>(block.getLiveOut());
            List<MidCode> icodes = block.getIcodes();
            for (int i = icodes.size()-1;i>=0;i--) {
                MidCode icode = icodes.get(i);
                String defVar = icode.getDefVar();
                HashSet<String> useVars = icode.getUseVar();
                if (defVar != null) {
                    conflictGraph.addNode(defVar);
                    for (String out:activeOut) {
                        conflictGraph.link(defVar,out);
                    }
                    activeOut.remove(defVar);
                }
                activeOut.addAll(useVars);
            }
        }
    }

    private void color() {
        int regNum = Register.SREG.size();
        Stack<String> stack = new Stack<>();
        Stack<HashSet<String>> connectNodes = new Stack<>();
        while (!conflictGraph.isEmpty()) {
            String var = conflictGraph.getMinDegVar();
            if (conflictGraph.getDeg(var) >= regNum) {
                var = conflictGraph.getMaxDegVar();
            }
            stack.push(var);
            connectNodes.push(conflictGraph.getCopySet(var));
            conflictGraph.removeNode(var);
        }
        while (!stack.isEmpty()) {
            String var = stack.pop();
            HashSet<String> nodes = connectNodes.pop();
            ArrayList<Reg> regs = new ArrayList<>(Register.SREG);
            for (String node:nodes) {
                regs.remove(curVar2Reg.get(node));
            }
            if (!regs.isEmpty()) {
                curVar2Reg.put(var,regs.get(0));
            }
        }
    }

    public Reg getReg(String funcName,String varName) {
        return funcVar2Reg.get(funcName).get(varName);
    }


}
