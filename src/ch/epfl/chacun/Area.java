package ch.epfl.chacun;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents an area of the board
 * @param zones The zones that make up the area
 * @param occupants The players that have placed an occupant in the area
 * @param openConnections The number of open connections in the area
 * @param <Z> The generic type used to allow the area to contain different types of zones
 * @author Antoine Bastide (375407)
 */
public record Area<Z extends Zone>(Set<Z> zones, List<PlayerColor> occupants, int openConnections) {
    /**
     * Used to create an area
     * @param zones The zones that make up the area
     * @param occupants The occupants of the area
     * @param openConnections The number of open connections in the area
     */
    public Area {
        // Make sure the arguments are valid
        Preconditions.checkArgument(openConnections >= 0);
        zones = Set.copyOf(zones);
        occupants = new ArrayList<>(occupants);
        Collections.sort(occupants);
        occupants = List.copyOf(occupants);
    }

    /**
     * Used to check if a forest area has at least one menhir
     * @param forest The forest area to check
     * @return True if the area has at least one menhir, False otherwise
     */
    public static boolean hasMenhir(Area<Zone.Forest> forest) {
        return forest.zones().stream()
                .anyMatch(zone -> zone.kind() == Zone.Forest.Kind.WITH_MENHIR);
    }

    /**
     * Used to count the number of mushroom groups in a forest area
     * @param forest The forest area to check
     * @return The number of mushroom groups in the forest area
     */
    public static int mushroomGroupCount(Area<Zone.Forest> forest) {
        return (int) forest.zones().stream()
                .filter(z -> z.kind() == Zone.Forest.Kind.WITH_MUSHROOMS)
                .count();
    }

    /**
     * Used to get all the animals in a meadow area that are not cancelled
     * @param meadow The meadow area to check
     * @param cancelledAnimals The animals that are cancelled out
     * @return The set of animals in the meadow that are not cancelled
     */
    public static Set<Animal> animals(Area<Zone.Meadow> meadow, Set<Animal> cancelledAnimals) {
        // Create the animal set and populate it with the animals in the meadow that are not cancelled
        Set<Animal> animals = meadow.zones().stream()
                .flatMap(zone -> zone.animals().stream())
                .filter(animal -> !cancelledAnimals.contains(animal))
                .collect(Collectors.toSet());
        return Set.copyOf(animals);
    }

    /**
     * Used to count the number of fish in a river area
     * @param river The river area to check
     * @return The number of fish in the river area
     */
    public static int riverFishCount(Area<Zone.River> river) {
        int fishCount = 0;
        Set<Zone.Lake> previousLakes = new HashSet<>();

        for (Zone.River zone : river.zones()) {
            fishCount += zone.fishCount();
            if (zone.hasLake() && previousLakes.add(zone.lake())) fishCount += zone.lake().fishCount();
        }

        return fishCount;
    }

    /**
     * Used to count the number of fish in a river system area
     * @param riverSystem The river system area to check
     * @return The number of fish in the river system area
     */
    public static int riverSystemFishCount(Area<Zone.Water> riverSystem) {
        return riverSystem.zones().stream()
                .mapToInt(Zone.Water::fishCount)
                .sum();
    }

    /**
     * Used to count the number of lakes in a river system area
     * @param riverSystem The river system area to check
     * @return The number of lakes in the river system area
     */
    public static int lakeCount(Area<Zone.Water> riverSystem) {
        return (int) riverSystem.zones().stream()
                .filter(zone -> zone instanceof Zone.Water.Lake)
                .count();
    }

    /**
     * Used to check if this area is closed
     * @return True if the area is closed, False otherwise
     */
    public boolean isClosed() {
        return openConnections == 0;
    }

    /**
     * Used to check if this area is occupied
     * @return True if the area has at least one occupant, False otherwise
     */
    public boolean isOccupied() {
        return !occupants.isEmpty();
    }

    /**
     * Used to get the majority occupants of the area
     * @return The set of majority occupants of the area
     */
    public Set<PlayerColor> majorityOccupants() {
        // If the area is not occupied by anyone, return an empty set
        if (!isOccupied()) return Set.of();

        // Count the number of occupants of each color
        Map<PlayerColor, Integer> countMap = occupants.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(_ -> 1)));

        // Find the maximum count of occupants
        Integer maxOccupantCount = countMap.values().stream()
                .max(Integer::compare)
                .orElse(0);

        // Filter the colors with the maximum count and collect them into a set
        Set<PlayerColor> majorityOccupants = countMap.keySet().stream()
                .filter(key -> countMap.get(key).equals(maxOccupantCount))
                .collect(Collectors.toSet());

        return Set.copyOf(majorityOccupants);
    }

    /**
     * Used to combine this area with another area
     * @param that The other area to combine with
     * @return The combination of both areas
     */
    public Area<Z> connectTo(Area<Z> that) {
        // Check if the two areas are the same
        boolean sameArea = this.equals(that);

        // Find the new zones and occupants of the area
        Set<Z> newZones = sameArea ? zones : new HashSet<>(zones);
        if (!sameArea) newZones.addAll(that.zones());
        List<PlayerColor> newOccupants = sameArea ? occupants : new ArrayList<>(occupants);
        if (!sameArea) newOccupants.addAll(that.occupants());

        // If this and that are different, the new area will have two less
        // open connections than the sum of the open connections of the two areas
        return new Area<>(newZones, newOccupants, openConnections + (sameArea ? 0 : that.openConnections()) - 2);
    }

    /**
     * Used change the occupant of an empty area
     * @param occupant The occupant to add to the area
     * @return The same area but with the given occupant
     * @throws IllegalArgumentException if the area is already occupied
     */
    public Area<Z> withInitialOccupant(PlayerColor occupant) {
        Preconditions.checkArgument(!isOccupied());
        return new Area<>(zones, List.of(occupant), openConnections);
    }

    /**
     * Used to remove an occupant from this area
     * @param occupant The occupant to remove to the area
     * @return The same area but without the given occupant
     * @throws IllegalArgumentException if the given occupant is not in the area
     */
    public Area<Z> withoutOccupant(PlayerColor occupant) {
        Preconditions.checkArgument(occupants.contains(occupant));
        List<PlayerColor> occupants = new ArrayList<>(this.occupants);
        occupants.remove(occupant);
        return new Area<>(zones, occupants, openConnections);
    }

    /**
     * Used to remove all occupants from this area
     * @return The same area but without any occupants
     */
    public Area<Z> withoutOccupants() {
        return new Area<>(zones, List.of(), openConnections);
    }

    /**
     * Used to get the ids of all the tiles that make up the area
     * @return The set of ids of all the tiles that make up the area
     */
    public Set<Integer> tileIds() {
        Set<Integer> tileIds = zones.stream()
                .map(zone -> Zone.tileId(zone.id()))
                .collect(Collectors.toSet());
        return Set.copyOf(tileIds);
    }

    /**
     * Used to try and find a zone with the given special power in this area
     * @param specialPower The special power to find
     * @return The zone with the given special power, or null if there is none
     */
    public Zone zoneWithSpecialPower(Zone.SpecialPower specialPower) {
        return zones.stream()
                .filter(z -> z.specialPower() == specialPower)
                .findFirst()
                .orElse(null);
    }
}
