package controller;

import model.Database;
import view.AuthView;
import view.MainView;
import myDatabase.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

public class MainController {
    private MainView mainView;
    private Database database;
    private AuthView authView;

    public MainController(MainView mainView, Database database, AuthView authView) throws IOException {
        this.mainView = mainView;
        this.database = database;
        this.authView = authView;

        // 初始化数据库列表
        refreshDatabaseList();
        refreshTableList();

        if (!database.validateCurrentDatabase()) {
            database.setCurrentDatabase("defaultdb");
        }

        // 添加监听器
        mainView.addExecuteListener(new ExecuteListener());
        mainView.addLogoutListener(new LogoutListener());
        mainView.addHelpListener(new HelpListener());
        mainView.addRefreshDbListener(new RefreshDbListener());
        mainView.addCreateDbListener(new CreateDbListener());
        mainView.addDropDbListener(new DropDbListener());
        mainView.addBackupListener(new BackupListener());
        mainView.addRestoreListener(new RestoreListener());

        // 添加表格选择监听器
        mainView.addTableSelectionListener(new TableSelectionListener());

        // 添加数据库切换监听
        database.addDatabaseChangeListener(dbName -> {
            refreshTableList();
            mainView.appendResult("切换到数据库: " + dbName);
        });

        // 添加回车键绑定
        bindEnterKeyToExecute();
    }

    private void refreshDatabaseList() {
        List<String> databases = Utils.getAllDatabase(SQLConstant.getRootPath());
        mainView.refreshDatabaseList(databases);
    }

    private void refreshTableList() {
        try {
            String currentDb = database.getCurrentDatabase();
            if (currentDb != null && !currentDb.isEmpty()) {
                List<String> tables = Utils.getAllTables(SQLConstant.getCurrentDbPath());
                mainView.updateTableList(tables);
            }
        } catch (Exception e) {
            mainView.appendResult("ERROR: 刷新表列表失败 - " + e.getMessage());
        }
    }

    class TableSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = mainView.getSelectedTable();
                if (selectedTable != null && !selectedTable.isEmpty()) {
                    String query = "SELECT * FROM " + selectedTable;
                    executeQuery(query); // 这里调用带参数的executeQuery
                }
            }
        }
    }


    private void bindEnterKeyToExecute() {
        JTextArea queryTextArea = mainView.getQueryTextArea();

        queryTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    executeQuery(); // 这里调用无参数的executeQuery
                }
            }
        });

        InputMap inputMap = queryTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = queryTextArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "executeQuery");
        actionMap.put("executeQuery", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeQuery(); // 这里调用无参数的executeQuery
            }
        });
    }

    // 无参数的executeQuery方法（从文本区域获取查询）
    private void executeQuery() {
        String query = mainView.getQuery().trim();
        executeQuery(query); // 调用带参数的版本
    }

    // 带参数的executeQuery方法（直接执行指定查询）
    private void executeQuery(String query) {
        mainView.appendQueryToResult(query);

        if (query.isEmpty()) {
            mainView.appendResult("ERROR: 请输入查询语句");
            return;
        }

        String result = DatabaseProcessor.executeQuery(query, database);
        mainView.appendResult(result);
        mainView.clearQuery();
        refreshDatabaseList();
        refreshTableList();
    }

    class ExecuteListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            executeQuery(); // 调用无参数的executeQuery
        }
    }

    class LogoutListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainView.setVisible(false);
            mainView.clearQuery();
            authView.setVisible(true);
        }
    }

    class HelpListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainView.appendResult(Help.getHelpText());
        }
    }

    class RefreshDbListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshDatabaseList();
            refreshTableList();
            mainView.appendResult("数据库列表已刷新");
        }
    }

    class CreateDbListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String dbName = JOptionPane.showInputDialog(mainView, "请输入数据库名称:");
            if (dbName != null && !dbName.trim().isEmpty()) {
                String sql = "CREATE DATABASE " + dbName + ";";
                String result = DatabaseProcessor.executeQuery(sql, database);
                mainView.appendResult(result);
                refreshDatabaseList();
            }
        }
    }

    class DropDbListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String dbName = mainView.getSelectedDatabase();
            if (dbName != null && !dbName.trim().isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(mainView,
                        "确定要删除数据库 '" + dbName + "' 吗?", "确认删除",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    String sql = "DROP DATABASE " + dbName + ";";
                    String result = DatabaseProcessor.executeQuery(sql, database);
                    mainView.appendResult(result);
                    refreshDatabaseList();
                }
            } else {
                mainView.appendResult("ERROR: 请先选择要删除的数据库");
            }
        }
    }

    class BackupListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String result = BackupRestore.backupDatabase(database);
            mainView.appendResult(result);
        }
    }

    class RestoreListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String result = BackupRestore.restoreDatabase(database);
            mainView.appendResult(result);
            refreshDatabaseList();
            refreshTableList();
        }
    }
}