package ch.epfl.chacun.extensions.gui;

import ch.epfl.chacun.*;
import ch.epfl.chacun.extensions.backend.Authentication;
import ch.epfl.chacun.extensions.backend.Database;
import ch.epfl.chacun.extensions.backend.Response;
import ch.epfl.chacun.extensions.bot.Bot;
import ch.epfl.chacun.extensions.data.GameData;
import ch.epfl.chacun.extensions.data.PlayerData;
import ch.epfl.chacun.extensions.json.JSONArray;
import ch.epfl.chacun.extensions.json.JSONValue;
import ch.epfl.chacun.gui.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

/**
 * Represents the full game UI of the ChaCuN game
 * @author Adam BEKKAR (379476)
 * @author Antoine Bastide (375407)
 */
public class GameUI {
    /** Shortcut for the game json of the main class */
    private static final SimpleObjectProperty<GameData> GAME_DATA = Main.GAME_DATA;
    /** The bot name */
    private static final String BOT_NAME = STR."\{GAME_DATA.get() == null ? "" : GAME_DATA.get().botDifficulty()} Bot";

    /** The scheduler to get the last action from the database */
    public static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    /** The last occupant */
    public static ObjectProperty<Occupant> lastOccupant = new SimpleObjectProperty<>(null);

    /** The list of bots */
    private final List<Bot> bots = new LinkedList<>();
    /** The list of bot json */
    private final List<PlayerData> botData = new LinkedList<>();

    /**
     * Used to show the game screen
     * @return The game scene
     */
    public Scene showGameScreen() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Configure the game and the bots depending on if the game requires a server
        if (!GAME_DATA.get().requiresServer()) {
            // Update the bot name
            bots.add(new Bot(GAME_DATA.get().botDifficulty()));
            botData.add(PlayerData.createFromUser(BOT_NAME, "(Bot)"));
            GAME_DATA.get().players().add(botData.getFirst());
        } else {
            // Set the players in game
            for (int i = 0; i < GAME_DATA.get().players().size(); i++) {
                GAME_DATA.get().withPlayerInGame(GAME_DATA.get().players().get(i).uuid(), true);
                bots.add(new Bot(GAME_DATA.get().botDifficulty()));
                botData.add(PlayerData.createFromUser(BOT_NAME, STR."(Bot \{i})"));
            }
            Database.put(STR."games/\{GAME_DATA.get().name()}/players", GAME_DATA.get().toJSON().get("players"));
        }

        // Create the player maps
        List<PlayerData> players = GAME_DATA.get().players();
        Map<PlayerColor, String> playerNames = new HashMap<>();
        Map<PlayerColor, String> playerDatas = new HashMap<>();
        Map<String, PlayerColor> reversePlayerDatas = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            playerNames.put(PlayerColor.ALL.get(i), players.get(i).username());
            playerDatas.put(PlayerColor.ALL.get(i), players.get(i).uuid());
            reversePlayerDatas.put(players.get(i).uuid(), PlayerColor.ALL.get(i));
        }
        List<PlayerColor> playerColors = playerNames.keySet().stream().sorted().toList();
        SimpleObjectProperty<PlayerColor> clientColor = new SimpleObjectProperty<>(
                reversePlayerDatas.get(GAME_DATA.get().getPlayer(Main.PLAYER_DATA.get().uuid()).uuid()));

        // Check the seed parameter
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(GAME_DATA.get().seed());

        // Create the tile deck and shuffle the tiles
        List<Tile> tiles = new ArrayList<>(Tiles.createSubTiles(GAME_DATA.get().cardCount()));
        Collections.shuffle(tiles, random);
        Map<Tile.Kind, List<Tile>> tilesByKind = tiles.stream().collect(Collectors.groupingBy(Tile::kind));
        TileDecks tileDecks = new TileDecks(
                tilesByKind.get(Tile.Kind.START),
                tilesByKind.get(Tile.Kind.NORMAL),
                Tiles.createSubTiles(GAME_DATA.get().cardCount()).stream().filter(t -> t.kind() == Tile.Kind.MENHIR).toList()
        );

        // Create the text maker and the game state
        TextMaker textMaker = new TextMakerFr(playerNames);
        GameState initialGamestate = GameState.initial(playerColors, tileDecks, textMaker);

        // Create the game state property
        ObjectProperty<GameState> gameStateP = new SimpleObjectProperty<>(initialGamestate);
        ObservableValue<Boolean> correctPlayer = gameStateP.map(g -> g.currentPlayer() == clientColor.get());
        // Create the tile to place rotation property
        ObjectProperty<Rotation> tileToPlaceRotationP = new SimpleObjectProperty<>(Rotation.NONE);
        // Create the actions UI dependencies
        ObjectProperty<List<String>> actionsP = new SimpleObjectProperty<>(new ArrayList<>());
        // Create the visible occupants observable value
        ObservableValue<Set<Occupant>> visibleOccupants = gameStateP.map(g -> {
            Set<Occupant> visibleOccupantsSet = new HashSet<>(g.board().occupants());
            // Add the potential occupants of the last tile placed
            if (g.nextAction() == GameState.Action.OCCUPY_TILE)
                visibleOccupantsSet.addAll(g.lastTilePotentialOccupants());
            return visibleOccupantsSet;
        });

        // Action handler
        Consumer<String> actionHandler = a -> addAction(gameStateP, tileToPlaceRotationP, actionsP, a, false,
                GAME_DATA.get().name());

        // Event handlers for the board UI
        Consumer<Pos> posConsumer = p -> {
            if (!correctPlayer.getValue()) return;
            GameState gameStateV = gameStateP.getValue();
            // Find the tile to place and check if it can be placed
            PlacedTile placedTile = new PlacedTile(gameStateV.tileToPlace(),
                    gameStateV.currentPlayer(), tileToPlaceRotationP.getValue(), p);
            if (gameStateV.board().canAddTile(placedTile)) {
                actionHandler.accept(ActionEncoder.withPlacedTile(gameStateV, placedTile).action());
            }
        };

        Consumer<Occupant> occupantConsumer = o -> {
            // Check if the game has ended and the player wants to return to the main menu
            tryReturnToMainMenu(gameStateP, isHost(), clientColor.get());
            if (!correctPlayer.getValue()) return;
            GameState gameStateV = gameStateP.getValue();
            // Add the occupant to the board if the player can occupy the tile
            if (gameStateV.nextAction() == GameState.Action.OCCUPY_TILE
                    && !gameStateV.board().occupants().contains(o))
                actionHandler.accept(ActionEncoder.withNewOccupant(gameStateV, o).action());
                // Remove the occupant from the board if the player can retake a pawn
            else if (gameStateV.nextAction() == GameState.Action.RETAKE_PAWN) {
                boolean validOccupant = Objects.isNull(o) ||
                        (o.kind() == Occupant.Kind.PAWN && gameStateV.board().occupants().contains(o) &&
                                gameStateV.board().tileWithId(Zone.tileId(o.zoneId())).placer() == gameStateV.currentPlayer());
                if (validOccupant)
                    actionHandler.accept(ActionEncoder.withOccupantRemoved(gameStateV, o).action());
            }
        };

        Consumer<Rotation> rotationConsumer = r -> {
            if (!correctPlayer.getValue()) return;
            tileToPlaceRotationP.setValue(tileToPlaceRotationP.getValue().add(r));
        };

        // Create the deck UI dependencies
        ObservableValue<Tile> tile = gameStateP.map(GameState::tileToPlace);
        ObservableValue<Integer> normalCount = gameStateP.map(g -> g.tileDecks().normalTiles().size());
        ObservableValue<Integer> menhirCount = gameStateP.map(g -> g.tileDecks().menhirTiles().size());
        // Map the correct text to show depending on the next action
        ObservableValue<String> text = gameStateP.map(GameState::nextAction).map(action -> {
            boolean defaultText = correctPlayer.getValue();
            String waitText = textMaker.waitForPlayer(playerNames.get(gameStateP.getValue().currentPlayer()));
            if (action == GameState.Action.END_GAME) return "Retour au menu principal";
            else if (action == GameState.Action.OCCUPY_TILE) return defaultText ? textMaker.clickToOccupy() : waitText;
            else if (action == GameState.Action.RETAKE_PAWN) return defaultText ? textMaker.clickToUnoccupy() : waitText;
            else return "";
        });

        // Create the message board UI
        ObservableValue<List<MessageBoard.Message>> messages = gameStateP.map(g -> g.messageBoard().messages());
        ObjectProperty<Set<Integer>> tileIds = new SimpleObjectProperty<>(Set.of());
        // Create the highlighted tiles property
        ObservableValue<Set<Integer>> highlightedTilesP = tileIds.map(_ -> {
            if (gameStateP.getValue().nextAction() == GameState.Action.END_GAME) return Set.of();
            else return tileIds.getValue();
        });

        // Create the board UI and the bot
        Node boardNode = BoardUI.create(Board.REACH = 12, gameStateP, tileToPlaceRotationP, visibleOccupants,
                highlightedTilesP, rotationConsumer, posConsumer, occupantConsumer, correctPlayer);
        ObjectProperty<Node> board = new SimpleObjectProperty<>(boardNode);

        gameStateP.map(GameState::nextAction).addListener((_, _, nextAction) -> {
            // Check if the game has ended
            if (nextAction == GameState.Action.END_GAME) {
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(_ -> board.setValue(EndGameBoard.create(8, gameStateP)));
                pause.play();
            }

        });

        gameStateP.addListener((_, _, n) -> {
            // Allow the bot to play
            if (!GAME_DATA.get().requiresServer() && n.nextAction() != GameState.Action.END_GAME &&
                    n.currentPlayer() == reversePlayerDatas.get(botData.getFirst().uuid())) {
                ActionEncoder.StateAction botStateAction = bots.getFirst().play(n);
                actionHandler.accept(botStateAction.action());
            }
        });

        // Create the UIs
        BorderPane playerUI = new BorderPane(PlayersUI.create(gameStateP, textMaker));
        BorderPane messageBoardUI = new BorderPane(MessageBoardUI.create(messages, tileIds));
        BorderPane decksUI = new BorderPane(DecksUI.create(tile, normalCount, menhirCount, text, occupantConsumer));
        BorderPane actionUI = new BorderPane(ActionUI.create(actionsP, actionHandler, correctPlayer));

        // Right-hand part of the GUI
        BorderPane right = new BorderPane();
        right.setTop(playerUI);
        right.setCenter(messageBoardUI);
        right.setBottom(new VBox(actionUI, decksUI));

        // The root of the scene graph
        BorderPane container = new BorderPane();
        container.setCenter(PlayerMessageBoardUI.create(gameStateP, tileIds, playerNames, board, textMaker));
        container.setRight(right);

        if (GAME_DATA.get().requiresServer()) {
            scheduler.scheduleAtFixedRate(() -> {
                if (!correctPlayer.getValue() && gameStateP.get().nextAction() != GameState.Action.END_GAME) {
                    try {
                        Platform.runLater(() -> {
                            Response response = Database.get(STR."games/\{GAME_DATA.get().name()}");
                            if (!response.isSuccess()) return;

                            // Update the players
                            Main.GAME_DATA.set(GAME_DATA.get().withPlayers(response.jsonObject().getArray("players")));
                            Main.GAME_DATA.set(GAME_DATA.get().withHost(response.jsonObject().get("host").asString()));

                            // Get the actions from the server
                            if (response.jsonObject().containsKey("actions")) {
                                // Format the actions from the server
                                List<String> resActions = new ArrayList<>(
                                        response.jsonObject().getArray("actions").values().stream()
                                                .map(JSONValue::asString).toList()
                                );
                                // Find the new actions
                                List<String> actions = resActions.subList(actionsP.get().size(), resActions.size());
                                // Add the actions to the game state
                                for (String a : actions)
                                    addAction(gameStateP, tileToPlaceRotationP, actionsP, a, true,
                                            GAME_DATA.get().name());

                            }


                            // Allow the bot to play if the player is not in the game
                            if (!isHost()) return;
                            PlayerData currentPlayer =
                                    GAME_DATA.get().getPlayer(playerDatas.get(gameStateP.getValue().currentPlayer()));
                            // Check if the player is not in the game
                            if (Objects.nonNull(currentPlayer) && !currentPlayer.inGame()) {
                                ActionEncoder.StateAction botStateAction = bots.get(GAME_DATA.get().players()
                                        .indexOf(currentPlayer)).play(gameStateP.getValue());
                                actionHandler.accept(botStateAction.action());
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else if (gameStateP.get().nextAction() == GameState.Action.END_GAME) scheduler.shutdown();
            }, 0, 1, TimeUnit.SECONDS);
        }

        gameStateP.set(gameStateP.get().withStartingTilePlaced());

        return new Scene(container, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /**
     * Used to add an action to the game state
     * @param gameStateP The game state property
     * @param tileToPlaceRotationP The tile to place rotation property
     * @param actionsP The action property
     * @param action The action to add
     * @param fromServer Whether the action comes from the server
     */
    private static void addAction(ObjectProperty<GameState> gameStateP, ObjectProperty<Rotation> tileToPlaceRotationP,
                                  ObjectProperty<List<String>> actionsP, String action, boolean fromServer, String name) {
        // Decode the action and apply it to the game state
        ActionEncoder.StateAction stateAction =
                ActionEncoder.decodeAndApply(gameStateP.getValue(), action);
        if (Objects.nonNull(stateAction)) {
            PlacedTile lastPlacedTile = stateAction.gameState().board().lastPlacedTile();
            if (Objects.nonNull(lastPlacedTile)) tileToPlaceRotationP.set(lastPlacedTile.rotation());
            List<String> newActions = new ArrayList<>(actionsP.get());
            newActions.add(stateAction.action());
            actionsP.set(newActions);
            gameStateP.set(stateAction.gameState());
            tileToPlaceRotationP.set(Rotation.NONE);
            // Send the action to the server
            if (!fromServer && GAME_DATA.get().requiresServer()) {
                JSONArray actions = new JSONArray().appendAll(newActions.stream().map(JSONValue::parse).toList());
                Database.put(STR."games/\{name}/actions", actions);
            }
        }
    }

    /**
     * Used to try to return to the main menu
     * @param gameState The game state property
     * @param isHost Whether the player is the host
     * @param color The player color
     */
    private static void tryReturnToMainMenu(ObjectProperty<GameState> gameState, boolean isHost, PlayerColor color) {
        if (gameState.get().nextAction() != GameState.Action.END_GAME) return;
        Platform.runLater(() -> {
            if (GAME_DATA.get().requiresServer()) {
                if (isHost) Database.delete(STR."games/\{GAME_DATA.get().name()}");
                // Update the user data
                Authentication.user = Authentication.user.withGame(
                        gameState.get().getWinners().getKey().contains(color),
                        gameState.get().messageBoard().points().getOrDefault(color, 0)
                );
            }

            // Return to the main menu
            Main.updateScene(Main.SceneType.MAIN);
        });
    }

    /**
     * Used to check if the player is the host
     * @return Whether the player is the host or not
     */
    private static boolean isHost() {
        return GAME_DATA.get().isHost(Main.PLAYER_DATA.get().uuid());
    }
}