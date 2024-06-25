package ch.epfl.chacun;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Used to encode, decode actions' parameters and apply these actions to a game state
 * @author Adam BEKKAR (379476)
 */
public class ActionEncoder {
    /** Private constructor to prevent instantiation */
    private ActionEncoder() {}

    /**
     * Used to get a list of sorted insertion positions of a game state by x and then by y
     * @param gameState The game state to get the sorted insertion positions from
     * @return The list of sorted insertion positions
     */
    private static List<Pos> getSortedPos(GameState gameState) {
        return gameState.board().insertionPositions().stream()
                .sorted(Comparator.comparingInt(Pos::x).thenComparingInt(Pos::y))
                .toList();
    }

    /**
     * Used to get a list of sorted occupants on the board by zone id
     * @param gameState The game state to get the sorted occupants from
     * @return The list of sorted occupants on the board
     */
    private static List<Occupant> getSortedOccupant(GameState gameState) {
        return gameState.board().occupants().stream()
                .sorted(Comparator.comparingInt(Occupant::zoneId))
                .toList();
    }

    /**
     * Used to get a StateAction of a game state that is placing a tile
     * @param gameState The game state to get the encoded action from
     * @param placedTile The placed tile to encode
     * @return The StateAction of the game state that is placing a tile
     * @throws IllegalArgumentException If the placed tile is not on an insertion position
     */
    public static StateAction withPlacedTile(GameState gameState, PlacedTile placedTile) {
        // Check if the placed tile is not null and is on an insertion position
        Objects.requireNonNull(placedTile);
        Preconditions.checkArgument(gameState.board().insertionPositions().contains(placedTile.pos()));

        // Sort the insertion positions by x and then by y
        List<Pos> sortedPos = getSortedPos(gameState);

        // Get the index of the placed tile in the sorted list of insertion positions
        char index = (char) sortedPos.indexOf(placedTile.pos());
        // Get the rotation of the placed tile
        char rotation = (char) placedTile.rotation().ordinal();

        // Return the new game state with the placed tile and the encoded action
        String action = Base32.encodeBits10((index & 0xff) << 2 | rotation & 0x3);
        return new StateAction(gameState.withPlacedTile(placedTile), action);
    }

    /**
     * Used to get a StateAction of a game state that is removing a tile
     * @param gameState The game state to get the encoded action from
     * @param occupant The occupant to remove
     * @return The StateAction of the game state that is removing a tile
     */
    public static StateAction withNewOccupant(GameState gameState, Occupant occupant) {
        // Check if the occupant can be placed on the last tile and get the encoded action
        // and return the new game state with the new occupant and the encoded action
        if (Objects.isNull(occupant))
            return new StateAction(gameState.withNewOccupant(null), Base32.encodeBits5(0x1f));

        int actionCode = (occupant.kind().ordinal() & 0x1) << 4 | Zone.localId(occupant.zoneId()) & 0xf;
        String action = Base32.encodeBits5(actionCode);
        return new StateAction(gameState.withNewOccupant(occupant), action);
    }

    /**
     * Used to get a StateAction of a game state that is removing an occupant
     * @param gameState The game state to get the encoded action from
     * @param occupant The occupant to remove
     * @return The StateAction of the game state that is removing an occupant
     * @throws IllegalArgumentException If the occupant is not a pawn
     */
    public static StateAction withOccupantRemoved(GameState gameState, Occupant occupant) {
        // Sort the occupants on the board by zone id
        List<Occupant> onBoardOccupants = getSortedOccupant(gameState);

        // Check if the occupant is on the board and get the encoded action
        // and return the new game state with the occupant removed and the encoded action
        int actionCode = onBoardOccupants.contains(occupant) && occupant.kind() == Occupant.Kind.PAWN
                ? onBoardOccupants.indexOf(occupant) : 0x1f;
        String action = Base32.encodeBits5(actionCode);
        return new StateAction(gameState.withOccupantRemoved(occupant), action);
    }

    /**
     * Used to decode and apply an action to a game state
     * @param gameState The game state to apply the action to
     * @param action The action to decode and apply
     * @return The new game state after decoding and applying the action
     * or null if the action is not valid
     */
    public static StateAction decodeAndApply(GameState gameState, String action) {
        try {
            return decoderAndApplier(gameState, action);
        } catch (StateActionException e) {
            // If the action is not valid, return null
            return null;
        }
    }

    /**
     * Used to decode and apply an action to a game state
     * This method needs to be used in the {@link #decodeAndApply(GameState, String)}} method only
     * @param gameState The game state to apply the action to
     * @param action The action to decode and apply
     * @return The new game state after decoding and applying the action
     * @throws StateActionException If the action is not valid
     */
    private static StateAction decoderAndApplier(GameState gameState, String action) throws StateActionException {
        if (!Base32.isValid(action)) throw new StateActionException();

        int actionCode = Base32.decode(action);
        return switch (gameState.nextAction()) {
            case PLACE_TILE -> {
                // Check if the action is valid and get the rotation and the position of the placed tile
                int rCode = actionCode & 0x3;
                int pCode = actionCode >> 2;

                List<Pos> sortedPos = getSortedPos(gameState);
                if (!(sortedPos.size() > pCode && action.length() == 2 && 0 <= pCode && pCode < 190))
                    throw new StateActionException();

                Rotation rotation = Rotation.ALL.get(rCode);
                Pos pos = sortedPos.get(pCode);
                Tile tileToPlace = gameState.tileToPlace();
                PlacedTile placedTile = new PlacedTile(tileToPlace, gameState.currentPlayer(), rotation, pos);

                // Return the state action with the new game state and the action
                if (gameState.board().canAddTile(placedTile))
                    yield new StateAction(gameState.withPlacedTile(placedTile), action);
                // If the action is not valid, throw an exception
                throw new StateActionException();
            }
            case OCCUPY_TILE -> {
                if (actionCode == 0x1f) yield new StateAction(gameState.withNewOccupant(null), action);
                int kCode = (actionCode >> 4) & 1;
                int zCode = actionCode & 0xf;

                // Check if the action is valid and get the kind and the zone id of the new occupant
                if (!(action.length() == 1 && zCode < 10)) throw new StateActionException();
                Occupant.Kind kind = (kCode == 0 ? Occupant.Kind.PAWN : Occupant.Kind.HUT);

                // For each occupant, check if the occupant is compatible with the zone id,
                // and if so return the state action with the new game state and the action
                for (Occupant o : gameState.lastTilePotentialOccupants())
                    if (Zone.localId(o.zoneId()) == Zone.localId(zCode) && o.kind() == kind)
                        yield new StateAction(gameState.withNewOccupant(o), action);
                // If the action is not valid, throw an exception
                throw new StateActionException();
            }
            case RETAKE_PAWN -> {
                if (actionCode == 0x1f) yield new StateAction(gameState.withOccupantRemoved(null), action);
                // Check if the action is valid and get the zone id of the pawn to remove
                if (!(action.length() == 1 && 0 <= actionCode && actionCode < 25)) throw new StateActionException();
                List<Occupant> occupants = getSortedOccupant(gameState);

                // If the action is valid, get the occupant to remove and
                // return the state action with the new game state and the action
                if (occupants.size() > actionCode) {
                    Occupant occupant = getSortedOccupant(gameState).get(actionCode);
                    yield new StateAction(gameState.withOccupantRemoved(occupant), action);
                }
                // If the action is not valid, throw an exception
                throw new StateActionException();
            }
            // If the action is START_GAME or END_GAME, throw an exception
            case START_GAME, END_GAME -> throw new StateActionException();
        };
    }

    /**
     * Represents a pair of a game state and an encoded action in Base32
     * @param gameState The game state after applying the action
     * @param action Code of the action applied to the game state in Base32
     */
    public record StateAction(GameState gameState, String action) {}

    /** The exception to throw when an action is not valid */
    private static class StateActionException extends Exception {}
}