package myDatabase;

import model.Database;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class Create {
    public static String processCreateDatabase(String sql, Database database) {
        String dbName = sql.replaceAll("(?i)^create\\s+database\\s+(\\w+)\\s*;$", "$1").trim();

        if (!DatabaseValidator.isValidName(dbName)) {
            return "ERROR: 数据库名称不合法";
        }

        try {
            if (database.isDatabaseExists(dbName)) {
                return "ERROR: 数据库 '" + dbName + "' 已存在";
            }

            // 创建数据库目录
            File dbDir = new File(SQLConstant.getRootPath() + "\\" + dbName);
            if (!dbDir.mkdir()) {
                return "ERROR: 无法创建数据库目录";
            }

            // 创建表描述文件
            File tableDescFile = new File(SQLConstant.getTableDescPath(dbName));
            if (!tableDescFile.createNewFile()) {
                dbDir.delete();
                return "ERROR: 无法创建表描述文件";
            }

            // 创建日志文件
            File logFile = new File(SQLConstant.getLogFilePath(dbName));
            if (!logFile.createNewFile()) {
                dbDir.delete();
                tableDescFile.delete();
                return "ERROR: 无法创建日志文件";
            }

            // 更新数据库描述文件
            database.addDatabase(dbName);

            return "操作成功: 数据库 '" + dbName + "' 创建成功";
        } catch (IOException e) {
            return "ERROR: 创建数据库失败 - " + e.getMessage();
        }
    }

    public static String processCreateTable(String sql, Database database) {
        if (!Utils.bracketMatch(sql)) {
            return "ERROR: 括号不匹配";
        }

        // 提取表名
        String tableName = sql.replaceAll("(?i)^create\\s+table\\s+(\\w+)\\s*\\(.*\\)\\s*;$", "$1").trim();
        if (!DatabaseValidator.isValidName(tableName)) {
            return "ERROR: 表名不合法";
        }

        String dbName = database.getCurrentDatabase();
        if (dbName == null || dbName.isEmpty()) {
            return "ERROR: 请先选择数据库";
        }

        try {
            // 检查表是否已存在
            if (isTableExists(dbName, tableName)) {
                return "ERROR: 表 '" + tableName + "' 已存在";
            }

            // 创建所有必需的表文件
            createTableFiles(dbName, tableName);

            // 解析表结构并写入定义文件
            TableSchema schema = parseTableDefinition(sql);
            writeTableSchema(dbName, tableName, schema);

            // 初始化记录文件（关键修改点：添加这行调用）
            initializeRecordFile(dbName, tableName, schema);

            // 更新数据库描述
            database.addTable(dbName, tableName);

            return "操作成功: 表 '" + tableName + "' 创建成功";
        } catch (InvalidDefinitionException e) {
            deleteTableFiles(dbName, tableName);
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            deleteTableFiles(dbName, tableName);
            return "ERROR: 表创建失败 - " + e.getMessage();
        }
    }

    private static void initializeRecordFile(String dbName, String tableName, TableSchema schema) throws IOException {
        String trdPath = SQLConstant.getTableRecordPath(dbName, tableName);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(trdPath))) {
            // 写入列标题行
            writer.write(String.join(SQLConstant.getFieldSeparator(),
                    schema.columns.stream().map(c -> c.name).collect(Collectors.toList())));
            writer.newLine();

            // 写入类型行
            writer.write(String.join(SQLConstant.getFieldSeparator(),
                    schema.columns.stream().map(c -> c.type).collect(Collectors.toList())));
            writer.newLine();

            // 写入约束行
            writer.write(String.join(SQLConstant.getFieldSeparator(),
                    schema.columns.stream().map(c -> String.join(" ", c.constraints)).collect(Collectors.toList())));
            writer.newLine();
        }
    }
    private static boolean isTableExists(String dbName, String tableName) throws IOException {
        String tableDescPath = SQLConstant.getTableDescPath(dbName);
        File tableDescFile = new File(tableDescPath);

        if (!tableDescFile.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(tableDescFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(tableName + "|")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void createTableFiles(String dbName, String tableName) throws IOException {
        String[] extensions = {
                SQLConstant.getTableDefinitionExtension(),
                SQLConstant.getTableRecordExtension(),
                SQLConstant.getTableIntegrityExtension(),
                SQLConstant.getTableIndexExtension()
        };

        for (String ext : extensions) {
            File file = new File(SQLConstant.getRootPath() + "\\" + dbName + "\\" + tableName + ext);
            if (!file.createNewFile()) {
                throw new IOException("无法创建文件: " + file.getPath());
            }
        }
    }

    private static void deleteTableFiles(String dbName, String tableName) {
        String[] extensions = {
                SQLConstant.getTableDefinitionExtension(),
                SQLConstant.getTableRecordExtension(),
                SQLConstant.getTableIntegrityExtension(),
                SQLConstant.getTableIndexExtension()
        };

        for (String ext : extensions) {
            File file = new File(SQLConstant.getRootPath() + "\\" + dbName + "\\" + tableName + ext);
            file.delete();
        }
    }

    private static TableSchema parseTableDefinition(String sql) throws InvalidDefinitionException {
        // 提取括号内的内容，考虑换行符情况
        Matcher matcher = Pattern.compile("\\(([\\s\\S]*)\\)").matcher(sql);
        if (!matcher.find()) {
            throw new InvalidDefinitionException("无效的表定义语法");
        }

        String fieldsStr = matcher.group(1).trim();
        List<String> fields = new ArrayList<>();

        // 改进的字段分割逻辑，正确处理嵌套括号
        int start = 0;
        int parenLevel = 0;
        boolean inString = false;

        for (int i = 0; i < fieldsStr.length(); i++) {
            char c = fieldsStr.charAt(i);
            if (c == '\'') inString = !inString;
            if (!inString) {
                if (c == '(') parenLevel++;
                if (c == ')') parenLevel--;
                if (c == ',' && parenLevel == 0) {
                    fields.add(fieldsStr.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        fields.add(fieldsStr.substring(start).trim());

        TableSchema schema = new TableSchema();
        Set<String> columnNames = new HashSet<>();

        for (String field : fields) {
            if (field.isEmpty()) continue;

            // 改进的字段解析正则表达式
            Matcher fieldMatcher = Pattern.compile(
                    "(\\w+)\\s+" +                   // 列名
                            "(\\w+(?:\\([^)]*\\))?)\\s*" +   // 类型定义
                            "(.*)?"                          // 约束部分
            ).matcher(field);

            if (!fieldMatcher.find()) {
                throw new InvalidDefinitionException("字段定义格式错误: " + field);
            }

            ColumnDefinition col = new ColumnDefinition();
            col.name = fieldMatcher.group(1);
            col.type = fieldMatcher.group(2).toUpperCase();

            // 处理约束条件（保留原始格式）
            String constraints = fieldMatcher.group(3);
            if (constraints != null && !constraints.trim().isEmpty()) {
                col.constraints.add(constraints.trim());
            }

            // 验证和添加列
            if (!DatabaseValidator.isValidName(col.name)) {
                throw new InvalidDefinitionException("无效的列名: " + col.name);
            }
            if (columnNames.contains(col.name)) {
                throw new InvalidDefinitionException("重复的字段名: " + col.name);
            }
            columnNames.add(col.name);
            schema.columns.add(col);
        }

        return schema;
    }
    // 在Create.java中，确保writeTableSchema方法正确写入所有字段
    private static void writeTableSchema(String dbName, String tableName, TableSchema schema) throws IOException {
        String tdfPath = SQLConstant.getTableDefinitionPath(dbName, tableName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tdfPath))) {
            // 写入列名行
            writer.write(schema.columns.stream()
                    .map(c -> c.name)
                    .collect(Collectors.joining(SQLConstant.getFieldSeparator())));
            writer.newLine();

            // 写入类型行（保留完整类型定义）
            writer.write(schema.columns.stream()
                    .map(c -> c.type)
                    .collect(Collectors.joining(SQLConstant.getFieldSeparator())));
            writer.newLine();

            // 写入约束行（保留原始约束格式）
            writer.write(schema.columns.stream()
                    .map(c -> String.join(" ", c.constraints))
                    .collect(Collectors.joining(SQLConstant.getFieldSeparator())));
            writer.newLine();
        }
    }


    private static class TableSchema {
        List<ColumnDefinition> columns = new ArrayList<>();
    }

    private static class ColumnDefinition {
        String name;
        String type;
        List<String> constraints = new ArrayList<>();
    }

    private static class InvalidDefinitionException extends Exception {
        public InvalidDefinitionException(String message) {
            super(message);
        }
    }
}