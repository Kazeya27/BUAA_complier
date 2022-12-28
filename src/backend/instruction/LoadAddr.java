package backend.instruction;

public class LoadAddr implements Instruction{
    private final String reg;
    private final String label;

    public LoadAddr(String reg, String label) {
        this.reg = reg;
        this.label = label;
    }

    @Override
    public String toString() {
        return "la " + reg + ", " + label;
    }

    @Override
    public String getReg1() {
        return label;
    }

    @Override
    public String getReg2() {
        return label;
    }

    @Override
    public String getDst() {
        return reg;
    }
}
