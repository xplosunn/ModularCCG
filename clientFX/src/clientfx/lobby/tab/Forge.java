package clientfx.lobby.tab;

import clientfx.lobby.component.CardPreview;
import common.card.Card;
import common.card.Summon;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class Forge {
    private final BorderPane root;
    private final CardPreview cardPreview;
    private Card card = new Summon();

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
        cardPreview = new CardPreview(card);
        cardPane.getChildren().add(cardPreview.getNode());
        cardPane.getChildren().add(buildPowerAndLifePanel());

    }

    private Node buildPowerAndLifePanel() {
        VBox vBox = new VBox();



        return vBox;
    }

    public BorderPane getRoot() {
        return root;
    }
}