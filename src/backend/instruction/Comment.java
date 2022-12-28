package backend.instruction;

public class Comment implements Instruction{
    private final String s;

    public Comment(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return "# " + s;
    }

    @Override
    public String getReg1() {
        return s;
    }

    @Override
    public String getReg2() {
        return s;
    }

    @Override
    public String getDst() {
        return s;
    }
}
