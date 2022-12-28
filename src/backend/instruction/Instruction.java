package backend.instruction;

public interface Instruction {
    String toString();
    String getReg1();
    String getReg2();
    String getDst();
}
