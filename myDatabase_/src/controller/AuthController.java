package controller;

import model.Database;
import model.User;
import view.AuthView;
import view.MainView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;


/**
 * AuthController类 负责用户登录和注册流程
 */
public class AuthController {
    private AuthView authView;
    private Database database;

    public AuthController(AuthView authView, Database database) {
        this.authView = authView;
        this.database = database;

        // 添加登录按钮监听器
        authView.getLoginButton().addActionListener(new LoginListener());

        // 添加注册按钮监听器
        authView.getRegisterButton().addActionListener(new RegisterListener());
    }

    class LoginListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = authView.getLoginUsername();
            String password = authView.getLoginPassword();

            if (username.isEmpty() || password.isEmpty()) {
                authView.showMessage("用户名和密码不能为空");
                return;
            }

            User user = database.authenticateUser(username, password);
            if (user != null) {
                authView.showMessage("登录成功!");
                authView.setVisible(false);
                authView.clearLoginFields();

                // 打开主界面
                MainView mainView = new MainView(user.getUsername());
                try {
                    new MainController(mainView, database, authView);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                mainView.setVisible(true);
            } else {
                authView.showMessage("用户名或密码错误");
            }
        }
    }

    class RegisterListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = authView.getRegisterUsername();
            String password = authView.getRegisterPassword();
            String email = authView.getRegisterEmail();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                authView.showMessage("所有字段都必须填写");
                return;
            }

            boolean success = database.registerUser(username, password, email);
            if (success) {
                authView.showMessage("注册成功! 请登录");
                authView.clearRegisterFields();
                authView.switchToLoginTab();
            } else {
                authView.showMessage("用户名已存在");
            }
        }
    }
}