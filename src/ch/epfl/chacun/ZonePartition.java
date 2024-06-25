package ch.epfl.chacun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Represents a zone partition
 * @param areas The areas that make up this ZonePartition
 * @param <Z> The generic type used to allow the zone builder to contain different types of zones
 * @author Antoine Bastide (375407)
 */
public record ZonePartition<Z extends Zone>(Set<Area<Z>> areas) {
    /**
     * Used to construct a ZonePartition
     * @param areas The areas that make up this zone partition
     */
    public ZonePartition {
        areas = Set.copyOf(areas);
    }

    /** Used to construct a ZonePartition with no areas */
    public ZonePartition() {
        this(Set.of());
    }

    /**
     * Used to get the area containing a zone
     * @param zone The zone to find the area for
     * @return The area containing the zone
     * @throws IllegalArgumentException If the zone is not in any area of the partition
     */
    public Area<Z> areaContaining(Z zone) {
        return areas.stream().filter(area -> area.zones().contains(zone))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * A builder class used to dynamically create a ZoneBuilder object
     * @param <Z> The generic type used to allow the builder to contain different types of zones
     * @author Antoine Bastide (375407)
     */
    public final static class Builder<Z extends Zone> {
        /** All the areas contained in this builder object */
        private final Set<Area<Z>> areas;

        /**
         * Used to construct the builder of a ZonePartition
         * @param partition The ZonePartition we are basing this Builder on
         */
        public Builder(ZonePartition<Z> partition) {
            areas = new HashSet<>(partition.areas());
        }

        /**
         * Used to add a given zone to this Builder
         * @param zone The zone to add to this Builder
         * @param openConnections The amount of open connections the given zone has
         */
        public void addSingleton(Z zone, int openConnections) {
            areas.add(new Area<>(Set.of(zone), new ArrayList<>(), openConnections));
        }

        /**
         * Used to get an area from areas that satisfies a given predicate
         * @param predicate The predicate to satisfy
         * @throws IllegalArgumentException If no area satisfies the given predicate
         * @return The area that satisfies the given predicate
         */
        private Area<Z> findAreaWithPredicate(Predicate<Area<Z>> predicate) {
            return areas.stream().filter(predicate)
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }

        /**
         * Used to add a given initial occupant to a given zone of this Builder
         * @param zone The zone we want to populate with the given occupant
         * @throws IllegalArgumentException If the given zone is not in any area of the partition, or if the given zone is already occupied
         * @param color The color of the occupant we want to add to the given zone
         */
        public void addInitialOccupant(Z zone, PlayerColor color) {
            Area<Z> areaOfInterest = findAreaWithPredicate(area -> area.zones().contains(zone) && !area.isOccupied());
            areas.remove(areaOfInterest);
            areas.add(areaOfInterest.withInitialOccupant(color));
        }

        /**
         * Used to remove a given occupant from a given zone in this Builder
         * @param zone The zone we want to remove an occupant from
         * @throws IllegalArgumentException If the given zone is not in this Builder, or if the given occupant does not occupy the given zone
         * @param color The occupant to remove
         */
        public void removeOccupant(Z zone, PlayerColor color) {
            Area<Z> areaOfInterest = findAreaWithPredicate(area -> area.zones().contains(zone) && area.isOccupied()
                    && area.occupants().contains(color));
            areas.remove(areaOfInterest);
            areas.add(areaOfInterest.withoutOccupant(color));
        }

        /**
         * Used to remove all the occupants from a given area of this Builder
         * @throws IllegalArgumentException If the given area is not a part of this Builder
         * @param area The area we want to rid occupants of
         */
        public void removeAllOccupantsOf(Area<Z> area) {
            Area<Z> areaOfInterest = findAreaWithPredicate(area::equals);
            areas.remove(areaOfInterest);
            areas.add(areaOfInterest.withoutOccupants());
        }

        /**
         * Used to connect two given zones in this Builder
         * @param zone1 The first zone to connect
         * @param zone2 The second zone to connect
         * @throws IllegalArgumentException If the given zones do not exist in this Builder
         */
        public void union(Z zone1, Z zone2) {
            // Find the areas containing the given zones, or throw an exception if they don't exist
            Area<Z> area1 = findAreaWithPredicate(area -> area.zones().contains(zone1));
            Area<Z> area2 = findAreaWithPredicate(area -> area.zones().contains(zone2));

            // Remove the areas and add the new connected area
            areas.removeIf(area -> area.equals(area1) || area.equals(area2));
            areas.add(area1.connectTo(area2));
        }

        /**
         * Used to build a ZoneBuilder
         * @return the ZoneBuilder built from this Builder
         */
        public ZonePartition<Z> build() {
            return new ZonePartition<>(areas);
        }
    }
}
