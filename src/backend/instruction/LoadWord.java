package backend.instruction;

import backend.Reg;

public class LoadWord implements Instruction{
    private final String dst;
    private final String addr;

    public LoadWord(String dst, String addr) {
        this.dst = dst;
        this.addr = addr;
    }

    @Override
    public String toString() {
        return "lw " + dst + ", " + addr;
    }

    @Override
    public String getReg1() {
        return addr;
    }

    @Override
    public String getReg2() {
        return addr;
    }

    @Override
    public String getDst() {
        return dst;
    }
}
