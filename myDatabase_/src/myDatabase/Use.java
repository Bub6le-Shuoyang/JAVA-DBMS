package myDatabase;

import model.Database;
import java.io.File;

public class Use {
    public static String useSql(String sql, Database database) {
        try {

            String dbName = sql.replaceAll("(?i)^use\\s+(\\w+)\\s*;?\\s*$", "$1").trim();

            if (dbName.isEmpty()) {
                return "ERROR: 语法错误，正确格式：USE 数据库名";
            }

            File dbDir = new File(SQLConstant.getRootPath() + "\\" + dbName);
            if (!dbDir.exists() || !dbDir.isDirectory()) {
                return "ERROR: 数据库 '" + dbName + "' 不存在";
            }

            File tableDescFile = new File(SQLConstant.getTableDescPath(dbName));
            if (!tableDescFile.exists()) {
                return "ERROR: 数据库 '" + dbName + "' 结构不完整";
            }

            if (database.setCurrentDatabase(dbName)) {
                return "切换到数据库" + dbName;
            } else {
                return "ERROR: 无法切换到数据库 '" + dbName + "'";
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}