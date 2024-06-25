package ch.epfl.chacun;

import java.util.List;
import java.util.Objects;

/**
 * Represents a zone
 * @author Adam BEKKAR (379476)
 */
public sealed interface Zone {
    /** The enum of the different special powers a zone can have */
    enum SpecialPower { SHAMAN, LOGBOAT, HUNTING_TRAP, PIT_TRAP, WILD_FIRE, RAFT }
    /**
     * Used to get the id of the tile from the zone id
     * @param zoneId The id of the zone
     * @return The id of the tile where the zone is
     */
    static int tileId(int zoneId) {
        return zoneId / 10;
    }

    /**
     * Used to get the local id of the zone in the tile from 0 to 9
     * @param zoneId The id of the zone
     * @return The local id of the zone
     */
    static int localId(int zoneId) {
        return zoneId % 10;
    }

    /** Used to get the id of this zone */
    int id();

    /**
     * Used to get the id of the tile where this zone is
     * @return The id of the tile where the zone is
     */
    default int tileId() {
        return tileId(id());
    }

    /**
     * Used to get the local id of this zone in the tile from 0 to 9
     * @return The local id of the zone
     */
    default int localId() {
        return localId(id());
    }

    /**
     * Used to get the special power of this zone
     * @return The special power of the zone
     */
    default SpecialPower specialPower() {
        return null;
    }

    /**
     * Used to represent a forest zone
     * @param id The id of the zone
     * @param kind The kind of the forest
     */
    record Forest(int id, Kind kind) implements Zone {
        /** The different types of forests the game has */
        public enum Kind { PLAIN, WITH_MENHIR, WITH_MUSHROOMS }
    }

    /**
     * Used to represents a meadow zone
     * @param id the id of the zone
     * @param animals the list of animals in the meadow
     * @param specialPower the special power of the meadow
     */
    record Meadow(int id, List<Animal> animals, SpecialPower specialPower) implements Zone {
        public Meadow {
            animals = List.copyOf(animals);
        }
    }

    /** Used to represent a water zone */
    sealed interface Water extends Zone {
        /** Used to get the number of fish in the water zone */
        int fishCount();
    }

    /**
     * Used to represent a lake zone
     * @param id the id of the zone
     * @param fishCount the number of fish in the lake
     * @param specialPower the special power of the lake
     */
    record Lake(int id, int fishCount, SpecialPower specialPower) implements Zone.Water {}

    /**
     * Used to represent a river zone
     * @param id the id of the zone
     * @param fishCount the number of fish in the river
     * @param lake the lake where the river is connected to
     */
    record River(int id, int fishCount, Lake lake) implements Zone.Water {
        /**
         * Used to check if the river is connected to a lake
         * @return True if the river is connected to a lake, False otherwise
         */
        public boolean hasLake() {
            return Objects.nonNull(lake);
        }
    }
}