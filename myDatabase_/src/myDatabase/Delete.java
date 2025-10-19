package myDatabase;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Delete {
    public static String processDelete(String sql) {
        try {
            // 统一去除语句末尾的分号（如果存在）
            sql = sql.replaceAll(";\\s*$", "").trim();

            // 解析DELETE语句
            if (sql.matches("(?i)^delete\\s+from\\s+\\w+\\s*$")) {
                return deleteAllRecords(sql);
            } else if (sql.matches("(?i)^delete\\s+from\\s+\\w+\\s+where\\s+.+$")) {
                return deleteWithCondition(sql);
            } else {
                return "ERROR: 语法错误，正确格式：DELETE FROM 表名 [WHERE 条件]";
            }
        } catch (Exception e) {
            return "ERROR: 删除记录失败 - " + e.getMessage();
        }
    }

    private static String deleteAllRecords(String sql) throws IOException {
        // 解析表名
        String tableName = sql.replaceAll("(?i)^delete\\s+from\\s+(\\w+)\\s*$", "$1");
        String tablePath = SQLConstant.getCurrentTableRecordPath(tableName);

        if (!Files.exists(Paths.get(tablePath))) {
            return "ERROR: 表 '" + tableName + "' 不存在";
        }

        // 读取表结构
        List<String> lines = Files.readAllLines(Paths.get(tablePath));

        if (lines.size() <= 3) {
            return "Query OK: 表中无记录可删除";
        }

        // 保留表结构，删除所有记录
        List<String> newLines = new ArrayList<>(lines.subList(0, 3));

        // 写回文件
        Files.write(Paths.get(tablePath), newLines);

        return "Query OK: 已删除所有记录";
    }

    private static String deleteWithCondition(String sql) throws IOException {
        // 修改正则表达式，确保正确截取WHERE条件
        Matcher matcher = Pattern.compile(
                "(?i)^delete\\s+from\\s+(\\w+)\\s+where\\s+(.+)$",
                Pattern.CASE_INSENSITIVE).matcher(sql);

        if (!matcher.find()) {
            return "ERROR: 语法错误，正确格式：DELETE FROM 表名 WHERE 条件";
        }

        String tableName = matcher.group(1);
        String condition = matcher.group(2).trim();

        String tablePath = SQLConstant.getCurrentTableRecordPath(tableName);
        if (!Files.exists(Paths.get(tablePath))) {
            return "ERROR: 表 '" + tableName + "' 不存在";
        }

        // 读取表数据
        List<String> lines = Files.readAllLines(Paths.get(tablePath));

        if (lines.size() <= 3) {
            return "Query OK: 表中无记录可删除";
        }

        // 获取列名
        String[] columns = lines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));

        // 创建临时文件
        Path tempPath = Paths.get(tablePath + ".tmp");
        int deletedCount = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(tempPath)) {
            // 保留表结构
            for (int i = 0; i < 3; i++) {
                writer.write(lines.get(i));
                writer.newLine();
            }

            // 处理记录
            for (int i = 3; i < lines.size(); i++) {
                String[] values = lines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator()), -1);

                if (!matchesCondition(condition, columns, values)) {
                    writer.write(lines.get(i));
                    writer.newLine();
                } else {
                    deletedCount++;
                }
            }
        }

        // 替换原文件
        Files.move(tempPath, Paths.get(tablePath), StandardCopyOption.REPLACE_EXISTING);

        return "Query OK: 删除了 " + deletedCount + " 条记录";
    }

    private static boolean matchesCondition(String condition, String[] columns, String[] values) {
        // 支持多种比较操作符和逻辑运算符
        String[] andConditions = condition.split("(?i)\\s+and\\s+");

        for (String andCond : andConditions) {
            if (!evaluateSingleCondition(andCond, columns, values)) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateSingleCondition(String condition, String[] columns, String[] values) {
        // 支持多种比较操作符 (=, !=, >, <, >=, <=, LIKE)
        Matcher matcher = Pattern.compile(
                "(\\w+)\\s*([=!<>]+|like)\\s*(.+)",
                Pattern.CASE_INSENSITIVE).matcher(condition.trim());

        if (!matcher.find()) {
            return false;
        }

        String column = matcher.group(1).trim().toLowerCase();
        String operator = matcher.group(2).trim().toLowerCase();
        String expectedValue = matcher.group(3).trim();

        // 处理字符串值的引号（单引号或双引号）
        if (expectedValue.startsWith("'") && expectedValue.endsWith("'") ||
                expectedValue.startsWith("\"") && expectedValue.endsWith("\"")) {
            expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
        }

        for (int i = 0; i < columns.length; i++) {
            if (columns[i].trim().toLowerCase().equals(column)) {
                String storedValue = values[i].trim();

                // 处理NULL值
                if ("null".equalsIgnoreCase(storedValue)) {
                    if ("=".equals(operator) && "null".equalsIgnoreCase(expectedValue)) {
                        return true;
                    } else if ("!=".equals(operator) && !"null".equalsIgnoreCase(expectedValue)) {
                        return true;
                    }
                    return false;
                }

                // 数字比较
                if (isNumeric(storedValue) && isNumeric(expectedValue)) {
                    try {
                        double storedNum = Double.parseDouble(storedValue);
                        double expectedNum = Double.parseDouble(expectedValue);

                        switch (operator) {
                            case "=": return storedNum == expectedNum;
                            case "!=": return storedNum != expectedNum;
                            case ">": return storedNum > expectedNum;
                            case "<": return storedNum < expectedNum;
                            case ">=": return storedNum >= expectedNum;
                            case "<=": return storedNum <= expectedNum;
                            default: return false;
                        }
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                // 字符串比较
                else {
                    switch (operator) {
                        case "=":
                            return storedValue.equalsIgnoreCase(expectedValue);
                        case "!=":
                            return !storedValue.equalsIgnoreCase(expectedValue);
                        case "like":
                            String regex = expectedValue.replace("%", ".*").replace("_", ".");
                            return storedValue.matches("(?i)" + regex);
                        default:
                            return false;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
}