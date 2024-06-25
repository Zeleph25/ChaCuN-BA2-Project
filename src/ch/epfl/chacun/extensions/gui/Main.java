package ch.epfl.chacun.extensions.gui;

import ch.epfl.chacun.extensions.backend.Authentication;
import ch.epfl.chacun.extensions.backend.Database;
import ch.epfl.chacun.extensions.backend.Response;
import ch.epfl.chacun.extensions.data.GameData;
import ch.epfl.chacun.extensions.data.PlayerData;
import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONParser;
import ch.epfl.chacun.extensions.json.JSONValue;
import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Represents the main class of the application
 * @author Antoine Bastide (375407)
 */
public class Main extends Application {
    /** The width of the window */
    public static final int WINDOW_WIDTH = 1440;
    /** The height of the window */
    public static final int WINDOW_HEIGHT = 1080;
    /** The title of the window */
    public static final String WINDOW_TITLE = "ChaCuN";

    /** The property of the game json */
    public static final SimpleObjectProperty<GameData> GAME_DATA = new SimpleObjectProperty<>();
    /** The property of the username */
    public static final SimpleObjectProperty<PlayerData> PLAYER_DATA = new SimpleObjectProperty<>();
    /** The property of the error message */
    public static final SimpleStringProperty ERROR_MESSAGE = new SimpleStringProperty("");
    /** If we are currently in the game ui */
    private static final SimpleBooleanProperty GAME_UI = new SimpleBooleanProperty(false);

    /** The stage of the application */
    public static Stage primaryStage;


    /** The different types of scenes in the game */
    public enum SceneType { AUTH, LOGIN, SIGNUP, MAIN, CREATE, JOIN, RULES, ACCOUNT, WAITING, GAME }

    /**
     * The main method of the application
     * @param args The arguments of the application
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Main.primaryStage = primaryStage;

        // Try to auto login the user
        try {
            JSONObject userJson = JSONParser.readJSONFromFile("resources/user.json").toJSONObject();
            Response response = Authentication.autoLogin(
                    userJson.get("uuid").asString(), userJson.get("username").asString()
            );
            if (!response.isSuccess()) updateScene(SceneType.AUTH);
            else updateScene(SceneType.MAIN);
        } catch (Exception _) {
            updateScene(SceneType.AUTH);
        }
        primaryStage.show();
    }

    /**
     * Used to update the scene
     * @param sceneType The type of the scene
     */
    public static void updateScene(SceneType sceneType) {
        // Get the correct scene
        Scene scene = switch (sceneType) {
            case AUTH -> AuthenticationUI.showAuthScreen();
            case LOGIN, SIGNUP -> AuthenticationUI.showAccountScreen(sceneType == SceneType.LOGIN);
            case MAIN -> {
                stopSchedulers();
                yield GamePlayUI.showMainMenu();
            }
            case CREATE -> GamePlayUI.showCreateGameScreen();
            case JOIN -> GamePlayUI.showJoinGameScreen();
            case RULES -> GamePlayUI.showRulesScreen();
            case ACCOUNT -> AuthenticationUI.showAccountScreen();
            case WAITING -> {
                GamePlayUI.scheduler = Executors.newSingleThreadScheduledExecutor();
                yield GamePlayUI.showWaitingScreen();
            }
            case GAME -> {
                GAME_UI.set(true);
                yield new GameUI().showGameScreen();
            }
        };
        ERROR_MESSAGE.set("");

        // Add styles to the scene
        scene.getStylesheets().add("main.css");
        for (Node node : scene.getRoot().getChildrenUnmodifiable()) {
            // Add the button style to all buttons
            if (node instanceof Button button)
                button.getStyleClass().add("button");
            // Focus on the title of the game
            if (node.getId() != null && node.getId().equals("label-title"))
                node.requestFocus();
        }

        // Update the primary stage
        primaryStage.setTitle(STR."\{WINDOW_TITLE}\{Objects.nonNull(GAME_DATA.get()) ?
                STR." - \{GAME_DATA.get().name()}" : ""}");
        primaryStage.setScene(scene);
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);
    }

    /**
     * Used to validate the input string
     * @param inputText The input text to validate
     * @param textType The type of text to validate
     * @return An error message if the input text is invalid, otherwise an empty string
     */
    public static String validateInputString(String inputText, String textType, boolean noSpaces) {
        if (inputText == null || inputText.isEmpty()) return STR."\{textType} ne peut pas être vide";
        else if (noSpaces && inputText.contains(" ")) return STR."\{textType} ne peut pas contenir d'espaces";
        else  {
            // Define the invisible characters to check for
            char[] invisibleCharacters = {
                    ' ', '\u00A0', '\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF'
            };

            for (char c : invisibleCharacters)
                if (inputText.chars().allMatch(ch -> ch == c))
                    return STR."\{textType} ne peut pas être composé uniquement de caractères invisibles";
            return "";
        }
    }

    /** Used to stop the schedulers */
    public static void stopSchedulers() {
        // Close the schedulers
        if (GamePlayUI.scheduler != null && !(GamePlayUI.scheduler.isShutdown() || GamePlayUI.scheduler.isTerminated()))
            GamePlayUI.scheduler.shutdown();
        if (GameUI.scheduler != null && !(GameUI.scheduler.isShutdown() || GameUI.scheduler.isTerminated()))
            GameUI.scheduler.shutdown();
    }

    @Override
    public void stop() throws Exception {
        stopSchedulers();
        Authentication.updateAccount(Authentication.user.uuid());

        if (!GAME_UI.get()) return;

        if (Objects.nonNull(GAME_DATA.get()) && GAME_DATA.get().requiresServer()) {
            // Do not do anything if the game does not exist any more
            Response response = Database.get(STR."games/\{GAME_DATA.get().name()}");
            if (!response.isSuccess()) return;

            // Update the game json
            GAME_DATA.set(GameData.fromJSON(GAME_DATA.get().name(), response.jsonObject()));

            // Make the player leave the game
            GAME_DATA.get().withPlayerInGame(PLAYER_DATA.get().uuid(), false);
            Database.put(STR."games/\{GAME_DATA.get().name()}/players", GAME_DATA.get().toJSON().get("players"));

            if (GAME_DATA.get().isHost(PLAYER_DATA.get().uuid())) {
                // Delete the game if the player is the host and no one is in the game
                if (!GAME_DATA.get().playersInGame()) Database.delete(STR."games/\{GAME_DATA.get().name()}");
                // Update the host of the game
                else {
                    PlayerData hostData = GAME_DATA.get().getPlayer(GAME_DATA.get().host());
                    List<PlayerData> temp = new ArrayList<>(GAME_DATA.get().players());
                    temp.remove(hostData);
                    String newHost = temp.getFirst().uuid();
                    Database.put(STR."games/\{GAME_DATA.get().name()}/host", JSONValue.parse(newHost));
                }
            }
        }
        super.stop();
    }
}
