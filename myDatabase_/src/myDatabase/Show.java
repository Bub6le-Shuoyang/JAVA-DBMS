package myDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现SHOW命令 - 修改版
 * 返回字符串结果而不是直接打印到控制台
 */
public class Show {

    /**
     * 处理SHOW语句并返回执行结果字符串
     */
    public static String processShow(String sql) {
        String normalized = sql.toLowerCase().trim();
        if (normalized.matches("show\\s+databases\\s*;?")) {
            return showDatabases();
        } else if (normalized.matches("show\\s+tables\\s*;?")) {
            return showTables();
        }
        return "ERROR: 无法识别的SHOW语句";
    }

    /**
     * 保留原有方法供兼容使用
     */
    public static void showSql(String sql) {
        System.out.println(processShow(sql));
    }

    /**
     * 显示所有数据库
     */
    private static String showDatabases() {
        String path = SQLConstant.getRootPath();
        List<String> dbList = Utils.getAllDatabase(path);

        if (dbList.isEmpty()) {
            return "没有可用的数据库";
        }

        List<String> headers = new ArrayList<>();
        headers.add("Database");

        List<List<String>> rows = new ArrayList<>();
        for (String db : dbList) {
            List<String> row = new ArrayList<>();
            row.add(db);
            rows.add(row);
        }

        return TableGenerator.generateTable(headers, rows);
    }

    /**
     * 显示当前数据库所有表
     */
    private static String showTables() {
        String nowPath = SQLConstant.getCurrentDbPath();
        List<String> tableList = Utils.getAllTables(nowPath);

        if (tableList.isEmpty()) {
            return "当前数据库中没有表";
        }

        // 获取数据库名
        int index = nowPath.lastIndexOf("\\");
        String dbName = nowPath.substring(index + 1);

        List<String> headers = new ArrayList<>();
        headers.add(dbName);

        List<List<String>> rows = new ArrayList<>();
        for (String table : tableList) {
            List<String> row = new ArrayList<>();
            row.add(table);
            rows.add(row);
        }

        return TableGenerator.generateTable(headers, rows);
    }
}