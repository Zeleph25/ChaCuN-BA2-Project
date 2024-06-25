package ch.epfl.chacun;

import java.util.List;

/**
 * Represents the different colors of the players: Red, Blue, Green, Yellow and Purple
 * @author Antoine Bastide (375407)
 */
public enum PlayerColor {
    RED, BLUE, GREEN, YELLOW, PURPLE;

    /** The list that contains all the possible colors */
    public static final List<PlayerColor> ALL = List.of(values());
}
