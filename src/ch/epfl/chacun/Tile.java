package ch.epfl.chacun;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a tile
 * @param id The id of the tile
 * @param kind The kind of the tile
 * @param n The north side of the tile
 * @param e The east side of the tile
 * @param s The south side of the tile
 * @param w The west side of the tile
 * @author Adam BEKKAR (379476)
 */
public record Tile(int id, Kind kind, TileSide n, TileSide e, TileSide s, TileSide w) {
    /** The enum of the different types of tiles that can be placed during the game */
    public enum Kind { START, NORMAL, MENHIR }

    /**
     * Used to get the sides of the tile in an unmodifiable list in the order n, e, s, w
     * @return The sides of the tile in the order n, e, s, w
     */
    public List<TileSide> sides() {
        return List.of(n, e, s, w);
    }

    /**
     * Used to get the zones of this tile touching its sides
     * @return The zones of this tile touching its sides
     */
    public Set<Zone> sideZones() {
        // Get the zones of each side and put all of them in a set
        return sides().stream().flatMap(side -> side.zones().stream()).collect(Collectors.toSet());
    }

    /**
     * Used to get the zones of the tile, lakes included
     * @return The zones of the tile, lakes included
     */
    public Set<Zone> zones() {
        Set<Zone> sideZones = new HashSet<>(sideZones());
        Set<Zone> lakes = sideZones.stream()
                .filter(zone -> zone instanceof Zone.River river && river.hasLake())
                .map(zone -> ((Zone.River) zone).lake()).collect(Collectors.toSet());
        // Add the lakes to the side zones to get all the zones
        sideZones.addAll(lakes);
        return sideZones;
    }
}