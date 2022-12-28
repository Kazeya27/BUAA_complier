package backend.instruction;

public class Label implements Instruction{
    private final String label;

    public Label(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
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
