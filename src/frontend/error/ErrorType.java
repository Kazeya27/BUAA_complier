package frontend.error;

public enum ErrorType {
    // 字符串非法符号
    ILGT("a"),
    // 名字重定义
    NRDF("b"),
    // 名字未定义
    NUDF("c"),
    // 参数个数不匹配
    FACM("d"),
    // 参数类型不匹配
    FATM("e"),
    // 无返回值的函数存在不匹配的return
    VDRT("f"),
    // 有返回值的缺少
    NORT("g"),
    // 不能改变常量的值
    COTC("h"),
    // 缺少分号
    MSSM("i"),
    // 缺少右小括号
    MSPR("j"),
    // 缺少右中括号
    MSBR("k"),
    // printf数量不匹配
    PFNE("l"),
    // 非循环块中使用break、continue
    WRBC("m");

    private final String code;

    ErrorType(String code) {
        this.code=code;
    }

    public String getCode() {
        return code;
    }
}