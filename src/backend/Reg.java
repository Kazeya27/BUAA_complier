package backend;

public class Reg implements Cloneable{
    public enum State implements Cloneable{
        FREE,USED;
    }

    private State state;
    private String name;
    private int id;
    private final String regName;

    public Reg(State state, int id, String regName) {
        this.state = state;
        this.id = id;
        this.regName = regName;
    }

    public Reg(String name, String regName) {
        this.name = name;
        this.regName = regName;
    }

    public String getRegName() {
        return regName;
    }

    public State getStatus() {
        return state;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(State state) {
        this.state = state;
    }

    public void copy(Reg reg) {
        this.state = reg.state;
        this.name = reg.name;
    }
}
