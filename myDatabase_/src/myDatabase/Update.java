package myDatabase;

import model.Database;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Update {
    public static String processUpdate(String sql, Database database) {
        try {
            String[] parts = sql.split("(?i)\\bwhere\\b");
            if (parts.length < 1) {
                return "ERROR: Invalid UPDATE syntax";
            }

            String setPart = parts[0].replaceAll("(?i)^update\\s+\\w+\\s+set\\s+", "").trim();
            String wherePart = parts.length > 1 ? parts[1].replaceAll(";\\s*$", "").trim() : "";

            String tableName = sql.replaceAll("(?i)^update\\s+(\\w+).*", "$1").trim();
            String tablePath = SQLConstant.getCurrentTableRecordPath(tableName);

            if (!Files.exists(Paths.get(tablePath))) {
                return "ERROR: Table '" + tableName + "' does not exist";
            }

            // 读取所有记录
            List<String> lines = Files.readAllLines(Paths.get(tablePath));
            if (lines.size() < 4) { // 3行表结构 + 至少1行数据
                return "ERROR: No records found (empty table)";
            }

            String[] columns = lines.get(0).split(Pattern.quote(SQLConstant.getFieldSeparator()));
            List<String> updatedLines = new ArrayList<>(lines.subList(0, 3)); // 保留表结构

            int updatedCount = 0;
            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] values = line.split(Pattern.quote(SQLConstant.getFieldSeparator()), -1);

                if (matchesCondition(wherePart, columns, values)) {
                    String[] updatedValues = applyUpdates(setPart, columns, Arrays.copyOf(values, values.length));
                    updatedLines.add(String.join(SQLConstant.getFieldSeparator(), updatedValues));
                    updatedCount++;
                } else {
                    updatedLines.add(line);
                }
            }

            if (updatedCount > 0) {
                Files.write(Paths.get(tablePath), updatedLines,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                return "Query OK: " + updatedCount + " row(s) affected";
            } else {
                return "Query OK: 0 rows affected (no matching records)";
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private static boolean matchesCondition(String where, String[] columns, String[] values) {
        if (where == null || where.trim().isEmpty()) {
            return true;
        }

        String[] conditions = where.split("(?i)\\s+and\\s+");
        for (String cond : conditions) {
            Matcher matcher = Pattern.compile("(\\w+)\\s*([=!<>]+)\\s*(.+)").matcher(cond.trim());
            if (!matcher.find()) continue;

            String column = matcher.group(1).trim();
            String operator = matcher.group(2).trim();
            String expectedValue = matcher.group(3).trim().replaceAll("^['\"]|['\"]$", "");

            int colIndex = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase(column)) {
                    colIndex = i;
                    break;
                }
            }

            if (colIndex < 0 || colIndex >= values.length) {
                return false;
            }

            String actualValue = values[colIndex] == null ? "" :
                    values[colIndex].replaceAll("^['\"]|['\"]$", "");

            if (isNumeric(expectedValue) && isNumeric(actualValue)) {
                try {
                    double expNum = Double.parseDouble(expectedValue);
                    double actNum = Double.parseDouble(actualValue);

                    switch (operator) {
                        case "=": if (actNum != expNum) return false; break;
                        case "!=": if (actNum == expNum) return false; break;
                        case ">": if (actNum <= expNum) return false; break;
                        case "<": if (actNum >= expNum) return false; break;
                        default: return false;
                    }
                } catch (NumberFormatException e) {}
            } else {
                switch (operator) {
                    case "=": if (!actualValue.equals(expectedValue)) return false; break;
                    case "!=": if (actualValue.equals(expectedValue)) return false; break;
                    default: return false;
                }
            }
        }
        return true;
    }

    private static String[] applyUpdates(String setClause, String[] columns, String[] values) {
        String[] updates = setClause.split("\\s*,\\s*");
        for (String update : updates) {
            String[] parts = update.split("\\s*=\\s*", 2);
            if (parts.length != 2) continue;

            String column = parts[0].trim();
            String newValueExpr = parts[1].trim();

            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase(column)) {
                    if (newValueExpr.contains(column)) {
                        try {
                            double current = values[i] == null || values[i].isEmpty() ? 0 :
                                    Double.parseDouble(values[i].replaceAll("^['\"]|['\"]$", ""));
                            double result = evaluateExpression(current, newValueExpr.replace(column, ""));
                            values[i] = String.valueOf(result);
                        } catch (Exception e) {
                            System.err.println("[WARN] Failed to calculate expression: " + newValueExpr);
                        }
                    } else {
                        // Remove quotes from string values
                        if (newValueExpr.startsWith("'") && newValueExpr.endsWith("'")) {
                            newValueExpr = newValueExpr.substring(1, newValueExpr.length() - 1);
                        }
                        values[i] = newValueExpr;
                    }
                    break;
                }
            }
        }
        return values;
    }

    private static double evaluateExpression(double current, String expr) {
        expr = expr.replaceAll("\\s+", "");
        if (expr.startsWith("+")) {
            return current + Double.parseDouble(expr.substring(1));
        } else if (expr.startsWith("-")) {
            return current - Double.parseDouble(expr.substring(1));
        } else if (expr.startsWith("*")) {
            return current * Double.parseDouble(expr.substring(1));
        } else if (expr.startsWith("/")) {
            return current / Double.parseDouble(expr.substring(1));
        }
        return current;
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
}