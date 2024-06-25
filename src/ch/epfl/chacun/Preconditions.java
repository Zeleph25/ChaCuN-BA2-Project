package ch.epfl.chacun;

/**
 * Represents a pre-condition that needs to be satisfied to check the validity of arguments in methods
 * @author Adam BEKKAR (379476)
 */
public final class Preconditions {
    /** Private constructor to prevent instantiation */
    private Preconditions() {}

    /**
     * Used to check that the given boolean is true
     * @param shouldBeTrue The boolean to check
     * @throws IllegalArgumentException If the condition is not met
     */
    public static void checkArgument(boolean shouldBeTrue) {
        if (!shouldBeTrue) throw new IllegalArgumentException();
    }
}
