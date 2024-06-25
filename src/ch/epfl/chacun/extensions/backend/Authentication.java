package ch.epfl.chacun.extensions.backend;

import ch.epfl.chacun.extensions.data.PlayerData;
import ch.epfl.chacun.extensions.data.UserData;
import ch.epfl.chacun.extensions.gui.Main;
import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONParser;
import ch.epfl.chacun.extensions.json.JSONValue;

/**
 * Represents the authentication requests to the database
 * @author Antoine Bastide (375407)
 */
public class Authentication extends Database {
    /** The user's uuid */
    private static String uuid = "";
    /** The user's data */
    public static UserData user = new UserData();

    /**
     * Used to sign up a new user with email, password, and display name
     * @param username The user's username
     * @param password The user's password
     * @return The response body and status code of the sign-up request
     */
    public static Response signup(String username, String password) {
        // Make sure the user does not already exist
        Response getResponse = get(STR."users/\{PlayerData.createUUID(username, password)}");
        if (getResponse.isSuccess()) return new Response("Username already exists", 409);
        // Create the user
        Main.PLAYER_DATA.set(PlayerData.createFromUser(username, password));
        updateUUID(username, password);
        user = new UserData(uuid, username);
        return put(STR."users/\{user.uuid()}", user.toJSON());
    }

    /**
     * Used to log in an existing user with email and password
     * @param username The user's username
     * @param password The user's password
     * @return The response body and status code of the login request
     */
    public static Response login(String username, String password) {
        Response response = get(STR."users/\{PlayerData.createUUID(username, password)}");
        // If the user exists, update the user's uuid
        if (response.isSuccess()) {
            Main.PLAYER_DATA.set(PlayerData.createFromUser(username, password));
            updateUUID(username, password);
            user = UserData.fromJSON(response.jsonObject(), uuid);
            return response;
        } else return new Response(new JSONObject().add("error", JSONValue.parse("Invalid username or password")), 401);
    }

    /**
     * Used to automatically log in an existing user with uuid
     * @param uuid The user's uuid
     * @param username The user's username
     * @return The response body and status code of the auto-login request
     */
    public static Response autoLogin(String uuid, String username) {
        Response response = get(STR."users/\{uuid}");
        if (response.isSuccess()) {
            Main.PLAYER_DATA.set(new PlayerData(uuid, username, false));
            user = UserData.fromJSON(response.jsonObject(), uuid);
            Authentication.uuid = uuid;
            return response;
        } else return new Response(new JSONObject().add("error", JSONValue.parse("Invalid username or password")), 401);
    }

    /**
     * Used to automatically log in an existing user with uuid
     *
     * @param uuid The user's uuid
     */
    public static void updateAccount(String uuid) {
        Response response = get(STR."users/\{uuid}");
        if (response.isSuccess()) put(STR."users/\{uuid}", user.toJSON());
    }

    /**
     * Used to delete the user's account
     */
    public static void logout() {
        user = new UserData();
        updateUUID("", "");
        Main.PLAYER_DATA.set(null);
    }

    /**
     * Used to delete the user's account
     * @return The response body and status code of the delete request
     */
    public static Response delete() {
        Response response = delete(STR."users/\{uuid}");
        logout();
        return response;
    }

    /**
     * Used to update the user's uuid
     * @param username The user's username
     * @param password The user's password
     */
    private static void updateUUID(String username, String password) {
        uuid = username.isEmpty() && password.isEmpty() ? "" : PlayerData.createUUID(username, password);
        JSONObject userJson = new JSONObject()
            .add("uuid", JSONValue.parse(uuid))
            .add("username", JSONValue.parse(username));
        JSONParser.writeJSONToFile("resources/user.json", userJson);
    }
}