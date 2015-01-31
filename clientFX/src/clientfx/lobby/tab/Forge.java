package clientfx.lobby.tab;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

        TableView deckTable = new TableView();
        deckPane.getChildren().add(deckTable);
        deckTable.setEditable(false);
        TableColumn cards = new TableColumn("Card");
        deckTable.getColumns().add(cards);
        TableColumn qt = new TableColumn("Quantity");
        deckTable.getColumns().add(qt);
        TableColumn actualQt = new TableColumn();
        TableColumn qtPlus = new TableColumn();
        TableColumn qtMinus = new TableColumn();
        qt.getColumns().addAll(actualQt, qtPlus, qtMinus);



        VBox cardPane = new VBox();
        leftPane.add(cardPane, 1, 0);
        Text cardText = new Text("Card Preview");
        cardPane.getChildren().add(cardText);


    }

    public BorderPane getRoot() {
        return root;
    }
}
