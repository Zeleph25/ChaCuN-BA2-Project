package ch.epfl.chacun.gui;

import ch.epfl.chacun.GameState;
import ch.epfl.chacun.Occupant;
import ch.epfl.chacun.PlayerColor;
import ch.epfl.chacun.TextMaker;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Map;

/**
 * Represents the GUI for the players
 * @author Antoine Bastide (375407)
 */
public class PlayersUI {
    /** Private constructor to prevent instantiation */
    private PlayersUI() {}

    /**
     * Used to create the GUI for the players
     * @param gs The current game state
     * @param textMaker The text maker to use
     * @return The GUI for the players
     */
    public static Node create(ObservableValue<GameState> gs, TextMaker textMaker) {
        // Create the container
        VBox root = new VBox();
        root.getStylesheets().add("players.css");
        root.setId("players");

        // Get the initial game state information
        ObservableValue<Map<PlayerColor, Integer>> points = gs.map(g -> g.messageBoard().points());
        ObservableValue<PlayerColor> currentPlayer = gs.map(GameState::currentPlayer);

        // Create each player's GUI
        for (PlayerColor color : gs.getValue().players()) {
            Node playerGUI = createPlayerGUI(points, currentPlayer, gs, textMaker, color);
            root.getChildren().add(playerGUI);
        }
        return root;
    }

    /**
     * Used to create the GUI for a player
     * @param points The points of the player
     * @param currentPlayer The current player
     * @param gs The current game state
     * @param textMaker The text maker to use
     * @param color The color of the player
     * @return The GUI for the player
     */
    private static Node createPlayerGUI(ObservableValue<Map<PlayerColor, Integer>> points,
                                        ObservableValue<PlayerColor> currentPlayer,
                                        ObservableValue<GameState> gs,
                                        TextMaker textMaker, PlayerColor color) {
        ObservableValue<String> playerPoints = points.map(p -> STR."  \{textMaker.playerName(color)}: \{
                textMaker.points(p.getOrDefault(color, 0))}\n");
        TextFlow playerGUI = new TextFlow();

        // Add the player GUI to the stylesheet and if the color matches
        // with the current player's one, add it also to the stylesheet
        playerGUI.getStyleClass().add("player");
        if (currentPlayer.getValue() == color) playerGUI.getStyleClass().add("current");

        // Create the player color and GUI
        playerGUI.getChildren().add(new Circle(10, ColorMap.fillColor(color)));
        Text text =new Text();
        text.textProperty().bind(playerPoints);
        playerGUI.getChildren().add(text);

        // Create the occupant GUIs
        playerGUI.getChildren().addAll(createOccupantsGUI(Occupant.Kind.HUT, gs, color));
        playerGUI.getChildren().add(new Text("   "));
        playerGUI.getChildren().addAll(createOccupantsGUI(Occupant.Kind.PAWN, gs, color));

        // Style the player's GUI and add it to the container
        currentPlayer.addListener((_, _, newValue) -> {
            playerGUI.getStyleClass().add(STR."\{newValue == color ? "current" : ""}");
            playerGUI.getStyleClass().remove(STR."\{newValue == color ? "" : "current"}");
        });

        return playerGUI;
    }

    /**
     * Used to create the GUI for the occupants
     * @param kind The kind of occupant
     * @param gs The current game state
     * @param color The color of the player
     * @return The GUI for the occupants
     */
    private static Node[] createOccupantsGUI(Occupant.Kind kind, ObservableValue<GameState> gs, PlayerColor color) {
        // Create the array of nodes that corresponds to the svg path of the
        // occupant GUI which depends on the number of them the player has
        Node[] occupants = new SVGPath[Occupant.occupantsCount(kind)];
        for (int j = 0; j < occupants.length; j++) {
            int fJ = j;
            occupants[j] = Icon.newFor(color, new Occupant(kind, 0));
            occupants[j].opacityProperty().bind(gs.map(g -> g.freeOccupantsCount(color, kind) > fJ ? 1 : 0.1));
        }
        return occupants;
    }
}
