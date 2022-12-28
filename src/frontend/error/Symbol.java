package frontend.error;

import frontend.syntax.decl.ConstInitVal;

import java.util.List;

public class Symbol {
    private final String name;
    private int address;
    private int size1; // 一维的size
    private int size2; // 二维的size
    private int level;
    private int value;
    private List<Integer> values;
    private int size = 0;
    private int line;
    private ConstInitVal constInitVal;

    public enum Kind {
        VAR,CONST,PARA,FUNC,TEMP
    }
    private final Kind kind;

    public enum DataType {
        INT,VOID,ARRAY;
    }

    private DataType dataType;

    public Symbol(String name, Kind kind, DataType dataType,int line) {
        this.name = name;
        this.kind = kind;
        this.dataType = dataType;
        this.size1 = -1;
        this.size2 = -1;
        this.line = line;
        setSize();
    }

    public Symbol(String name, int size1, int size2, int value, Kind kind, DataType dataType, int line) {
        this.name = name;
        this.size1 = size1;
        this.size2 = size2;
        this.value = value;
        this.kind = kind;
        this.dataType = dataType;
        this.line = line;
        setSize();
    }

    public Symbol(String name, int size1, int size2, List<Integer> values, Kind kind, DataType dataType,int line) {
        this.name = name;
        this.size1 = size1;
        this.size2 = size2;
        this.values = values;
        this.kind = kind;
        this.dataType = dataType;
        this.line = line;
        setSize();
    }

    public Symbol(String name, int size1, int size2, Kind kind, DataType dataType,int line) {
        this.name = name;
        this.size1 = size1;
        this.size2 = size2;
        this.kind = kind;
        this.dataType = dataType;
        this.line = line;
        setSize();
    }

    public Symbol(String name, Kind kind) {
        this.name = name;
        this.size1 = -1;
        this.size2 = -1;
        this.kind = kind;
        setSize();
    }

    private void setSize() {
        if (dataType == DataType.INT) {
            size = 4;
        }
        else if (dataType == DataType.ARRAY){
            size = 4*size1;
            if (size2 > 0)
                size *= size2;
        }
    }

    public String getName() {
        return name;
    }

    public int getAddress() {
        return address;
    }

    public int getSize1() {
        return size1;
    }

    public int getSize2() {
        return size2;
    }

    public int getLevel() {
        return level;
    }

    public int getLine() {
        return line;
    }

    public int getValue(int i, int j) {
        if (dataType == DataType.ARRAY) {
            int index = i;
            if (size2 != -1) {
                index = i*size2 + j;
            }
            if (index < values.size()) {
                return values.get(index);
            }
            else
                return 0;
        }
        else
            return value;
    }

    public int getDim() {
        if (dataType != DataType.ARRAY) {
            return 0;
        }
        if (size2 >= 0)
            return 2;
        if (size1 >= 0)
            return 1;
        return 0;
    }

    public int getValue() {
        return value;
    }

    public List<Integer> getValues() {
        return values;
    }

    public Kind getKind() {
        return kind;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int setAddress(int address) {
        if (kind.equals(Kind.FUNC)) {
            this.address = address;
            return address;
        }
        else if (kind.equals(Kind.VAR) || kind.equals(Kind.CONST)) {
            this.address = address;
            return address + size;
        }
        else if (kind.equals(Kind.PARA) || kind.equals(Kind.TEMP)) {
            this.address = address;
            return address + 4;
        }
        return address;
    }

    public String getMidName() {
        if (kind.equals(Kind.FUNC) || kind.equals(Kind.TEMP)) {
            return name;
        }
        else {
            return name + "@<" + line + ">";
        }
    }

    public boolean isPara() {
        return kind.equals(Kind.PARA);
    }

    public boolean isArray() {
        return dataType.equals(DataType.ARRAY);
    }

    public boolean isVar() {
        return kind.equals(Kind.VAR) || kind.equals(Kind.CONST) || kind.equals(Kind.PARA);
    }

    public boolean isConst() {
        return kind.equals(Kind.CONST);
    }

}
