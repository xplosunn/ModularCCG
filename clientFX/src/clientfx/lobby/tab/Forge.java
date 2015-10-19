package clientfx.lobby.tab;

import clientfx.lobby.component.CardPreview;
import com.sun.javafx.collections.ObservableListWrapper;
import common.card.Card;
import common.card.Summon;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.util.*;

public class Forge {
    private final Pane root;
    private final CardPreview cardPreview;
    private Card card = new Summon();
    private List<DeckCard> deck = new ArrayList<>();

    private Text deckText;
    private TableView deckTable;

    public Forge() {
        GridPane contentPane = new GridPane();
        root = contentPane;

        //Left
        VBox deckPane = new VBox();
        contentPane.add(deckPane, 0, 0);
        deckText = new Text("Deck 0/30");

        deckTable = new TableView();
        deckTable.setEditable(false);
        deckTable.setItems(new ObservableListWrapper<>(deck));
        TableColumn cards = new TableColumn("Card");
        TableColumn qt = new TableColumn("Quantity");
        deckTable.getColumns().addAll(cards, qt);
        TableColumn actualQt = new TableColumn();
        TableColumn qtPlus = new TableColumn();
        TableColumn qtMinus = new TableColumn();
        qt.getColumns().addAll(actualQt, qtPlus, qtMinus);

        HBox deckManagement = new HBox();
        Button newDeck = new Button("New Deck");
        newDeck.setOnAction(event -> newDeckPressed());
        Button loadDeck = new Button("Load Deck");
        loadDeck.setOnAction(event -> loadDeckPressed());
        Button saveDeck = new Button("Save Deck");
        saveDeck.setOnAction(event -> saveDeckPressed());
        deckManagement.getChildren().addAll(newDeck, loadDeck, saveDeck);

        deckPane.getChildren().addAll(deckText, deckTable, deckManagement);

        //Mid
        VBox cardPane = new VBox();
        contentPane.add(cardPane, 1, 0);
        Text cardText = new Text("Card Preview");
        cardPane.getChildren().add(cardText);
        cardPreview = new CardPreview(card);
        cardPane.getChildren().add(cardPreview.getNode());
        cardPane.getChildren().add(buildPowerAndLifePanel());
    }

    private Node buildPowerAndLifePanel() {
        VBox vBox = new VBox();
        Button plusPlus = new Button("+1/+1");
        plusPlus.setOnAction(event -> {
            Summon summon = ((Summon)card);
            summon.power(summon.power() + 1);
            summon.life(summon.life() + 1);
            cardPreview.set(card);
        });
        Label plusPlusLabel = new Label("(+1 cost added)");
        Button plusMinus = new Button("+1/-1");
        plusMinus.setOnAction(event -> {
            Summon summon = ((Summon)card);
            if(summon.life() > 1) {
                summon.power(summon.power() + 1);
                summon.life(summon.life() - 1);
                cardPreview.set(card);
            }
        });
        Button minusPlus = new Button("-1/+1");
        minusPlus.setOnAction(event -> {
            Summon summon = ((Summon)card);
            if(summon.power() > 0) {
                summon.power(summon.power() - 1);
                summon.life(summon.life() + 1);
                cardPreview.set(card);
            }
        });
        Button add = new Button("Add to deck");
        add.setOnAction(event -> {
            Optional<DeckCard> deckCard = deck.stream().filter(dc -> dc.card.equals(card)).findAny();
            if(deckCard.isPresent())
                deckCard.get().quantity += 1;
            else
                deck.add(new DeckCard(card, 1));
            updateDeckTextQuantity();
        });

        HBox topLine = new HBox();
        topLine.getChildren().addAll(plusPlus, plusPlusLabel);
        HBox botLine = new HBox();
        botLine.getChildren().addAll(plusMinus, minusPlus);

        vBox.getChildren().addAll(topLine, botLine, add);
        return vBox;
    }

    public Pane getRoot() {
        return root;
    }

    private void newDeckPressed(){
        //TODO
    }

    private void loadDeckPressed(){
        //TODO
    }

    private void saveDeckPressed(){
        //TODO
    }

    private void updateDeckTextQuantity(){
        deckText.setText("Deck " + deck.stream().mapToInt(dc -> dc.quantity).sum() + "/30");
    }

    private static class DeckCard{
        public final Card card;
        public int quantity;

        public DeckCard(Card card, int quantity) {
            this.card = card;
            this.quantity = quantity;
        }
    }
}