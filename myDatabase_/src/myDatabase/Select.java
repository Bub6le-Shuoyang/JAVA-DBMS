package myDatabase;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;


public class Select {
    public static String processSelect(String sql) {
        try {
            sql = sql.replaceAll(";\\s*$", "").trim();

            // 解析SELECT语句
            SelectQuery query = parseSelectQuery(sql);

            // 验证表是否存在
            if (!validateTablesExist(query)) {
                return "ERROR: 表不存在";
            }

            // 执行查询
            QueryResult result = executeQuery(query);

            // 返回格式化结果
            return formatResult(result);

        } catch (InvalidQueryException e) {
            return "ERROR: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR: 查询失败 - " + e.getMessage();
        }
    }

    // 解析SELECT查询
    private static SelectQuery parseSelectQuery(String sql) throws InvalidQueryException {
        SelectQuery query = new SelectQuery();

        // 解析SELECT部分
        Matcher selectMatcher = Pattern.compile(
                "(?i)^SELECT\\s+(.+?)\\s+FROM\\s+(.+?)(?:\\s+WHERE\\s+(.+?))?(?:\\s*;?\\s*)$"
        ).matcher(sql);

        if (!selectMatcher.find()) {
            throw new InvalidQueryException("语法错误，正确格式：SELECT 字段 FROM 表 [WHERE 条件]");
        }

        // 解析字段列表
        query.columns = parseColumns(selectMatcher.group(1).trim());

        // 解析FROM部分（可能包含JOIN）
        parseFromClause(selectMatcher.group(2).trim(), query);

        // 解析WHERE条件（如果有）
        if (selectMatcher.group(3) != null) {
            query.whereClause = selectMatcher.group(3).trim();
        }

        return query;
    }

    // 解析列名列表
    private static List<String> parseColumns(String columnsPart) {
        List<String> columns = new ArrayList<>();

        // 处理*或具体列名
        if (columnsPart.equals("*")) {
            // 特殊处理，表示所有列
            columns.add("*");
        } else {
            // 分割列名，处理可能的表别名
            String[] parts = columnsPart.split("\\s*,\\s*");
            for (String part : parts) {
                columns.add(part.trim());
            }
        }
        return columns;
    }

    // 解析FROM子句（支持多表JOIN）
    private static void parseFromClause(String fromPart, SelectQuery query) throws InvalidQueryException {
        // 检查是否有JOIN
        if (fromPart.toUpperCase().contains(" JOIN ")) {
            // 解析JOIN查询
            parseJoinClause(fromPart, query);
        } else {
            // 简单单表查询
            query.tables.add(new QueryTable(fromPart.trim(), null));
        }
    }

    // 解析JOIN子句
    private static void parseJoinClause(String fromPart, SelectQuery query) throws InvalidQueryException {
        // 简化处理，实际应使用更复杂的解析
        String[] parts = fromPart.split("(?i)\\s+JOIN\\s+");
        if (parts.length < 2) {
            throw new InvalidQueryException("无效的JOIN语法");
        }

        // 解析主表
        String[] mainTableParts = parts[0].split("\\s+");
        String mainTable = mainTableParts[0].trim();
        String mainAlias = mainTableParts.length > 1 ? mainTableParts[1].trim() : null;
        query.tables.add(new QueryTable(mainTable, mainAlias));

        // 解析JOIN表和条件
        for (int i = 1; i < parts.length; i++) {
            String[] joinParts = parts[i].split("(?i)\\s+ON\\s+");
            if (joinParts.length != 2) {
                throw new InvalidQueryException("JOIN必须包含ON条件");
            }

            String[] tableParts = joinParts[0].split("\\s+");
            String joinTable = tableParts[0].trim();
            String joinAlias = tableParts.length > 1 ? tableParts[1].trim() : null;

            query.tables.add(new QueryTable(joinTable, joinAlias));
            query.joinConditions.add(joinParts[1].trim());
        }
    }

    // 验证表是否存在
    private static boolean validateTablesExist(SelectQuery query) {
        for (QueryTable table : query.tables) {
            String tablePath = SQLConstant.getCurrentTableRecordPath(table.tableName);
            if (!Files.exists(Paths.get(tablePath))) {
                return false;
            }
        }
        return true;
    }

    // 执行查询
    private static QueryResult executeQuery(SelectQuery query) throws IOException {
        QueryResult result = new QueryResult();

        if (query.tables.size() == 1) {
            // 单表查询
            result = executeSingleTableQuery(query);
        } else {
            // 多表JOIN查询
            result = executeJoinQuery(query);
        }

        return result;
    }

    // 执行单表查询
    private static QueryResult executeSingleTableQuery(SelectQuery query) throws IOException {
        QueryResult result = new QueryResult();
        String tablePath = SQLConstant.getCurrentTableRecordPath(query.tables.get(0).tableName);

        // 读取表数据
        List<String> lines = Files.readAllLines(Paths.get(tablePath));
        if (lines.size() < 4) return result;

        // 获取表头
        String[] headers = lines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));

        // 确定要选择的列
        List<Integer> selectedColumns = getSelectedColumns(query.columns, headers);

        // 构建结果表头
        result.headers = new ArrayList<>();
        for (int col : selectedColumns) {
            result.headers.add(headers[col]);
        }

        // 处理数据行
        for (int i = 3; i < lines.size(); i++) {
            String[] values = lines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator()), -1);

            // 添加WHERE条件判断
            if (query.whereClause == null || matchesCondition(query.whereClause, headers, values)) {
                List<String> row = new ArrayList<>();
                for (int col : selectedColumns) {
                    row.add(col < values.length ? values[col] : "NULL");
                }
                result.rows.add(row);
            }
        }

        return result;
    }

    // 在 Select.java 中添加
    private static boolean matchesCondition(String where, String[] columns, String[] values) {
        if (where == null || where.trim().isEmpty()) {
            return true;
        }

        String[] conditions = where.split("(?i)\\s+and\\s+");
        for (String cond : conditions) {
            Matcher matcher = Pattern.compile("(\\w+)\\s*([=!<>]+)\\s*(.+)").matcher(cond.trim());
            if (!matcher.find()) continue;

            String column = matcher.group(1).trim();
            String operator = matcher.group(2).trim();
            String expectedValue = matcher.group(3).trim().replaceAll("^['\"]|['\"]$", "");

            int colIndex = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase(column)) {
                    colIndex = i;
                    break;
                }
            }

            if (colIndex < 0 || colIndex >= values.length) {
                return false;
            }

            String actualValue = values[colIndex] == null ? "" :
                    values[colIndex].replaceAll("^['\"]|['\"]$", "");

            // 数值比较
            if (isNumeric(expectedValue) && isNumeric(actualValue)) {
                try {
                    double expNum = Double.parseDouble(expectedValue);
                    double actNum = Double.parseDouble(actualValue);

                    switch (operator) {
                        case "=": if (actNum != expNum) return false; break;
                        case "!=": if (actNum == expNum) return false; break;
                        case ">": if (actNum <= expNum) return false; break;
                        case "<": if (actNum >= expNum) return false; break;
                        default: return false;
                    }
                } catch (NumberFormatException e) {}
            }
            // 字符串比较
            else {
                switch (operator) {
                    case "=": if (!actualValue.equals(expectedValue)) return false; break;
                    case "!=": if (actualValue.equals(expectedValue)) return false; break;
                    default: return false;
                }
            }
        }
        return true;
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }


    private static QueryResult executeJoinQuery(SelectQuery query) throws IOException {
        QueryResult result = new QueryResult();

        // 只处理简单的两表JOIN
        if (query.tables.size() != 2 || query.joinConditions.isEmpty()) {
            result.headers.add("ERROR");
            result.rows.add(Arrays.asList("只支持两表JOIN查询"));
            return result;
        }

        // 读取左表数据
        QueryTable leftTable = query.tables.get(0);
        List<String[]> leftData = readTableData(leftTable.tableName);
        if (leftData.isEmpty()) {
            return result;
        }
        String[] leftHeaders = leftData.get(0);

        // 读取右表数据
        QueryTable rightTable = query.tables.get(1);
        List<String[]> rightData = readTableData(rightTable.tableName);
        if (rightData.isEmpty()) {
            return result;
        }
        String[] rightHeaders = rightData.get(0);

        // 解析JOIN条件
        String joinCondition = query.joinConditions.get(0);
        String[] joinParts = parseJoinCondition(joinCondition, leftHeaders, rightHeaders);
        if (joinParts.length < 4) {
            result.headers.add("ERROR");
            result.rows.add(Arrays.asList("无效的JOIN条件"));
            return result;
        }

        // 执行嵌套循环连接
        for (int i = 1; i < leftData.size(); i++) { // 跳过表头
            String[] leftRow = leftData.get(i);
            for (int j = 1; j < rightData.size(); j++) {
                String[] rightRow = rightData.get(j);

                if (matchesJoinCondition(joinParts, leftRow, leftHeaders, rightRow, rightHeaders)) {
                    List<String> combinedRow = new ArrayList<>();
                    // 添加左表所有列
                    Collections.addAll(combinedRow, leftRow);
                    // 添加右表所有列
                    Collections.addAll(combinedRow, rightRow);
                    result.rows.add(combinedRow);
                }
            }
        }

        // 设置表头
        for (String h : leftHeaders) {
            result.headers.add(leftTable.alias != null ? leftTable.alias + "." + h : leftTable.tableName + "." + h);
        }
        for (String h : rightHeaders) {
            result.headers.add(rightTable.alias != null ? rightTable.alias + "." + h : rightTable.tableName + "." + h);
        }

        return result;
    }

    /**
     * 读取表数据，返回包含表头和数据行的列表
     * @param tableName 表名
     * @return List<String[]> 第一项是表头，后续项是数据行
     */
    private static List<String[]> readTableData(String tableName) throws IOException {
        List<String[]> result = new ArrayList<>();
        String tablePath = SQLConstant.getCurrentTableRecordPath(tableName);

        if (!Files.exists(Paths.get(tablePath))) {
            return result;
        }

        List<String> lines = Files.readAllLines(Paths.get(tablePath));
        if (lines.size() < 3) {
            return result;
        }

        // 添加表头行（列名）
        result.add(lines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator())));

        // 添加数据行（跳过表结构的前3行）
        for (int i = 3; i < lines.size(); i++) {
            result.add(lines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator())));
        }

        return result;
    }

    /**
     * 解析JOIN条件，返回[leftTable, leftColumn, rightTable, rightColumn]
     */
    private static String[] parseJoinCondition(String condition, String[] leftHeaders, String[] rightHeaders) {
        // 解析格式如：e.department = d.dept_name
        String[] parts = condition.split("\\s*=\\s*");
        if (parts.length != 2) {
            return new String[0];
        }

        String[] leftSide = parts[0].trim().split("\\.");
        String leftTable = leftSide.length > 1 ? leftSide[0].trim() : null;
        String leftColumn = leftSide.length > 1 ? leftSide[1].trim() : leftSide[0].trim();

        String[] rightSide = parts[1].trim().split("\\.");
        String rightTable = rightSide.length > 1 ? rightSide[0].trim() : null;
        String rightColumn = rightSide.length > 1 ? rightSide[1].trim() : rightSide[0].trim();

        return new String[]{leftTable, leftColumn, rightTable, rightColumn};
    }

    /**
     * 检查行数据是否满足JOIN条件
     */
    private static boolean matchesJoinCondition(String[] joinParts, String[] leftRow, String[] leftHeaders,
                                                String[] rightRow, String[] rightHeaders) {
        if (joinParts == null || joinParts.length < 4) {
            return false;
        }

        // 获取左表连接列的值
        String leftValue = null;
        for (int i = 0; i < leftHeaders.length; i++) {
            if (joinParts[1].equalsIgnoreCase(leftHeaders[i])) {
                leftValue = leftRow[i];
                break;
            }
        }

        // 获取右表连接列的值
        String rightValue = null;
        for (int i = 0; i < rightHeaders.length; i++) {
            if (joinParts[3].equalsIgnoreCase(rightHeaders[i])) {
                rightValue = rightRow[i];
                break;
            }
        }

        // 比较值是否相等（字符串比较）
        return leftValue != null && rightValue != null && leftValue.equals(rightValue);
    }
    // 确定选择的列索引
    private static List<Integer> getSelectedColumns(List<String> selectedColumns, String[] allColumns) {
        List<Integer> indices = new ArrayList<>();

        if (selectedColumns.size() == 1 && selectedColumns.get(0).equals("*")) {
            // 选择所有列
            for (int i = 0; i < allColumns.length; i++) {
                indices.add(i);
            }
        } else {
            // 选择指定列
            for (String col : selectedColumns) {
                for (int i = 0; i < allColumns.length; i++) {
                    if (allColumns[i].equalsIgnoreCase(col.replaceAll(".*\\.", ""))) {
                        indices.add(i);
                        break;
                    }
                }
            }
        }

        return indices;
    }

    // 格式化结果
    private static String formatResult(QueryResult result) {
        if (result.rows.isEmpty()) {
            return "Query OK: 0 rows returned";
        }

        return TableGenerator.generateTable(result.headers, result.rows) +
                "\nQuery OK: " + result.rows.size() + " rows returned";
    }

    // 辅助类：表示查询信息
    private static class SelectQuery {
        List<String> columns = new ArrayList<>();
        List<QueryTable> tables = new ArrayList<>();
        List<String> joinConditions = new ArrayList<>();
        String whereClause;
    }

    // 辅助类：表示查询表
    private static class QueryTable {
        String tableName;
        String alias;

        QueryTable(String tableName, String alias) {
            this.tableName = tableName;
            this.alias = alias;
        }
    }

    // 辅助类：表示查询结果
    private static class QueryResult {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
    }

    // 自定义异常类
    private static class InvalidQueryException extends Exception {
        InvalidQueryException(String message) {
            super(message);
        }
    }
}