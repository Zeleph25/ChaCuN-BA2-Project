package ch.epfl.chacun;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a tile placed on the board
 * @author Adam BEKKAR (379476)
 */
public record PlacedTile(Tile tile, PlayerColor placer, Rotation rotation, Pos pos, Occupant occupant) {
    /**
     * Used to construct a placed tile
     * @param tile The tile to place
     * @param placer The color of the player who placed the tile
     * @param rotation The rotation of the tile
     * @param pos The position of the tile
     * @param occupant The occupant of the tile
     */
    public PlacedTile {
        // Make sure the arguments are valid
        Objects.requireNonNull(tile);
        Objects.requireNonNull(rotation);
        Objects.requireNonNull(pos);
    }

    /**
     * Used to construct a placed tile, without an occupant
     * @param tile The tile to place
     * @param placer The color of the player who placed the tile
     * @param rotation The rotation of the tile
     * @param pos The position of the tile
     */
    public PlacedTile(Tile tile, PlayerColor placer, Rotation rotation, Pos pos) {
        this(tile, placer, rotation, pos, null);
    }

    /**
     * Used to get the id of the placed tile
     * @return The id of the placed tile
     */
    public int id() {
        return tile.id();
    }

    /**
     * Used to get the kind of the placed tile
     * @return The kind of the placed tile
     */
    public Tile.Kind kind() {
        return tile.kind();
    }

    /**
     * Used to get the side of the tile in the given direction compared to the placed tile
     * @param direction The direction of the side we compare to the placed tile
     * @return The side of the tile in the given direction
     */
    public TileSide side(Direction direction) {
        return switch (direction.rotated(rotation().negated())) {
            case N -> tile.n();
            case E -> tile.e();
            case S -> tile.s();
            case W -> tile.w();
        };
    }

    /**
     * Used to get the zone of the placed tile with the given id
     * @return The zone of the placed tile with the given id
     * @throws IllegalArgumentException If the tile does not have a zone with the given id
     */
    public Zone zoneWithId(int id) {
        return tile.zones().stream().filter(zone -> zone.id() == id)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Used to get the zone of the placed tile with special powers, or null if there is none
     * @return The zones of the placed tile with special powers
     */
    public Zone specialPowerZone() {
        return tile.zones().stream().filter(zone -> Objects.nonNull(zone.specialPower()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Used to get the zones of the placed tile that are forests
     * @return The zones of the placed tile that are forests
     */
    public Set<Zone.Forest> forestZones() {
        return tile.zones().stream().filter(zone -> zone instanceof Zone.Forest)
                .map(zone -> (Zone.Forest) zone)
                .collect(Collectors.toSet());
    }

    /**
     * Used to get the zones of the placed tile that are meadows
     * @return The zones of the placed tile that are meadows
     */
    public Set<Zone.Meadow> meadowZones() {
        return tile.zones().stream().filter(zone -> zone instanceof Zone.Meadow)
                .map(zone -> (Zone.Meadow) zone)
                .collect(Collectors.toSet());
    }

    /**
     * Used to get the zones of the placed tile that are rivers
     * @return The zones of the placed tile that are rivers
     */
    public Set<Zone.River> riverZones() {
        return tile.zones().stream().filter(zone -> zone instanceof Zone.River)
                .map(zone -> (Zone.River) zone)
                .collect(Collectors.toSet());
    }

    /**
     * Used to get the potential occupants of the placed tile
     * @return The potential occupants of the placed tile
     */
    public Set<Occupant> potentialOccupants() {
        if (Objects.isNull(placer)) return Set.of();

        Set<Occupant> potentialOccupants = new HashSet<>();
        for (Zone zone : tile.zones()) {
            // Check if the side zones contain the zone
            if (tile.sideZones().contains(zone)) potentialOccupants.add(new Occupant(Occupant.Kind.PAWN, zone.id()));

            // Check the special requirements for the water zones
            if (zone instanceof Zone.River river && !river.hasLake() || zone instanceof Zone.Lake)
                potentialOccupants.add(new Occupant(Occupant.Kind.HUT, zone.id()));
        }
        return potentialOccupants;
    }

    /**
     * Used to get the same placed tile but with the given occupant
     * @return the same placed tile but with the given occupant
     * @throws IllegalArgumentException If the tile is already occupied
     */
    public PlacedTile withOccupant(Occupant occupant) {
        Preconditions.checkArgument(Objects.isNull(this.occupant));
        return new PlacedTile(tile, placer, rotation, pos, occupant);
    }

    /**
     * Used to get the same placed tile but with no occupant
     * @return The same placed tile but with no occupant
     */
    public PlacedTile withNoOccupant() {
        return Objects.nonNull(occupant) ? new PlacedTile(tile, placer, rotation, pos) : this;
    }

    /**
     * Used to get the id of the zone occupied by the given kind of occupant, or -1 if there is none
     * @param occupantKind the kind of the occupant
     * @return The id of the zone occupied by the given kind of occupant, or -1 if there is none
     */
    public int idOfZoneOccupiedBy(Occupant.Kind occupantKind) {
        return Objects.nonNull(occupant) && occupantKind == occupant.kind() ? occupant.zoneId() : -1;
    }
}