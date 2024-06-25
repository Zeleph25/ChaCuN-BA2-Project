package ch.epfl.chacun;

/**
 * Used to represent a position
 * @param x The x coordinate of the position
 * @param y The y coordinate of the position
 * @author Antoine Bastide (375407)
 */
public record Pos(int x, int y) {
    /** The origin of the position system */
    public static final Pos ORIGIN = new Pos(0, 0);

    /**
     * Used to get the position translated by the given amount.
     * @param dX The amount to translate on the x-axis
     * @param dY The amount to translate on the y-axis
     * @return The translated position
     */
    public Pos translated(int dX, int dY) {
        return new Pos(x() + dX, y() + dY);
    }

    /**
     * Used to get the neighbor of the origin in the given direction.
     * @param direction The direction of the neighbor
     * @return The neighbor of the origin in the given direction
     */
    public Pos neighbor(Direction direction) {
        return direction.ordinal() % 2 == 0 ?
                new Pos(x(), y() + direction.ordinal() - 1) :
                new Pos(x() + (direction.ordinal() + 2) % Direction.COUNT - 2, y());
    }

    @Override
    public String toString() {
        return STR."(\{x()}, \{y()})";
    }
}
