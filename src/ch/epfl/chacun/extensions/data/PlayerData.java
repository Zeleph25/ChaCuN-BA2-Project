package ch.epfl.chacun.extensions.data;

import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONValue;

import java.util.UUID;

/**
 * Used to represent the player json
 * @param uuid The uuid of the player
 * @param username The username of the player
 * @param inGame Whether the player is in a game or not
 * @author Adam Bekkar (379476)
 */
public record PlayerData(String uuid, String username, boolean inGame) {
    /**
     * Used to create the uuid of the player
     * @param username The username of the player
     * @param password The password of the player
     * @return The uuid of the player
     */
    public static String createUUID(String username, String password) {
        return UUID.nameUUIDFromBytes((username + password).getBytes()).toString();
    }

    /**
     * Used to create the player json from a username and password
     * @param username The username of the player
     * @param password The password of the player
     * @return The player json
     */
    public static PlayerData createFromUser(String username, String password) {
        return new PlayerData(createUUID(username, password), username, false);
    }

    /**
     * Used to create the player json from a JSON object
     * @param playerData The JSON object representing the player json
     * @return The player json
     */
    public static PlayerData fromJSON(JSONObject playerData) {
        String hash = playerData.get("uuid").asString();
        String username = playerData.get("username").asString();
        boolean inGame = playerData.get("inGame").asBoolean();
        return new PlayerData(hash, username, inGame);
    }

    /**
     * Used to convert the player json to a JSON object
     * @return The JSON object representing the player json
     */
    public JSONObject toJSON() {
        return new JSONObject()
                .add("uuid", JSONValue.parse(uuid))
                .add("username", JSONValue.parse(username))
                .add("inGame", JSONValue.parse(String.valueOf(inGame)));
    }

    /**
     * Used to update the in game state of the player
     * @param inGame Whether the player is in a game or not
     * @return The updated player json
     */
    public PlayerData withInGame(boolean inGame) {
        return new PlayerData(uuid, username, inGame);
    }
}