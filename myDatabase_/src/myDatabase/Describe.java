package myDatabase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class Describe {
    public static String describeSql(String sql) {
        try {
            String tableName = sql.replaceAll("(?i)^describe\\s+(\\w+)\\s*;?\\s*$", "$1").trim();
            String tablePath = SQLConstant.getCurrentDbPath() + File.separator +
                    tableName + SQLConstant.getTableDefinitionExtension();

            // 读取表结构文件
            List<String> lines = Files.readAllLines(Paths.get(tablePath));
            if (lines.size() < 3) {
                return "ERROR: 表结构不完整";
            }

            // 使用严格的分隔符解析
            String separator = Pattern.quote(SQLConstant.getFieldSeparator());
            String[] columns = lines.get(0).split(separator, -1);
            String[] types = lines.get(1).split(separator, -1);
            String[] constraints = lines.get(2).split(separator, -1);

            // 构建表格数据
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < columns.length; i++) {
                List<String> row = new ArrayList<>();
                row.add(columns[i]);
                row.add(types[i]);
                row.add(i < constraints.length ? constraints[i] : "");
                rows.add(row);
            }

            return TableGenerator.generateTable(
                    Arrays.asList("Field", "Type", "Constraints"),
                    rows
            );
        } catch (Exception e) {
            return "ERROR: 描述表失败 - " + e.getMessage();
        }
    }
}