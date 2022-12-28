package backend.instruction;

public class LoadImm implements Instruction{
    private final String reg;
    private final String imm;

    public LoadImm(String reg, String imm) {
        this.reg = reg;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "li " + reg + ", " + imm;
    }

    @Override
    public String getReg1() {
        return imm;
    }

    @Override
    public String getReg2() {
        return imm;
    }

    @Override
    public String getDst() {
        return reg;
    }
}
