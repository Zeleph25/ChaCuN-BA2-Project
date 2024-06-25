package ch.epfl.chacun.gui;

import ch.epfl.chacun.Occupant;
import ch.epfl.chacun.Tile;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * Represents the GUI for the decks
 * @author Adam BEKKAR (379476)
 */
public class DecksUI {
    /** The deck ids */
    private static final String[] DECK_IDS = { "NORMAL", "MENHIR" };

    /** Private constructor to prevent instantiation */
    private DecksUI() {}

    /**
     * Used to create the GUI for the decks
     * @param observableTile The observable tile
     * @param observableNormalDeckSize The observable normal deck size
     * @param observableMenhirDeckSize The observable menhir deck size
     * @param observableString The observable string
     * @param eventHandler The event handler
     */
    public static Node create(ObservableValue<Tile> observableTile, ObservableValue<Integer> observableNormalDeckSize,
                              ObservableValue<Integer> observableMenhirDeckSize, ObservableValue<String> observableString,
                              Consumer<Occupant> eventHandler) {
        // Create the horizontal container for the decks
        HBox horizontalContainer = createHBox(observableNormalDeckSize, observableMenhirDeckSize);
        // Create the StackPane for the next tile to place
        StackPane tileToPlace = createStackPane(observableTile, observableString, eventHandler);

        // Create the vertical container and add the stylesheet to it
        VBox root = new VBox(horizontalContainer, tileToPlace);
        root.getStylesheets().add("decks.css");

        return root;
    }

    /**
     * Used to create the horizontal container for the decks
     * @param observableNormalDeckSize The observable normal deck size
     * @param observableMenhirDeckSize The observable menhir deck size
     * @return The horizontal container for the decks
     */
    private static HBox createHBox(ObservableValue<Integer> observableNormalDeckSize,
                                   ObservableValue<Integer> observableMenhirDeckSize) {
        // Create the decks
        StackPane[] decks = new StackPane[DECK_IDS.length];
        for (int i = 0; i < DECK_IDS.length; i++) {
            // Create the deck and give it an id
            ImageView deckImage = new ImageView(STR."/256/\{DECK_IDS[i]}.jpg");
            deckImage.setFitHeight(ImageLoader.NORMAL_TILE_FIT_SIZE);
            deckImage.setFitWidth(ImageLoader.NORMAL_TILE_FIT_SIZE);

            // Create the StackPane for the deck and give it an id
            Text deckText = new Text();
            deckText.textProperty().bind(i == 0 ? observableNormalDeckSize.map(String::valueOf)
                    : observableMenhirDeckSize.map(String::valueOf));
            decks[i] = new StackPane(deckImage, deckText);
            decks[i].setId(DECK_IDS[i]);
        }

        // Create the horizontal container and add the decks to it
        HBox horizontalContainer = new HBox(decks);
        horizontalContainer.setId("decks");
        return horizontalContainer;
    }

    /**
     * Used to create the StackPane for the next tile to place
     * @param observableTile The observable tile
     * @param observableString The observable string
     * @param eventHandler The event handler
     * @return The StackPane for the next tile to place
     */
    private static StackPane createStackPane(ObservableValue<Tile> observableTile,
                                             ObservableValue<String> observableString, Consumer<Occupant> eventHandler) {
        // Create the text for the next tile to place
        Text tileText = new Text();
        tileText.setWrappingWidth(.8 * ImageLoader.LARGE_TILE_FIT_SIZE);
        tileText.visibleProperty().bind(observableString.map(s -> !s.isEmpty()));
        tileText.textProperty().bind(observableString);

        // Handles when a non-empty text is clicked, give null to the event handler to
        // signal that the current player does not want to place or remove an occupant
        tileText.setOnMouseClicked(_ -> {
            if (!tileText.getText().isEmpty()) eventHandler.accept(null);
        });

        // Create the image view for the next tile to place and dimension it properly
        ImageView tileImage = new ImageView();
        tileImage.imageProperty().bind(observableTile.map(ImageLoader::largeImageForTile));
        tileImage.setFitWidth(ImageLoader.LARGE_TILE_FIT_SIZE);
        tileImage.setFitHeight(ImageLoader.LARGE_TILE_FIT_SIZE);
        tileImage.visibleProperty().bind(observableString.map(String::isEmpty));

        StackPane stackPane = new StackPane(tileText, tileImage);
        stackPane.setId("next-tile");
        return stackPane;
    }
}