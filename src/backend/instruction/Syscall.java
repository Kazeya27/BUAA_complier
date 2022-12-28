package backend.instruction;

public class Syscall implements Instruction{
    @Override
    public String toString() {
        return "syscall";
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
