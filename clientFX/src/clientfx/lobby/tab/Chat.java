package clientfx.lobby.tab;

import clientfx.network.ServerHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.Callback;


/**
 * Created by xs on 20-01-2015.
 */
public class Chat {
    private final ServerHandler serverHandler;
    private final BorderPane root;
    private final ObservableList<String> userList;
    private final ObservableList<String> messageList;

    public Chat(ServerHandler serverHandler){
        this.serverHandler = serverHandler;
        this.root = new BorderPane();

        TextField textField = new TextField();
        textField.setOnKeyPressed((KeyEvent e) -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                serverHandler.chat(textField.getText());
                textField.clear();
            }
        });

        root.setBottom(textField);

        BorderPane rightPane = new BorderPane();
        rightPane.setTop(new Label("Others in Chat"));
        ListView<String> userListView = new ListView<>();
        userList = FXCollections.observableArrayList();
        userListView.setItems(userList);
        rightPane.setCenter(userListView);

        root.setRight(rightPane);

        ListView<String> msgListView = new ListView<>();
        messageList = FXCollections.observableArrayList();
        msgListView.setCellFactory(list -> new ListCell<String>() {
            {
                Text text = new Text();
                text.wrappingWidthProperty().bind(list.widthProperty().subtract(15));
                text.textProperty().bind(itemProperty());

                setPrefWidth(0);
                setGraphic(text);
            }
        });
        msgListView.setItems(messageList);

        root.setCenter(msgListView);

    }

    public BorderPane getRoot() {
        return root;
    }

    public void join(String chatName, String[] users){
        Platform.runLater(() -> userList.addAll(users));
    }

    public void addUser(String user) {
        Platform.runLater(() -> userList.add(user));
    }

    public void removeUser(String user) {
        Platform.runLater(() -> userList.remove(user));
    }

    public void roomMessage(String room, String sender, String message) {
        Platform.runLater(() -> messageList.add(sender + ": " + message));
    }
}
