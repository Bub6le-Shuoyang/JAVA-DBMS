package myDatabase;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import java.util.function.Predicate;

public class Insert {
    public static String processInsert(String sql) {
        try {
            Pattern pattern = Pattern.compile(
                    "(?i)^insert\\s+into\\s+(\\w+)\\s*(?:\\(([^)]+)\\))?\\s*values\\s*\\(([^)]+)\\)\\s*;?$");
            Matcher matcher = pattern.matcher(sql.trim());

            if (!matcher.find()) {
                return "ERROR: 语法错误，正确格式：INSERT INTO 表名(字段1,...) VALUES(值1,...)";
            }

            String tableName = matcher.group(1);
            String columnsPart = matcher.group(2);
            String valuesPart = matcher.group(3);

            String currentDbName = SQLConstant.getCurrentDbPath().substring(SQLConstant.getRootPath().length() + 1);
            String tablePath = SQLConstant.getTableRecordPath(currentDbName, tableName);

            // 读取表结构
            List<String> tableStructure = readTableStructure(
                    SQLConstant.getTableDefinitionPath(currentDbName, tableName));
            if (tableStructure.size() < 3) {
                return "ERROR: 表结构不完整";
            }

            String[] columnNames = tableStructure.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            String[] columnTypes = tableStructure.get(1).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            String[] constraints = tableStructure.get(2).split(Pattern.quote(SQLConstant.getFieldSeparator()));

            // 处理值
            String[] values = valuesPart.split("\\s*,\\s*");

            // 检查列数量匹配
            if (columnsPart == null && values.length != columnNames.length) {
                return "ERROR: 值数量(" + values.length + ")与列数量(" + columnNames.length + ")不匹配";
            }

            // 构建完整记录
            String[] record = buildCompleteRecord(columnsPart, values, columnNames, columnTypes, constraints);
            if (record == null) {
                return "ERROR: 记录构建失败";
            }

            // 验证所有约束
            String validationResult = validateConstraints(tablePath, columnNames, columnTypes, constraints, record);
            if (validationResult != null) {
                return validationResult;
            }

            // 写入记录
            writeRecord(tablePath, String.join(SQLConstant.getFieldSeparator(), record));
            return "Query OK: 插入成功";

        } catch (Exception e) {
            return "ERROR: 插入失败 - " + e.getMessage();
        }
    }

    private static String[] buildCompleteRecord(String columnsPart, String[] values,
                                                String[] columnNames, String[] columnTypes, String[] constraints) {
        String[] record = new String[columnNames.length];
        Arrays.fill(record, "null");

        if (columnsPart == null) {
            // 不指定列名的插入
            for (int i = 0; i < values.length; i++) {
                String processedValue = processValue(values[i], columnTypes[i]);
                if (processedValue == null) {
                    return null;
                }
                record[i] = processedValue;
            }
        } else {
            // 指定列名的插入
            String[] columns = columnsPart.split("\\s*,\\s*");
            if (columns.length != values.length) {
                return null;
            }

            for (int i = 0; i < columns.length; i++) {
                boolean columnFound = false;
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j].equalsIgnoreCase(columns[i].trim())) {
                        String processedValue = processValue(values[i], columnTypes[j]);
                        if (processedValue == null) {
                            return null;
                        }
                        record[j] = processedValue;
                        columnFound = true;
                        break;
                    }
                }
                if (!columnFound) {
                    return null;
                }
            }
        }
        return record;
    }

    private static String validateConstraints(String tablePath, String[] columnNames,
                                              String[] columnTypes, String[] constraints, String[] record) throws IOException {
        // 1. 检查主键约束
        for (int i = 0; i < constraints.length; i++) {
            if (constraints[i].contains("PRIMARY KEY")) {
                String primaryKeyValue = record[i];
                if (!checkPrimaryKeyConstraint(tablePath, i, primaryKeyValue)) {
                    return "ERROR: 主键冲突，值 '" + primaryKeyValue + "' 已存在";
                }
            }
        }

        // 2. 检查NOT NULL约束
        for (int i = 0; i < constraints.length; i++) {
            if (constraints[i].contains("NOT NULL") && "null".equalsIgnoreCase(record[i])) {
                return "ERROR: 字段 '" + columnNames[i] + "' 不允许为NULL";
            }
        }

        // 3. 检查CHECK约束
        for (int i = 0; i < constraints.length; i++) {
            Matcher checkMatcher = Pattern.compile("CHECK\\s*\\((.+)\\)", Pattern.CASE_INSENSITIVE)
                    .matcher(constraints[i]);
            if (checkMatcher.find()) {
                String checkCondition = checkMatcher.group(1);
                if (!validateCheckConstraint(checkCondition, columnNames, record)) {
                    return "ERROR: CHECK约束失败 - " + checkCondition;
                }
            }
        }

        // 4. 检查外键约束
        for (int i = 0; i < constraints.length; i++) {
            Matcher fkMatcher = Pattern.compile("REFERENCES\\s+(\\w+)\\s*\\(?(\\w*)\\)?",
                    Pattern.CASE_INSENSITIVE).matcher(constraints[i]);
            if (fkMatcher.find()) {
                String refTable = fkMatcher.group(1);
                String refColumn = fkMatcher.group(2).isEmpty() ? columnNames[i] : fkMatcher.group(2);
                if (!validateForeignKeyConstraint(refTable, refColumn, record[i])) {
                    return "ERROR: 外键约束失败 - 表" + refTable + "中不存在" + refColumn + "=" + record[i];
                }
            }
        }

        return null;
    }

    private static boolean checkPrimaryKeyConstraint(String tablePath, int columnIndex, String value)
            throws IOException {
        if ("null".equalsIgnoreCase(value)) {
            return false;
        }

        List<String> lines = Files.readAllLines(Paths.get(tablePath));
        for (int i = 3; i < lines.size(); i++) {
            String[] values = lines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            if (values.length > columnIndex && value.equals(values[columnIndex])) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateCheckConstraint(String condition, String[] columnNames, String[] record) {
        // 1. 清理约束条件字符串
        condition = condition.trim()
                .replaceAll(">\\s*$", "") // 移除可能残留的>符号
                .replaceAll("[\\s\\n\\r]+", " "); // 标准化空格

        // 2. 构建列值映射（不区分大小写）
        Map<String, String> columnMap = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            columnMap.put(columnNames[i].toLowerCase(), record[i]);
        }

        // 3. 替换列名为实际值（带类型处理）
        String evalCondition = condition;
        for (Map.Entry<String, String> entry : columnMap.entrySet()) {
            String colName = entry.getKey();
            String value = entry.getValue();

            // 数值类型不加引号，字符串加引号
            String replacement = isNumeric(value) ? value : "'" + value + "'";
            evalCondition = evalCondition.replaceAll("(?i)\\b" + colName + "\\b", replacement);
        }

        // 4. 特殊处理布尔值和NULL
        evalCondition = evalCondition.replace("true", "1")
                .replace("false", "0")
                .replace("'null'", "null")
                .replace("\"null\"", "null");

        // 5. 调试输出清理后的表达式
        System.out.println("[DEBUG] Clean condition: " + evalCondition);

        // 6. 表达式求值
        try {
            return evaluateCondition(evalCondition);
        } catch (Exception e) {
            System.err.println("Condition evaluation failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean evaluateCondition(String condition) {
        // 移除所有空白字符（简化解析）
        condition = condition.replaceAll("\\s+", "");

        // 处理复合条件（AND优先于OR）
        int andIndex = condition.indexOf("AND");
        if (andIndex > 0) {
            String left = condition.substring(0, andIndex);
            String right = condition.substring(andIndex + 3);
            return evaluateCondition(left) && evaluateCondition(right);
        }

        int orIndex = condition.indexOf("OR");
        if (orIndex > 0) {
            String left = condition.substring(0, orIndex);
            String right = condition.substring(orIndex + 2);
            return evaluateCondition(left) || evaluateCondition(right);
        }

        // 处理比较表达式
        Matcher matcher = Pattern.compile("([^=<>!]+)([=<>!]+)(.+)").matcher(condition);
        if (matcher.find()) {
            String left = matcher.group(1);
            String op = matcher.group(2);
            String right = matcher.group(3);

            // 处理NULL比较
            if ("null".equalsIgnoreCase(left) || "null".equalsIgnoreCase(right)) {
                return evaluateNullComparison(left, op, right);
            }

            // 数值比较
            if (isNumeric(left) && isNumeric(right)) {
                double leftVal = Double.parseDouble(left);
                double rightVal = Double.parseDouble(right);
                return compareNumbers(leftVal, op, rightVal);
            }

            // 字符串比较
            return compareStrings(left, op, right);
        }

        // 处理布尔值
        return "true".equalsIgnoreCase(condition) || "1".equals(condition);
    }



    private static boolean validateForeignKeyConstraint(String refTable, String refColumn, String value)
            throws IOException {
        if ("null".equalsIgnoreCase(value)) {
            return true; // 允许外键为NULL
        }

        String refTablePath = SQLConstant.getCurrentTableRecordPath(refTable);
        if (!Files.exists(Paths.get(refTablePath))) {
            return false;
        }

        List<String> lines = Files.readAllLines(Paths.get(refTablePath));
        if (lines.size() < 4) return false;

        // 查找引用列索引
        String[] headers = lines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));
        int refColIndex = -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(refColumn)) {
                refColIndex = i;
                break;
            }
        }
        if (refColIndex == -1) return false;

        // 检查值是否存在
        for (int i = 3; i < lines.size(); i++) {
            String[] values = lines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            if (values.length > refColIndex && value.equals(values[refColIndex])) {
                return true;
            }
        }

        return false;
    }

    // 辅助方法
    private static boolean evaluateNullComparison(String left, String op, String right) {
        boolean leftNull = "null".equalsIgnoreCase(left);
        boolean rightNull = "null".equalsIgnoreCase(right);

        switch (op) {
            case "=": return leftNull && rightNull;
            case "!=": return leftNull != rightNull;
            case "IS": return leftNull && rightNull;
            case "IS NOT": return !leftNull && !rightNull;
            default: return false;
        }
    }

    private static boolean compareNumbers(double left, String op, double right) {
        switch (op) {
            case ">=": return left >= right;
            case "<=": return left <= right;
            case ">": return left > right;
            case "<": return left < right;
            case "=":
            case "==": return Math.abs(left - right) < 0.000001; // 处理浮点精度
            case "!=": return Math.abs(left - right) > 0.000001;
            default: return false;
        }
    }

    private static boolean compareStrings(String left, String op, String right) {
        // 先尝试日期比较
        boolean leftIsDate = isDate(left.replaceAll("^['\"]|['\"]$", ""));
        boolean rightIsDate = isDate(right.replaceAll("^['\"]|['\"]$", ""));

        if (leftIsDate && rightIsDate) {
            LocalDate leftDate = parseDate(left);
            LocalDate rightDate = parseDate(right);
            if (leftDate != null && rightDate != null) {
                System.out.println("[DEBUG] Comparing dates: " + leftDate + " " + op + " " + rightDate);
                return compareDates(leftDate, op, rightDate);
            }
        }

        // 默认字符串比较
        left = left.replaceAll("^['\"]|['\"]$", "");
        right = right.replaceAll("^['\"]|['\"]$", "");

        switch (op) {
            case "=": return left.equalsIgnoreCase(right);
            case "!=": return !left.equalsIgnoreCase(right);
            case "LIKE": return left.matches(right.replace("%", ".*").replace("_", "."));
            default: return false;
        }
    }

    // 新增日期辅助方法
    private static boolean isDate(String str) {
        return str.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private static LocalDate parseDate(String dateStr) {
        try {
            // 移除可能的引号
            dateStr = dateStr.replaceAll("^['\"]|['\"]$", "");
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.err.println("Date parse failed: " + dateStr);
            return null;
        }
    }

    private static boolean compareDates(LocalDate left, String op, LocalDate right) {
        switch (op) {
            case ">": return left.isAfter(right);
            case ">=": return left.isAfter(right) || left.equals(right);
            case "<": return left.isBefore(right);
            case "<=": return left.isBefore(right) || left.equals(right);
            case "=": return left.equals(right);
            case "!=": return !left.equals(right);
            default: return false;
        }
    }


    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static List<String> readTableStructure(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 3) {
                lines.add(line.trim());
                count++;
            }
        }
        return lines;
    }

    private static void writeRecord(String tablePath, String record) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tablePath, true))) {
            writer.write(record);
            writer.newLine();
        }
    }

    private static String processValue(String value, String type) {
        value = value.trim();
        try {
            if (type.equalsIgnoreCase("INTEGER")) {
                Integer.parseInt(value);
                return value;
            } else if (type.equalsIgnoreCase("DOUBLE")) {
                Double.parseDouble(value);
                return value;
            } else if (type.equalsIgnoreCase("BOOL")) {
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    return value.toLowerCase();
                }
                return null;
            } else if (type.toUpperCase().startsWith("VARCHAR") || type.equalsIgnoreCase("STRING")) {
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    return value.substring(1, value.length()-1);
                }
                return null;
            } else if (type.equalsIgnoreCase("DATE")) {
                // 简单日期格式验证
                if (value.matches("\\d{4}-\\d{2}-\\d{2}") ||
                        (value.startsWith("'") && value.endsWith("'") &&
                                value.substring(1, value.length()-1).matches("\\d{4}-\\d{2}-\\d{2}"))) {
                    return value.replaceAll("^['\"]|['\"]$", "");
                }
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}