package backend.instruction;

public class Mult implements Instruction{
    private final String reg1;
    private final String reg2;

    public Mult(String reg1, String reg2) {
        this.reg1 = reg1;
        this.reg2 = reg2;
    }

    @Override
    public String toString() {
        return "mult " + reg1 + ", " + reg2;
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
        return null;
    }
}
