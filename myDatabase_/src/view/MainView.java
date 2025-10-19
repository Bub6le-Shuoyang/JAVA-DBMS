package view;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class MainView extends JFrame {
    private JTextArea queryTextArea;
    private JTextArea resultTextArea;
    private JButton executeButton;
    private JButton logoutButton;
    private JButton helpButton;
    private JLabel welcomeLabel;
    private JPanel mainPanel;
    private JComboBox<String> databaseComboBox;
    private JButton refreshDbButton;
    private JButton createDbButton;
    private JButton dropDbButton;
    private JButton backupButton;
    private JButton restoreButton;
    private JList<String> tableList;
    private DefaultListModel<String> tableListModel;
    private JTree fileTree;
    private JSplitPane leftSplitPane;

    public MainView(String username) {
        setTitle("DBMS - 主界面");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI(username);
    }

    private void initUI(String username) {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 顶部面板（保持不变）
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        welcomeLabel = new JLabel("欢迎, " + username + "!  数据库管理系统");
        welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        welcomePanel.add(welcomeLabel);

        JPanel dbOperationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        createDbButton = new JButton("创建数据库");
        dropDbButton = new JButton("删除数据库");
        backupButton = new JButton("备份");
        restoreButton = new JButton("还原");
        dbOperationPanel.add(createDbButton);
        dbOperationPanel.add(dropDbButton);
        dbOperationPanel.add(backupButton);
        dbOperationPanel.add(restoreButton);

        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        dbPanel.add(new JLabel("当前数据库:"));
        databaseComboBox = new JComboBox<>();
        databaseComboBox.setPreferredSize(new Dimension(150, 25));
        refreshDbButton = new JButton("刷新");
        refreshDbButton.setPreferredSize(new Dimension(80, 25));
        dbPanel.add(databaseComboBox);
        dbPanel.add(refreshDbButton);

        JPanel dbTopPanel = new JPanel(new BorderLayout());
        dbTopPanel.add(dbOperationPanel, BorderLayout.WEST);
        dbTopPanel.add(dbPanel, BorderLayout.EAST);

        topPanel.add(welcomePanel, BorderLayout.WEST);
        topPanel.add(dbTopPanel, BorderLayout.CENTER);

        // 主内容区域
        JSplitPane mainSplitPane = new JSplitPane();

        // 左侧面板 - 使用垂直分割面板
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setResizeWeight(0.5);
        leftSplitPane.setDividerLocation(300);
        leftSplitPane.setBorder(BorderFactory.createEmptyBorder());

        // 上部分：数据库表 - 固定高度
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("数据库表"));
        tablePanel.setPreferredSize(new Dimension(200, 300));
        tableListModel = new DefaultListModel<>();
        tableList = new JList<>(tableListModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        // 添加固定大小的滚动面板
        JScrollPane listScrollPane = new JScrollPane(tableList);
        listScrollPane.setPreferredSize(new Dimension(200, 300));
        tablePanel.add(listScrollPane, BorderLayout.CENTER);

        // 下部分：文件浏览器 - 固定高度
        JPanel fileBrowserPanel = new JPanel(new BorderLayout());
        fileBrowserPanel.setBorder(BorderFactory.createTitledBorder("数据库文件结构"));
        fileBrowserPanel.setPreferredSize(new Dimension(200, 300));

        // 创建文件树
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("数据库文件");
        fileTree = new JTree(root);
        fileTree.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // 添加固定大小的滚动面板
        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        treeScrollPane.setPreferredSize(new Dimension(200, 300));

        // 添加刷新按钮
        JButton refreshFileTreeButton = new JButton("刷新文件结构");
        refreshFileTreeButton.addActionListener(e -> refreshFileTree());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshFileTreeButton);

        fileBrowserPanel.add(treeScrollPane, BorderLayout.CENTER);
        fileBrowserPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 组装左侧分割面板
        leftSplitPane.setTopComponent(tablePanel);
        leftSplitPane.setBottomComponent(fileBrowserPanel);

        // 右侧主内容面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(900, 600));

        // 查询面板 - 固定高度
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.setBorder(BorderFactory.createTitledBorder("输入SQL查询"));
        queryPanel.setPreferredSize(new Dimension(900, 200));
        queryTextArea = new JTextArea();
        queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        queryTextArea.setLineWrap(true);
        queryTextArea.setWrapStyleWord(true);

        // 添加固定大小的滚动面板
        JScrollPane queryScrollPane = new JScrollPane(queryTextArea);
        queryScrollPane.setPreferredSize(new Dimension(900, 200));
        queryPanel.add(queryScrollPane, BorderLayout.CENTER);

        // 结果面板 - 固定高度
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("查询结果"));
        resultPanel.setPreferredSize(new Dimension(900, 400));
        resultTextArea = new JTextArea();
        resultTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultTextArea.setEditable(false);

        // 添加固定大小的滚动面板
        JScrollPane resultScrollPane = new JScrollPane(resultTextArea);
        resultScrollPane.setPreferredSize(new Dimension(900, 400));
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);

        // 组装右侧面板
        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(queryPanel, BorderLayout.CENTER);
        rightPanel.add(resultPanel, BorderLayout.SOUTH);

        // 设置主分割面板
        mainSplitPane.setLeftComponent(leftSplitPane);
        mainSplitPane.setRightComponent(rightPanel);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.2);

        // 按钮面板 - 固定高度
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        bottomButtonPanel.setPreferredSize(new Dimension(1200, 60));
        helpButton = new JButton("帮助");
        helpButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        helpButton.setPreferredSize(new Dimension(120, 35));

        executeButton = new JButton("执行查询");
        executeButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        executeButton.setPreferredSize(new Dimension(120, 35));

        logoutButton = new JButton("退出登录");
        logoutButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        logoutButton.setPreferredSize(new Dimension(120, 35));

        bottomButtonPanel.add(helpButton);
        bottomButtonPanel.add(executeButton);
        bottomButtonPanel.add(logoutButton);

        // 组装主面板
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }



    // 刷新文件树方法
    public void refreshFileTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("数据库文件");

        // 获取数据库根目录
        File dbRoot = new File("D:\\desktop\\DBMS\\DATA"); // 根据您的SQLConstant调整

        if (dbRoot.exists() && dbRoot.isDirectory()) {
            // 添加数据库文件夹
            for (File dbDir : dbRoot.listFiles()) {
                if (dbDir.isDirectory()) {
                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(dbDir.getName());
                    root.add(dbNode);

                    // 添加表文件
                    for (File tableFile : dbDir.listFiles()) {
                        if (tableFile.isFile()) {
                            dbNode.add(new DefaultMutableTreeNode(tableFile.getName()));
                        }
                    }
                } else {
                    // 添加根目录下的文件（如ruanko.db, users.dat）
                    root.add(new DefaultMutableTreeNode(dbDir.getName()));
                }
            }
        }

        // 更新树模型
        fileTree.setModel(new DefaultTreeModel(root));

        // 展开第一级节点
        for (int i = 0; i < fileTree.getRowCount(); i++) {
            fileTree.expandRow(i);
        }
    }

    // ... 保留所有原有的getter和setter方法 ...
    public void updateTableList(List<String> tables) {
        tableListModel.clear();
        for (String table : tables) {
            tableListModel.addElement(table);
        }
    }

    public String getSelectedTable() {
        return tableList.getSelectedValue();
    }

    public void addTableSelectionListener(ListSelectionListener listener) {
        tableList.addListSelectionListener(listener);
    }

    public void addCreateDbListener(ActionListener listener) {
        createDbButton.addActionListener(listener);
    }

    public void addDropDbListener(ActionListener listener) {
        dropDbButton.addActionListener(listener);
    }

    public void addBackupListener(ActionListener listener) {
        backupButton.addActionListener(listener);
    }

    public void addRestoreListener(ActionListener listener) {
        restoreButton.addActionListener(listener);
    }

    public void refreshDatabaseList(List<String> databases) {
        databaseComboBox.removeAllItems();
        for (String db : databases) {
            databaseComboBox.addItem(db);
        }
    }

    public String getSelectedDatabase() {
        return (String) databaseComboBox.getSelectedItem();
    }

    public String getQuery() {
        return queryTextArea.getText();
    }

    public void clearQuery() {
        queryTextArea.setText("");
    }

    public void appendResult(String result) {
        resultTextArea.append(result + "\n");
        resultTextArea.setCaretPosition(resultTextArea.getText().length());
    }

    public void clearResult() {
        resultTextArea.setText("");
    }

    public void addExecuteListener(ActionListener listener) {
        executeButton.addActionListener(listener);
    }

    public void addLogoutListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    public void addHelpListener(ActionListener listener) {
        helpButton.addActionListener(listener);
    }

    public void addRefreshDbListener(ActionListener listener) {
        refreshDbButton.addActionListener(listener);
    }

    public JTextArea getQueryTextArea() {
        return queryTextArea;
    }

    public void appendQueryToResult(String query) {
        if (!resultTextArea.getText().isEmpty()) {
            resultTextArea.append("\n----------------\n");
        }
        resultTextArea.append("> " + query + "\n");
        resultTextArea.setCaretPosition(resultTextArea.getText().length());
    }
}