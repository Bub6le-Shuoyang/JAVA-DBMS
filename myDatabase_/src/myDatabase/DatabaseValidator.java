package myDatabase;

import java.util.regex.Pattern;

/**
 * 数据库对象名称验证工具类
 */
public class DatabaseValidator {
    // 名称最大长度
    private static final int MAX_NAME_LENGTH = 128;
    // 合法名称正则表达式
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z_]\\w*$");

    /**
     * 验证数据库/表/字段名称是否合法
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            return false;
        }
        return NAME_PATTERN.matcher(name).matches();
    }


}