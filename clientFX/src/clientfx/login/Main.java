package clientfx.login;

import clientfx.network.ServerHandler;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;


/**
 * Created by xs on 19-01-2015.
 */
public class Main extends Application {
    @FXML private TextField userText;
    @FXML private Text actionText;

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        Scene scene = new Scene(root, 300, 200);

        stage.setTitle("Warsmith");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public void handleSubmitButtonAction(){
        String userName = userText.getText();
        if(userName.trim().length() > 0){
            actionText.setText("Connecting...");
            ServerHandler handler = new ServerHandler(userName);
            boolean connect = handler.connect();
            if(connect)
                actionText.setText("Logged in!");
            else
                actionText.setText(handler.getErrorMessage());
        }
        else
            actionText.setText("Invalid user");
    }
}
