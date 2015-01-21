package clientfx.lobby;

import clientfx.lobby.tab.Chat;
import clientfx.network.ServerHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by xs on 20-01-2015.
 */
public class Lobby {

    private final ServerHandler serverHandler;
    private final Chat chat;

    public Lobby(ServerHandler serverHandler){
        this.serverHandler = serverHandler;
        this.chat = new Chat(serverHandler);
        serverHandler.setLobby(this);

        TabPane tabPane = new TabPane();
        Scene scene = new Scene(tabPane, 750, 650);

        Tab chatTab = new Tab("Chat");
        chatTab.setContent(chat.getRoot());
        chatTab.setClosable(false);
        tabPane.getTabs().add(chatTab);

        Stage stage = new Stage();
        stage.setTitle("Warsmith");
        stage.setScene(scene);
        stage.show();
    }
}
