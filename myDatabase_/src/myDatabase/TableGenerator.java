package myDatabase;

import java.util.*;
import java.util.function.Function;

/**
 * 生成表格的类, 仅对英文的表格有效
 */
public class TableGenerator {

    private static int PADDING_SIZE = 2;
    private static String NEW_LINE = "\n";
    private static String TABLE_JOINT_SYMBOL = "+";
    private static String TABLE_V_SPLIT_SYMBOL = "|";
    private static String TABLE_H_SPLIT_SYMBOL = "-";

    /**
     *
     * @param headersList
     * @param rowsList
     * @param overRiddenHeaderHeight
     * @return
     */

    public static String generateTable(List<String> headersList, List<List<String>> rowsList,
                                       int... overRiddenHeaderHeight) {
        StringBuilder stringBuilder = new StringBuilder();

        int rowHeight = overRiddenHeaderHeight.length > 0 ? overRiddenHeaderHeight[0] : 1;

        Map<Integer, Integer> columnMaxWidthMapping = getMaximumWidhtofTable(headersList, rowsList);

        createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
        stringBuilder.append(NEW_LINE);

        for (int headerIndex = 0; headerIndex < headersList.size(); headerIndex++) {
            fillCell(stringBuilder, headersList.get(headerIndex), headerIndex, columnMaxWidthMapping);
        }
        stringBuilder.append(NEW_LINE);
        createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);

        for (List<String> row : rowsList) {
            for (int i = 0; i < rowHeight; i++) {
                stringBuilder.append(NEW_LINE);
            }

            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                fillCell(stringBuilder, row.get(cellIndex), cellIndex, columnMaxWidthMapping);
            }
        }

        stringBuilder.append(NEW_LINE);
        createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
        return stringBuilder.toString();
    }



    private static void fillSpace(StringBuilder stringBuilder, int length) {
        for (int i = 0; i < length - 1; i++) {
            stringBuilder.append(" ");
        }
    }

    private static void createRowLine(StringBuilder stringBuilder, int headersListSize,
                                      Map<Integer, Integer> columnMaxWidthMapping) {
        for (int i = 0; i < headersListSize; i++) {
            if (i == 0) {
                stringBuilder.append(TABLE_JOINT_SYMBOL);
            }

            for (int j = 0; j < columnMaxWidthMapping.get(i) + PADDING_SIZE * 2; j++) {
                stringBuilder.append(TABLE_H_SPLIT_SYMBOL);
            }
            stringBuilder.append(TABLE_JOINT_SYMBOL);
        }
    }

    private static Map<Integer, Integer> getMaximumWidhtofTable(List<String> headers, List<List<String>> rows) {
        Map<Integer, Integer> columnMaxWidthMapping = new HashMap<>();

        // 处理中英文字符宽度
        Function<String, Integer> strWidth = s -> {
            int width = 0;
            for (char c : s.toCharArray()) {
                width += (c >= '\u4e00' && c <= '\u9fa5') ? 2 : 1; // 中文算2个宽度
            }
            return width;
        };

        // 计算每列最大宽度
        for (int i = 0; i < headers.size(); i++) {
            int max = strWidth.apply(headers.get(i));
            for (List<String> row : rows) {
                if (i < row.size()) {
                    max = Math.max(max, strWidth.apply(row.get(i)));
                }
            }
            columnMaxWidthMapping.put(i, max + 2); // 增加额外间距
        }
        return columnMaxWidthMapping;
    }

    private static int getOptimumCellPadding(int cellIndex, int datalength, Map<Integer, Integer> columnMaxWidthMapping,
                                             int cellPaddingSize) {
        if (datalength % 2 != 0) {
            datalength++;
        }

        if (datalength < columnMaxWidthMapping.get(cellIndex)) {
            cellPaddingSize = cellPaddingSize + (columnMaxWidthMapping.get(cellIndex) - datalength) / 2;
        }

        return cellPaddingSize;
    }

    private static void fillCell(StringBuilder stringBuilder, String cell, int cellIndex,
                                 Map<Integer, Integer> columnMaxWidthMapping) {
        int cellPaddingSize = getOptimumCellPadding(cellIndex, cell.length(), columnMaxWidthMapping, PADDING_SIZE);

        if (cellIndex == 0) {
            stringBuilder.append(TABLE_V_SPLIT_SYMBOL);
        }

        // 处理长文本换行
        if (cell.length() > columnMaxWidthMapping.get(cellIndex)) {
            String truncated = cell.substring(0, columnMaxWidthMapping.get(cellIndex)-3) + "...";
            stringBuilder.append(" ").append(truncated).append(" ");
        } else {
            stringBuilder.append(" ").append(cell);
            fillSpace(stringBuilder, columnMaxWidthMapping.get(cellIndex) - cell.length() + 1);
        }

        stringBuilder.append(TABLE_V_SPLIT_SYMBOL);
    }
}
