package backend.instruction;

public class BinaryRegReg extends Binary {
    public BinaryRegReg(BinaryInstr op, String destination, String source1, String source2) {
        super(op, destination, source1, source2);
    }

    private String getInstr() {
        switch (op) {
            case ADD: return "addu";
            case SUB: return "subu";
            case MUL: return "mul";
            case DIV: return "div";
            case MOD: return "rem";
            case SLL: return "sll";
        }
        return "error";
    }

    @Override
    public String toString() {
        return getInstr() + " " + destination + ", " + source1 + ", " + source2;
    }
}
