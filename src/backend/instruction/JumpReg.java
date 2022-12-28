package backend.instruction;

public class JumpReg implements Instruction{
    private final String reg;

    public JumpReg(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return "jr " + reg;
    }

    @Override
    public String getReg1() {
        return reg;
    }

    @Override
    public String getReg2() {
        return null;
    }

    @Override
    public String getDst() {
        return null;
    }
}
