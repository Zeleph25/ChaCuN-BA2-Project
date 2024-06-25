package ch.epfl.chacun;

import java.util.List;

/**
 * Represents the different directions: North, East, South and West
 * @author Antoine Bastide (375407)
 */
public enum Direction {
    N, E, S, W;

    /** The list of all the possible directions */
    public static final List<Direction> ALL = List.of(values());
    /** The number of possible directions */
    public static final int COUNT = ALL.size();

    /**
     * Returns the direction rotated by the given rotation.
     * @param rotation The rotation to apply
     * @return The new rotated direction
     */
    public Direction rotated(Rotation rotation) {
        return ALL.get((ordinal() + rotation.ordinal()) % COUNT);
    }

    /**
     * Returns the opposite direction of the current direction.
     * @return The opposite direction
     */
    public Direction opposite() {
        return rotated(Rotation.HALF_TURN);
    }
}
