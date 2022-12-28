package frontend.error;

public class Error implements Comparable<Error> {
    private final ErrorType errorType;
    private final int line;

    public Error(ErrorType errorType, int line) {
        this.errorType = errorType;
        this.line = line;
    }

    @Override
    public String toString() {
        return line + " " + errorType.getCode();
    }

    @Override
    public int compareTo(Error o) {
        return Integer.compare(this.line,o.line);
    }
}
