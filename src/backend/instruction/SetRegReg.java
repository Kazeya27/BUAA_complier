package backend.instruction;

public class SetRegReg extends SetInstr{
    public SetRegReg(LogicInstr op, String destination, String source1, String source2) {
        super(op, destination, source1, source2);
    }

    private String getInstr() {
        switch (op) {
            case EQL: return "seq";
            case NEQ: return "sne";
            case LSS: return "slt";
            case LEQ: return "sle";
            case GEQ: return "sge";
            case GRE: return "sgt";
        }
        return "error";
    }

    @Override
    public String toString() {
        return getInstr() + " " + destination + ", " + source1 + ", " + source2;
    }
}
