package ch.epfl.chacun;

import java.util.Objects;

/**
 * Represents an occupant of a zone
 * @author Antoine Bastide (375407)
 */
public record Occupant(Kind kind, int zoneId) {
    /** The enum of the different kinds of occupants */
    public enum Kind { PAWN, HUT }

    /**
     * Used to construct an occupant
     * @param kind The kind of the occupant
     * @param zoneId The id of the zone where the occupant is
     * @throws NullPointerException If the kind is null
     * @throws IllegalArgumentException If the zone id is negative
     */
    public Occupant {
        // Make sure the arguments are valid
        Objects.requireNonNull(kind);
        Preconditions.checkArgument(zoneId >= 0);
    }

    /**
     * Used to get the number of occupants for the given kind
     * @param kind The kind of the occupants
     * @return The number of occupants
     */
    public static int occupantsCount(Kind kind) {
        return kind == Kind.PAWN ? 5 : 3;
    }
}
