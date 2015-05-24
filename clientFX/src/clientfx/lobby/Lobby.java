package clientfx.lobby;

import clientfx.lobby.tab.Chat;
import clientfx.lobby.tab.Forge;
import clientfx.login.Main;
import clientfx.network.ServerHandler;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

/**
 * Created by xs on 20-01-2015.
 */
public class Lobby {
    private final ServerHandler serverHandler;
    public final Chat chat;
    private final Forge forge;
    private final Stage stage;

    public Lobby(ServerHandler serverHandler){
        this.serverHandler = serverHandler;
        this.chat = new Chat(serverHandler);
        this.forge = new Forge();
        serverHandler.setLobby(this);

        TabPane tabPane = new TabPane();
        Scene scene = new Scene(tabPane, 750, 650);

        Tab chatTab = new Tab("Chat");
        chatTab.setContent(chat.getRoot());
        chatTab.setClosable(false);
        tabPane.getTabs().add(chatTab);

        Tab forgeTab = new Tab("Forge");
        forgeTab.setContent(forge.getRoot());
        forgeTab.setClosable(false);
        tabPane.getTabs().add(forgeTab);

        stage = new Stage();
        stage.setTitle("Warsmith");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {Platform.exit(); serverHandler.endConnection();});

    }

    public void show(){
        stage.show();
    }

    public void disconnect(){
        Platform.runLater(() -> {
            stage.close();
            try {
                new Main().start(new Stage());
            } catch (Exception e) {
                //TODO could not load login screen
            }
        });
    }
}
