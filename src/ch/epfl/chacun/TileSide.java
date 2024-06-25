package ch.epfl.chacun;

import java.util.List;

/**
 * Represents a side of a tile
 * @author Adam BEKKAR (379476)
 */
public sealed interface TileSide {
    /**
     * Used to get the zones touching the current side (this)
     * @return the zones touching the current side (this)
     */
    List<Zone> zones();

    /**
     * Used to check if a connected side is of the same kind as the current side
     * @return True if the connected side is of the same kind as the current side, False otherwise
     */
    boolean isSameKindAs(TileSide that);
    /**
     * Represents a forest's side of a tile
     * @param forest the forest zone touching the side
     */
    record Forest(Zone.Forest forest) implements TileSide {
        @Override
        public List<Zone> zones() {
            return List.of(forest);
        }
        @Override
        public boolean isSameKindAs(TileSide that) {
            return that instanceof Forest;
        }
    }

    /**
     * Represents a meadow's side of a tile
     * @param meadow the meadow zone touching the side
     */
    record Meadow(Zone.Meadow meadow) implements TileSide {
        @Override
        public List<Zone> zones() {
            return List.of(meadow);
        }

        @Override
        public boolean isSameKindAs(TileSide that) {
            return that instanceof Meadow;
        }
    }

    /**
     * Represents a river's side of a tile
     * @param meadow1 the first meadow zone encircling the river and touching the side
     * @param river the river zone touching the side
     * @param meadow2 the second meadow zone encircling the river and touching the side
     */
    record River(Zone.Meadow meadow1, Zone.River river, Zone.Meadow meadow2) implements TileSide {
        @Override
        public List<Zone> zones() {
            return List.of(meadow1, river, meadow2);
        }

        @Override
        public boolean isSameKindAs(TileSide that) {
            return that instanceof River;
        }
    }
}
