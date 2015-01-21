package clientfx.login;

import clientfx.lobby.Lobby;
import clientfx.network.ServerHandler;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;


public class Main extends Application {
    @FXML private TextField userText;
    @FXML private Text actionText;

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("Warsmith");
        loginWindow();
    }

    public static void main(String[] args) {
        launch();
    }

    private void loginWindow() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        Scene scene = new Scene(root, 300, 200);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void handleSubmitButtonAction(){
        String userName = userText.getText();
        if(userName.trim().length() > 0){
            actionText.setText("Connecting...");
            ServerHandler handler = new ServerHandler(userName);
            boolean connect = handler.connect();
            if(connect){
                primaryStage.close();
                new Lobby(handler);
            }
            else
                actionText.setText(handler.getErrorMessage());
        }
        else
            actionText.setText("Invalid user");
    }

}
