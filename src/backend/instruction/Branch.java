package backend.instruction;

import middle.MidCode;

public class Branch implements Instruction{
    private final MidCode.Operation op;
    private final String reg1;
    private final String reg2;
    private final String label;

    public Branch(MidCode.Operation op, String reg1, String reg2, String label) {
        this.op = op;
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.label = label;
    }

    @Override
    public String toString() {
        String instr = (op.equals(MidCode.Operation.BEQ))?"beq":"bne";
        return instr + " " + reg1 + ", " + reg2 + ", " + label;
    }

    @Override
    public String getReg1() {
        return reg1;
    }

    @Override
    public String getReg2() {
        return reg2;
    }

    @Override
    public String getDst() {
        return label;
    }
}
