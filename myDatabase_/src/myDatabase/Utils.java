package myDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Utils {
    public static List<String> getAllDatabase(String path) {
        List<String> list = new ArrayList<>();
        File file = new File(path);
        File[] fileList = file.listFiles();


        list.add(SQLConstant.getSystemDirName());

        if (fileList != null) {
            for (File f : fileList) {
                if (f.isDirectory() && !f.getName().equalsIgnoreCase(SQLConstant.getSystemDirName())) {
                    list.add(f.getName());
                }
            }
        }
        return list;
    }

    public static boolean bracketMatch(String sql) {
        Stack<String> stack = new Stack<>();
        char[] chars = sql.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '(') {
                stack.push("(");
            } else if (chars[i] == ')') {
                if (stack.empty()) {
                    return false;
                } else {
                    stack.pop();
                }
            }
        }
        return stack.empty();
    }

    public static List<String> getAllTables(String nowPath) {
        List<String> list = new ArrayList<>();
        File dir = new File(nowPath);
        File[] files = dir.listFiles((dir1, name) ->
                name.endsWith(SQLConstant.getTableDefinitionExtension()) &&
                        !name.equals(SQLConstant.getSystemTableFileName()));

        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                list.add(name.substring(0, name.lastIndexOf('.')));
            }
        }
        return list;
    }
}