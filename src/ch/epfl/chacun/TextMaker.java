package ch.epfl.chacun;

import java.util.Map;
import java.util.Set;

/** Represents the text of the messages displayed on the message board.
 * @author Adam BEKKAR (379476)
 * @author Antoine Bastide (375407)
 */
public interface TextMaker {
    /**
     * Return the name of the player of the given color.
     * @param playerColor The color of the player
     * @return The name of the player
     */
    String playerName(PlayerColor playerColor);

    /**
     * Return the textual representation of the number of points given (e.g. "3 points").
     * @param points The number of points
     * @return The textual representation of the number of points
     */
    String points(int points);

    /**
     * Return the text of a message declaring that a player has closed a forest with a menhir.
     * @param player The player who closed the forest
     * @return The text of the message
     */
    String playerClosedForestWithMenhir(PlayerColor player);

    /**
     * Return the text of a message declaring that the majority occupants of a forest newly closed,
     * constituted of a certain number of tiles and containing a certain number of groups of mushrooms,
     * have won the corresponding points.
     * @param scorers The majority occupants of the forest
     * @param points The points won
     * @param mushroomGroupCount The number of mushroom groups that the forest contains
     * @param tileCount The number of tiles that constitute the forest
     * @return The text of the message
     */
    String playersScoredForest(Set<PlayerColor> scorers, int points, int mushroomGroupCount, int tileCount);

    /**
     * Return the text of a message declaring that the majority occupants of a river newly closed,
     * consisting of a certain number of tiles and containing a certain number of fish, have won
     * the corresponding points.
     * @param scorers The majority occupants of the river
     * @param points The points won
     * @param fishCount The number of fish swimming in the river or the lakes adjacent to it
     * @param tileCount The number of tiles that constitute the river
     * @return The text of the message
     */
    String playersScoredRiver(Set<PlayerColor> scorers, int points, int fishCount, int tileCount);

    /**
     * Return the text of a message declaring that a player has placed the hunting trap in a meadow containing it
     * and on the eight neighboring tiles, certain animals, and won the corresponding points.
     * @param scorer The player who placed the hunting trap
     * @param points The points won
     * @param animals The animals present in the same meadow as the hunting trap and on the eight neighboring tiles
     */
    String playerScoredHuntingTrap(PlayerColor scorer, int points, Map<Animal.Kind, Integer> animals);

    /**
     * Return the text of a message declaring that a player has placed the log boat in a river system
     * containing a certain number of lakes, and won the corresponding points.
     * @param scorer The player who placed the log boat
     * @param points The points won
     * @param lakeCount The number of lakes accessible to the log boat
     * @return The text of the message
     */
    String playerScoredLogboat(PlayerColor scorer, int points, int lakeCount);

    /**
     * Return the text of a message declaring that the majority occupants of a meadow containing certain
     * animals have won the corresponding points.
     * @param scorers The majority occupants of the meadow
     * @param points The points won
     * @param animals The animals present in the meadow (without those that have been previously cancelled)
     * @return The text of the message
     */
    String playersScoredMeadow(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals);

    /**
     * Return the text of a message declaring that the majority occupants of a river system
     * containing a certain number of fish have won the corresponding points.
     * @param scorers The majority occupants of the river system
     * @param points The points won
     * @param fishCount The number of fish swimming in the river system
     * @return The text of the message
     */
    String playersScoredRiverSystem(Set<PlayerColor> scorers, int points, int fishCount);

    /**
     * Return the text of a message declaring that the majority occupants of a forest containing the
     * menhir have won the corresponding points.
     * @param scorers The majority occupants of the forest containing the menhir
     * @param points The points won
     * @param animals The animals present in the neighboring tiles of the hunting trap
     * (without those that have been previously cancelled)
     * @return The text of the message
     */
    String playersScoredPitTrap(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals);

    /**
     * Return the text of a message declaring that the majority occupants of a river system containing the
     * raft have won the corresponding points.
     * @param scorers The majority occupants of the river system containing the raft
     * @param points The points won
     * @param lakeCount The number of lakes contained in the river system
     * @return The text of the message
     */
    String playersScoredRaft(Set<PlayerColor> scorers, int points, int lakeCount);

    /**
     * Return the text of a message declaring that one or more players have won the game, with a certain
     * number of points.
     * @param winners The players who have won the game
     * @param points The points of the winners
     * @return The text of the message
     */
    String playersWon(Set<PlayerColor> winners, int points);

    /**
     * Return the text asking the current player to click on the pawn he wants to place, or on the text
     * of the message if he does not want to place any pawn.
     * @return The text in question
     */

    String clickToOccupy();
    /**
     * Return the text asking the current player to click on the pawn he wants to take back, or on the text
     * of the message if he does not want to take back any pawn.
     * @return The text in question
     */
    String clickToUnoccupy();

    /**
     * Return the text asking the current player to wait for the other players to finish their turn.
     * @param playerName The name of the player
     * @return The text in question
     */
    String waitForPlayer(String playerName);

    /**
     * Return the text declaring that a player has placed a tile
     * @param playerName The name of the player
     * @param placedTile The placed tile
     * @return The text in question
     */
    String withPlacedTile(String playerName, PlacedTile placedTile);

    /**
     * Return the text declaring that a player has occupied a tile with an occupant
     * @param playerName The name of the player
     * @param occupant The occupant
     * @return The text in question
     */
    String withOccupant(String playerName, Occupant occupant);

    /**
     * Return the text declaring that a player has retaken a pawn
     * @param playerName The name of the player
     * @return The text in question
     */
    String withRetakePawn(String playerName, Occupant occupant);
}