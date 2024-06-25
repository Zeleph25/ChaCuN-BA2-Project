package ch.epfl.chacun.extensions.gui;

import ch.epfl.chacun.GameState;
import ch.epfl.chacun.PlayerColor;
import ch.epfl.chacun.Pos;
import ch.epfl.chacun.Preconditions;
import ch.epfl.chacun.gui.BoardUI;
import ch.epfl.chacun.gui.ColorMap;
import ch.epfl.chacun.gui.ImageLoader;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the GUI for board at the end of the game
 * @author : Adam BEKKAR (379476)
 */
public class EndGameBoard {
    /** The map containing all the generated veils */
    private static final Map<Color, ColorInput> cachedVeilColors = createVeilColors();
    /** The map containing all the previously generated veils */
    private static final Map<Pane, Blend> cachedVeils = new HashMap<>();
    /** The size of the tile */
    public static final double TILE_SIZE = ImageLoader.NORMAL_TILE_FIT_SIZE * 0.6;
    /** The padding of the tile */
    public static final double PADDING = ImageLoader.NORMAL_TILE_FIT_SIZE * 0.025;
    /** The size of a letter in the score display */
    public static final double LETTER_SIZE = TILE_SIZE * 0.5;

    /**
     * Used to create the veil colors
     * @return The map containing all the veil colors
     */
    private static Map<Color, ColorInput> createVeilColors() {
        double size = TILE_SIZE - PADDING;
        return BoardUI.getColorColorInputMap(size);
    }

    /**
     * Used to get the nextColor in the animation at the end of the game
     * @param gameState The game state of the game
     * @param currentColor The actual color of the group
     * @return The next color in the animation at the end of the game depending on the actual one
     */
    private static Color nextColor(GameState gameState, Color currentColor) {
        // Get the list of player colors to loop through them and get the next color
        List<PlayerColor> playerColors = gameState.players();
        Color nextColor = null;
        for (int i = 0; i < playerColors.size(); i++) {
            if (i < playerColors.size() - 1) {
                // Get the next color in the animation at the end of the game depending on the actual one
                if (ColorMap.fillColor(playerColors.get(i)).equals(currentColor)) {
                    nextColor = ColorMap.fillColor(playerColors.get(i + 1));
                    break;
                }
            } else nextColor = ColorMap.fillColor(playerColors.getFirst()); // Loop back to the first color
        }
        return nextColor;
    }

    /** Private constructor to prevent instantiation */
    private EndGameBoard() {}

    /**
     * Used to create the GUI for the board
     * @param reach The reach of the board
     * @param gameState The current game state
     * @return The GUI for the board
     * @throws IllegalArgumentException if the game state is not at the end of the game
     */
    public static Node create(int reach, ObservableValue<GameState> gameState) {
        Preconditions.checkArgument(gameState.getValue().nextAction() == GameState.Action.END_GAME);

        // Create the grid pane and the scroll pane
        GridPane grid = new GridPane();
        ScrollPane scrollPane = new ScrollPane(grid);

        // Loop through the tiles and create the tile panes
        for (int i = -reach; i <= reach; i++) {
            for (int j = -reach + 1; j <= reach - 1; j++) {
                Pane tilePane = createTilePane(reach, i, j, gameState);
                grid.add(tilePane, i + reach, j + reach);
            }
        }

        // Set the fit size of the scroll pane to center it
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);

        return scrollPane;
    }

    /**
     * Used to create the tile pane
     * @param reach The reach of the board
     * @param x The x coordinate of the tile
     * @param y The y coordinate of the tile
     * @param gameState The current game state
     * @return The tile pane
     */
    private static Pane createTilePane(int reach, int x, int y, ObservableValue<GameState> gameState) {
        // Create a new rectangle to represent the tile
        Rectangle tileRect = new Rectangle();
        tileRect.setFill(Color.TRANSPARENT);

        // Calculate the size of the tile based on the normal tile fit size
        tileRect.setWidth(TILE_SIZE);
        tileRect.setHeight(TILE_SIZE);
        tileRect.setFill(Color.TRANSPARENT);
        // Calculate the position of the tile in the grid
        Pos pos = new Pos(x, y);
        // Create a new stack pane to hold the tile rectangle
        Pane tilePane = new StackPane(tileRect);

        // Create the color property
        ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.TRANSPARENT);
        // Create the timeline, the first time and the key frame for the animation
        Timeline timeline = new Timeline();
        double time = (pos.y() + reach) * 30 + (pos.x() + reach) * 25;
        KeyFrame keyFrame;

        // Get the list of player colors
        List<PlayerColor> playerColors = gameState.getValue().getWinners().getKey();

        // Assign color based on the diagonal (x + y)
        int diagonalIndex = (x + y + reach * 2) % playerColors.size();
        Color diagonalColor = ColorMap.fillColor(playerColors.get(diagonalIndex));

        // Loop through the colors of the animation and add them to the timeline
        for (int k = 0; k < 16; k++) {
            // Create a key frame to change the color of the tile
            keyFrame = new KeyFrame(Duration.millis(time), _ ->
                    color.set(nextColor(gameState.getValue(), color.get())));
            // Add the key frame to the timeline
            timeline.getKeyFrames().add(keyFrame);
            // Update to timer to create the next key frame
            time = time + 15 * (k + 2);
        }

        // Create a key frame to change the color of the tile to
        // the winner color at the end of the animation
        int maximumScore = gameState.getValue().getWinners().getValue();
        keyFrame = new KeyFrame(Duration.millis(time + 1), _ -> color.set(diagonalColor));
        timeline.getKeyFrames().add(keyFrame);

        // Check if the position is inside the display
        if (isInsideDisplay(pos)) {
            // Check if the position is a position to display a number
            if (getCoordinates(maximumScore).contains(pos)) {
                keyFrame = new KeyFrame(Duration.millis(time + 1), _ -> color.set(Color.BLACK));
                timeline.getKeyFrames().add(keyFrame);
            }
        } else {
            // Create a key frame to change the color of the tile
            keyFrame = new KeyFrame(Duration.millis(time), _ ->
                    color.set(nextColor(gameState.getValue(), color.get())));
            // Add the key frame to the timeline
            timeline.getKeyFrames().add(keyFrame);

            if ((pos.x() + pos.y()) % 2 == 0) {
                // Create a key frame to change the color of the tile to the winner's color at the end of the animation
                keyFrame = new KeyFrame(Duration.millis(time + 1), _ -> color.set(diagonalColor));
                timeline.getKeyFrames().add(keyFrame);
            }
        }

        // Bind the color of the tile pane
        bindTilePaneColor(tilePane, color, timeline, (long) time, pos, maximumScore);
        // Check if the position is a position to display a number
        drawScore(tilePane, timeline, time + 1, pos);

        // Play the timeline and return the tile pane
        timeline.play();
        return tilePane;
    }

    /**
     * Used to draw the score of the player
     *
     * @param tilePane The pane of the tile
     * @param timeline The timeline of the animation
     * @param time     The time at which the score is displayed in the timeline
     * @param pos      The position of the tile
     */
    private static void drawScore(Pane tilePane, Timeline timeline, double time, Pos pos) {
        // Check if the position is inside the display, if not exit the method
        if (pos.x() < -4|| pos.x() > 4 || (pos.y() != -3 && pos.y() != 3)) return;

        // Create a list of characters to display the score
        List<Character> letters;
        if (pos.y() == -3) letters = List.of('M', 'A', 'X', ' ', 'S', 'C', 'O', 'R', 'E'); // "Max Score"
        else letters = List.of('C', 'O', 'N', 'G', 'R', 'A', 'T', 'S', '!'); // "Congratulations"

        // Calculate the position of the letter in the list
        int indexPos = pos.x() + 4;
        // Get the current letter
        char letter = letters.get(indexPos);

        // Create a new Text object with the current letter
        Text text = new Text(String.valueOf(letter));
        // Set the fill color of the text to black
        text.setFill(Color.BLACK);
        // Set the font and size of the text
        text.setFont(Font.font("Arial", LETTER_SIZE));

        // Add the text to the tilePane after the animation is done
        KeyFrame keyFrame = new KeyFrame(Duration.millis(time + 1), _ ->  tilePane.getChildren().add(text));
        timeline.getKeyFrames().add(keyFrame);
    }

    /**
     * Used to bind the color of the tile pane
     * @param tilePane The pane of the tile
     * @param color The color of the tile
     * @param timeline The timeline of the animation
     */
    private static void bindTilePaneColor(Pane tilePane, ObjectProperty<Color> color, Timeline timeline, long time, Pos pos, int maxScore) {
        // Create a key frame to change the color of the tile to
        // the color property at the beginning of the animation
        KeyFrame keyFrame = new KeyFrame(Duration.ZERO, _ -> color.set(Color.TRANSPARENT));
        timeline.getKeyFrames().add(keyFrame);

        // Bind the fill color of the tile pane to the color property
        tilePane.effectProperty().bind(color.map(_ ->
                cachedVeils.computeIfAbsent(tilePane, _ -> {
                    Blend blend = new Blend(BlendMode.SRC_OVER);
                    blend.topInputProperty().bind(color.map(cachedVeilColors::get));
                    blend.opacityProperty().bind(color.map(_ -> 0.5));

                    // Check if the position is inside the display
                    if (getCoordinates(maxScore).contains(pos)) {
                        KeyFrame keyFrameOpacity = new KeyFrame(Duration.millis(time + 1), _ -> {
                            blend.opacityProperty().unbind();
                            // Set the opacity to 1
                            blend.opacityProperty().setValue(1);
                        });
                        timeline.getKeyFrames().add(keyFrameOpacity);

                    }

                    return blend;
                })
        ));
    }

    /**
     * Used to check if the position is inside the score display
     * @param pos The position to check if it is inside the score display
     * @return  True if the position is inside the score display, false otherwise
     */
    private static boolean isInsideDisplay(Pos pos) {
        return -1 <= pos.x() && pos.x() <= 1 && -2 <= pos.y() && pos.y() <= 2 ||
                3 <= pos.x() && pos.x() <= 5 && -2 <= pos.y() && pos.y() <= 2 ||
                -5 <= pos.x() && pos.x() <= -3 && -2 <= pos.y() && pos.y() <= 2;
    }

    /**
     * Used to get the coordinates to display the score
     * @param score The score of the player
     * @return The list of coordinates to display the score
     * @throws IllegalArgumentException if the score is greater than 999
     */
    private static List<Pos> getCoordinates(int score) {
        Preconditions.checkArgument(score <= 999);

        // Get the hundreds, tens and units of the score
        int hundreds = score / 100;
        int tens = (score % 100) / 10;
        int units = score % 10;

        // Create a list of coordinates to display the score
        List<Pos> posList = new ArrayList<>();
        posList.addAll(getCoordinates(new Pos(-4, 0), hundreds));
        posList.addAll(getCoordinates(new Pos(0, 0), tens));
        posList.addAll(getCoordinates(new Pos(4, 0), units));

        return List.copyOf(posList);
    }

    /**
     * Used to get the coordinates to display a unit from 0 to 9
     * @param center The center of the unit
     * @param unit The unit of the score
     * @return The list of coordinates to display the unit
     * @throws IllegalArgumentException if the unit is not between 0 and 9
     */
    private static List<Pos> getCoordinates(Pos center, int unit) {
        Preconditions.checkArgument(0 <= unit && unit <= 9);
        if (unit == 0) return List.of(center.translated(0, 2), center.translated(0, -2),
                center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                center.translated(1, 1), center.translated(-1, -1), center.translated(-1, 1), center.translated(1, -1),
                center.translated(-1, 0), center.translated(1, 0));
        else if (unit == 1) return List.of(center.translated(1, 0), center.translated(1, 1), center.translated(1, 2),
                center.translated(1, -1), center.translated(1, -2));
        else if (unit == 2) return List.of(center.translated(0, 2), center.translated(0, -2),
                center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                center, center.translated(1, 0), center.translated(-1, 0), center.translated(-1, 1), center.translated(1, -1));
        else if (unit == 3) return List.of(center.translated(0, 2), center.translated(0, -2),
                center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                center, center.translated(1, 0), center.translated(1, 1), center.translated(1, -1));
        else if (unit == 4) return List.of(center.translated(-1, -2), center.translated(-1, -1),
                center.translated(-1, 0), center, center.translated(1, 0), center.translated(1, 1),
                center.translated(1, 2), center.translated(1, -1), center.translated(1, -2));
        else if (unit == 5) return List.of(center.translated(0, 2), center.translated(0, -2),
                center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                center, center.translated(1, 0), center.translated(-1, 0), center.translated(-1, -1), center.translated(1, 1));
        else if (unit == 6) return List.of(center.translated(0, 2), center.translated(0, -2),
                center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                center, center.translated(1, 0), center.translated(-1, 0), center.translated(-1, -1), center.translated(1, 1),
                center.translated(-1, 1));
        else if (unit == 7) return List.of(center.translated(1, 0), center.translated(1, 1), center.translated(1, 2),
                center.translated(1, -1), center.translated(1, -2), center.translated(0, -2), center.translated(-1, -2));
        else if (unit == 8) return List.of(center.translated(0, 2), center.translated(0, -2),
                center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                center.translated(1, 1), center.translated(-1, -1), center.translated(-1, 1), center.translated(1, -1),
                center.translated(-1, 0), center.translated(1, 0), center);
        else return List.of(center.translated(0, 2), center.translated(0, -2),
                    center.translated(1, 2), center.translated(1, -2), center.translated(-1, 2), center.translated(-1, -2),
                    center.translated(1, 1), center.translated(-1, -1), center.translated(1, -1), center.translated(-1, 0),
                    center.translated(1, 0), center);
    }
}
