import controller.AuthController;
import model.Database;
import view.AuthView;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {

        System.setProperty("sun.java2d.png.disableGamma", "true");
        //初始化,连接数据库
        Database db = new Database();
        db.initialize();

        //创建登录界面和控制器
        AuthView authView = new AuthView();
        new AuthController(authView, db);

        //启动登录界面
        authView.setVisible(true);
    }
}