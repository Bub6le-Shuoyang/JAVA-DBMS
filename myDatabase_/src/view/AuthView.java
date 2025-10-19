package view;

import javax.swing.*;
import java.awt.*;

public class AuthView extends JFrame {
    private JTabbedPane tabbedPane;
    private JPanel loginPanel;
    private JPanel registerPanel;

    // 登录组件
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JButton loginButton;

    // 注册组件
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JTextField registerEmailField;
    private JButton registerButton;

    public AuthView() {
        setTitle("DBMS - 登录/注册");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();

        // 登录面板
        loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        loginPanel.add(new JLabel("用户名:"));
        loginUsernameField = new JTextField();
        loginPanel.add(loginUsernameField);

        loginPanel.add(new JLabel("密码:"));
        loginPasswordField = new JPasswordField();
        loginPanel.add(loginPasswordField);

        loginButton = new JButton("登录");
        loginPanel.add(new JLabel());
        loginPanel.add(loginButton);

        // 注册面板
        registerPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        registerPanel.add(new JLabel("用户名:"));
        registerUsernameField = new JTextField();
        registerPanel.add(registerUsernameField);

        registerPanel.add(new JLabel("密码:"));
        registerPasswordField = new JPasswordField();
        registerPanel.add(registerPasswordField);

        registerPanel.add(new JLabel("邮箱:"));
        registerEmailField = new JTextField();
        registerPanel.add(registerEmailField);

        registerButton = new JButton("注册");
        registerPanel.add(new JLabel());
        registerPanel.add(registerButton);

        tabbedPane.addTab("登录", loginPanel);
        tabbedPane.addTab("注册", registerPanel);

        add(tabbedPane);
    }

    // Getters for login components
    public String getLoginUsername() {
        return loginUsernameField.getText();
    }

    public String getLoginPassword() {
        return new String(loginPasswordField.getPassword());
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    // Getters for register components
    public String getRegisterUsername() {
        return registerUsernameField.getText();
    }

    public String getRegisterPassword() {
        return new String(registerPasswordField.getPassword());
    }

    public String getRegisterEmail() {
        return registerEmailField.getText();
    }

    public JButton getRegisterButton() {
        return registerButton;
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void clearLoginFields() {
        loginUsernameField.setText("");
        loginPasswordField.setText("");
    }

    public void clearRegisterFields() {
        registerUsernameField.setText("");
        registerPasswordField.setText("");
        registerEmailField.setText("");
    }

    // 添加新方法
    public void switchToLoginTab() {
        tabbedPane.setSelectedIndex(0);
    }
}