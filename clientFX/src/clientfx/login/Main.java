package clientfx.login;

import clientfx.lobby.Lobby;
import clientfx.network.ServerHandler;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class Main extends Application implements Initializable {
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
            Lobby lobby = new Lobby(handler);
            boolean connect = handler.connect();
            if(connect){
                primaryStage.close();
                lobby.show();
            }
            else
                actionText.setText(handler.getLastErrorMessage());
        }
        else
            actionText.setText("Invalid user");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert userText != null;
        userText.setOnKeyPressed((KeyEvent e) -> {
            if (e.getCode().equals(KeyCode.ENTER))
                handleSubmitButtonAction();
        });
    }
}
