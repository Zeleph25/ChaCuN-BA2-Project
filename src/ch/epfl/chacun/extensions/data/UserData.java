package ch.epfl.chacun.extensions.data;

import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONValue;

/**
 * Represents the user data
 * @param uuid The user's uuid
 * @param username The user's username
 * @param gamesWon The number of games won
 * @param gamesLost The number of games lost
 * @param bestScore The best score of the user
 * @param worstScore The worst score of the user
 */
public record UserData(String uuid, String username, int gamesWon, int gamesLost, int bestScore, int worstScore) {
    public UserData() {
        this("", "", 0, 0, 0, -1);
    }

    public UserData(String uuid, String username) {
        this(uuid, username, 0, 0, 0, -1);
    }

    /**
     * Used to create user data from a JSON object
     * @param json The JSON object
     * @return The user data
     */
    public static UserData fromJSON(JSONObject json, String uuid) {
        return new UserData(
            uuid,
            json.get("username").asString(),
            json.get("gamesWon").asInteger(),
            json.get("gamesLost").asInteger(),
            json.get("bestScore").asInteger(),
            json.get("worstScore").asInteger()
        );
    }

    /**
     * Used to convert the user data to a JSON object
     * @return The JSON object
     */
    public JSONObject toJSON() {
        return new JSONObject()
                .add("username", JSONValue.parse(username))
                .add("gamesWon", JSONValue.parse(String.valueOf(gamesWon)))
                .add("gamesLost", JSONValue.parse(String.valueOf(gamesLost)))
                .add("bestScore", JSONValue.parse(String.valueOf(bestScore)))
                .add("worstScore", JSONValue.parse(String.valueOf(worstScore)));
    }

    public int gamesPlayed() {
        return gamesWon + gamesLost;
    }

    /**
     * Used to increment the number of games won
     * @return The user data with the incremented number of games won
     */
    public UserData withGame(boolean won, int score) {
        return new UserData(uuid, username,
                gamesWon + (won ? 1 : 0),
                gamesLost + (won ? 0 : 1),
                Math.max(bestScore, score),
                worstScore < 0 ? score : Math.min(worstScore, score)
        );
    }
}
