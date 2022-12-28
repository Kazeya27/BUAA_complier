package backend.instruction;

public class StoreWord implements Instruction{
    private final String source;
    private final String addr;

    public StoreWord(String source, String addr) {
        this.source = source;
        this.addr = addr;
    }

    @Override
    public String toString() {
        return "sw " + source + ", " + addr;
    }

    @Override
    public String getReg1() {
        return source;
    }

    @Override
    public String getReg2() {
        return source;
    }

    @Override
    public String getDst() {
        return addr;
    }
}
