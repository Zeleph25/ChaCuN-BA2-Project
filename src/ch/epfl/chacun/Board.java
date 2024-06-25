package ch.epfl.chacun;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents the board of the game
 * @author Adam Bekkar (379476), Antoine Bastide (375407)
 */
public final class Board {
    /** The number of tiles that separate the center of the board from one edge */
    public static int REACH = 12;
    /** The number of tiles per row */
    private static final int TILES_PER_SIDE = 2 * REACH + 1;
    /** The total number of tiles on the board */
    private static final int TOTAL_TILE_COUNT = TILES_PER_SIDE * TILES_PER_SIDE;
    /** The empty board */
    public static final Board EMPTY = new Board(
            new PlacedTile[TOTAL_TILE_COUNT], new int[0], ZonePartitions.EMPTY, new HashSet<>());
    /** The array of the placed tiles on the board */
    private final PlacedTile[] placedTiles;
    /** The array of the index of the placed tile in the placedTiles array in the order in which it has been placed */
    private final int[] placedTilesIndex;
    /** The zone partitions of the board */
    private final ZonePartitions zonePartitions;
    /** The cancelled animals of the board */
    private final Set<Animal> cancelledAnimals;

    /**
     * <Constructs a board with the given placed tiles, placed tiles index,
     * zone partitions and cancelled animals
     * @param placedTiles The placed tiles of the board
     * @param placedTilesIndex The index of the placed tile in the placedTiles array in the order in which it has been placed
     * @param zonePartitions The zone partitions of the board
     * @param cancelledAnimals The cancelled animals of the board
     */
    private Board(PlacedTile[] placedTiles, int[] placedTilesIndex, ZonePartitions zonePartitions, Set<Animal> cancelledAnimals) {
        this.placedTiles = placedTiles;
        this.placedTilesIndex = placedTilesIndex;
        this.zonePartitions = zonePartitions;
        this.cancelledAnimals = Set.copyOf(cancelledAnimals);
    }

    /**
     * Used to return the placed tile at the given position
     * @param pos The position of the tile to find
     * @return The placed tile at the given position or null if there is none or the position is out of the board
     */
    public PlacedTile tileAt(Pos pos) {
        int index = indexOf(pos);
        return -1 < index && index < TOTAL_TILE_COUNT ? placedTiles[index] : null;
    }

    /**
     * Used to return the placed tile given its id
     * @param tileId The id of the tile to find
     * @throws IllegalArgumentException If the tile is not on the board
     * @return The placed tile with the given id
     */
    public PlacedTile tileWithId(int tileId) {
        int index = Arrays.stream(placedTilesIndex)
                .filter(i -> placedTiles[i].id() == tileId)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        return placedTiles[index];
    }

    /**
     * Used to return the set of the cancelled animals of the board
     * @return The set of the cancelled animals of the board
     */
    public Set<Animal> cancelledAnimals() {
        return cancelledAnimals;
    }

    /**
     * Used to return the set of all the occupants of the board
     * @return The set of all the occupants of the board
     */
    public Set<Occupant> occupants() {
        return Arrays.stream(placedTilesIndex)
                .filter(i -> Objects.nonNull(placedTiles[i].occupant()))
                // Map the index of the placed tile to its occupant since it is not null
                .mapToObj(i -> placedTiles[i].occupant())
                .collect(Collectors.toSet());
    }

    /**
     * Used to return the forest area containing the given forest zone
     * @param forest The forest zone to find the area for
     * @throws IllegalArgumentException If the forest zone is not on the board
     * @return The area containing the forest zone
     */
    public Area<Zone.Forest> forestArea(Zone.Forest forest) {
        return zonePartitions.forests().areaContaining(forest);
    }

    /**
     * Used to return the meadow area containing the given meadow zone
     * @param meadow The meadow zone to find the area forT
     * @throws IllegalArgumentException If the meadow zone is not on the board
     * @return The area containing the meadow zone
     */
    public Area<Zone.Meadow> meadowArea(Zone.Meadow meadow) {
        return zonePartitions.meadows().areaContaining(meadow);
    }

    /**
     * Used to return the river area containing the given river zone
     * @param riverZone The river zone to find the area for
     * @throws IllegalArgumentException If the river zone is not on the board
     * @return The area containing the river zone
     */
    public Area<Zone.River> riverArea(Zone.River riverZone) {
        return zonePartitions.rivers().areaContaining(riverZone);
    }

    /**
     * Used to return the river system area containing the given river system zone
     * @param water The river system zone to find the area for
     * @throws IllegalArgumentException If the river system zone is not on the board
     * @return The area containing the river system zone
     */
    public Area<Zone.Water> riverSystemArea(Zone.Water water) {
        return zonePartitions.riverSystems().areaContaining(water);
    }

    /**
     * Used to return the set of all the meadow areas of the board
     * @return The set of all the meadow areas of the board
     */
    public Set<Area<Zone.Meadow>> meadowAreas() {
        return zonePartitions.meadows().areas();
    }

    /**
     * Used to return the set of all the river systems areas of the board
     * @return The set of all the river systems areas of the board
     */
    public Set<Area<Zone.Water>> riverSystemAreas() {
        return zonePartitions.riverSystems().areas();
    }

    /**
     * Used to return the set of all the forest areas of the board
     * @return The set of all the forest areas of the board
     */
    public Set<Area<Zone.Forest>> forestAreas() {
        return zonePartitions.forests().areas();
    }

    /**
     * Used to return the adjacent meadow area to the given zone of the given position
     * @param pos The position to find the adjacent meadow area for
     * @param meadowZone The meadow zone to find the adjacent meadow area for
     * @return The adjacent meadow area to the given zone of the given position
     */
    public Area<Zone.Meadow> adjacentMeadow(Pos pos, Zone.Meadow meadowZone) {
        Set<Zone.Meadow> adjacentMeadows = new HashSet<>();

        // Iterate through the 8 adjacent tiles
        for (int dx = -1 ; dx < 2 ; dx++) {
            for (int dy = -1 ; dy < 2 ; dy++) {
                // Add all the meadow zones of the adjacent tiles to the set adjacentMeadows
                PlacedTile placedTile = tileAt(pos.translated(dx, dy));
                if (Objects.isNull(placedTile)) continue;
                adjacentMeadows.addAll(placedTile.meadowZones());
            }
        }

        Set<Zone.Meadow> meadowSet = new HashSet<>();
        List<PlayerColor> occupants = new ArrayList<>();

        // Iterate through all the meadow areas of the board
        for (Area<Zone.Meadow> meadowArea : meadowAreas()) {
            if (meadowArea.zones().contains(meadowZone)) {
                // Add all the meadow zones and occupants of the meadow area to the set meadowSet and occupants
                meadowSet.addAll(meadowArea.zones());
                occupants.addAll(meadowArea.occupants());
            }
        }
        // Retain only the intersecting meadow zones
        meadowSet.retainAll(adjacentMeadows);

        return new Area<>(meadowSet, occupants, 0);
    }

    /**
     * Used to return the number of occupants of the given kind of the given player
     * @param player The player to find the number of occupants for
     * @param occupantKind The kind of the occupant to find the number of
     * @return The number of occupants of the given kind of the given player
     */
    public int occupantCount(PlayerColor player, Occupant.Kind occupantKind) {
        Predicate<PlacedTile> hasOccupant =
                p -> p.placer() == player && Objects.nonNull(p.occupant()) && p.occupant().kind() == occupantKind;
        // Return the number of occupants that match the predicate
        return (int) Arrays.stream(placedTilesIndex)
                .mapToObj(i -> placedTiles[i])
                .filter(hasOccupant)
                .count();
    }

    /**
     * Used to return the set of the positions where a tile can be inserted
     * @return The set of the positions where a tile can be inserted
     */
    public Set<Pos> insertionPositions() {
        Set<Pos> insertPositions = new HashSet<>();

        // For each placed tile on the board, consider its neighbour tiles and for each
        // of them check if it is not on the board and if it is inside the board
        for (PlacedTile placedTile : placedTiles) {
            if (Objects.isNull(placedTile)) continue;

            for (Pos p : List.of(new Pos(-1, 0), new Pos(1, 0), new Pos(0, -1), new Pos(0, 1))) {
                Pos pos = placedTile.pos().translated(p.x(), p.y());
                if (Objects.isNull(tileAt(pos)) && Math.abs(pos.x()) <= REACH && Math.abs(pos.y()) <= REACH)
                    insertPositions.add(pos);
            }
        }

        return Set.copyOf(insertPositions);
    }

    /** Used to return the last placed tile of the board or null if there is none */
    public PlacedTile lastPlacedTile() {
        return placedTilesIndex.length > 0 ? placedTiles[placedTilesIndex[placedTilesIndex.length - 1]] : null;
    }

    /**
     * Used to return the set of the forests closed by the last placed tile
     * @return The set of the forests closed by the last placed tile
     */
    public Set<Area<Zone.Forest>> forestsClosedByLastTile() {
        PlacedTile lastPlacedTile = lastPlacedTile();
        if (Objects.isNull(lastPlacedTile)) return Set.of();

        return closedAreas(lastPlacedTile().forestZones(), this::forestArea);
    }

    /**
     * Used to return the set of the rivers closed by the last placed tile
     * @return The set of the rivers closed by the last placed tile
     */
    public Set<Area<Zone.River>> riversClosedByLastTile() {
        PlacedTile lastPlacedTile = lastPlacedTile();
        if (Objects.isNull(lastPlacedTile)) return Set.of();

        return closedAreas(lastPlacedTile().riverZones(), this::riverArea);
    }

    /**
     * Used to return the set of areas (forests or rivers) closed by the last placed tile
     * @param zones The set of zones to map to their area
     * @param areaMapper The function to map a zone to its area
     * @return The set of areas closed by the last placed tile (forests or rivers)
     * @param <S> The type of the zone (forest or river)
     */
    private <S extends Zone> Set<Area<S>> closedAreas(Set<S> zones, Function<S, Area<S>> areaMapper) {
        // Return the set of the closed rivers
        return zones.stream()
                .map(areaMapper)
                .filter(Area::isClosed)
                .collect(Collectors.toSet());
    }

    /**
     * Used to indicate if the given tile can be added to the board
     * @param tile The tile to add to the board
     * @return True if the given tile can be added to the board, false otherwise
     */
    public boolean canAddTile(PlacedTile tile) {
        // Predicate that checks if the tile matches neighbours
        for (Direction d : Direction.ALL) {
            PlacedTile neighbour = tileAt(tile.pos().neighbor(d));
            if (Objects.nonNull(neighbour) && !neighbour.side(d.opposite()).isSameKindAs(tile.side(d))) return false;
        }
        // Check if the tile matches the insertion positions and the neighbours
        return insertionPositions().contains(tile.pos());
    }

    /**
     * Used to indicate if the given tile can be placed on the board with an eventual rotation
     * @param tile The tile to place on the board
     * @return True if the given tile can be placed on the board with an eventual rotation, false otherwise
     */
    public boolean couldPlaceTile(Tile tile) {
        return couldPlaceTile(tile, null);
    }

    /**
     * Used to indicate if the given tile can be placed on the board with an eventual rotation
     * @param tile The tile to place on the board
     * @return True if the given tile can be placed on the board with an eventual rotation, false otherwise
     */
    public boolean couldPlaceTile(Tile tile, Pos pos) {
        if (Objects.nonNull(pos))
            return Rotation.ALL.stream().map(r -> new PlacedTile(tile, PlayerColor.RED, r, pos))
                .anyMatch(this::canAddTile);

        return insertionPositions().stream()
                .flatMap(p -> Rotation.ALL.stream().map(r -> new PlacedTile(tile, PlayerColor.RED, r, p)))
                .anyMatch(this::canAddTile);
    }

    /**
     * Used to return a new board with the given tile added to it
     * @param tile The tile to add to the board
     * @return A new board with the given tile added to it
     * @throws IllegalArgumentException If the tile is not empty and cannot be added to the board
     */
    public Board withNewTile(PlacedTile tile) {
        Preconditions.checkArgument(placedTilesIndex.length == 0 || canAddTile(tile));

        // Add the new placed tile to the array of placed
        // tiles and update the placedTilesIndex array
        PlacedTile[] newPlacedTiles = Arrays.copyOf(placedTiles, placedTiles.length);
        int[] newPlacedTilesIndex = Arrays.copyOf(placedTilesIndex, placedTilesIndex.length + 1);

        // Index of the new placed tile in the placedTiles array
        int placedTileIndex = indexOf(tile.pos());

        newPlacedTilesIndex[newPlacedTilesIndex.length - 1] = placedTileIndex;
        newPlacedTiles[placedTileIndex] = tile;

        // Add the new tile to the zone partitions
        ZonePartitions.Builder newZonePartitions = new ZonePartitions.Builder(zonePartitions);
        newZonePartitions.addTile(tile.tile());

        // If possible, connect the sides of the new tile with the sides of its neighbours
        for (Direction d : Direction.ALL) {
            PlacedTile neighbour = tileAt(tile.pos().neighbor(d));
            // If the neighbour exists and the sides are of the same kind, connect them
            if (Objects.nonNull(neighbour) && neighbour.side(d.opposite()).isSameKindAs(tile.side(d)))
                newZonePartitions.connectSides(tile.side(d), neighbour.side(d.opposite()));
        }

        return new Board(newPlacedTiles, newPlacedTilesIndex, newZonePartitions.build(), cancelledAnimals);
    }

    /**
     * Used to return a board like this one but with the given occupant added to it
     * @param occupant The occupant to add to the board
     * @return A board like this one but with the given occupant added to it
     * @throws IllegalArgumentException If the tile of the occupant is already occupied
     */
    public Board withOccupant(Occupant occupant) {
        int tileId = Zone.tileId(occupant.zoneId());
        Preconditions.checkArgument(Objects.isNull(tileWithId(tileId).occupant()));
        PlacedTile placedTile = tileWithId(tileId).withOccupant(occupant);

        // Replace the tile that has the given occupant by a new placed tile with the given occupant
        PlacedTile[] newPlacedTiles = Arrays.copyOf(placedTiles, placedTiles.length);
        newPlacedTiles[indexOf(placedTile.pos())] = placedTile;

        // Add the occupant to the zone partitions
        ZonePartitions.Builder newZonePartitions = new ZonePartitions.Builder(zonePartitions);
        Zone zoneOfOccupant = placedTile.zoneWithId(occupant.zoneId());
        if (Objects.nonNull(zoneOfOccupant))
            newZonePartitions.addInitialOccupant(placedTile.placer(), occupant.kind(), zoneOfOccupant);

        return new Board(newPlacedTiles, placedTilesIndex, newZonePartitions.build(), cancelledAnimals);
    }

    /**
     * Used to remove a certain occupant from the board
     * @param occupant The occupant to remove from the board
     * @return A new board without the given occupant
     */
    public Board withoutOccupant(Occupant occupant) {
        int tileId = Zone.tileId(occupant.zoneId());
        PlacedTile placedTile = tileWithId(tileId);

        // Replace the tile that has the given occupant by a new placed tile without the given occupant
        PlacedTile[] newPlacedTiles = Arrays.copyOf(placedTiles, placedTiles.length);
        newPlacedTiles[indexOf(placedTile.withNoOccupant().pos())] = placedTile.withNoOccupant();

        // Remove the occupant from the zone partitions
        ZonePartitions.Builder newZonePartitions = new ZonePartitions.Builder(zonePartitions);
        // If the zone of the occupant exists and is occupied, remove one pawn
        Zone zoneOfOccupant = placedTile.zoneWithId(occupant.zoneId());
        if (Objects.nonNull(zoneOfOccupant))
            newZonePartitions.removePawn(placedTile.placer(), zoneOfOccupant);

        return new Board(newPlacedTiles, placedTilesIndex, newZonePartitions.build(), cancelledAnimals);
    }

    /**
     * Used to return a new board with the gatherers and fishers of the given forests and rivers removed
     * @param forests The forests to remove the gatherers of
     * @param rivers The rivers to remove the fishers of
     * @return A new board with the gatherers and fishers of the given forests and rivers removed
     */
    public Board withoutGatherersOrFishersIn(Set<Area<Zone.Forest>> forests, Set<Area<Zone.River>> rivers) {
        ZonePartitions.Builder newZonePartitions = new ZonePartitions.Builder(zonePartitions);

        // Remove the gatherers from the given forests
        clearOccupants(forests, newZonePartitions::clearGatherers);
        clearOccupants(rivers, newZonePartitions::clearFishers);

        return new Board(placedTiles, placedTilesIndex, newZonePartitions.build(), cancelledAnimals);
    }

    /**
     * Used to return a new board cleared of gatherers or fishers from the given areas
     * @param areas The areas to clear the gatherers and fishers from
     * @param clearOccupants The consumer that clears the occupants of the area
     * @param <S> The type of the zone (forest or river)
     */
    private <S extends Zone> void clearOccupants(Set<Area<S>> areas, Consumer<Area<S>> clearOccupants) {
        for (Area<S> area : areas) {
            for (int id : area.tileIds()) {
                PlacedTile placedTile = tileWithId(id);
                if (Objects.isNull(placedTile.occupant()) || placedTile.occupant().kind() != Occupant.Kind.PAWN) continue;
                boolean isCorrectPawn = area.zones().stream()
                        .map(Zone::id)
                        .anyMatch(i -> i == placedTile.occupant().zoneId());
                if (isCorrectPawn) placedTiles[indexOf(placedTile.pos())] = placedTile.withNoOccupant();
            }
            clearOccupants.accept(area);
        }
    }

    /**
     * Used to return a new board with the given animals added to the cancelled animals of the board
     * @param newlyCancelledAnimals The newly cancelled animals to add to the board
     * @return A new board with the newly cancelled animals added to the cancelled animals of the board
     */
    public Board withMoreCancelledAnimals(Set<Animal> newlyCancelledAnimals) {
        Set<Animal> newCancelledAnimals = new HashSet<>(cancelledAnimals);
        newCancelledAnimals.addAll(newlyCancelledAnimals);
        return new Board(placedTiles, placedTilesIndex, zonePartitions, Collections.unmodifiableSet(newCancelledAnimals));
    }

    @Override
    public boolean equals(Object o) {
        // Easier cases: return false if o is null or not an instance of Board, and true if o is this
        if (!(o instanceof Board board)) return false;
        else if (o == this) return true;
        // Deep copy: compare the placed tiles, placed tiles index, zone partitions and cancelled animals
        else return Arrays.equals(placedTiles, board.placedTiles) && Arrays.equals(placedTilesIndex, board.placedTilesIndex)
                    && zonePartitions.equals(board.zonePartitions) && cancelledAnimals.equals(board.cancelledAnimals);
    }

    @Override
    public int hashCode() {
        // Hash the placed tiles, placed tiles index, zone partitions and cancelled animals
        return Objects.hash(Arrays.hashCode(placedTiles), Arrays.hashCode(placedTilesIndex),
                zonePartitions, cancelledAnimals);
    }


    /**
     * Used to return the index of the placed tile in the placedTiles array
     * @param pos The position of the placed tile
     * @return The index of the placed tile in the placedTiles array
     */
    private int indexOf(Pos pos) {
        return TILES_PER_SIDE * (REACH + pos.y()) + REACH + pos.x();
    }
}