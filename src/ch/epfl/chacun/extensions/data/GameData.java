package ch.epfl.chacun.extensions.data;

import ch.epfl.chacun.extensions.bot.Bot;
import ch.epfl.chacun.extensions.gui.Main;
import ch.epfl.chacun.extensions.json.JSONArray;
import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONValue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Used to represent the game json
 * @param name The name of the game
 * @param players The players
 * @param seed The seed
 * @param started Whether the game has started
 * @param cardCount The number of cards in the game
 * @param host The host of the game
 * @param requiresServer Whether the game requires access to the server
 * @author Antoine Bastide
 */
public record GameData(String name, LinkedList<PlayerData> players, int seed, boolean started, int cardCount,
                       String host, boolean requiresServer, Bot.Level botDifficulty) {
    /**
     * The constructor of the game json
     * @param name The name of the game
     * @param seed The seed
     * @param started Whether the game has started
     * @param cardCount The number of cards in the game
     */
    public GameData(String name, int seed, boolean started, int cardCount, boolean requiresServer) {
        this(name, new LinkedList<>(Collections.singleton(Main.PLAYER_DATA.get())), seed, started, cardCount,
                Main.PLAYER_DATA.get().uuid(), requiresServer, Bot.Level.EASY);
    }

    /**
     * Used to create the game json from a JSON object
     * @param name The name of the game
     * @param gameData The JSON object representing the game json
     * @return The game json
     */
    public static GameData fromJSON(String name, JSONObject gameData) {
        JSONArray jsonPlayers = gameData.getArray("players");
        LinkedList<PlayerData> players = new LinkedList<>();
        for (JSONValue entry : jsonPlayers.values())
            players.add(PlayerData.fromJSON(entry.toJSONObject()));
        int seed = gameData.get("seed").asInteger();
        boolean started = gameData.get("started").asBoolean();
        int cardCount = gameData.get("cardCount").asInteger();
        String host = gameData.get("host").asString();
        Bot.Level difficulty = Bot.Level.valueOf(gameData.get("botDifficulty").asString());
        return new GameData(name, players, seed, started, cardCount, host, true, difficulty);
    }

    /**
     * Used to convert the game json to a JSON object
     * @return The JSON object representing the game json
     */
    public JSONObject toJSON() {
        StringBuilder playersString = new StringBuilder("[");
        for (int i = 0; i < players.size(); i++) {
            playersString.append(players.get(i).toJSON().toString());
            if (i < players.size() - 1) playersString.append(", ");
            else playersString.append("]");
        }
        return new JSONObject()
                .add("actions", new JSONArray())
                .add("players", JSONArray.parse(playersString.toString()))
                .add("seed", JSONValue.parse(STR."\{seed}"))
                .add("started", JSONValue.parse(STR."\{started}"))
                .add("cardCount", JSONValue.parse(STR."\{cardCount}"))
                .add("host", JSONValue.parse(host))
                .add("botDifficulty", JSONValue.parse(botDifficulty.toString()));
    }

    /**
     * Used to check if the given username is the host
     * @param hash The uuid to check
     * @return Whether the given username is the host or not
     */
    public boolean isHost(String hash) {
        return hash.equals(host);
    }

    /**
     * Used to get the player with the given username
     * @param hash The uuid of the player
     * @return The player with the given username
     */
    public PlayerData getPlayer(String hash) {
        return players.stream().filter(p -> p.uuid().equals(hash)).findFirst().orElse(null);
    }

    /**
     * Used to check if there are players left in the game
     * @return Whether there are players left in the game or not
     */
    public boolean playersInGame() {
        return players.stream().anyMatch(PlayerData::inGame);
    }

    /**
     * Used to update the in game state of the player
     * @param hash The uuid of the player to update
     * @param inGame Whether the player is in a game or not
     */
    public void withPlayerInGame(String hash, boolean inGame) {
        boolean isCurrentPlayer = Objects.equals(hash, Main.PLAYER_DATA.get().uuid());
        PlayerData playerData = isCurrentPlayer ? Main.PLAYER_DATA.get() : getPlayer(hash);
        int index = players.stream().map(PlayerData::uuid).toList().indexOf(hash);
        players.set(index, playerData.withInGame(inGame));
        if (isCurrentPlayer) Main.PLAYER_DATA.set(playerData.withInGame(inGame));
    }

    /**
     * Used to update the card count of the game
     * @param cardCount The new card count
     * @return The updated game json
     */
    public GameData withCardCount(int cardCount) {
        return new GameData(name, players, seed, started, cardCount, host, requiresServer, botDifficulty);
    }

    /**
     * Used to update the botDifficulty of the bot
     * @param difficulty The botDifficulty of the bot
     * @return The updated game json
     */
    public GameData withBotDifficulty(Bot.Level difficulty) {
        return new GameData(name, players, seed, started, cardCount, host, requiresServer, difficulty);
    }

    /**
     * Used to update the players of the game
     * @param players The new players
     * @return The updated game json
     */
    public GameData withPlayers(JSONArray players) {
        LinkedList<PlayerData> newPlayers = new LinkedList<>();
        for (JSONValue entry : players.values()) {
            newPlayers.add(PlayerData.fromJSON(entry.toJSONObject()));
            if (entry.toJSONObject().get("uuid").asString().equals(Main.PLAYER_DATA.get().uuid()))
                Main.PLAYER_DATA.set(PlayerData.fromJSON(entry.toJSONObject()));
        }
        return new GameData(name, newPlayers, seed, started, cardCount, host, requiresServer, botDifficulty);
    }

    public GameData withHost(String host) {
        return new GameData(name, players, seed, started, cardCount, host, requiresServer, botDifficulty);
    }
}
