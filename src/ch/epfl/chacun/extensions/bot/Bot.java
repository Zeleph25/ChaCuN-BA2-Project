package ch.epfl.chacun.extensions.bot;

import ch.epfl.chacun.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a bot that can play the game
 * @author Adam BEKKAR (379476)
 */
public class Bot {
    /** The maximum number of computations the bot can do to simulate the tile placing */
    private int maxComputationsPlacingTile;
    /** The maximum number of computations the bot can do to simulate the occupation */
    private int maxComputationsOccupyingRetaking;

    /** The level of the bot */
    private final Level botLevel;

    /** The last adaptive strategy used by the bot */
    private AdaptiveStrategy lastStrategy = AdaptiveStrategy.EARLY;
    /** The current strategy of the bot */
    private AdaptiveStrategy currentStrategy = AdaptiveStrategy.EARLY;
    /** List of all placed tiles */
    private final List<PlacedTile> lastPlacedTiles = new ArrayList<>();

    /** Enum representing the levels of the bot */
    public enum Level { BABY, EASY, MEDIUM, HARD, IMPOSSIBLE }

    /** Enum representing the adaptive strategies of the bot */
    private enum AdaptiveStrategy {
        EARLY, MID, LATE;

        /**
         * Used to determine the strategy of the bot
         * @param freePawns The number of free pawns
         * @param freeHuts The number of free huts
         * @param occupantsOnBoard The number of occupants on the board
         * @param tilesPlaced The number of tiles placed
         * @param lastStrategy The last strategy used
         * @return The strategy of the bot
         */
        public static AdaptiveStrategy determineStrategy(int freePawns, int freeHuts, int occupantsOnBoard, int tilesPlaced, AdaptiveStrategy lastStrategy) {
            if ((freePawns == 0 || freeHuts == 0 || occupantsOnBoard > 20 || tilesPlaced > 25) && lastStrategy == MID) {
                return LATE;
            } else if ((tilesPlaced > 20 || occupantsOnBoard > 10) && lastStrategy == EARLY) {
                return MID;
            } else {
                return EARLY;
            }
        }
    }

    /** Used to create a bot with a certain level */
    public Bot(Level level) {
        botLevel = level;
        switch (level) {
            case BABY -> {
                maxComputationsPlacingTile = 1;
                maxComputationsOccupyingRetaking = 1;
            }
            case EASY -> {
                maxComputationsPlacingTile = 1;
                maxComputationsOccupyingRetaking = 1;
            }
            case MEDIUM -> {
                maxComputationsPlacingTile = 2;
                maxComputationsOccupyingRetaking = 2;
            }
            case HARD -> {
                maxComputationsPlacingTile = 4;
                maxComputationsOccupyingRetaking = 4;
            }
            case IMPOSSIBLE -> {
                maxComputationsPlacingTile = 5;
                maxComputationsOccupyingRetaking = 5;
            }
        }
    }

    /**
     * Used to update the maximum number of computations based on the strategy
     */
    private void updateComputationsBasedOnStrategy() {
        switch (currentStrategy) {
            case EARLY -> {
                maxComputationsPlacingTile = Math.min(maxComputationsPlacingTile, 2);
                maxComputationsOccupyingRetaking = Math.min(maxComputationsOccupyingRetaking, 2);
            }
            case MID -> {
                maxComputationsPlacingTile = Math.min(maxComputationsPlacingTile, 3);
                maxComputationsOccupyingRetaking = Math.min(maxComputationsOccupyingRetaking, 3);
            }
            case LATE -> {
                maxComputationsPlacingTile = Math.min(maxComputationsPlacingTile, 2);
                maxComputationsOccupyingRetaking = Math.min(maxComputationsOccupyingRetaking, 2);
            }
        }
    }

    /**
     * Used to create a bot with a certain botDifficulty
     * @param gameState The game state
     * @return The bot with the botDifficulty
     */
    public ActionEncoder.StateAction play(GameState gameState) {
        if (botLevel == Level.BABY) return playRandomAction(gameState);

        int freePawns = gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.PAWN);
        int freeHuts = gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.HUT);
        int occupantsOnBoard = gameState.board().occupants().size();
        int tilesPlaced = lastPlacedTiles.size();

        currentStrategy = AdaptiveStrategy.determineStrategy
                (freePawns, freeHuts, occupantsOnBoard, tilesPlaced, lastStrategy);
        lastStrategy = currentStrategy;
        updateComputationsBasedOnStrategy();

        ActionEncoder.StateAction stateAction = simulateGame(gameState);
        if (gameState.nextAction() == GameState.Action.PLACE_TILE)
            lastPlacedTiles.add(stateAction.gameState().board().lastPlacedTile());
        return stateAction;
    }

    /**
     * Used to play a random action (for the BABY level bot)
     * @param gameState The game state
     * @return A random action to play
     */
    private ActionEncoder.StateAction playRandomAction(GameState gameState) {
        List<ActionEncoder.StateAction> possibleActions = new ArrayList<>();
        switch (gameState.nextAction()) {
            case PLACE_TILE -> {
                Tile tileToPlace = gameState.tileToPlace();
                List<PlacedTile> potentialPlaceTiles = getPotentialPlacedTiles(gameState, tileToPlace);
                possibleActions.addAll(potentialPlaceTiles.stream()
                        .map(tile -> ActionEncoder.withPlacedTile(gameState, tile))
                        .toList());
            }
            case OCCUPY_TILE -> {
                for (Occupant occupant : gameState.lastTilePotentialOccupants()) {
                    possibleActions.add(ActionEncoder.withNewOccupant(gameState, occupant));
                }
            }
            case RETAKE_PAWN -> {
                for (Occupant occupant : gameState.board().occupants()) {
                    possibleActions.add(ActionEncoder.withOccupantRemoved(gameState, occupant));
                }
            }
            case START_GAME, END_GAME -> throw new IllegalArgumentException();
        }
        return possibleActions.get(new Random().nextInt(possibleActions.size()));
    }


    /**
     * Used to get the tiles that enlarge areas if placed and a code to determine the type area
     * with the following code : 0 - Forest, 1 - Meadow, 2 - River
     * @param gameState The game state
     * @return The tile that enlarges an area if placed and the code to determine the type area
     */
    private Map.Entry<PlacedTile, Integer> determineEnlargingTile(GameState gameState) {
        Tile tileToPlace = gameState.tileToPlace();
        List<PlacedTile> potentialPlacedTiles = getPotentialPlacedTiles(gameState, tileToPlace);

        List<Map.Entry<PlacedTile, Integer>> enlargingTiles = new ArrayList<>();

        for (PlacedTile placedTile : potentialPlacedTiles) {
            GameState newGameState = ActionEncoder.withPlacedTile(gameState, placedTile).gameState();

            Set<Area<Zone.Water>> riverSystems = newGameState.board().riverSystemAreas();
            Set<Area<Zone.Meadow>> meadows = newGameState.board().meadowAreas();
            Set<Area<Zone.Forest>> forests = newGameState.board().forestAreas();

            Area<Zone.Forest> forestExtended = forests.stream()
                    .filter(area -> area.zones().stream().anyMatch(z -> placedTile.forestZones().contains(z))
                            && area.zones().stream().anyMatch(z1 -> lastPlacedTiles.stream()
                            .map(PlacedTile::forestZones).anyMatch(z2 -> z2.contains(z1)))).findFirst().orElse(null);

            Area<Zone.Meadow> meadowExtended = meadows.stream()
                    .filter(area -> area.zones().stream().anyMatch(z -> placedTile.meadowZones().contains(z))
                            && area.zones().stream().anyMatch(z1 -> lastPlacedTiles.stream()
                            .map(PlacedTile::meadowZones).anyMatch(z2 -> z2.contains(z1)))).findFirst().orElse(null);

            Area<Zone.Water> riverSystemExtended = riverSystems.stream()
                    .filter(area -> area.zones().stream().anyMatch(z -> placedTile.riverZones().contains(z))
                            && area.zones().stream().anyMatch(z1 -> lastPlacedTiles.stream()
                            .map(PlacedTile::riverZones).anyMatch(z2 -> z2.contains(z1)))).findFirst().orElse(null);

            if (Objects.nonNull(forestExtended)) enlargingTiles.add(Map.entry(placedTile, 1));
            else if (Objects.nonNull(meadowExtended)) enlargingTiles.add(Map.entry(placedTile, 2));
            else if (Objects.nonNull(riverSystemExtended)) enlargingTiles.add(Map.entry(placedTile, 3));
        }
        return enlargingTiles.stream()
                .max(Comparator.comparingInt(e -> getPointsForTile(gameState, e.getKey())))
                .orElse(null);
    }

    /**
     * Used to simulate the game and get the best action to play
     * @param gameState The game state
     * @return The best action to play
     */
    private ActionEncoder.StateAction simulateGame(GameState gameState) {
        Map<Integer, Integer> rankingTree = new TreeMap<>();
        int maxDepth = getMaxDepthForStrategy(gameState);
        simulateGameRecursive(gameState, rankingTree, 0, 0, maxDepth);

        // Find the best decision based on rankings
        int bestIndex = rankingTree.entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey).orElse(0);

        int bestDecision = bestIndex % 3;

        return getActionBasedOnDecision(bestDecision, gameState);
    }

    private int getMaxDepthForStrategy(GameState gameState) {
        int freePawns = gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.PAWN);
        int freeHuts = gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.HUT);

        return switch (currentStrategy) {
            case EARLY -> freePawns + freeHuts > 2 ? 6 : 4;
            case MID -> freePawns + freeHuts > 2 ? 5 : 3;
            case LATE -> 3;
        };
    }

    /**
     * Used to get the action based on the decision
     * @param bestDecision The best decision
     * @param gameState The game state
     * @return The action to play
     */
    private ActionEncoder.StateAction getActionBasedOnDecision
    (int bestDecision, GameState gameState) {
        return switch (gameState.nextAction()) {
            case PLACE_TILE -> switch (bestDecision) {
                case 0 -> playMaximizingPointsTurnForBot(gameState);
                case 1 -> playMinimizingPointsGameForBestOpponent(gameState);
                case 2 -> playMinimizingPointsGameForAllOpponentsAndMaximizingForBot(gameState);
                default -> throw new IllegalArgumentException();
            };
            case OCCUPY_TILE, RETAKE_PAWN -> handleOccupyOrRetake(gameState);
            case START_GAME, END_GAME -> throw new IllegalArgumentException();
        };
    }

    /**
     * Used to handle the occupy or retake action
     * @param gameState The game state
     * @return The action to play
     */
    private ActionEncoder.StateAction handleOccupyOrRetake(GameState gameState) {
        return switch (gameState.nextAction()) {
            case OCCUPY_TILE -> {
                Occupant bestOccupantToOccupy = getBestOccupantToOccupy(gameState);
                yield ActionEncoder.withNewOccupant(gameState, bestOccupantToOccupy);
            }
            case RETAKE_PAWN -> {
                Occupant bestOccupantToRetake = getBestOccupantToRetake(gameState);
                yield ActionEncoder.withOccupantRemoved(gameState, bestOccupantToRetake);
            }
            case START_GAME, PLACE_TILE, END_GAME -> throw new IllegalArgumentException();
        };
    }

    /**
     * Used to simulate the game recursively
     * @param gameState The game state
     * @param rankingTree The ranking tree
     * @param currentIndex The current index
     * @param depth The depth of the tree
     * @param maxDepth The maximum depth of the tree
     */
    private void simulateGameRecursive
    (GameState gameState, Map<Integer, Integer> rankingTree, int currentIndex, int depth, int maxDepth) {
        // Exit condition for maximum depth and end game
        if (depth == maxDepth || gameState.nextAction() == GameState.Action.END_GAME) return;

        int limitAction = getLimitAction(gameState);

        // Perform iterations for different actions
        for (int i = 0; i < limitAction; i++) {
            GameState newGameState = getBestGameState(gameState);
            if (newGameState == null) break;

            // Perform action and update ranking
            PlayerColor newCurrentPlayer = newGameState.currentPlayer();
            List<PlayerColor> playerColorsRanking = getRankingInWholeGame(newGameState,
                    getGameStateComparatorInWholeGame(newGameState));
            if (playerColorsRanking.contains(newCurrentPlayer))
                rankingTree.put(currentIndex * limitAction + i, playerColorsRanking.indexOf(newCurrentPlayer));

            // Recursive call for the next depth
            simulateGameRecursive(newGameState, rankingTree, currentIndex * 3 + i, depth + 1, maxDepth);
        }
    }

    /**
     * Used to get the new game state for a certain action
     * @param gameState The game state
     * @return The new game state for the action
     */
    private GameState getBestGameState(GameState gameState) {
        return switch (gameState.nextAction()) {
            case PLACE_TILE -> getBestGameStateForPlacingTile(gameState);
            case OCCUPY_TILE -> getBestGameStateForOccupyingTile(gameState);
            case RETAKE_PAWN -> getBestGameStateForRetakingPawn(gameState);
            case START_GAME, END_GAME -> throw new IllegalArgumentException();
        };
    }

    /**
     * Used to get the best game state for placing a tile
     * @param gameState The game state
     * @return The best game state for placing a tile
     */
    private GameState getBestGameStateForPlacingTile(GameState gameState) {
        Tile tileToPlace = gameState.tileToPlace();
        List<PlacedTile> potentialPlaceTiles = getPotentialPlacedTiles(gameState, tileToPlace);

        // Step 1: Check for tiles that allow immediate point gain
        List<PlacedTile> immediatePointsTiles = potentialPlaceTiles.stream()
                .filter(tile -> getPointsForTile(gameState, tile) > 0)
                .toList();

        if (!immediatePointsTiles.isEmpty()) {
            // Choose the best among the immediate points tiles
            PlacedTile bestImmediatePointsTile = immediatePointsTiles.stream()
                    .max(Comparator.comparingInt(tile -> getPointsForTile(gameState, tile)))
                    .orElse(immediatePointsTiles.get(0));

            return ActionEncoder.withPlacedTile(gameState, bestImmediatePointsTile).gameState();
        }

        // Fallback to the original logic
        List<PlacedTile> maxPointsPlacedTiles = maxPointsInOneTurnForBot(gameState, potentialPlaceTiles);
        if (maxPointsPlacedTiles.isEmpty()) throw new IllegalArgumentException();

        Set<PlacedTile> closestTiles = new HashSet<>();
        if (!lastPlacedTiles.isEmpty()) {
            closestTiles = maxPointsPlacedTiles.stream()
                    .filter(tile -> lastPlacedTiles.stream().anyMatch(placedTile ->
                            isAdjacent(tile.pos(), placedTile.pos()) && Objects.nonNull(placedTile.occupant())))
                    .collect(Collectors.toSet());
        }

        Comparator<PlacedTile> ptComparator =  Comparator.comparingInt(tile -> getMaxPointsForTile(gameState, tile));
        PlacedTile bestPlacedTile = !closestTiles.isEmpty()
                ? closestTiles.stream().max(ptComparator).orElse(maxPointsPlacedTiles.get(0))
                : maxPointsPlacedTiles.get(0);

        return ActionEncoder.withPlacedTile(gameState, bestPlacedTile).gameState();
    }

    private int getPointsForTile(GameState gameState, PlacedTile placedTile) {
        GameState newGameState = ActionEncoder.withPlacedTile(gameState, placedTile).gameState();
        return newGameState.messageBoard().points().getOrDefault(gameState.currentPlayer(), 0) -
                gameState.messageBoard().points().getOrDefault(gameState.currentPlayer(), 0);
    }

    private Occupant getOccupantEnlargingAreaIfAny(GameState gameState, PlacedTile placedTile) {
        Preconditions.checkArgument((gameState.nextAction() == GameState.Action.PLACE_TILE
                && Objects.nonNull(placedTile)) || gameState.nextAction() == GameState.Action.OCCUPY_TILE);

        GameState newGameState = gameState.nextAction() == GameState.Action.PLACE_TILE
                ? ActionEncoder.withPlacedTile(gameState, placedTile).gameState()
                : gameState;

        if (newGameState.nextAction() != GameState.Action.OCCUPY_TILE) return null;

        for (PlacedTile lastPlacedTile : lastPlacedTiles) {
            for (Occupant occupant : newGameState.lastTilePotentialOccupants()) {
                GameState newGameStateWithOccupant = ActionEncoder.withNewOccupant(newGameState, occupant).gameState();

                Set<Area<Zone.Water>> riverSystems = newGameStateWithOccupant.board().riverSystemAreas();
                Set<Area<Zone.Meadow>> meadows = newGameStateWithOccupant.board().meadowAreas();
                Set<Area<Zone.Forest>> forests = newGameStateWithOccupant.board().forestAreas();

                boolean extendsForest = forests.stream()
                        .anyMatch(area -> area.zones().stream().anyMatch(z -> lastPlacedTile.forestZones().contains(z))
                                && area.zones().stream().anyMatch(z -> z.id() == occupant.zoneId()));
                boolean extendsMeadow = meadows.stream()
                        .anyMatch(area -> area.zones().stream().anyMatch(z -> lastPlacedTile.meadowZones().contains(z))
                                && area.zones().stream().anyMatch(z -> z.id() == occupant.zoneId()));
                boolean extendsRiverSystem = riverSystems.stream()
                        .anyMatch(area -> area.zones().stream().anyMatch(z -> lastPlacedTile.riverZones().contains(z))
                                && area.zones().stream().anyMatch(z -> z.id() == occupant.zoneId()));

                if (extendsForest || extendsMeadow || extendsRiverSystem) return occupant;
            }
        }
        return null;
    }

    /**
     * Used to get the maximum number of computations for a certain action
     * @param gameState The game state
     * @return The maximum number of computations for the action
     */
    private int getLimitAction(GameState gameState) {
        return switch (gameState.nextAction()) {
            case PLACE_TILE -> maxComputationsPlacingTile;
            case OCCUPY_TILE, RETAKE_PAWN -> maxComputationsOccupyingRetaking;
            case START_GAME, END_GAME -> throw new IllegalArgumentException();
        };
    }

    private boolean isAdjacent(Pos pos1, Pos pos2) {
        int dx = Math.abs(pos1.x() - pos2.x());
        int dy = Math.abs(pos1.y() - pos2.y());
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }

    /**
     * Used to get the best game state for occupying a tile
     * @param gameState The game state
     * @return The best game state for occupying a tile
     */
    private GameState getBestGameStateForOccupyingTile(GameState gameState) {
        Occupant bestOccupantToOccupy = getBestOccupantToOccupy(gameState);
        return ActionEncoder.withNewOccupant(gameState, bestOccupantToOccupy).gameState();
    }

    /**
     * Used to get the best game state for retaking a pawn
     * @param gameState The game state
     * @return The best game state for retaking a pawn
     */
    private GameState getBestGameStateForRetakingPawn(GameState gameState) {
        Occupant bestOccupantToRetake = getBestOccupantToRetake(gameState);
        return ActionEncoder.withOccupantRemoved(gameState, bestOccupantToRetake).gameState();
    }

    /**
     * Used to get the placed tiles that maximize the points in one turn for the bot
     * @param gameState The game state
     * @param potentialPlaceTiles The potential place tiles
     * @return The placed tiles that maximize the points in one turn for the bot
     */
    private List<PlacedTile> maxPointsInOneTurnForBot(GameState gameState, List<PlacedTile> potentialPlaceTiles) {
        if (potentialPlaceTiles.isEmpty()) return List.of();
        int maxPoints = getMaxPointsForTiles(gameState, potentialPlaceTiles);

        return potentialPlaceTiles.stream()
                .filter(p -> getMaxPointsForTile(gameState, p) == maxPoints)
                .toList();
    }

    /**
     * Used to calculate the maximum points for a list of tiles
     * @param gameState           The game state
     * @param potentialPlaceTiles The potential place tiles
     * @return The maximum points for the list of tiles
     */
    private int getMaxPointsForTiles(GameState gameState, List<PlacedTile> potentialPlaceTiles) {
        return potentialPlaceTiles.stream()
                .map(p -> getMaxPointsForTile(gameState, p))
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * Used to calculate the maximum points for a single tile
     * @param gameState  The game state
     * @param placedTile The placed tile
     * @return The maximum points for the tile
     */
    private int getMaxPointsForTile(GameState gameState, PlacedTile placedTile) {
        GameState newGameState = ActionEncoder.withPlacedTile(gameState, placedTile).gameState();
        return newGameState.messageBoard().points().getOrDefault(gameState.currentPlayer(), 0) -
                gameState.messageBoard().points().getOrDefault(gameState.currentPlayer(), 0);
    }

    /**
     * Used to get the best occupant to retake
     * @param gameState The game state
     * @return The best occupant to retake
     */
    private Occupant getBestOccupantToRetake(GameState gameState) {
        boolean shouldPrioritize = currentStrategy == AdaptiveStrategy.LATE &&
                (gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.PAWN) == 0 ||
                        gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.HUT) == 0);

        List<Occupant> validOccupants = gameState.board().occupants().stream()
                .filter(o -> {
                    Board board = gameState.board();
                    PlayerColor placer = board.tileWithId(Zone.tileId(o.zoneId())).placer();
                    return o.kind() == Occupant.Kind.PAWN && board.occupants().contains(o)
                            && placer == gameState.currentPlayer();
                })
                .toList();

        return validOccupants.stream()
                .max(Comparator.comparingInt(o -> {
                    GameState newGameStateWithOccupantRemoved = gameState.withOccupantRemoved(o);
                    int points = simulateGame(newGameStateWithOccupantRemoved).gameState().messageBoard().points()
                            .getOrDefault(gameState.currentPlayer(), 0);
                    if (shouldPrioritize) points += 5; // Add a bias to prioritize this action
                    else points -= 5; // Temporize by reducing the score
                    return points;
                }))
                .orElse(null);
    }

    /**
     * Used to get the best occupant to occupy
     * @param gameState The game state
     * @return The best occupant to occupy
     */
    private Occupant getBestOccupantToOccupy(GameState gameState) {
        // Check for an occupant that gives immediate points
        Optional<Occupant> immediatePointsOccupant = gameState.lastTilePotentialOccupants().stream()
                .filter(occupant -> getSignedPointsIfOccupying(gameState, occupant).get(gameState.currentPlayer()) > 0)
                .max(Comparator.comparingInt(occupant ->
                        getSignedPointsIfOccupying(gameState, occupant).get(gameState.currentPlayer())));

        // If there is an occupant that gives immediate points, return it
        if (immediatePointsOccupant.isPresent()) return immediatePointsOccupant.get();

        // If no immediate points occupant and no enlarging occupants,
        // find the one that gives the most points in the long term
        boolean shouldPrioritize = currentStrategy == AdaptiveStrategy.LATE &&
                (gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.PAWN) == 0 ||
                        gameState.freeOccupantsCount(gameState.currentPlayer(), Occupant.Kind.HUT) == 0);

        Map<Occupant, Map<PlayerColor, Integer>> signedPointsWhenOccupying = new HashMap<>();

        for (Occupant occupant : gameState.lastTilePotentialOccupants()) {
            Map<PlayerColor, Integer> pointsDistribution = getSignedPointsIfOccupying(gameState, occupant);
            signedPointsWhenOccupying.put(occupant, pointsDistribution);
        }

        return signedPointsWhenOccupying.entrySet().stream()
                .max(Comparator.comparingInt(e -> {
                    int points = e.getValue().getOrDefault(gameState.currentPlayer(), 0);
                    if (shouldPrioritize) points += 5; // Add a bias to prioritize this action
                    else points -= 5; // Temporize by reducing the score
                    return points;
                }))
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    /**
     * Used to get the signed points if occupying a certain occupant
     * @param gameState The game state
     * @param occupant The occupant to occupy
     * @return The signed points if occupying the occupant
     */
    private Map<PlayerColor, Integer> getSignedPointsIfOccupying(GameState gameState, Occupant occupant) {
        return gameState.players().stream().collect(Collectors.toMap(Function.identity(), player -> {
            GameState newGameState = gameState.withNewOccupant(occupant);
            int newPoints = newGameState.messageBoard().points().getOrDefault(player, 0) -
                    gameState.messageBoard().points().getOrDefault(player, 0);
            return player.equals(gameState.currentPlayer()) ? newPoints : -newPoints;
        }));
    }

    /**
     * Used to play the maximizing points turn for the bot
     * @param gameState The game state
     * @return The action to play
     */
    private ActionEncoder.StateAction playMaximizingPointsTurnForBot(GameState gameState) {
        PlacedTile bestPlacedTile = getBestPlacedTile(gameState);
        return ActionEncoder.withPlacedTile(gameState, bestPlacedTile);
    }

    private PlacedTile getBestPlacedTile(GameState gameState) {
        Tile tileToPlace = gameState.tileToPlace();
        List<PlacedTile> potentialPlaceTiles = getPotentialPlacedTiles(gameState, tileToPlace);
        List<PlacedTile> maxPointsPlacedTiles = maxPointsInOneTurnForBot(gameState, potentialPlaceTiles);

        Comparator<PlayerColor> placedTileComparator =
                getPlayerColorComparatorInOneTurn(gameState, maxPointsPlacedTiles);
        Map<PlacedTile, List<PlayerColor>> placedTileRanking =
                getRankingInOneBestTurnForBot(gameState, maxPointsPlacedTiles, placedTileComparator);

        return maxPointsPlacedTiles.stream()
                .max(Comparator.comparingInt(p -> placedTileRanking.get(p).indexOf(gameState.currentPlayer())))
                .orElse(null);
    }

    /**
     * Used to play the minimizing points game for the best opponent
     * @param gameState The game state
     * @return The action to play
     */
    private ActionEncoder.StateAction playMinimizingPointsGameForBestOpponent(GameState gameState) {
        Tile tileToPlace = gameState.tileToPlace();
        List<PlacedTile> potentialPlaceTiles = getPotentialPlacedTiles(gameState, tileToPlace);
        List<PlacedTile> minPointsPlacedTiles = minPointsInOneTurnForBestOpponent(gameState, potentialPlaceTiles);

        PlayerColor bestOpponent = getBestOpponent(gameState);

        Comparator<PlayerColor> placedTileComparator =
                getPlayerColorComparatorInOneTurn(gameState, minPointsPlacedTiles);
        Map<PlacedTile, List<PlayerColor>> placedTileRanking =
                getRankingInOneWorseTurnForBestOpponent(gameState, minPointsPlacedTiles, placedTileComparator);

        PlacedTile bestPlacedTile = minPointsPlacedTiles.stream()
                .min(Comparator.comparingInt(p -> placedTileRanking.get(p).indexOf(bestOpponent)))
                .orElse(null);

        return ActionEncoder.withPlacedTile(gameState, bestPlacedTile);
    }

    /**
     * Used to play the minimizing points game for all opponents and maximizing for the bot
     * @param gameState The game state
     * @return The action to play
     */
    private ActionEncoder.StateAction playMinimizingPointsGameForAllOpponentsAndMaximizingForBot(GameState gameState) {
        Tile tileToPlace = gameState.tileToPlace();
        Preconditions.checkArgument(Objects.nonNull(tileToPlace));
        List<PlacedTile> potentialPlaceTiles = getPotentialPlacedTiles(gameState, tileToPlace);
        List<PlacedTile> minPointsPlacedTiles = minPointsInOneTurnForBestOpponent(gameState, potentialPlaceTiles);

        PlacedTile bestPlacedTile = minPointsPlacedTiles.stream()
                .max(getPlacedTileComparatorForBot(gameState)).orElse(null);

        return ActionEncoder.withPlacedTile(gameState, bestPlacedTile);
    }

    /**
     * Used to get the potential place tiles for a certain tile
     * @param gameState The game state
     * @param tileToPlace The tile to place
     * @return The potential place tiles for the tile
     */
    private List<PlacedTile> getPotentialPlacedTiles(GameState gameState, Tile tileToPlace) {
        Preconditions.checkArgument(Objects.nonNull(tileToPlace));
        return gameState.board().insertionPositions().stream()
                .flatMap(pos -> Rotation.ALL.stream()
                        .map(r -> new PlacedTile(tileToPlace, gameState.currentPlayer(), r, pos)))
                .filter(p -> gameState.board().canAddTile(p))
                .toList();
    }

    /**
     * Used to get the placed tiles that minimize the points in one turn for the best opponent
     * @param gameState The game state
     * @param potentialPlaceTiles The potential place tiles
     * @return The placed tiles that minimize the points in one turn for the best opponent
     */
    private List<PlacedTile> minPointsInOneTurnForBestOpponent(GameState gameState, List<PlacedTile> potentialPlaceTiles) {
        PlayerColor bestOpponent = getBestOpponent(gameState);
        if (Objects.isNull(bestOpponent)) return List.of();

        int minPoints = getMaxPointsForTiles(gameState, potentialPlaceTiles);

        return potentialPlaceTiles.stream()
                .filter(p -> getMaxPointsForTile(gameState, p) == minPoints)
                .toList();
    }

    /**
     * Used to get the best opponent in the game
     * @param gameState The game state
     * @return The best opponent in the game
     */
    private PlayerColor getBestOpponent(GameState gameState) {
        return gameState.players().stream()
                .filter(p -> !p.equals(gameState.currentPlayer()))
                .max(getGameStateComparatorInWholeGame(gameState))
                .orElse(null);
    }

    /**
     * Used to get the comparator for the game state in the whole game
     * @param gameState The game state
     * @return The comparator for the game state in the whole game
     */
    private Comparator<PlayerColor> getGameStateComparatorInWholeGame(GameState gameState) {
        return Comparator.comparingInt(p -> gameState.messageBoard().points().getOrDefault(p, 0));
    }

    /**
     * Used to get the comparator for the player color in one turn
     * @param gameState The game state
     * @param maxPointsPlacedTiles The placed tiles that maximize the points in one turn
     * @return The comparator for the player color in one turn
     */
    private Comparator<PlayerColor> getPlayerColorComparatorInOneTurn(GameState gameState,
                                                                      List<PlacedTile> maxPointsPlacedTiles) {
        return (p1, p2) -> {
            int p1Points = maxPointsPlacedTiles.stream()
                    .map(p -> ActionEncoder.withPlacedTile(gameState, p).gameState())
                    .map(g -> g.messageBoard().points().getOrDefault(p1, 0))
                    .max(Integer::compareTo).orElse(0);
            int p2Points = maxPointsPlacedTiles.stream()
                    .map(p -> ActionEncoder.withPlacedTile(gameState, p).gameState())
                    .map(g -> g.messageBoard().points().getOrDefault(p2, 0))
                    .max(Integer::compareTo).orElse(0);
            return p1Points - p2Points;
        };
    }

    /**
     * Used to get the comparator for the placed tile in the whole game
     * @param gameState The game state
     * @return The comparator for the placed tile in the whole game
     */
    private Comparator<PlacedTile> getPlacedTileComparatorForBot(GameState gameState) {
        return (p1, p2) -> {
            int p1Points = getSignedPointsIfPlacedTile(gameState, p1).values().stream()
                    .mapToInt(Integer::intValue).sum();
            int p2Points = getSignedPointsIfPlacedTile(gameState, p2).values().stream()
                    .mapToInt(Integer::intValue).sum();
            return p1Points - p2Points;
        };
    }

    /**
     * Used to get the placed tiles that maximize the points in one turn for the bot
     * @param placedTiles The placed tiles
     * @param gameState The game state
     * @param placedTileComparator The comparator for the placed tile
     * @return The ranking for the placed tiles
     */
    private Map<PlacedTile, List<PlayerColor>> getPlacedTilesRanking
    (List<PlacedTile> placedTiles, GameState gameState, Comparator<PlayerColor> placedTileComparator) {
        return placedTiles.stream()
                .collect(Collectors.toMap(Function.identity(), _ -> gameState.players().stream()
                        .sorted(placedTileComparator).toList()));
    }

    /**
     * Used to get the ranking in one best turn for the bot
     * @param gameState The game state
     * @param maxPointsPlacedTiles The placed tiles that maximize the points in one turn
     * @param placedTileComparator The comparator for the placed tile
     * @return The ranking in one best turn for the bot
     */
    private Map<PlacedTile, List<PlayerColor>> getRankingInOneBestTurnForBot
    (GameState gameState, List<PlacedTile> maxPointsPlacedTiles, Comparator<PlayerColor> placedTileComparator) {
        List<PlacedTile> placedTiles = maxPointsInOneTurnForBot(gameState, maxPointsPlacedTiles);
        return getPlacedTilesRanking(placedTiles, gameState, placedTileComparator);
    }

    /**
     * Used to get the ranking in one worse turn for the best opponent of the bot
     * @param gameState The game state
     * @param minPointsPlacedTiles The placed tiles that minimize the points in one turn
     * @param placedTileComparator The comparator for the placed tile
     * @return The ranking in one worse turn for the best opponent of the bot
     */
    private Map<PlacedTile, List<PlayerColor>> getRankingInOneWorseTurnForBestOpponent
    (GameState gameState, List<PlacedTile> minPointsPlacedTiles, Comparator<PlayerColor> placedTileComparator) {
        List<PlacedTile> placedTiles = minPointsInOneTurnForBestOpponent(gameState, minPointsPlacedTiles);
        return getPlacedTilesRanking(placedTiles, gameState, placedTileComparator);
    }

    /**
     * Used to get the ranking in the whole game
     * @param gameState The game state
     * @param gameComparator The comparator for the game
     * @return The ranking in the whole game
     */
    private List<PlayerColor> getRankingInWholeGame(GameState gameState, Comparator<PlayerColor> gameComparator) {
        return gameState.players().stream().sorted(gameComparator).toList();
    }

    /**
     * Used to get the signed points if a placed tile is placed
     * @param gameState The game state
     * @param placedTile The placed tile
     * @return The signed points if the placed tile is placed
     */
    private Map<PlayerColor, Integer> getSignedPointsIfPlacedTile(GameState gameState, PlacedTile placedTile) {
        return gameState.players().stream().collect(Collectors.toMap(Function.identity(), player -> {
            GameState newGameState = ActionEncoder.withPlacedTile(gameState, placedTile).gameState();
            int newPoints = newGameState.messageBoard().points().getOrDefault(player, 0) -
                    gameState.messageBoard().points().getOrDefault(player, 0);
            return player.equals(gameState.currentPlayer()) ? newPoints : -newPoints;
        }));
    }
}