package ch.epfl.chacun.extensions.gui;

import ch.epfl.chacun.GameState;
import ch.epfl.chacun.PlacedTile;
import ch.epfl.chacun.PlayerColor;
import ch.epfl.chacun.TextMaker;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class that creates the GUI that displays the player messages
 * @author Adam Bekkar (379476)
 */
public class PlayerMessageBoardUI {
    /**
     * Used to create the GUI that displays the player messages
     * @param gameStateP The property containing the current game state
     * @param tileIds The property containing the tile IDs
     * @param playerNames The map containing the player names
     * @param board The property containing the board
     * @param textMaker The text maker
     * @return The node containing the player message board
     */
    public static Node create(ObjectProperty<GameState> gameStateP, ObjectProperty<Set<Integer>> tileIds,
                              Map<PlayerColor, String> playerNames, ObjectProperty<Node> board,
                              TextMaker textMaker) {
        // Create the VBox
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(7.5));
        vBox.setSpacing(10);
        vBox.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, null)));
        vBox.setBackground(new Background(new BackgroundFill(Color.gray((0.98)), null, null)));

        // Create the text property that will be displayed
        SimpleStringProperty text = new SimpleStringProperty("");
        gameStateP.map(GameState::lastAction).map(action -> {
            GameState gameState = gameStateP.getValue();
            String currentPlayer = playerNames.get(gameState.currentPlayer());
            String lastPlayer = playerNames.get(gameState.players().getLast());
            return switch (action) {
                case PLACE_TILE -> textMaker.withPlacedTile(currentPlayer, gameState.board().lastPlacedTile());
                case OCCUPY_TILE -> textMaker.withOccupant(lastPlayer, GameUI.lastOccupant.get());
                case RETAKE_PAWN -> textMaker.withRetakePawn(currentPlayer, GameUI.lastOccupant.get());
                default -> "";
            };
        }).addListener((_, _, newValue) -> text.set(newValue));

        // Create the label that will display the text
        Label label = new Label();
        label.setTextAlignment(TextAlignment.CENTER);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Color.BLACK);
        label.textProperty().bind(text);
        vBox.getChildren().add(label);

        vBox.visibleProperty().bind(gameStateP.map(g ->
                !(g.nextAction() == GameState.Action.END_GAME || g.lastAction() == GameState.Action.START_GAME)
        ));
        vBox.onMouseEnteredProperty().set(_ -> {
            PlacedTile lastPlacedTile = gameStateP.getValue().board().lastPlacedTile();
            tileIds.set(Objects.nonNull(lastPlacedTile) ? Set.of(lastPlacedTile.id()) : Set.of());
        });
        vBox.onMouseExitedProperty().set(_ -> tileIds.set(Set.of()));

        // Add the board and left BOX to the pane and set alignment to the top left
        BorderPane boardPane = new BorderPane();
        boardPane.centerProperty().bind(board);
        StackPane centerPane = new StackPane(boardPane, vBox);
        StackPane.setAlignment(vBox, javafx.geometry.Pos.TOP_LEFT);

        // Bind the size of the left VBox depending on the size of the board
        vBox.setMaxSize(610, 40);

        return centerPane;
    }
}
