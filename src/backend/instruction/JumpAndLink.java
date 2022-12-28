package backend.instruction;

public class JumpAndLink implements Instruction{
    public final String label;

    public JumpAndLink(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "jal " + label;
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
