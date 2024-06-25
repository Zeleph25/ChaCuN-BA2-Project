package ch.epfl.chacun.extensions.gui;

import ch.epfl.chacun.extensions.backend.Database;
import ch.epfl.chacun.extensions.backend.Response;
import ch.epfl.chacun.extensions.bot.Bot;
import ch.epfl.chacun.extensions.data.GameData;
import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONValue;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Represents the game play user interface
 * @author Antoine Bastide (375407)
 */
public class GamePlayUI {
    /** The card types */
    private static final String FULL_GAME = "Complete";
    private static final String HALF_GAME = "Demie";
    private static final String QUICK_GAME = "Rapide";
    private static final String CUSTOM_GAME =  "Personnalisée";
    /** The card count map */
    private static final Map<String, Integer> CARD_COUNT = Map.of(
            FULL_GAME, 78,
            HALF_GAME, 40,
            QUICK_GAME, 20
    );

    /** The scheduled executor service for the join game refresher */
    public static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static Button refreshButton;

    /** Private constructor to prevent instantiation */
    private GamePlayUI() {}

    /** Used to show the main menu screen */
    public static Scene showMainMenu() {
        // Title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");

        // Join Game button
        Button joinGameButton = new Button("Rejoindre une Partie");
        joinGameButton.setId("green-button");
        joinGameButton.setOnAction(_ -> Main.updateScene(Main.SceneType.JOIN));
        joinGameButton.setMaxWidth(500);

        // Create Game button
        Button createGameButton = new Button("Créer une Partie");
        createGameButton.setId("blue-button");
        createGameButton.setOnAction(_ -> Main.updateScene(Main.SceneType.CREATE));
        createGameButton.setMaxWidth(500);

        // Create Game button
        Button createBotGameButton = new Button("Jouer contre un Bot");
        createBotGameButton.setId("purple-button");
        createBotGameButton.setOnAction(_ -> {
            Main.GAME_DATA.set(new GameData("BotGame", new Random().nextInt(3000), false, CARD_COUNT.get(FULL_GAME), false));
            Main.updateScene(Main.SceneType.WAITING);
        });
        createBotGameButton.setMaxWidth(500);

        // Rules button
        Button rulesButton = new Button("Règles du Jeu");
        rulesButton.setId("yellow-button");
        rulesButton.setOnAction(_ -> Main.updateScene(Main.SceneType.RULES));
        rulesButton.setMaxWidth(500);

        // Account Button
        Button accountButton = new Button("Compte");
        accountButton.setId("orange-button");
        accountButton.setOnAction(_ -> Main.updateScene(Main.SceneType.ACCOUNT));
        accountButton.setMaxWidth(500);

        // Quit button
        Button quitButton = new Button("Quitter");
        quitButton.setId("red-button");
        quitButton.setOnAction(_ -> Main.primaryStage.close());
        quitButton.setMaxWidth(500);

        // Error label
        Label errorLabel = new Label();
        errorLabel.setId("error-label");
        errorLabel.textProperty().bind(Main.ERROR_MESSAGE);

        // Layout setup
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, joinGameButton, createGameButton, createBotGameButton);
        layout.getChildren().addAll(rulesButton, accountButton, quitButton, errorLabel);
        layout.setId("vbox");

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /** Used to show the creation game screen */
    public static Scene showCreateGameScreen() {
        // Title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");

        // Sub-Title label
        Label subtitleLabel = new Label();
        subtitleLabel.setId("label");
        subtitleLabel.setText("Nom de la Partie");

        // Create the text field
        TextField textField = new TextField();
        textField.setId("input-field");
        textField.setMaxWidth(500);
        textField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String text = change.getControlNewText();
                if (text.length() > 15) return null;
            }
            return change;
        }));
        textField.setPromptText("Entrez le nom de la partie …");

        // Create button
        Button createButton = new Button("Créer");
        createButton.setId("green-button");
        createButton.setOnAction(_ -> {
            String gameName = textField.getText();
            String validationMessage = Main.validateInputString(gameName, "Nom de la Partie", true);
            Main.ERROR_MESSAGE.set(validationMessage);
            if (validationMessage.isEmpty()) {
                // Create the game json
                int seed = new Random().nextInt(10001);
                Main.GAME_DATA.set(new GameData(gameName, seed, false, CARD_COUNT.get(FULL_GAME), true));
                Response response = Database.get(STR."games/\{gameName}");
                if (response.statusCode() == 404 || !response.body().equals("null")) {
                    Main.ERROR_MESSAGE.set(response.statusCode() == 404 ?
                            "Erreur de connection au serveur" : "Ce nom de partie est déjà pris");
                    return;
                }
                Database.put(STR."games/\{gameName}", Main.GAME_DATA.get().toJSON());
                Main.updateScene(Main.SceneType.WAITING);
            }
        });

        // Layout setup
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, subtitleLabel, textField);
        layout.getChildren().addAll(buttonsAndErrorLabel(createButton));
        layout.setId("vbox");

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /** Used to show the join game screen */
    public static Scene showJoinGameScreen() {
        // Title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");

        // Sub-Title label
        Label subtitleLabel = new Label();
        subtitleLabel.setId("label");
        subtitleLabel.setText("Rejoindre une Partie");

        // Game list container
        ObjectProperty<VBox> gameList = new SimpleObjectProperty<>(getGameList());

        // ScrollPane for game list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.contentProperty().bind(gameList);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(600);

        // Refresh button
        refreshButton = new Button("Rafraîchir");
        refreshButton.setId("green-button");
        refreshButton.setOnAction(_ -> gameList.set(getGameList()));

        // Layout setup
        VBox layout = new VBox(20, titleLabel, subtitleLabel, scrollPane);
        layout.getChildren().addAll(buttonsAndErrorLabel(refreshButton));
        layout.setAlignment(Pos.CENTER);
        layout.setId("vbox");

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /**
     * Used to get the game list
     * @return The game list container
     */
    private static VBox getGameList() {
        JSONObject games = Database.get("games").jsonObject();

        // Game list container
        VBox gameList = new VBox(20);
        gameList.setPadding(new Insets(10));
        gameList.setAlignment(Pos.TOP_CENTER);
        gameList.setPrefSize(1000, 600);

        // Add some sample game items
        for (Map.Entry<String, JSONValue> entry : games.get().entrySet()) {
            int playerCount = entry.getValue().get("players").toJSONArray().size();
            boolean started = entry.getValue().get("started").asBoolean();
            gameList.getChildren().add(createGameItem(entry.getKey(), playerCount, started));
        }

        return gameList;
    }

    /** Used to create a game item UI */
    private static HBox createGameItem(String gameName, int currentPlayers, boolean started) {
        Insets padding = new Insets(20);

        // Game name label
        Label gameNameLabel = new Label(gameName);
        gameNameLabel.setId("label-small");
        gameNameLabel.setMaxWidth(Double.MAX_VALUE);
        gameNameLabel.setPadding(padding);

        // Player count label
        Label playerCountLabel = new Label(STR."Joueur\{currentPlayers == 1 ? ": " : "s:"} \{currentPlayers}/5");
        playerCountLabel.setId("label-small");
        playerCountLabel.setMaxWidth(Double.MAX_VALUE);
        playerCountLabel.setAlignment(Pos.CENTER);
        playerCountLabel.setPadding(padding);
        HBox.setHgrow(playerCountLabel, Priority.ALWAYS);

        // Get the game json
        Response response = Database.get(STR."games/\{gameName}");
        if (response.statusCode() == 404) {
            Main.updateScene(Main.SceneType.MAIN);
            Main.ERROR_MESSAGE.set("Erreur de connection au serveur");
            return null;
        }
        GameData gameData = GameData.fromJSON(gameName, response.jsonObject());
        boolean containsPlayer = gameData.getPlayer(Main.PLAYER_DATA.get().uuid()) != null &&
                !gameData.getPlayer(Main.PLAYER_DATA.get().uuid()).inGame();
        boolean canRejoin = containsPlayer && gameData.started();
        boolean canJoin = canRejoin || (currentPlayers < 5 && !started && !containsPlayer);

        // Join button
        Button joinButton = new Button(
                canRejoin ? "Re-Rejoindre" :
                        started ? "En Cours" :
                                containsPlayer ? "Déjà Rejoint" :
                                        currentPlayers < 5 ? "Rejoindre" : "Pleine"
        );
        joinButton.setId(STR."\{canJoin ? "green" : "red"}-button");
        joinButton.setDisable(!canJoin);
        joinButton.setPadding(padding);
        joinButton.setOnAction(_ -> {
            Response newResponse = Database.get(STR."games/\{gameName}");
            if (newResponse.body().equals("null")) {
                Main.ERROR_MESSAGE.set("Cette partie n'existe plus");
                refreshButton.fire();
                return;
            } else if (newResponse.statusCode() == 404) {
                Main.updateScene(Main.SceneType.MAIN);
                Main.ERROR_MESSAGE.set("Erreur de connection au serveur");
                return;
            }
            JSONObject newJsonGame = Database.get(STR."games/\{gameName}").jsonObject();
            GameData newGameData = GameData.fromJSON(gameName, newJsonGame);

            if (newGameData.players().size() >= 5) {
                Main.ERROR_MESSAGE.set("Cette partie est pleine");
                refreshButton.fire();
            }
            else if (newGameData.started()) {
                // Rejoin the game
                if (newGameData.players().stream().anyMatch(p -> p.uuid().equals(Main.PLAYER_DATA.get().uuid()) && !p.inGame())) {
                    Main.GAME_DATA.set(newGameData);
                    Main.GAME_DATA.get().withPlayerInGame(Main.PLAYER_DATA.get().uuid(), true);
                    Database.put(STR."games/\{gameName}/players", Main.GAME_DATA.get().toJSON().get("players"));
                    Main.updateScene(Main.SceneType.GAME);
                } else {
                    Main.ERROR_MESSAGE.set("Cette partie a déjà commencé");
                    refreshButton.fire();
                }
            }
            // Join the game
            else {
                newGameData.players().add(Main.PLAYER_DATA.get());
                Main.GAME_DATA.set(newGameData);
                Database.put(STR."games/\{gameName}/players", Main.GAME_DATA.get().toJSON().get("players"));
                Main.updateScene(Main.SceneType.WAITING);
            }
        });

        // Item container
        HBox itemContainer = new HBox(20, gameNameLabel, playerCountLabel, joinButton);
        itemContainer.setPadding(new Insets(10));
        itemContainer.setId("hbox");
        return itemContainer;
    }

    /** Used to show the rule screen */
    public static Scene showRulesScreen() {
        // Title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");

        // Sub-Title label
        Label subtitleLabel = new Label();
        subtitleLabel.setId("label");
        subtitleLabel.setText("Règles du Jeu:");

        // Rules text
        String rulesText = """
        ChaCuN est un jeu conçu pour 2 à 5 joueurs, dont le but est de construire progressivement un paysage préhistorique en plaçant côte à côte des tuiles carrées. Les différentes parties du paysage ainsi construit peuvent être occupées par des chasseurs, cueilleurs ou pêcheurs, dans le but d'obtenir des points.
        
        Chacun leur tour, les joueurs tirent une nouvelle tuile au hasard, qu'ils placent sur la surface de jeu—éventuellement après l'avoir tournée—de manière à ce qu'elle soit voisine d'au moins une tuile déjà posée, et que les bords des tuiles qui se touchent forment un paysage continu.
        
        Normalement, chaque joueur ne peut placer qu'une seule tuile durant son tour. Toutefois, si la tuile qu'il pose ferme au moins une forêt contenant un menhir, alors il a le droit de placer une seconde tuile, tirée du tas des «tuiles menhir», distinct du tas normal. Les tuiles menhir sont généralement de plus grande valeur que les autres, et certaines possèdent même un pouvoir spécial.
      
        Les pouvoir spéciaux des tuiles menhirs sont les suivants:
         - Le Chaman: permet au joueur qui le pose de récupérer, s'il le désire, l'un de ses pions
         - La Pirogue: rapporte au joueur qui la pose un nombre de points dépendant du nombre de lacs accessibles à la pirogue
         - La Fosse à Pieux: rapporte au joueur qui la pose un nombre de points dépendant des animaux présents sur les tuiles voisines
         - La Grande Fosse à Pieux: rapporte aux chasseurs majoritaires du pré la contenant des points supplémentaires pour les animaux présents sur les tuiles voisines de la fosse.
         - Le Feu: fait fuir tous les smilodons du pré qui le contient, évitant ainsi qu'ils ne dévorent des cerfs.
         - Le Radeau: rapporte aux pêcheurs majoritaires du réseau hydrographique le contenant des points additionnels dépendant du nombre de lacs qu'il contient.
        
        Après avoir placé une tuile, un joueur peut éventuellement l'occuper au moyen de l'un des 5 pions ou de l'une des 3 huttes qu'il possède. Suivant où ils sont placés, ces pions et huttes jouent différents rôles. Par exemple, un pion placé dans une forêt joue le rôle d'un cueilleur.
        
        Les occupants permettent aux joueurs de remporter des points à différents moments de la partie. Par exemple, lorsqu'une forêt est totalement fermée, le ou les joueurs possédant le plus de cueilleurs dans cette forêt remportent un nombre de points qui dépend de la taille de la forêt. Une fois les points comptabilisés, tous les cueilleurs présents dans une forêt fermée sont retournés à leurs propriétaires, qui peuvent les réutiliser ultérieurement pour occuper d'autres tuiles.
        
        En plus des forêts, les autres éléments du paysage qu'il est possible d'occuper sont:
         - les rivières, qui peuvent être occupées par des pions jouant le rôle de pêcheurs,
         - les prés, qui peuvent être occupés par des pions jouant le rôle de chasseurs,
         - les réseaux hydrographiques — constitués de rivières reliées entre elles par des lacs — qui peuvent être occupés par des huttes de pêcheurs.
        
        Les rivières fonctionnent comme les forêts, dans le sens où dès que l'une d'entre elles est terminée aux deux bouts—soit par un lac, soit par un autre élément de paysage—le ou les joueurs y possédant le plus grand nombre de pêcheurs remportent des points, et tous les occupants de la rivière sont retournés à leur propriétaire.
        
        Les prés et les réseaux hydrographiques, par contre, ne rapportent des points à leurs chasseurs et pêcheurs respectifs qu'au moment où la partie se termine, c.-à-d. que la dernière tuile a été posée. Les chasseurs et huttes de pêcheurs posés restent donc à leur place jusqu'à la fin de la partie et ne peuvent pas être réutilisés.
        """;

        // Rules label
        Label rulesLabel = new Label(rulesText);
        rulesLabel.setId("label-small");
        rulesLabel.setWrapText(true);
        rulesLabel.setPrefWidth(1300);

        // Container for rules label with padding
        VBox rulesContainer = new VBox(rulesLabel);
        rulesContainer.setPadding(new Insets(20));

        // Scroll pane for rules
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(rulesContainer);
        scrollPane.setPrefWidth(900);
        scrollPane.setPrefHeight(700);

        // Back button
        Button backButton = new Button("Retour");
        backButton.setId("blue-button");
        backButton.setOnAction(_ -> Main.updateScene(Main.SceneType.MAIN));

        // Layout setup
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, subtitleLabel, scrollPane, backButton);
        layout.setId("vbox");

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /** Used to show the waiting screen */
    public static Scene showWaitingScreen() {
        ObservableValue<String> playerListO = Main.GAME_DATA.map(g -> {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < (g.requiresServer() ? 5 : 4); i++) {
                String player = i < g.players().size() ? g.players().get(i).username() :
                        !g.requiresServer() && i == g.players().size() ?
                                STR."\{g.botDifficulty()} Bot" : "";
                stringBuilder.append(player.isEmpty() ? "\n " : STR."\{i + 1}) \{player}\n");
            }
            return stringBuilder.toString();
        });
        ObservableValue<String> gameTypeO = Main.GAME_DATA.map(g -> STR."\{g.cardCount()} carte\{g.cardCount() == 1 ? "" : "s"}");
        ObservableValue<Boolean> isHost = Main.GAME_DATA.map(g -> g.isHost(Main.PLAYER_DATA.get().uuid()));

        // Title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");

        // Sub-Title label
        Label subtitleLabel = new Label();
        subtitleLabel.setId("label");
        subtitleLabel.setText("Players:");

        // Players list
        Label playerList = new Label();
        playerList.setId("label-small");
        playerList.textProperty().bind(playerListO);
        VBox playersBox = new VBox(10, subtitleLabel, playerList);

        // Custom settings
        VBox customSettingsBox = new VBox();
        Label cardCountLabel = new Label("Nombres de Cartes:");
        cardCountLabel.setId("label-small");
        Slider cardCountSlider = new Slider(1, CARD_COUNT.get(FULL_GAME), CARD_COUNT.get(FULL_GAME));
        cardCountSlider.setShowTickLabels(true);
        cardCountSlider.setShowTickMarks(true);
        cardCountSlider.setBlockIncrement(1);
        Label cardCountValue = new Label();
        cardCountValue.textProperty().bind(cardCountSlider.valueProperty().map(d ->
                String.valueOf((int) Math.floor((double) d))));
        cardCountValue.setId("label-small");

        // Game type
        Label gameTypeLabel = new Label();
        gameTypeLabel.textProperty().bind(isHost.map(b -> b ? "Type de Partie:" : "Nombres de Cartes:"));
        gameTypeLabel.setId("label");

        // Game type combo box
        ComboBox<String> gameTypeComboBox = new ComboBox<>();
        gameTypeComboBox.getItems().addAll(FULL_GAME, HALF_GAME, QUICK_GAME, CUSTOM_GAME);
        gameTypeComboBox.setValue(FULL_GAME);
        gameTypeComboBox.setId("label-small");
        gameTypeComboBox.setPrefWidth(250);
        gameTypeComboBox.setOnAction(_ -> {
            int count = switch (gameTypeComboBox.getValue()) {
                case FULL_GAME, HALF_GAME, QUICK_GAME -> CARD_COUNT.get(gameTypeComboBox.getValue());
                case CUSTOM_GAME -> (int) Math.floor(cardCountSlider.getValue());
                default -> 0;
            };
            Main.GAME_DATA.set(Main.GAME_DATA.get().withCardCount(count));
            if (Main.GAME_DATA.get().requiresServer())
                Database.put(STR."games/\{Main.GAME_DATA.get().name()}/cardCount", JSONValue.parse(STR."\{count}"));
        });
        gameTypeComboBox.visibleProperty().bind(isHost);

        // Pause transition to send the card count to the server after the user stops sliding
        PauseTransition pause = new PauseTransition(Duration.seconds(0.25));
        pause.setOnFinished(_ ->
            Database.put(STR."games/\{Main.GAME_DATA.get().name()}/cardCount", Main.GAME_DATA.get().toJSON().get("cardCount"))
        );
        cardCountSlider.valueProperty().addListener((_, _, n) -> {
            Main.GAME_DATA.set(Main.GAME_DATA.get().withCardCount((int) Math.floor((double) n)));
            if (Main.GAME_DATA.get().requiresServer()) pause.playFromStart();
        });

        customSettingsBox.getChildren().addAll(cardCountLabel, cardCountSlider, cardCountValue);
        customSettingsBox.setAlignment(Pos.CENTER);
        customSettingsBox.visibleProperty().bind(gameTypeComboBox.valueProperty()
                .map(s -> s.equals(CUSTOM_GAME) && isHost.getValue()));

        Label gameTypeValue = new Label();
        gameTypeValue.setPrefWidth(200);
        gameTypeValue.setId("label-game-type");
        gameTypeValue.textProperty().bind(gameTypeO);
        gameTypeValue.visibleProperty().bind(isHost.map(b -> !b));
        gameTypeValue.managedProperty().bind(gameTypeValue.visibleProperty());

        VBox gameTypeBox = new VBox(10, gameTypeLabel, gameTypeValue, gameTypeComboBox, customSettingsBox);
        gameTypeBox.setAlignment(Pos.CENTER);
        gameTypeBox.setPadding(new Insets(0, 0, 45, 0));

        // Bot botDifficulty label
        Label botDifficultyLabel = new Label("Difficulté du Bot:");
        botDifficultyLabel.setId("label");

        // Bot botDifficulty choice
        ComboBox<Bot.Level> botComboBox = new ComboBox<>();
        botComboBox.getItems().addAll(Bot.Level.values());
        botComboBox.setValue(Bot.Level.EASY);
        botComboBox.setId("label-small");
        botComboBox.setPrefWidth(225);
        botComboBox.setOnAction(_ -> {
            Main.GAME_DATA.set(Main.GAME_DATA.get().withBotDifficulty(botComboBox.getValue()));
            if (!Main.GAME_DATA.get().requiresServer()) return;
            Database.put(STR."games/\{Main.GAME_DATA.get().name()}/botDifficulty", JSONValue.parse(botComboBox.getValue().toString()));

        });
        botComboBox.visibleProperty().bind(isHost);
        botComboBox.managedProperty().bind(botComboBox.visibleProperty());

        // Bot botDifficulty value label
        Label botDifficultyValue = new Label();
        botDifficultyValue.setId("label-game-type");
        botDifficultyValue.textProperty().bind(Main.GAME_DATA.map(GameData::botDifficulty).map(Bot.Level::toString));
        botDifficultyValue.visibleProperty().bind(isHost.map(b -> !b));
        botDifficultyValue.managedProperty().bind(botDifficultyValue.visibleProperty());
        botDifficultyValue.setPrefWidth(225);

        // Bot botDifficulty box
        VBox botDifficultyBox = new VBox(10, botDifficultyLabel, botComboBox, botDifficultyValue);
        botDifficultyBox.setPadding(new Insets(0, 0, 150, 0));

        // Info box
        HBox infoBox = new HBox(50, playersBox, gameTypeBox, botDifficultyBox);
        infoBox.setAlignment(Pos.CENTER);

        // Buttons
        Button leaveButton = new Button("Quitter");
        leaveButton.setId("red-button");
        leaveButton.setOnAction(_ -> {
            Main.updateScene(Main.SceneType.MAIN);
            Main.GAME_DATA.get().players().remove(Main.GAME_DATA.get().getPlayer(Main.PLAYER_DATA.get().uuid()));
            if (!Main.GAME_DATA.get().requiresServer()) return;
            if (Main.GAME_DATA.get().players().isEmpty()) Database.delete(STR."games/\{Main.GAME_DATA.get().name()}");
            else {
                Response response = Database.get(STR."games/\{Main.GAME_DATA.get().name()}");
                if (response.body().equals("null")) return;
                if (isHost.getValue())
                    Database.put(STR."games/\{Main.GAME_DATA.get().name()}/host", JSONValue.parse(Main.GAME_DATA.get().players().getFirst().uuid()));
                Database.put(STR."games/\{Main.GAME_DATA.get().name()}/players", Main.GAME_DATA.get().toJSON().get("players"));
            }
            Main.stopSchedulers();
        });
        leaveButton.prefWidthProperty().bind(isHost.map(b -> b ? 300 : 500));
        leaveButton.setAlignment(Pos.CENTER);

        // Show the start button if the user is the creator
        Button startButton = new Button("Lancer");
        startButton.setId("green-button");
        startButton.setOnAction(_ -> {
            if (!Main.GAME_DATA.get().requiresServer()) Main.updateScene(Main.SceneType.GAME);
            else Database.put(STR."games/\{Main.GAME_DATA.get().name()}/started", JSONValue.parse("true"));
        });
        startButton.visibleProperty().bind(isHost);
        startButton.managedProperty().bind(isHost);
        startButton.disableProperty().bind(Main.GAME_DATA.map(g -> g.requiresServer() && g.players().size() < 2));

        HBox buttonBox = new HBox(50, leaveButton, startButton);
        buttonBox.setAlignment(Pos.CENTER);
        VBox layout = new VBox(40, titleLabel, infoBox, buttonBox);
        layout.setAlignment(Pos.CENTER);

        // Add a delay to call this method again
        if (Main.GAME_DATA.get().requiresServer()) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    Platform.runLater(() -> {
                        Response response = Database.get(STR."games/\{Main.GAME_DATA.get().name()}");
                        if (!response.isSuccess()) return;

                        GameData newGameData = GameData.fromJSON(Main.GAME_DATA.get().name(), response.jsonObject());
                        Main.GAME_DATA.set(newGameData);
                        if (newGameData.started()) {
                            Main.updateScene(Main.SceneType.GAME);
                            scheduler.shutdown();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, 0, 1, TimeUnit.SECONDS);
        }

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /**
     * Used to create the buttons and error label
     * @return The array of nodes
     */
    private static Node[] buttonsAndErrorLabel(Button button) {
        // Back button
        Button backButton = new Button("Retour");
        backButton.setId("blue-button");
        backButton.setOnAction(_ -> Main.updateScene(Main.SceneType.MAIN));

        // Button container
        HBox buttonContainer = new HBox(20, backButton, button);
        buttonContainer.setAlignment(Pos.CENTER);

        // Error label
        Label errorLabel = new Label();
        errorLabel.setId("error-label");
        errorLabel.textProperty().bind(Main.ERROR_MESSAGE);

        return new Node[] { buttonContainer, errorLabel };
    }
}
