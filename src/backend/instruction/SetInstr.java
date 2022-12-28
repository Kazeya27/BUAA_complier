package backend.instruction;

public class SetInstr implements Instruction{
    @Override
    public String getReg1() {
        return source1;
    }

    @Override
    public String getReg2() {
        return source2;
    }

    @Override
    public String getDst() {
        return destination;
    }

    public enum LogicInstr {
        EQL,NEQ,LSS,LEQ,GRE,GEQ
    }

    protected final LogicInstr op;
    protected final String destination;
    protected final String source1;
    protected final String source2;

    public SetInstr(LogicInstr op, String destination, String source1, String source2) {
        this.op = op;
        this.destination = destination;
        this.source1 = source1;
        this.source2 = source2;
    }
}
