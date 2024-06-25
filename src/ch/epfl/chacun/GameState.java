package ch.epfl.chacun;

import ch.epfl.chacun.extensions.gui.GameUI;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Used to represent the state of the game at a given moment
 * @param players The list of players in the game in the order they play, with the first player being the current player
 * @param tileDecks The decks of tiles available to the players
 * @param tileToPlace The tile to place on the board, or null if no tile is currently being placed
 * @param board The board containing the tiles and occupants placed on it
 * @param nextAction The next action to be taken by the current player
 * @param messageBoard The message board containing the messages displayed to the players up to this point
 * @author Antoine Bastide (375407)
 * @author Adam Bekkar (379476)
 */
public record GameState(List<PlayerColor> players, TileDecks tileDecks, Tile tileToPlace,
                        Board board, Action nextAction, MessageBoard messageBoard) {
    /** The last action played in the game */
    private static Action lastAction = Action.START_GAME;
    /** The winners of the game and their score */
    private static Map.Entry<List<PlayerColor>, Integer> playerWinners =
            new AbstractMap.SimpleEntry<>(new ArrayList<>(), 0);

    /**
     * Used to create a new state of the game
     * @param players The list of players in the game in the order they play, with the first player being the current player
     * @param tileDecks The decks of tiles available to the players
     * @param tileToPlace The tile to place on the board, or null if no tile is currently being placed
     * @param board The board containing the tiles and occupants placed on it
     * @param nextAction The next action to be taken by the current player
     * @param messageBoard The message board containing the messages displayed to the players up to this point
     */
    public GameState {
        Preconditions.checkArgument(players.size() > 1);
        Preconditions.checkArgument(Objects.isNull(tileToPlace) ^ nextAction == Action.PLACE_TILE);
        players = List.copyOf(players);
    }

    /**
     * Used to get the last action played in the game
     * @return The last action played in the game
     */
    public Action lastAction() {
        return lastAction;
    }

    /**
     * Used to get the winners of the game and their score
     * @return The winners of the game and their score
     */
    public Map.Entry<List<PlayerColor>, Integer> getWinners() {
        return Map.Entry.copyOf(playerWinners);
    }

    /** Represents the next action to be taken by the current player */
    public enum Action { START_GAME, PLACE_TILE, OCCUPY_TILE, RETAKE_PAWN, END_GAME }

    /**
     * Used to get the initial state of the game
     * @param players The list of players in the game in the order they play,
     *                with the first player being the current player
     * @param tileDecks The decks of tiles available to the players
     * @param textMaker The text maker used to generate the messages
     * @return The initial state of the game
     */
    public static GameState initial(List<PlayerColor> players, TileDecks tileDecks, TextMaker textMaker) {
        return new GameState(players, tileDecks, null, Board.EMPTY,
                Action.START_GAME, new MessageBoard(textMaker, List.of()));
    }

    /**
     * Used to get the player that is currently playing
     * @return The player that is currently playing, or null if the next action is START_GAME or END_GAME
     */
    public PlayerColor currentPlayer() {
        return nextAction == Action.START_GAME || nextAction == Action.END_GAME ? null : players.getFirst();
    }

    /**
     * Used to get the number of free occupants of a given kind that a player can place on the board
     * @param player The player for which to get the number of free occupants
     * @param kind The kind of occupant for which to get the number of free occupants
     * @return The number of free occupants of the given kind that the player can place on the board
     */
    public int freeOccupantsCount(PlayerColor player, Occupant.Kind kind) {
        return Occupant.occupantsCount(kind) - board.occupantCount(player, kind);
    }

    /**
     * Used to get the set of potential occupants of the last placed tile
     * @return The set of potential occupants of the last placed tile
     * @throws IllegalArgumentException If no tile has been placed yet
     */
    public Set<Occupant> lastTilePotentialOccupants() {
        PlacedTile placedTile = board.lastPlacedTile();
        Preconditions.checkArgument(!board.equals(Board.EMPTY) && Objects.nonNull(placedTile));

        Set<Occupant> occupants = new HashSet<>(placedTile.potentialOccupants());
        occupants.removeIf(o -> {
            // Check if the player has any free occupant of the given kind
            if (freeOccupantsCount(currentPlayer(), o.kind()) == 0) return true;

            return board.tileWithId(Zone.tileId(o.zoneId())).tile().zones().stream().anyMatch(
                    zone -> {
                        if (zone.id() != o.zoneId()) return false;
                        return switch (zone) {
                            case Zone.Meadow meadow when o.kind() == Occupant.Kind.PAWN ->
                                    board.meadowArea(meadow).isOccupied();
                            case Zone.Forest forest when o.kind() == Occupant.Kind.PAWN ->
                                    board.forestArea(forest).isOccupied();
                            case Zone.River river when o.kind() == Occupant.Kind.PAWN ->
                                    board.riverArea(river).isOccupied();
                            case Zone.River river when o.kind() == Occupant.Kind.HUT ->
                                    board.riverSystemArea(river).isOccupied();
                            case Zone.Lake lake when o.kind() == Occupant.Kind.HUT ->
                                    board.riverSystemArea(lake).isOccupied();
                            default -> false;
                        };
                    });
        });

        return Set.copyOf(occupants);
    }

    /**
     * Used to place the starting tile of the game on the board and start the game
     * @return The new state of the game with the starting tile placed
     * @throws IllegalArgumentException If the next action is not START_GAME
     */
    public GameState withStartingTilePlaced() {
        Preconditions.checkArgument(nextAction == Action.START_GAME);
        // Place the starting tile on the board
        PlacedTile placedTile = new PlacedTile(tileDecks.topTile(Tile.Kind.START), null, Rotation.NONE, Pos.ORIGIN);
        Board newBoard = board.withNewTile(placedTile);

        // Draw the first normal tile and return the new game state
        Tile tileToPlace = tileDecks.topTile(Tile.Kind.NORMAL);
        TileDecks newTileDecks = tileDecks.withTopTileDrawn(Tile.Kind.START).withTopTileDrawn(Tile.Kind.NORMAL);
        return new GameState(players, newTileDecks, tileToPlace, newBoard, Action.PLACE_TILE, messageBoard);
    }

    /**
     * Used to place a tile on the board
     * @param placedTile The tile to place on the board
     * @return The new state of the game with the tile placed
     * @throws IllegalArgumentException If the next action is not PLACE_TILE
     */
    public GameState withPlacedTile(PlacedTile placedTile) {
        Preconditions.checkArgument(nextAction == Action.PLACE_TILE && Objects.isNull(placedTile.occupant()));
        lastAction = Action.PLACE_TILE;
        // Update the board, the message board and the tile decks
        Board nB = board.withNewTile(placedTile);
        MessageBoard nMB = messageBoard;

        // Check if the placed tile has special powers
        Zone spZone = placedTile.specialPowerZone();
        if (Objects.nonNull(spZone)) {
            switch (spZone.specialPower()) {
                // Score the log-boat
                case LOGBOAT -> nMB = nMB.withScoredLogboat(currentPlayer(),
                        nB.riverSystemArea((Zone.Water) spZone));
                // Score the hunting trap and remove the animals
                case HUNTING_TRAP -> {
                    // Find the meadows adjacent to the meadow where the hunting trap is placed and the animals in them
                    Area<Zone.Meadow> adjacentMeadows = nB.adjacentMeadow(placedTile.pos(), (Zone.Meadow) spZone);
                    Set<Animal> animals = Area.animals(adjacentMeadows, nB.cancelledAnimals());
                    Set<Animal> animalsToCancel = animalsToCancel(adjacentMeadows, nB, false, null);
                    // Cancel the animals in the meadows and score the hunting trap
                    nB = nB.withMoreCancelledAnimals(animalsToCancel);
                    nMB = nMB.withScoredHuntingTrap(currentPlayer(), adjacentMeadows, animalsToCancel);
                    nB = nB.withMoreCancelledAnimals(animals);
                }
                case SHAMAN -> {
                    if (nB.occupantCount(currentPlayer(), Occupant.Kind.PAWN) > 0)
                        return new GameState(players, tileDecks, null, nB, Action.RETAKE_PAWN, nMB);
                }
            }
        }


        // Check if the player can occupy a tile, or tally the points at the end of the turn
        GameState tempGameState = new GameState(players, tileDecks, placedTile.tile(), nB, Action.PLACE_TILE, nMB);
        if (!tempGameState.lastTilePotentialOccupants().isEmpty())
            return new GameState(players, tileDecks, null, nB, Action.OCCUPY_TILE, nMB);
        else return tallyTurnPoints(nB, nMB);
    }

    /**
     * Used to retake a pawn on the board
     * @param occupant The occupant to remove from the board
     * @return The new state of the game with the occupant removed
     * @throws IllegalArgumentException If the next action is not RETAKE_PAWN
     */
    public GameState withOccupantRemoved(Occupant occupant) {
        // Check the arguments and update the board
        Preconditions.checkArgument(nextAction == Action.RETAKE_PAWN &&
                (Objects.isNull(occupant) || occupant.kind() == Occupant.Kind.PAWN));
        Board newBoard = Objects.nonNull(occupant) ? board.withoutOccupant(occupant) : board;

        lastAction = Action.RETAKE_PAWN;
        GameUI.lastOccupant.set(occupant);
        // Check if the player can occupy a tile, or tally the points at the end of the turn
        if (!lastTilePotentialOccupants().isEmpty())
            return new GameState(players, tileDecks, null, newBoard, Action.OCCUPY_TILE, messageBoard);
        else return tallyTurnPoints(newBoard, messageBoard);
    }

    /**
     * Used to occupy a tile on the board
     * @param occupant The occupant to place on the board
     * @return The new state of the game with the occupant placed
     * @throws IllegalArgumentException If the next action is not OCCUPY_TILE
     */
    public GameState withNewOccupant(Occupant occupant) {
        Preconditions.checkArgument(nextAction == Action.OCCUPY_TILE);
        lastAction = Action.OCCUPY_TILE;
        GameUI.lastOccupant.set(occupant);
        return tallyTurnPoints(Objects.isNull(occupant) ? board : board.withOccupant(occupant), messageBoard);
    }

    /**
     * Used to check if a given tile has closed a menhir forest
     * @param board The board
     * @param tileDecks The tile decks
     * @return True if the tile has closed a menhir forest, false otherwise
     */
    private boolean canPlayMenhir(Board board, TileDecks tileDecks) {
        Set<Area<Zone.Forest>> closedForests = board.forestsClosedByLastTile();
        PlacedTile placedTile = board.lastPlacedTile();
        return Objects.nonNull(placedTile) && placedTile.kind() == Tile.Kind.NORMAL
                && tileDecks.deckSize(Tile.Kind.MENHIR) > 0
                && !closedForests.isEmpty() && closedForests.stream().anyMatch(Area::hasMenhir);
    }

    /**
     * Used to cancel the animals in a meadow area
     * @param meadowArea The meadow area in which to cancel the animals
     * @param newBoard The current state of the board
     * @param hasWildFire True if the meadow area has a wildfire, false otherwise
     * @return The set of animals to cancel in the meadow area
     */
    private Set<Animal> animalsToCancel(Area<Zone.Meadow> meadowArea, Board newBoard, boolean hasWildFire,
                                        Zone.Meadow pitTrapZone) {
        // Make sur there are animals in the meadows
        Set<Animal> animals = Area.animals(meadowArea, newBoard.cancelledAnimals());
        if (animals.isEmpty()) return Set.of();

        // Find all the tigers
        Set<Animal> animalsToCancel = animals.stream()
                .filter(a -> a.kind() == Animal.Kind.TIGER)
                .collect(Collectors.toSet());
        Set<Animal> deerToCancel = animals.stream()
                .filter(a -> a.kind() == Animal.Kind.DEER)
                .collect(Collectors.toSet());

        // Count all the deer and tigers in the meadows
        int tigerCount = animalsToCancel.size();
        int deerCount = deerToCancel.size();

        // If there are no deer or the meadow has a wildfire, cancel all the tigers
        if (tigerCount > 0 && !hasWildFire && deerCount > 0) {
            // Check for a pit trap
            if (Objects.nonNull(pitTrapZone) && deerCount >= tigerCount) {
                // Find the tile ids that are adjacent to the pit trap
                Pos pos = newBoard.tileWithId(Zone.tileId(pitTrapZone.id())).pos();
                Set<Zone.Meadow> adjacentZones = newBoard.adjacentMeadow(pos, pitTrapZone).zones();
                Set<Integer> tileIds = meadowArea.zones().stream()
                        .filter(zone -> !adjacentZones.contains(zone))
                        .map(Zone::tileId)
                        .collect(Collectors.toSet());

                // Find the deer to keep, i.e. the ones that are in the adjacent meadows
                // The number of deer to keep is deerCount - tigerCount, since deerCount >= tigerCount
                Set<Animal> deerToKeep = deerToCancel.stream()
                        .filter(d -> !tileIds.contains(d.tileId()))
                        .limit(deerCount - tigerCount)
                        .collect(Collectors.toSet());

                // Find the deer to remove
                deerToCancel.removeAll(deerToKeep);
            }
            // Remove the correct number of deer from the set of animals to cancel
            animalsToCancel.addAll(
                    deerToCancel.stream()
                            .limit(Math.min(tigerCount, deerCount))
                            .collect(Collectors.toSet())
            );
        }

        // Do not cancel the tigers if the meadow does not have a wildfire or a pit trap
        animalsToCancel.removeIf(a -> a.kind() == Animal.Kind.TIGER && (!hasWildFire || Objects.nonNull(pitTrapZone)));
        return animalsToCancel;
    }

    /**
     * Used to tally the points at the end of a player's turn
     * @param newBoard The new state of the board
     * @param newMessageBoard The new state of the message board
     * @return The new state of the game after tallying the points
     */
    private GameState tallyTurnPoints(Board newBoard, MessageBoard newMessageBoard) {
        // Create the new tile decks with the normal top tile drawn
        TileDecks normalDeck = tileDecks
                .withTopTileDrawnUntil(Tile.Kind.NORMAL, newBoard::couldPlaceTile);
        // Create the new tile decks with the menhir top tile drawn
        TileDecks menhirDeck = tileDecks
                .withTopTileDrawnUntil(Tile.Kind.MENHIR, newBoard::couldPlaceTile);

        Set<Area<Zone.Forest>> closedForests = newBoard.forestsClosedByLastTile();
        Set<Area<Zone.River>> closedRivers = newBoard.riversClosedByLastTile();

        // Check if the player has closed a forest
        for (Area<Zone.Forest> forestArea : closedForests)
            newMessageBoard = newMessageBoard.withScoredForest(forestArea);
        // Check if the player has closed a river
        for (Area<Zone.River> riverArea : closedRivers)
            newMessageBoard = newMessageBoard.withScoredRiver(riverArea);

        // Remove the gatherers and fishers from the closed forests and rivers
        newBoard = newBoard.withoutGatherersOrFishersIn(closedForests, closedRivers);

        // If the placed tile has closed a menhir forest, the player can place another tile
        boolean secondTurn = false;
        if (canPlayMenhir(newBoard, menhirDeck)) {
            // Assign the points to the player that has closed the menhir forest
            for (Area<Zone.Forest> forestArea : closedForests)
                if (Area.hasMenhir(forestArea)) {
                    newMessageBoard = newMessageBoard.withClosedForestWithMenhir(currentPlayer(), forestArea);
                    break;
                }
            secondTurn = true;
        }
        // If there are no more tiles, tally the final scores
        else if (normalDeck.deckSize(Tile.Kind.NORMAL) == 0)
            return tallyFinalScores(newBoard, newMessageBoard, normalDeck);

        // Draw a new tile and rotate the players
        Tile.Kind tileKind = secondTurn ? Tile.Kind.MENHIR : Tile.Kind.NORMAL;
        TileDecks newTileDecks = secondTurn ? menhirDeck : normalDeck;
        Tile tile = newTileDecks.topTile(tileKind);
        List<PlayerColor> newPlayers = new ArrayList<>(players);
        if (!secondTurn) Collections.rotate(newPlayers, -1);
        newTileDecks = newTileDecks.withTopTileDrawn(tileKind);
        return new GameState(newPlayers, newTileDecks, tile, newBoard, Action.PLACE_TILE, newMessageBoard);
    }

    /**
     * Used to tally the final scores at the end of the game
     * @param newBoard The new state of the board
     * @param newMessageBoard The new state of the message board
     * @param newTileDecks The new state of the tile decks
     * @return The new state of the game after tallying the final scores
     */
    private GameState tallyFinalScores(Board newBoard, MessageBoard newMessageBoard, TileDecks newTileDecks) {
        // Tally the scores for the meadows
        for (Area<Zone.Meadow> meadowArea : newBoard.meadowAreas()) {
            // Check if the meadow has a wildfire or a pit trap
            boolean hasWildFire = meadowArea.zoneWithSpecialPower(Zone.SpecialPower.WILD_FIRE) != null;
            Zone.Meadow pitTrapZone = (Zone.Meadow) meadowArea.zoneWithSpecialPower(Zone.SpecialPower.PIT_TRAP);

            // Find the animals to cancel in the meadow area and score the area
            Set<Animal> animalsToCancel = animalsToCancel(meadowArea, newBoard, hasWildFire, pitTrapZone);
            newBoard = newBoard.withMoreCancelledAnimals(animalsToCancel);
            newMessageBoard = newMessageBoard.withScoredMeadow(meadowArea, animalsToCancel);
            // Check if the meadow has a pit trap and score it
            if (Objects.nonNull(pitTrapZone)) {
                Pos pos = newBoard.tileWithId(Zone.tileId(pitTrapZone.id())).pos();
                Area<Zone.Meadow> adjacentMeadow = newBoard.adjacentMeadow(pos, pitTrapZone);
                newMessageBoard = newMessageBoard.withScoredPitTrap(adjacentMeadow, newBoard.cancelledAnimals());
            }
        }

        // Tally the scores for the river systems
        for (Area<Zone.Water> riverSystemArea : newBoard.riverSystemAreas()) {
            boolean hasRaft = riverSystemArea.zoneWithSpecialPower(Zone.SpecialPower.RAFT) != null;
            if (hasRaft) newMessageBoard = newMessageBoard.withScoredRaft(riverSystemArea);
            newMessageBoard = newMessageBoard.withScoredRiverSystem(riverSystemArea);
        }

        // Find the winners
        Map<PlayerColor, Integer> results = newMessageBoard.points();
        int maxScore = results.values().stream().max(Integer::compare).orElse(0);
        Set<PlayerColor> winners;
        if (maxScore != 0) {
            winners = results.entrySet().stream()
                    .filter(e -> e.getValue() == maxScore)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
        } else winners = new HashSet<>(players);
        playerWinners.getKey().clear();
        playerWinners.getKey().addAll(winners);
        playerWinners.setValue(maxScore);
        lastAction = Action.END_GAME;
        newMessageBoard = newMessageBoard.withWinners(winners, maxScore);
        return new GameState(players, newTileDecks, null, newBoard, Action.END_GAME, newMessageBoard);
    }
}