package ch.epfl.chacun.extensions.gui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * A password field that allows the user to toggle the visibility of the password
 * @author Antoine Bastide (375407)
 */
public class VisiblePasswordField {
    /** The password field that is used to store the password */
    private final PasswordField passwordField;
    /** The container that contains the UI elements of the VisiblePasswordField */
    private final VBox container;

    public VisiblePasswordField(boolean confirm) {
        // Create the password field
        passwordField = new PasswordField();
        passwordField.setId("input-field");
        passwordField.setPromptText(STR."Entrer votre mot de passe \{confirm ? "à nouveau " : ""}...");

        // Create the visible password field
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setId("input-field");
        visiblePasswordField.setPromptText(STR."Entrer votre mot de passe \{confirm ? "à nouveau " : ""}...");

        // Create the visibility button
        CheckBox visibilityButton = new CheckBox();
        visibilityButton.setMaxSize(100, 100);

        // Set the initial state of the UI elements
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        visibilityButton.setSelected(false);

        // Bind the properties of the UI elements
        visibilityButton.textProperty().bind(
            Bindings.when(passwordField.visibleProperty())
                .then("Show")
                .otherwise("Hide")
        );
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Bind the visibility of the UI elements
        passwordField.visibleProperty().bind(visibilityButton.selectedProperty().not());
        passwordField.managedProperty().bind(visibilityButton.selectedProperty().not());
        visiblePasswordField.visibleProperty().bind(visibilityButton.selectedProperty());
        visiblePasswordField.managedProperty().bind(visibilityButton.selectedProperty());

        // Add the UI elements to the container
        container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(500);
        container.getChildren().addAll(visiblePasswordField, passwordField, visibilityButton);
    }

    /** Returns the password that the user has entered */
    public String getPassword() {
        return passwordField.getText();
    }

    /** Returns the container that contains the UI elements of the VisiblePasswordField */
    public Node node() {
        return container;
    }
}
