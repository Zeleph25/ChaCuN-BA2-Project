package ch.epfl.chacun.gui;

import ch.epfl.chacun.Occupant;
import ch.epfl.chacun.PlayerColor;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/**
 * Used to create icons for the GUI
 * @author Antoine Bastide (375407)
 */
public class Icon {
    /** Private constructor to prevent instantiation */
    private Icon() {}

    /**
     * Used to create a new icon for a given player color and occupant
     * @param color The color of the player
     * @param occupant The occupant of the icon
     * @return The icon for the given player color and occupant
     */
    public static Node newFor(PlayerColor color, Occupant occupant) {
        SVGPath icon = new SVGPath();
        icon.setFill(ColorMap.fillColor(color));
        icon.setStroke(ColorMap.strokeColor(color));
        icon.setContent(switch (occupant.kind()) {
            case PAWN -> "M -10 10 H -4 L 0 2 L 6 10 H 12 L 5 0 L 12 -2 L 12 -4 L 6 -6" +
                    "L 6 -10 L 0 -10 L -2 -4 L -6 -2 L -8 -10 L -12 -10 L -8 6 Z";
            case HUT -> "M -8 10 H 8 V 2 H 12 L 0 -10 L -12 2 H -8 Z";
        });
        return icon;
    }
}
