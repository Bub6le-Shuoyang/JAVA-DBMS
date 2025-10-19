package controller;

import model.Database;
import myDatabase.*;


/**
 * DatabaseProcessor类。解析不同类型的SQL语句，分发到相应的处理类
 */
public class DatabaseProcessor {
    public static String executeQuery(String sql, Database database) {
        System.out.println("[DEBUG][Processor] Received SQL: " + sql);
        String normalizedSql = sql.toLowerCase().trim();

        try {
            String result = null;
            if (normalizedSql.equals("help;")) {
                result = Help.getHelpText();
                return Help.getHelpText();
            } else if (normalizedSql.startsWith("select")) {
                return Select.processSelect(sql);
            } else if (normalizedSql.startsWith("insert")) {
                return Insert.processInsert(sql);
            } else if (normalizedSql.startsWith("update")) {
                System.out.println("[DEBUG][Processor] Routing to Update processor");
                result = Update.processUpdate(sql, database);
                return Update.processUpdate(sql, database);
            } else if (normalizedSql.startsWith("delete")) {
                return Delete.processDelete(sql);
            } else if (normalizedSql.startsWith("create table")) {
                return Create.processCreateTable(sql, database);
            } else if (normalizedSql.startsWith("alter table")) {
                return Alter.processAlter(sql);
            } else if (normalizedSql.startsWith("describe")) {
                return Describe.describeSql(sql);
            } else if (normalizedSql.startsWith("show")) {
                return Show.processShow(sql);
            } else if (normalizedSql.startsWith("drop table")) {
                return Drop.processDropTable(sql, database);
            } else if (normalizedSql.startsWith("use")) {
                return Use.useSql(sql, database);
            } else if (normalizedSql.startsWith("create database")) {
                return Create.processCreateDatabase(sql, database);
            } else if (normalizedSql.startsWith("drop database")) {
                return Drop.processDropDatabase(sql, database);
            } else if (normalizedSql.startsWith("backup database")) {
                return BackupRestore.backupDatabase(database);
            } else if (normalizedSql.startsWith("restore database")) {
                return BackupRestore.restoreDatabase(database);
            } else {
                System.out.println("[DEBUG][Processor] Execution result: " + result);
                return "ERROR: 不支持的SQL语句类型";
            }
        } catch (Exception e) {
            String error = "ERROR: " + e.getMessage();
            System.out.println("[DEBUG][Processor] Error: " + error);
            return error;
        }
    }
}