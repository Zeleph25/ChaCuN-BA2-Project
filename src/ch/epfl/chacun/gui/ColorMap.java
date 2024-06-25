package ch.epfl.chacun.gui;

import ch.epfl.chacun.PlayerColor;
import javafx.scene.paint.Color;

/**
 * Used to map PlayerColor to javafx.scene.paint.Color
 * @author Antoine Bastide (375407)
 */
public class ColorMap {
    /** Private constructor to prevent instantiation */
    private ColorMap() {}

    /**
     * Used to get the color corresponding to a player color
     * @param color The player color
     * @return The color corresponding to the PlayerColor
     */
    public static Color fillColor(PlayerColor color) {
        return switch (color) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.LIME;
            case YELLOW -> Color.YELLOW;
            case PURPLE -> Color.PURPLE;
        };
    }

    /**
     * Used to get the stroke color from a player color
     * @param color The player color
     * @return The stroke color corresponding to the player color
     */
    public static Color strokeColor(PlayerColor color) {
        return switch (color) {
            case GREEN, YELLOW -> fillColor(color).deriveColor(0, 1, 0.6, 1);
            default -> Color.WHITE;
        };
    }
}