package myDatabase;

/**
 * Help:帮助信息类，列出所有帮助信息
 */
public class Help {
    public static String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 数据库管理系统帮助 ==========\n");
        sb.append("1. 数据库操作:\n");
        sb.append("   create database 数据库名; - 创建数据库\n");
        sb.append("   show databases; - 列出所有数据库\n");
        sb.append("   use 数据库名; - 切换当前数据库\n");
        sb.append("   drop database 数据库名; - 删除数据库\n\n");

        sb.append("2. 表操作:\n");
        sb.append("   create table 表名(字段1 类型 约束, ...); - 创建表\n");
        sb.append("   show tables; - 列出当前数据库的所有表\n");
        sb.append("   describe 表名; - 显示表结构\n");
        sb.append("   drop table 表名; - 删除表\n");
        sb.append("   alter table 表名 add column 列名 类型 [约束]; - 添加列\n");
        sb.append("   alter table 表名 drop column 列名; - 删除列\n\n");

        sb.append("3. 数据操作:\n");
        sb.append("   insert into 表名(字段1,...) values(值1,...); - 插入数据\n");
        sb.append("   select * from 表名; - 查询表中所有数据\n");
        sb.append("   update 表名 set 字段=值 where 条件; - 更新数据\n");
        sb.append("   delete from 表名 [where 条件]; - 删除数据\n\n");

        sb.append("4. 其他命令:\n");
        sb.append("   help; - 显示本帮助信息\n");
        sb.append("   quit; - 退出系统\n");
        sb.append("====================================\n");
        return sb.toString();
    }

}