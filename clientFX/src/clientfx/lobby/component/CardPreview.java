package clientfx.lobby.component;

import common.card.Card;
import common.card.Summon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;

/**
 * Created by HugoSousa on 08-03-2015.
 */
public class CardPreview {
    private Label nameLabel = new Label("");
    private Image image = new Image("clientfx/resources/blankcard.gif");
    private ObservableList<String> abilityList = FXCollections.observableArrayList();
    private Node node;

    private Label powerLabel = new Label("");
    private Label lifeLabel = new Label("");

    public CardPreview(Card card){
        buildLayout();
        set(card);
    }

    public void set(Card card){
        abilityList.clear();
        if(card == null){
            nameLabel.setText("");
            powerLabel.setText("");
            lifeLabel.setText("");
        } else {
            nameLabel.setText("  [" + card.cost() + "] " + card.name());
            if(card instanceof Summon){
                Summon summon = (Summon) card;
                powerLabel.setText("" + summon.power());
                lifeLabel.setText("" + summon.life());
                powerLabel.setVisible(true);
                lifeLabel.setVisible(true);
            } else{
                powerLabel.setVisible(false);
                lifeLabel.setVisible(false);
            }
        }
    }

    public Node getNode() {
        return node;
    }

    private void buildLayout(){
        BorderPane container = new BorderPane();
        container.setBackground(new Background(new BackgroundFill(Paint.valueOf("#cdc"), null, null)));
        container.setMaxHeight(350);
        container.setTop(nameLabel);

        VBox center = new VBox();
        center.setAlignment(Pos.BOTTOM_CENTER);
        center.getChildren().add(new ImageView(image));
        ListView listView = new ListView(abilityList);
        Label leftGap = new Label("   ");
        Label rightGap = new Label("   ");
        HBox listViewHolder = new HBox();
        listViewHolder.getChildren().addAll(leftGap, listView, rightGap);
        center.getChildren().add(listViewHolder);
        container.setCenter(center);

        GridPane bottom = new GridPane();
        powerLabel.setAlignment(Pos.BASELINE_LEFT);
        lifeLabel.setAlignment(Pos.BASELINE_RIGHT);
        bottom.setAlignment(Pos.BASELINE_CENTER);
        bottom.setHgap(170);
        bottom.add(powerLabel, 0, 0);
        bottom.add(lifeLabel, 1, 0);
        container.setBottom(bottom);

        node = container;
    }
}
