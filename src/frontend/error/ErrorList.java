package frontend.error;

import frontend.lexical.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class ErrorList {
    private final ArrayList<Error> errors;
    private static ErrorList instance;

    private ErrorList() {
        errors = new ArrayList<>();
    }

    public static ErrorList getInstance() {
        if (instance == null)
            instance = new ErrorList();
        return instance;
    }

    public void output(String path,boolean isDebug) {
        if (isDebug) {
            for (Error error:errors)
                System.err.println(error.toString());
        }
        else {
            try (FileWriter fileWriter = new FileWriter(path)) {
                for (Error error:errors)
                    fileWriter.append(error.toString()).append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sort() {
        Collections.sort(errors);
    }

    public void add(Error error) {
        errors.add(error);
    }
}
