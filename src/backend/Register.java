package backend;

import java.util.*;

public class Register {
    public static final Collection<Reg> SREG = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            new Reg(Reg.State.FREE,0,"$s0"),
            new Reg(Reg.State.FREE,1,"$s1"),
            new Reg(Reg.State.FREE,2,"$s2"),
            new Reg(Reg.State.FREE,3,"$s3"),
            new Reg(Reg.State.FREE,4,"$s4"),
            new Reg(Reg.State.FREE,5,"$s5"),
            new Reg(Reg.State.FREE,6,"$s6"),
            new Reg(Reg.State.FREE,7,"$s7"),
            new Reg(Reg.State.FREE,8,"$fp"),
            new Reg(Reg.State.FREE,9,"$k0"),
            new Reg(Reg.State.FREE,10,"$k1"),
            new Reg(Reg.State.FREE,11,"$gp")
    )));
    private final ArrayList<Reg> sRegs = new ArrayList<>();
    private final ArrayList<Reg> tRegs = new ArrayList<>();
    private final ArrayList<Reg> aRegs = new ArrayList<>();
    private final HashMap<String, Reg> name2Reg = new HashMap<String, Reg>(){
        {
            put("*RST*",new Reg(Reg.State.USED,0,"$v0"));
            put("0",new Reg(Reg.State.USED,0,"$zero"));
        }
    };

    private final LinkedHashSet<Reg> cache = new LinkedHashSet<>();

    public Register() {
        for (int i = 0;i<=3;i++)
            aRegs.add(new Reg(Reg.State.FREE,i,"$a"+i));
        for (int i = 0;i<8;i++) {
            sRegs.add(new Reg(Reg.State.FREE,i,"$s"+i));
            tRegs.add(new Reg(Reg.State.FREE,i,"$t"+i));
        }
        sRegs.add(new Reg(Reg.State.FREE,8,"$fp"));
        tRegs.add(new Reg(Reg.State.FREE,8,"$t8"));
        tRegs.add(new Reg(Reg.State.FREE,9,"$t9"));
    }

    public String allocSReg(String name) {
        for (Reg reg:sRegs) {
            if (reg.getStatus().equals(Reg.State.FREE)) {
                reg.setName(name);
                reg.setStatus(Reg.State.USED);
                name2Reg.put(name,reg);
                cache.add(reg);
                return reg.getRegName();
            }
        }
        return "FULL";
    }

    public String allocTReg(String name) {
        for (Reg reg:tRegs) {
            if (reg.getStatus().equals(Reg.State.FREE)) {
                reg.setName(name);
                reg.setStatus(Reg.State.USED);
                name2Reg.put(name,reg);
                cache.add(reg);
                return reg.getRegName();
            }
        }
        return "FULL";
    }

    public String allocAReg(String name,String regName) {
        int id;
        if (regName.contains("0"))
            id = 0;
        else if (regName.contains("1"))
            id = 1;
        else if (regName.contains("2"))
            id = 2;
        else
            id = 3;
        Reg reg = aRegs.get(id);
        freeReg(reg);
        reg.setStatus(Reg.State.USED);
        // alloc A寄存器时，不需要把名字存进来，免得退出block时无法回存
        // reg.setName(name);
        // name2Reg.put(name,reg);
        reg.setName("**AREG**");
        name2Reg.put("**AREG**",reg);
        cache.add(reg);
        return reg.getRegName();
    }

    //  storeImm,a寄存器
    public void replaceWithoutName(String regName) {
        Reg reg = getRegWithRegName(regName);
        freeReg(reg);
        reg.setStatus(Reg.State.USED);
        reg.setName("*IMM*");
        name2Reg.put("*IMM*",reg);
        cache.add(reg);
    }

    public void freeRegs(HashSet<String> regNames) {
        for (String regName:regNames) {
            Reg reg = getRegWithRegName(regName);
            freeReg(reg);
        }
    }

    public void freeReg(Reg reg) {
        reg.setStatus(Reg.State.FREE);
        name2Reg.remove(reg.getName());
        cache.remove(reg);
    }

    public boolean allocated(String name) {
        return name2Reg.containsKey(name);
    }

    public String getRegWithSymbol(String name) {
        return name2Reg.get(name).getRegName();
    }

    public Reg getRegWithRegName(String regName) {
        int id;
        Reg reg;
        if (regName.contains("t")) {
            id = Integer.parseInt(regName.split("t")[1]);
            reg = tRegs.get(id);
        }
        else if (regName.contains("a")) {
            id = Integer.parseInt(regName.split("a")[1]);
            reg = aRegs.get(id);
        }
        else if (regName.contains("s")) {
            id = Integer.parseInt(regName.split("s")[1]);
            reg = sRegs.get(id);
        }
        else {
            // $fp
            id = 8;
            reg = sRegs.get(id);
        }
        return reg;
    }

    public HashMap<String, Reg> getName2Reg() {
        return name2Reg;
    }

    public Reg replaceTReg(String name) {
        Reg reg;
        Iterator<Reg> iterator = cache.iterator();
        do {
            reg = iterator.next();
        } while (!reg.getRegName().contains("t"));
        Reg old = new Reg(reg.getName(),reg.getRegName());
        freeReg(reg);
        reg.setName(name);
        reg.setStatus(Reg.State.USED);
        name2Reg.put(name,reg);
        cache.add(reg);
        return old;
    }

    public Reg replaceSReg(String name) {
        Reg reg;
        Iterator<Reg> iterator = cache.iterator();
        do {
            reg = iterator.next();
        } while (!reg.getRegName().contains("s") && !reg.getRegName().equals("$fp"));
        Reg old = new Reg(reg.getName(),reg.getRegName());
        freeReg(reg);
        reg.setName(name);
        reg.setStatus(Reg.State.USED);
        name2Reg.put(name,reg);
        cache.add(reg);
        return old;
    }

    public Reg replace(String name) {
        Reg reg = cache.iterator().next();
        Reg old = new Reg(reg.getName(),reg.getRegName());
        freeReg(reg);
        reg.setName(name);
        reg.setStatus(Reg.State.USED);
        name2Reg.put(name,reg);
        cache.add(reg);
        return old;
    }

    public Register copy() {
        Register register = new Register();
        for (int i = 0;i<sRegs.size();i++) {
            register.sRegs.get(i).copy(this.sRegs.get(i));
        }
        for (int i = 0;i<aRegs.size();i++) {
            register.aRegs.get(i).copy(this.aRegs.get(i));
        }
        for (int i = 0;i<tRegs.size();i++) {
            register.tRegs.get(i).copy(this.tRegs.get(i));
        }
        for (Map.Entry<String,Reg> entry: name2Reg.entrySet()) {
            int id = entry.getValue().getId();
            String regName = entry.getValue().getRegName();
            if (regName.contains("t")) {
                register.name2Reg.put(entry.getKey(),register.tRegs.get(id));
            }
            else if (regName.contains("s")) {
                register.name2Reg.put(entry.getKey(),register.sRegs.get(id));
            }
            else if (regName.equals("$fp")) {
                register.name2Reg.put(entry.getKey(),register.sRegs.get(8));
            }
        }
        for (Reg reg:cache) {
            if (reg.getRegName().charAt(1) == 't') {
                register.cache.add(register.tRegs.get(reg.getId()));
            }
            else if (reg.getRegName().charAt(1) == 's') {
                register.cache.add(register.sRegs.get(reg.getId()));
            }
            else if (reg.getRegName().equals("$fp")) {
                register.cache.add(register.sRegs.get(8));
            }
        }
        return register;
    }

    public LinkedHashSet<Reg> getCache() {
        return cache;
    }

    public ArrayList<Reg> getsRegs() {
        return sRegs;
    }

    public ArrayList<Reg> gettRegs() {
        return tRegs;
    }

    public ArrayList<Reg> getaRegs() {
        return aRegs;
    }
}
