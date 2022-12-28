package backend.instruction;

public class BGTZ implements Instruction{

    String reg1;
    String label;

    public BGTZ(String reg1, String label) {
        this.reg1 = reg1;
        this.label = label;
    }

    @Override
    public String getReg1() {
        return reg1;
    }

    @Override
    public String getReg2() {
        return reg1;
    }

    @Override
    public String getDst() {
        return label;
    }

    @Override
    public String toString() {
        return "bgtz " + reg1 + ", " + label;
    }
}
