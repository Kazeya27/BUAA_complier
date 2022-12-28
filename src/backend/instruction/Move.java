package backend.instruction;

public class Move implements Instruction{
    private final String dst;
    private final String src;

    public Move(String dst, String src) {
        this.dst = dst;
        this.src = src;
    }

    @Override
    public String toString() {
        return "move " + dst + ", " + src;
    }

    @Override
    public String getReg1() {
        return src;
    }

    @Override
    public String getReg2() {
        return src;
    }

    @Override
    public String getDst() {
        return dst;
    }
}
