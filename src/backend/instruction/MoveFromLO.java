package backend.instruction;

public class MoveFromLO implements Instruction{
    private final String dst;

    public MoveFromLO(String dst) {
        this.dst = dst;
    }

    @Override
    public String toString() {
        return "mflo " + dst;
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
