package clientfx.lobby.tab;

import clientfx.network.ServerHandler;
import javafx.scene.layout.BorderPane;

/**
 * Created by xs on 20-01-2015.
 */
public class Chat {
    private final ServerHandler serverHandler;
    private final BorderPane root;

    public Chat(ServerHandler serverHandler){
        this.serverHandler = serverHandler;
        this.root = new BorderPane();
    }

    public BorderPane getRoot() {
        return root;
    }
}
