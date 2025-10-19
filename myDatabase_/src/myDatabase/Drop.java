package myDatabase;

import model.Database;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Drop {
    public static String processDropDatabase(String sql, Database database) {
        String dbName = sql.replaceAll("(?i)^drop\\s+database\\s+(\\w+)\\s*;$", "$1").trim();

        // 系统数据库保护
        if (dbName.equalsIgnoreCase("system") || dbName.equalsIgnoreCase("backup")) {
            return "ERROR: 系统数据库不能被删除";
        }

        try {
            // 强制刷新数据库列表
            database.refreshDatabaseList();

            // 双重检查数据库是否存在
            if (!database.isDatabaseExists(dbName)) {
                return "ERROR: 数据库 '" + dbName + "' 不存在";
            }

            // 检查是否正在使用
            if (database.getCurrentDatabase() != null &&
                    database.getCurrentDatabase().equalsIgnoreCase(dbName)) {
                return "ERROR: 不能删除当前正在使用的数据库";
            }

            // 删除数据库目录
            String dbPath = SQLConstant.getRootPath() + "\\" + dbName;
            File dbDir = new File(dbPath);

            if (!dbDir.exists()) {
                database.removeDatabase(dbName);
                return "Query OK: 数据库 '" + dbName + "' 已标记删除";
            }

            // 尝试多次删除（解决文件锁定问题）
            boolean deleted = false;
            for (int i = 0; i < 3; i++) {
                if (deleteDirectory(dbDir)) {
                    deleted = true;
                    break;
                }
                System.gc();
                Thread.sleep(100);
            }

            if (!deleted) {
                return "ERROR: 无法删除数据库目录（可能被占用）";
            }

            // 从数据库描述文件中移除
            database.removeDatabase(dbName);
            database.refreshDatabaseList();

            return "Query OK: 数据库 '" + dbName + "' 删除成功";
        } catch (Exception e) {
            return "ERROR: 删除数据库失败 - " + e.getMessage();
        }
    }
    public static String processDropTable(String sql, Database database) {
        String tableName = sql.replaceAll("(?i)^drop\\s+table\\s+(\\w+)\\s*;$", "$1").trim();

        String dbName = database.getCurrentDatabase();
        if (dbName == null || dbName.isEmpty()) {
            return "ERROR: 请先选择数据库";
        }

        try {
            if (!isTableExists(dbName, tableName)) {
                return "ERROR: 表 '" + tableName + "' 不存在";
            }

            // 删除表文件
            deleteTableFiles(dbName, tableName);

            // 从表描述文件中移除
            database.removeTable(dbName, tableName);

            return "Query OK: 表 '" + tableName + "' 删除成功";
        } catch (IOException e) {
            return "ERROR: 删除表失败 - " + e.getMessage();
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

    // 优化目录删除方法
    private static boolean deleteDirectory(File directory) {
        if (!directory.exists()) return true;

        // 尝试设置可写权限
        directory.setWritable(true);

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.setWritable(true);
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }
}