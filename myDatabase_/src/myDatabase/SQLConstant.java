package myDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 数据库系统常量配置类
 */
public class SQLConstant {
    // 数据库根目录路径
    private static final String ROOT_PATH = "D:\\desktop\\DBMS\\DATA";
    // 当前使用的数据库路径
    private static String currentDbPath = ROOT_PATH;

    // 系统文件配置
    private static final String SYSTEM_DIR_NAME = "system";
    private static final String DB_DESCRIPTION_FILE = "ruanko.db";
    private static final String USER_DATA_FILE = "users.dat";

    // 数据库文件扩展名
    private static final String TABLE_DESC_EXTENSION = ".tb";
    private static final String LOG_FILE_EXTENSION = ".log";

    // 表文件扩展名
    private static final String TABLE_DEFINITION_EXTENSION = ".tdf";
    private static final String TABLE_RECORD_EXTENSION = ".trd";
    private static final String TABLE_INTEGRITY_EXTENSION = ".tic";
    private static final String TABLE_INDEX_EXTENSION = ".tid";

    // 新增方法：获取用户数据文件路径
    public static String getUserDataPath() {
        return getRootPath() + "\\" + USER_DATA_FILE;
    }

    // 新增方法：获取系统表文件名
    public static String getSystemTableFileName() {
        return SYSTEM_DIR_NAME + TABLE_DESC_EXTENSION; // 返回"system.tb"
    }

    public static String getRootPath() {
        return ROOT_PATH;
    }



    public static String getCurrentDbPath() {
        return currentDbPath;
    }

    public static void setCurrentDbPath(String dbName) {
        currentDbPath = ROOT_PATH + "\\" + dbName;
    }

    public static String getFieldSeparator() {
        return "\u0001"; // 使用不可见字符作为分隔符
    }

    public static String getSystemDirName() {
        return SYSTEM_DIR_NAME;
    }

    public static String getDbDescriptionFilePath() {
        return ROOT_PATH + "\\" + DB_DESCRIPTION_FILE;
    }

    public static String getBackupDirPath() {
        //return ROOT_PATH + "\\" + BACKUP_DIR;
        String path = getRootPath() + File.separator + "backup";
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException ignored) {}
        return path;

    }

    public static String getTableDefinitionExtension() {
        return TABLE_DEFINITION_EXTENSION;
    }

    public static String getTableRecordExtension() {
        return TABLE_RECORD_EXTENSION;
    }

    public static String getTableIntegrityExtension() {
        return TABLE_INTEGRITY_EXTENSION;
    }

    public static String getTableIndexExtension() {
        return TABLE_INDEX_EXTENSION;
    }


    // 路径构建方法
    public static String getTableDefinitionPath(String dbName, String tableName) {
        return ROOT_PATH + "\\" + dbName + "\\" + tableName + TABLE_DEFINITION_EXTENSION;
    }

    public static String getTableRecordPath(String dbName, String tableName) {
        return ROOT_PATH + "\\" + dbName + "\\" + tableName + TABLE_RECORD_EXTENSION;
    }

    public static String getTableIntegrityPath(String dbName, String tableName) {
        return ROOT_PATH + "\\" + dbName + "\\" + tableName + TABLE_INTEGRITY_EXTENSION;
    }

    public static String getTableIndexPath(String dbName, String tableName) {
        return ROOT_PATH + "\\" + dbName + "\\" + tableName + TABLE_INDEX_EXTENSION;
    }

    public static String getTableDescPath(String dbName) {
        return ROOT_PATH + "\\" + dbName + "\\" + dbName + TABLE_DESC_EXTENSION;
    }

    public static String getLogFilePath(String dbName) {
        return ROOT_PATH + "\\" + dbName + "\\" + dbName + LOG_FILE_EXTENSION;
    }


    // 获取当前数据库的表记录文件路径
    public static String getCurrentTableRecordPath(String tableName) {
        return getCurrentDbPath() + "\\" + tableName + TABLE_RECORD_EXTENSION;
    }


}