package frontend.lexical;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private final ArrayList<Token> tokenList = new ArrayList<>();
    private final String[] lines; // 按行存储源代码
    private String code; // 当前行剩余代码
    private int curLine; // 记录当前行

    public Tokenizer(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        lines = new String(bytes, StandardCharsets.UTF_8).split("(?<=\n)");
        curLine = 0;
        // System.out.println(code);
    }

    /**
     * 删除空白前缀，如果所有都是空白符，返回true，否则返回false
     */
    private boolean deleteWhite() {
        int index = 0;
        while (Character.isWhitespace(code.charAt(index))) {
            index++;
            if (index == code.length())
                return true;
        }
        code = code.substring(index);
        return false;
    }

    /**
     * 下移一行，超过文件返回true，否则返回false
     */
    private boolean addLine() {
        ++curLine;
        if (curLine >= lines.length)
            return true;
        code = lines[curLine];
        return false;
    }

    private int deleteAnnotation(int index) {
        index += 2;
        while (true) {
            if (index + 2 >= code.length()) {
                if (addLine())
                    break;
                index = 0;
            }
            else if (code.startsWith("*/", index)) {
                code = code.substring(index + 2);
                index = 0;
                break;
            }
            else
                index++;
        }
        return index;
    }

    public void tokenize() {
        int index = 0;
        code = lines[0];
        while (curLine < lines.length) {
            if (deleteWhite()) {
                if (addLine()) break;
            }
            if (code.startsWith("/*", index)) {
                index = deleteAnnotation(index);
                continue;
            }
            for (Type type:Type.values()) {
                Pattern pattern = type.getPattern();
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    if (type.toString().equals("SINC")) {
                        if (addLine()) break;
                        break;
                    }
                    String token = matcher.group(0);
                    tokenList.add(new Token(type,token,curLine+1));
                    index += token.length();
                    if (index >= code.length()) {
                        if (addLine())
                            break;
                    }
                    else
                        code = code.substring(index);
                    index = 0;
                    break;
                }
            }
        }
    }

    public ArrayList<Token> getTokenList() {
        return tokenList;
    }

    public void output(String path,boolean isDebug) {
        if (isDebug) {
            for (Token token:tokenList) {
                System.out.println(token.toString());
            }
        }
        else {
            try (FileWriter fileWriter = new FileWriter(path)) {
                for (Token token:tokenList)
                    fileWriter.append(token.toString()).append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}