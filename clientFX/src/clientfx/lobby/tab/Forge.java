package clientfx.lobby.tab;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Created by xs on 21-01-2015.
 */
public class Forge {
    private final BorderPane root;

    public Forge() {
        root = new BorderPane();

        GridPane leftPane = new GridPane();
        root.setLeft(leftPane);

        VBox deckPane = new VBox();
        leftPane.add(deckPane, 0, 0);
        Text deckText = new Text("Deck 0/30");
        deckPane.getChildren().add(deckText);


    }

    public BorderPane getRoot() {
        return root;
    }
}
