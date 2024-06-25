package ch.epfl.chacun;

import java.util.List;

/**
 * Represent the rotation of a tile. It can be None, Right, Half Turn or Left.
 * @author Adam BEKKAR (379476)
 */
public enum Rotation {
    NONE, RIGHT, HALF_TURN, LEFT;

    /** The list that contains all the possible directions */
    public static final List<Rotation> ALL = List.of(values());
    /** The number of possible rotations */
    public static final int COUNT = ALL.size();
    /** The number of degrees in a quarter turn */
    public static final int QUARTER_TURN_DEGREES = 90;

    /**
     * Used to add two rotations together
     * @param that The rotation to add to our current rotation
     * @return The new rotation
     */
    public Rotation add(Rotation that) {
        return ALL.get((that.ordinal() + ordinal()) % COUNT);
    }

    /**
     * Used to negate a rotation
     * @return The negation of the current rotation
     */
    public Rotation negated() {
        return ALL.get((COUNT - ordinal()) % COUNT);
    }

    /**
     * Used to get the number of quarter turns clockwise
     * @return The number of quarter turns clockwise
     */
    public int quarterTurnsCW() {
        return ordinal();
    }

    /**
     * Used to get the number of degrees clockwise
     * @return The number of degrees clockwise
     */
    public int degreesCW() {
        return quarterTurnsCW() * QUARTER_TURN_DEGREES;
    }
}