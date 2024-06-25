package ch.epfl.chacun.extensions.gui;

import ch.epfl.chacun.extensions.backend.Authentication;
import ch.epfl.chacun.extensions.backend.Response;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Represents the authentication user interface
 * @author Adam Bekkar (379476)
 */
public class AuthenticationUI {
    /** Private constructor to prevent instantiation */
    private AuthenticationUI() {}


    /**
     * Used to show the authentication screen
     * @return The authentication screen
     */
    public static Scene showAuthScreen() {
        // Create title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");
        titleLabel.setPadding(new Insets(0, 0, 50, 0));

        // Login Labels
        Label loginLabel = new Label("Se Connecter");
        loginLabel.setId("label");
        Label loginInfoLabel = new Label("Connecter vous pour récupérer");
        loginInfoLabel.setId("label-small");
        Label loginInfoLabel2 = new Label("les informations liées à votre compte");
        loginInfoLabel2.setId("label-small");
        loginInfoLabel2.setPadding(new Insets(-20, 0, 0, 0));

        // Login button
        Button loginButton = new Button("Continuer");
        loginButton.setId("green-button");
        loginButton.setMaxWidth(500);
        loginButton.setOnAction(_ -> Main.updateScene(Main.SceneType.LOGIN));

        // Login layout
        VBox loginLayout = new VBox(20);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.getChildren().addAll(loginLabel, loginInfoLabel, loginInfoLabel2, loginButton);
        loginLayout.setMaxWidth(500);
        loginLayout.getStyleClass().add("auth-layout");

        // Signup Labels
        Label signupLabel = new Label("Créer un Compte");
        signupLabel.setId("label");
        Label signupInfoLabel = new Label("Créer un compte pour sauvegarder");
        signupInfoLabel.setId("label-small");
        Label signupInfoLabel2 = new Label("les informations liées à vos parties");
        signupInfoLabel2.setId("label-small");
        signupInfoLabel2.setPadding(new Insets(-20, 0, 0, 0));

        // Create Account button
        Button signupButton = new Button("Continuer");
        signupButton.setId("green-button");
        signupButton.setMaxWidth(500);
        signupButton.setOnAction(_ -> Main.updateScene(Main.SceneType.SIGNUP));

        // Login layout
        VBox signupLayout = new VBox(20);
        signupLayout.setAlignment(Pos.CENTER);
        signupLayout.getChildren().addAll(signupLabel, signupInfoLabel, signupInfoLabel2, signupButton);
        signupLayout.setMaxWidth(500);
        signupLayout.getStyleClass().add("auth-layout");

        // Quit button
        Button quitButton = new Button("Quitter");
        quitButton.setId("red-button");
        quitButton.setMaxWidth(500);
        quitButton.setOnAction(_ -> Main.primaryStage.close());

        // Adding buttons to a vertical box
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(titleLabel, loginLayout, signupLayout, quitButton);
        vbox.setId("vbox");

        return new Scene(vbox, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    /**
     * Used to show the account screen
     * @param isLogin Whether the screen is for logging in or creating an account
     * @return The account screen
     */
    public static Scene showAccountScreen(boolean isLogin) {
        // Create title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");
        titleLabel.setPadding(new Insets(0, 0, 50, 0));

        // Creating title label
        Label usernameLabel = new Label("Nom d'Utilisateur");
        usernameLabel.setId("label");
        Label passwordLabel = new Label("Mot de Passe");
        passwordLabel.setId("label");
        Label confirmPasswordLabel = new Label("Vérification du Mot de Passe");
        confirmPasswordLabel.setId("label");

        // Creating text fields
        TextField usernameField = new TextField();
        usernameField.setId("input-field");
        usernameField.setMaxWidth(500);
        usernameField.setPromptText("Entrez votre nom d'utilisateur ...");
        usernameField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String text = change.getControlNewText();
                if (text.length() > 15) return null;
            }
            return change;
        }));

        // Password fields
        VisiblePasswordField passwordField = new VisiblePasswordField(false);
        VisiblePasswordField confirmPasswordField = new VisiblePasswordField(true);

        // Back button
        Button backButton = new Button("Retour");
        backButton.setId("blue-button");
        backButton.setMaxWidth(500);
        backButton.setOnAction(_ -> Main.updateScene(Main.SceneType.AUTH));

        // Create Account button
        Button createButton = new Button(isLogin ? "Confirmer" : "Créer");
        createButton.setId("green-button");
        createButton.setMaxWidth(500);
        createButton.setOnAction(_ -> {
            String username = usernameField.getText();
            String password = passwordField.getPassword();
            String confirmPassword = confirmPasswordField.getPassword();
            String validationMessage = Main.validateInputString(username, "Nom d'Utilisateur", false);
            if (validationMessage.isEmpty()) {
                validationMessage = Main.validateInputString(password, "Mot de Passe", false);
                if (validationMessage.isEmpty()) {
                    if (isLogin) {
                        Response response = Authentication.login(username, password);
                        if (!response.isSuccess()) validationMessage = response.errorMessage();
                        else Main.updateScene(Main.SceneType.MAIN);
                    } else {
                        validationMessage = Main.validateInputString(confirmPassword, "Vérification du Mot de Passe", false);
                        if (validationMessage.isEmpty()) {
                            if (!password.equals(confirmPassword))
                                validationMessage = "Les mots de passe ne correspondent pas";
                            else {
                                Response response = Authentication.signup(username, password);
                                if (!response.isSuccess()) validationMessage = response.errorMessage();
                                else Main.updateScene(Main.SceneType.MAIN);
                            }
                        }
                    }
                }
            }
            Main.ERROR_MESSAGE.set(validationMessage);
        });

        // Button container
        HBox buttons = new HBox(50);
        buttons.setPadding(new Insets(50, 0, 0, 0));
        buttons.getChildren().addAll(backButton, createButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(1100);

        // Error label
        Label errorLabel = new Label();
        errorLabel.setId("error-label");
        errorLabel.textProperty().bind(Main.ERROR_MESSAGE);

        // Adding elements to the grid pane
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, usernameLabel, usernameField, passwordLabel, passwordField.node());
        if (!isLogin) layout.getChildren().addAll(confirmPasswordLabel, confirmPasswordField.node());
        layout.getChildren().addAll(buttons, errorLabel);

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

    public static Scene showAccountScreen() {
        // Create title label
        Label titleLabel = new Label(Main.WINDOW_TITLE);
        titleLabel.setId("label-title");
        titleLabel.setPadding(new Insets(0, 0, 50, 0));

        // Create the info label
        Label infoLabel = new Label(STR."Informations liées à votre compte @\{Authentication.user.username()}:");
        infoLabel.setId("label");

        // Stats strings
        String noInfo = "Vous n'avez pas encore joué de parties.\nJouez pour obtenir des statistiques.";
        String stats =
STR."Vous avez joué \{Authentication.user.gamesPlayed()} partie\{Authentication.user.gamesPlayed() > 1 ? "s" : ""}.\n" +
STR."Vous avez gagné \{Authentication.user.gamesWon()} partie\{Authentication.user.gamesPlayed() > 1 ? "s" : ""}.\n" +
STR."Vous avez perdu \{Authentication.user.gamesLost()} partie\{Authentication.user.gamesPlayed() > 1 ? "s" : ""}.\n" +
STR."Le meilleur score que vous avez obtenu est \{Authentication.user.bestScore()}.\n" +
STR."Le pire score que vous avez obtenu est \{Authentication.user.worstScore()}.";

        // Stats label
        Label statsLabel = new Label(Authentication.user.gamesPlayed() > 0 ? stats : noInfo);
        statsLabel.setId("label-small");
        statsLabel.setAlignment(Pos.CENTER);

        // Pie chart
        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        PieChart.Data wonData = new PieChart.Data("Gagnées", Authentication.user.gamesWon());
        PieChart.Data lostData = new PieChart.Data("Perdues", Authentication.user.gamesLost());
        pieChart.getData().addAll(wonData, lostData);
        wonData.getNode().setStyle("-fx-pie-color: #00FF00;");
        lostData.getNode().setStyle("-fx-pie-color: #FF0000;");


        // Delete account button
        Button logoutButton = new Button("Déconnexion");
        logoutButton.setId("orange-button");
        logoutButton.setOnAction(_ -> {
            Authentication.logout();
            Main.updateScene(Main.SceneType.AUTH);
        });

        // Delete account button
        Button deleteAccountButton = new Button("Supprimer");
        deleteAccountButton.setId("red-button");
        deleteAccountButton.setOnAction(_ -> {
            Response response = Authentication.delete();
            if (!response.isSuccess()) Main.ERROR_MESSAGE.set(response.errorMessage());
            else Main.updateScene(Main.SceneType.AUTH);
        });

        // Back button
        Button backButton = new Button("Retour");
        backButton.setId("blue-button");
        backButton.setOnAction(_ -> Main.updateScene(Main.SceneType.MAIN));

        // Button container
        HBox buttons = new HBox(50);
        buttons.getChildren().addAll(backButton, logoutButton, deleteAccountButton);
        buttons.setAlignment(Pos.CENTER);

        // Error label
        Label errorLabel = new Label();
        errorLabel.setId("error-label");
        errorLabel.textProperty().bind(Main.ERROR_MESSAGE);

        // Adding elements to the grid pane
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, infoLabel, statsLabel);
        if (Authentication.user.gamesPlayed() > 0) layout.getChildren().add(pieChart);
        layout.getChildren().addAll(buttons, errorLabel);

        return new Scene(layout, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }
}
