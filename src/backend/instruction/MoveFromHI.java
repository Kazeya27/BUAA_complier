package backend.instruction;

public class MoveFromHI implements Instruction{
    private final String dst;

    public MoveFromHI(String dst) {
        this.dst = dst;
    }

    @Override
    public String toString() {
        return "mfhi " + dst;
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
        return dst;
    }
}
