package backend;

import java.util.LinkedHashSet;
import java.util.LinkedList;

public class Scheduler {
    private VarRegMap varRegMap;
    private final LinkedHashSet<Reg> cache = new LinkedHashSet<>();
    private final LinkedList<Reg> tRegs = new LinkedList<>();

}
