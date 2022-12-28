package backend.instruction;

public class BinaryRegImm extends Binary{
    public BinaryRegImm(BinaryInstr op, String destination, String source1, String source2) {
        super(op, destination, source1, source2);
    }

    private String getInstr() {
        switch (op) {
            case ADD: return "addiu";
            case SUB: return "subiu";
            case MUL: return "mul";
            case DIV: return "div";
            case MOD: return "rem";
            case SLL: return "sll";
            case SRA: return "sra";
            case SRL: return "srl";

        }
        return "error";
    }

    private boolean isPower(int num) {
        return (num & (num - 1)) == 0;
    }

    private String log(int num) {
        return Integer.toString((int) Math.floor(Math.log(num) / Math.log(2)));
    }

    @Override
    public String toString() {
        if (isPower(Math.abs(Integer.parseInt(source2)))) {
            int num = Integer.parseInt(source2);
            if (op.equals(BinaryInstr.MUL)) {
                if (num > 0) {
                    return "sll " + destination + ", " + source1 + ", " + log(num);
                }
                else {
                    String instr = "sll " + destination + ", " + source1 + ", " + log(Math.abs(num)) + "\n";
                    instr += "subu " + destination + ", $zero, " + destination;
                    return instr;
                }
            }
            else if (op.equals(BinaryInstr.DIV)) {
                if (num > 0) {

                }
            }
        }
        return getInstr() + " " + destination + ", " + source1 + ", " + source2;
    }
}
