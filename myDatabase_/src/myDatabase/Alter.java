package myDatabase;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Alter {
    public static String processAlter(String sql) {
        try {
            // 统一去除语句末尾的分号（如果存在）
            sql = sql.replaceAll(";\\s*$", "").trim();

            // 解析ALTER语句类型
            if (sql.matches("(?i)^alter\\s+table\\s+\\w+\\s+add\\s+column\\s+\\w+\\s+\\w+.*$")) {
                return handleAddColumn(sql);
            } else if (sql.matches("(?i)^alter\\s+table\\s+\\w+\\s+modify\\s+column\\s+\\w+\\s+\\w+.*$")) {
                return handleModifyColumn(sql);
            } else if (sql.matches("(?i)^alter\\s+table\\s+\\w+\\s+drop\\s+column\\s+\\w+$")) {
                return handleDropColumn(sql);
            } else {
                return "ERROR: 不支持的ALTER TABLE语法\n支持的格式：\n"
                        + "ALTER TABLE 表名 ADD COLUMN 列名 类型 [约束]\n"
                        + "ALTER TABLE 表名 MODIFY COLUMN 列名 新类型 [新约束]\n"
                        + "ALTER TABLE 表名 DROP COLUMN 列名";
            }
        } catch (Exception e) {
            return "ERROR: 修改表结构失败 - " + e.getMessage();
        }
    }

    private static String handleAddColumn(String sql) throws IOException {
        Matcher matcher = Pattern.compile(
                "(?i)^alter\\s+table\\s+(\\w+)\\s+add\\s+column\\s+(\\w+)\\s+((?:\\w+)(?:\\([^)]+\\))?)(.*)$",
                Pattern.CASE_INSENSITIVE).matcher(sql);

        if (!matcher.find()) {
            return "ERROR: 语法错误，正确格式：ALTER TABLE 表名 ADD COLUMN 列名 类型 [约束]";
        }

        String tableName = matcher.group(1);
        String columnName = matcher.group(2);
        String columnType = matcher.group(3).toUpperCase();
        String constraints = matcher.group(4).trim().replaceAll(";\\s*$", "");

        return addColumnToTable(tableName, columnName, columnType, constraints);
    }

    private static String handleModifyColumn(String sql) throws IOException {
        Matcher matcher = Pattern.compile(
                "(?i)^alter\\s+table\\s+(\\w+)\\s+modify\\s+column\\s+(\\w+)\\s+((?:\\w+)(?:\\([^)]+\\))?)(.*)$",
                Pattern.CASE_INSENSITIVE).matcher(sql);

        if (!matcher.find()) {
            return "ERROR: 语法错误，正确格式：ALTER TABLE 表名 MODIFY COLUMN 列名 新类型 [新约束]";
        }

        String tableName = matcher.group(1);
        String columnName = matcher.group(2);
        String newType = matcher.group(3).toUpperCase();
        String newConstraints = matcher.group(4).trim().replaceAll(";\\s*$", "");

        return modifyColumnInTable(tableName, columnName, newType, newConstraints);
    }

    private static String handleDropColumn(String sql) throws IOException {
        Matcher matcher = Pattern.compile(
                "(?i)^alter\\s+table\\s+(\\w+)\\s+drop\\s+column\\s+(\\w+)$",
                Pattern.CASE_INSENSITIVE).matcher(sql);

        if (!matcher.find()) {
            return "ERROR: 语法错误，正确格式：ALTER TABLE 表名 DROP COLUMN 列名";
        }

        String tableName = matcher.group(1);
        String columnName = matcher.group(2);

        return dropColumnFromTable(tableName, columnName);
    }

    private static String addColumnToTable(String tableName, String columnName, String columnType, String constraints)
            throws IOException {
        String currentDbName = SQLConstant.getCurrentDbPath().substring(SQLConstant.getRootPath().length() + 1);
        String tdfPath = SQLConstant.getTableDefinitionPath(currentDbName, tableName);
        String trdPath = SQLConstant.getTableRecordPath(currentDbName, tableName);

        // 读取表结构
        List<String> tdfLines = Files.readAllLines(Paths.get(tdfPath));
        if (tdfLines.size() < 3) {
            return "ERROR: 表结构不完整";
        }

        // 检查列是否已存在
        String[] columns = tdfLines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));
        for (String col : columns) {
            if (col.equalsIgnoreCase(columnName)) {
                return "ERROR: 列 '" + columnName + "' 已存在";
            }
        }

        //更新表定义文件
        tdfLines.set(0, tdfLines.get(0) + SQLConstant.getFieldSeparator() + columnName);
        tdfLines.set(1, tdfLines.get(1) + SQLConstant.getFieldSeparator() + columnType);
        tdfLines.set(2, tdfLines.get(2) + SQLConstant.getFieldSeparator() + constraints);
        Files.write(Paths.get(tdfPath), tdfLines);

        //更新表记录文件
        List<String> trdLines = Files.readAllLines(Paths.get(trdPath));
        for (int i = 3; i < trdLines.size(); i++) {
            trdLines.set(i, trdLines.get(i) + SQLConstant.getFieldSeparator() + "null");
        }
        Files.write(Paths.get(trdPath), trdLines);

        return "操作成功: 成功添加列 " + columnName;
    }

    private static String modifyColumnInTable(String tableName, String columnName, String newType, String newConstraints)
            throws IOException {
        String currentDbName = SQLConstant.getCurrentDbPath().substring(SQLConstant.getRootPath().length() + 1);
        String tdfPath = SQLConstant.getTableDefinitionPath(currentDbName, tableName);

        //读取表结构
        List<String> tdfLines = Files.readAllLines(Paths.get(tdfPath));
        if (tdfLines.size() < 3) {
            return "ERROR: 表结构不完整";
        }

        //查找要修改的列索引
        String[] columns = tdfLines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));
        int columnIndex = -1;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex == -1) {
            return "ERROR: 列 '" + columnName + "' 不存在";
        }

        //更新表定义文件
        String[] types = tdfLines.get(1).split(Pattern.quote(SQLConstant.getFieldSeparator()));
        String[] constraints = tdfLines.get(2).split(Pattern.quote(SQLConstant.getFieldSeparator()));

        types[columnIndex] = newType;
        constraints[columnIndex] = newConstraints;

        tdfLines.set(1, String.join(SQLConstant.getFieldSeparator(), types));
        tdfLines.set(2, String.join(SQLConstant.getFieldSeparator(), constraints));
        Files.write(Paths.get(tdfPath), tdfLines);

        return "操作成功: 成功修改列 " + columnName;
    }

    private static String dropColumnFromTable(String tableName, String columnName) throws IOException {
        String currentDbName = SQLConstant.getCurrentDbPath().substring(SQLConstant.getRootPath().length() + 1);
        String tdfPath = SQLConstant.getTableDefinitionPath(currentDbName, tableName);
        String trdPath = SQLConstant.getTableRecordPath(currentDbName, tableName);

        //读取表结构
        List<String> tdfLines = Files.readAllLines(Paths.get(tdfPath));
        if (tdfLines.size() < 3) {
            return "ERROR: 表结构不完整";
        }

        //查找要删除的列索引
        String[] columns = tdfLines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));
        int columnIndex = -1;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex == -1) {
            return "ERROR: 列 '" + columnName + "' 不存在";
        }

        //更新表定义文件
        for (int i = 0; i < 3; i++) {
            String[] parts = tdfLines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            StringBuilder newLine = new StringBuilder();
            for (int j = 0; j < parts.length; j++) {
                if (j != columnIndex) {
                    if (newLine.length() > 0) {
                        newLine.append(SQLConstant.getFieldSeparator());
                    }
                    newLine.append(parts[j]);
                }
            }
            tdfLines.set(i, newLine.toString());
        }
        Files.write(Paths.get(tdfPath), tdfLines);

        //更新表记录文件
        List<String> trdLines = Files.readAllLines(Paths.get(trdPath));
        for (int i = 3; i < trdLines.size(); i++) {
            String[] values = trdLines.get(i).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            StringBuilder newLine = new StringBuilder();
            for (int j = 0; j < values.length; j++) {
                if (j != columnIndex) {
                    if (newLine.length() > 0) {
                        newLine.append(SQLConstant.getFieldSeparator());
                    }
                    newLine.append(values[j]);
                }
            }
            trdLines.set(i, newLine.toString());
        }
        Files.write(Paths.get(trdPath), trdLines);

        return "操作成功: 成功删除列 " + columnName;
    }
}