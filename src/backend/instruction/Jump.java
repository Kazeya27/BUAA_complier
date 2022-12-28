package backend.instruction;

public class Jump implements Instruction{
    public final String label;

    public Jump(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "j " + label;
    }

    @Override
    public String getReg1() {
        return null;
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
