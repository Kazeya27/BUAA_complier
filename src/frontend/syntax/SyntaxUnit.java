package frontend.syntax;

import frontend.lexical.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class SyntaxUnit {
    private final String type;
    protected final List<SyntaxUnit> units;
    private static final HashSet<String> ignore = new HashSet<String>() {
        {
            add("Decl");
            add("BType");
            add("BlockItem");
            add("AssignStmt");
            add("ExpStmt");
            add("BreakStmt");
            add("CtnStmt");
            add("RtnStmt");
            add("GIStmt");
            add("PFStmt");
            add("SingleStmt");
            add("BrcStmt");
            add("LoopStmt");
            add("MulStmt");
        }
    };
    private static FileWriter FILE_WRITER = null;

    public SyntaxUnit(String type, List<SyntaxUnit> units) {
        this.type = type;
        this.units = units;
    }

    public List<SyntaxUnit> getUnits() {
        return units;
    }

    public String getSyntaxType() {
        return type;
    }

    public SyntaxUnit getLVal() {
        if (!this.type.equals("Exp"))
            return null;
        if (this.units == null)
            return null;
        SyntaxUnit addExp = this.units.get(0);
        if (addExp == null || addExp.units == null)
            return null;
        SyntaxUnit mulExp = addExp.units.get(0);
        if (mulExp == null || mulExp.units == null)
            return null;
        SyntaxUnit unaryExp = mulExp.units.get(0);
        if (unaryExp == null || unaryExp.units == null)
            return null;
        SyntaxUnit primaryExp = unaryExp.units.get(0);
        if (primaryExp == null || primaryExp.units == null)
            return null;
        SyntaxUnit LVal = primaryExp.units.get(0);
        if (LVal.type.equals("LVal"))
            return LVal;
        else
            return null;
    }

    public boolean isIncludeLVal() {
        if (!this.type.equals("Exp"))
            return false;
        SyntaxUnit addExp = this.units.get(0);
        SyntaxUnit mulExp = addExp.units.get(0);
        SyntaxUnit unaryExp = mulExp.units.get(0);
        SyntaxUnit primaryExp = unaryExp.units.get(0);
        return primaryExp.units.get(0).type.equals("LVal");
    }

    public static void setFileWriter(String path) throws IOException {
        FILE_WRITER = new FileWriter(path);
    }

    public static FileWriter getFileWriter() {
        return FILE_WRITER;
    }

    public void output(boolean debug) throws IOException {
        if (debug) {
            if (this instanceof Token) {
                System.out.println(this);
            }
            else {
                if (units != null) {
                    for (SyntaxUnit unit: units) {
                        unit.output(true);
                    }
                }
                if (!ignore.contains(type))
                    System.out.println("<" + type + ">");
            }
        }
        else {
            if (this instanceof Token) {
                // System.out.println(this);
                FILE_WRITER.append(this.toString()).append("\n");
            }
            else {
                if (units != null) {
                    for (SyntaxUnit unit: units) {
                        unit.output(false);
                    }
                }
                if (!ignore.contains(type)) {
                    // System.out.println("<" + type + ">");
                    FILE_WRITER.append("<").append(type).append(">").append("\n");
                }
            }
        }
    }

    public int getLine() {
        return 0;
    }

}
