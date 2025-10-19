package model;

import myDatabase.SQLConstant;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Database类，管理数据库连接和状态
 */
public class Database {
    private String currentDatabase;
    private Map<String, List<String>> databaseTables = new HashMap<>();
    private Map<String, User> users = new HashMap<>();
    private List<DatabaseChangeListener> databaseChangeListeners = new ArrayList<>();

    // 数据库变更监听器接口
    public interface DatabaseChangeListener {
        void onDatabaseChanged(String dbName);
    }

    public Database() {
        loadUsers();
    }

    // 添加数据库变更监听器
    public void addDatabaseChangeListener(DatabaseChangeListener listener) {
        databaseChangeListeners.add(listener);
    }

    // 移除数据库变更监听器
    public void removeDatabaseChangeListener(DatabaseChangeListener listener) {
        databaseChangeListeners.remove(listener);
    }

    // 通知监听器数据库已变更
    private void notifyDatabaseChanged(String dbName) {
        for (DatabaseChangeListener listener : databaseChangeListeners) {
            listener.onDatabaseChanged(dbName);
        }
    }

    // 加载用户数据
    private void loadUsers() {
        File file = new File(SQLConstant.getUserDataPath());
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
            } catch (Exception e) {
                System.err.println("加载用户数据失败: " + e.getMessage());
            }
        }
    }

    // 保存用户数据
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SQLConstant.getUserDataPath()))) {
            oos.writeObject(users);
        } catch (Exception e) {
            System.err.println("保存用户数据失败: " + e.getMessage());
        }
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }

    public boolean setCurrentDatabase(String dbName) throws IOException {
        // 直接检查数据库目录是否存在
        File dbDir = new File(SQLConstant.getRootPath() + "\\" + dbName);
        if (!dbDir.exists() || !dbDir.isDirectory()) {
            return false;
        }

        // 检查是否有表描述文件
        File tableDescFile = new File(SQLConstant.getTableDescPath(dbName));
        if (!tableDescFile.exists()) {
            return false;
        }

        this.currentDatabase = dbName;
        SQLConstant.setCurrentDbPath(dbName);
        notifyDatabaseChanged(dbName); // 通知监听器数据库已变更
        return true;
    }

    // 添加验证方法
    public boolean validateCurrentDatabase() {
        return currentDatabase != null && !currentDatabase.isEmpty();
    }

    public User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public boolean registerUser(String username, String password, String email) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, new User(username, password, email));
        saveUsers();
        return true;
    }

    // 增强的刷新方法
    public synchronized void refreshDatabaseList() throws IOException {
        databaseTables.clear();
        File rootDir = new File(SQLConstant.getRootPath());
        File[] dbDirs = rootDir.listFiles(File::isDirectory);

        if (dbDirs != null) {
            for (File dbDir : dbDirs) {
                String dbName = dbDir.getName();
                if (!dbName.equals(SQLConstant.getSystemDirName())) {
                    databaseTables.put(dbName, loadTables(dbName));
                }
            }
        }
        // 更新系统数据库
        databaseTables.put(SQLConstant.getSystemDirName(), loadTables(SQLConstant.getSystemDirName()));
    }

    // 数据库描述文件操作
    public synchronized void addDatabase(String dbName) throws IOException {
        File dbFile = new File(SQLConstant.getDbDescriptionFilePath());
        if (!dbFile.exists()) {
            dbFile.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            writer.write(dbName + "\n");
        }
        databaseTables.put(dbName, new ArrayList<>());
        notifyDatabaseChanged(dbName); // 通知监听器数据库已变更
    }

    public synchronized void removeDatabase(String dbName) throws IOException {
        File dbFile = new File(SQLConstant.getDbDescriptionFilePath());
        List<String> databases = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().equalsIgnoreCase(dbName)) {
                    databases.add(line);
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            for (String db : databases) {
                writer.write(db + "\n");
            }
        }
        databaseTables.remove(dbName);
        notifyDatabaseChanged(null); // 通知监听器数据库已删除
    }

    public synchronized boolean isDatabaseExists(String dbName) throws IOException {
        // 检查内存中的数据库列表
        if (databaseTables.containsKey(dbName)) {
            return true;
        }

        // 检查文件系统
        File dbDir = new File(SQLConstant.getRootPath() + "\\" + dbName);
        if (!dbDir.exists() || !dbDir.isDirectory()) {
            return false;
        }

        // 检查表描述文件是否存在
        File tableDescFile = new File(SQLConstant.getTableDescPath(dbName));
        return tableDescFile.exists();
    }

    // 表描述文件操作
    public synchronized void addTable(String dbName, String tableName) throws IOException {
        String tableDescPath = SQLConstant.getTableDescPath(dbName);
        File tableDescFile = new File(tableDescPath);

        if (!tableDescFile.exists()) {
            tableDescFile.createNewFile();
        }

        String tdfPath = SQLConstant.getTableDefinitionPath(dbName, tableName);
        String trdPath = SQLConstant.getTableRecordPath(dbName, tableName);
        String ticPath = SQLConstant.getTableIntegrityPath(dbName, tableName);
        String tidPath = SQLConstant.getTableIndexPath(dbName, tableName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableDescFile, true))) {
            writer.write(String.format("%s|%s|%s|%s|%s\n",
                    tableName, tdfPath, trdPath, ticPath, tidPath));
        }

        if (!databaseTables.containsKey(dbName)) {
            databaseTables.put(dbName, new ArrayList<>());
        }
        databaseTables.get(dbName).add(tableName);
        notifyDatabaseChanged(dbName); // 通知监听器表结构已变更
    }

    public synchronized void removeTable(String dbName, String tableName) throws IOException {
        String tableDescPath = SQLConstant.getTableDescPath(dbName);
        File tableDescFile = new File(tableDescPath);
        List<String> tables = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(tableDescFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(tableName + "|")) {
                    tables.add(line);
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableDescFile))) {
            for (String table : tables) {
                writer.write(table + "\n");
            }
        }

        if (databaseTables.containsKey(dbName)) {
            databaseTables.get(dbName).remove(tableName);
        }
        notifyDatabaseChanged(dbName); // 通知监听器表结构已变更
    }

    // 数据库初始化
    public void initialize() throws IOException {
        // 创建系统数据库
        if (!isDatabaseExists(SQLConstant.getSystemDirName())) {
            addDatabase(SQLConstant.getSystemDirName());
            File systemDir = new File(SQLConstant.getRootPath() + "\\" + SQLConstant.getSystemDirName());
            systemDir.mkdir();

            // 创建系统表描述文件
            File systemTableFile = new File(SQLConstant.getTableDescPath(SQLConstant.getSystemDirName()));
            systemTableFile.createNewFile();

            // 创建系统日志文件
            File systemLogFile = new File(SQLConstant.getLogFilePath(SQLConstant.getSystemDirName()));
            systemLogFile.createNewFile();
        }

        // 加载现有数据库
        File dbFile = new File(SQLConstant.getDbDescriptionFilePath());
        if (dbFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String dbName = line.trim();
                    if (!dbName.isEmpty()) {
                        databaseTables.put(dbName, loadTables(dbName));
                    }
                }
            }
        }
    }

    private List<String> loadTables(String dbName) throws IOException {
        List<String> tables = new ArrayList<>();
        String tableDescPath = SQLConstant.getTableDescPath(dbName);
        File tableDescFile = new File(tableDescPath);

        if (tableDescFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(tableDescFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length > 0) {
                        tables.add(parts[0]);
                    }
                }
            }
        }
        return tables;
    }
}