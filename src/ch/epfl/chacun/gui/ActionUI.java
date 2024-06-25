package ch.epfl.chacun.gui;

import ch.epfl.chacun.Base32;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents the GUI for the actions
 * @author Adam BEKKAR (379476)
 */
public class ActionUI {
    /** The maximum number of characters in an action */
    public static final double ACTION_MAX_CHAR_SIZE = 2;
    /** The maximum number of actions to display */
    public static final int ACTION_MAX_NUMBER = 4;

    /** Private constructor to prevent instantiation */
    private ActionUI() {}

    /**
     * Used to create the GUI for the actions
     * @param actionsP The observable list of actions
     * @param actionHandler The action handler
     * @return The GUI for the actions
     */
    public static Node create(ObservableValue<List<String>> actionsP, Consumer<String> actionHandler,
                              ObservableValue<Boolean> correctPlayer) {
        // Create the text to display the last four actionsP
        Text text = new Text();
        text.textProperty().bind(actionsP.map(_ -> getTextActions(actionsP)));

        // Create the horizontal box to display the actionsP and the text field
        HBox hBox = new HBox(text, getTextField(actionHandler, correctPlayer));
        hBox.getStylesheets().add("actions.css");
        hBox.setId("actions");
        return hBox;
    }

    /**
     * Used to get the text of the actionsP
     * @param actionsP The observable list of actionsP
     * @return The text of the actionsP
     */
    private static String getTextActions(ObservableValue<List<String>> actionsP) {
        // Get the list of actionsP and create a string joiner
        List<String> actions = actionsP.getValue();
        StringJoiner sb = new StringJoiner(", ");

        // Get the starting integer of the loop depending on the number of actionsP already displayed
        int start = actions.size() > ACTION_MAX_NUMBER ? actions.size() - ACTION_MAX_NUMBER : 0;
        // For each action to display, add it with an index to the string joiner
        for (int i = start ; i < actions.size() ; i++)
            sb.add(STR."\{i + 1}:\{actions.get(i)}");
        return sb.toString();
    }

    /**
     * Used to create the text field for the actions
     * @param actionHandler The action handler
     * @return The text field for the actions
     */
    private static TextField getTextField(Consumer<String> actionHandler, ObservableValue<Boolean> correctPlayer) {
        TextField textField = new TextField();
        textField.setId("action-field");
        textField.visibleProperty().bind(correctPlayer);

        // Add a formatter to the text field
        textField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlText();
            String nextText = change.getText().chars()
                    .map(Character::toUpperCase)
                    .mapToObj(c -> String.valueOf((char) c))
                    .filter(Base32::isValid)
                    // Limit the number of characters to the maximum size
                    .limit((long) ACTION_MAX_CHAR_SIZE - text.length())
                    .collect(Collectors.joining());
            change.setText(nextText);
            return change;
        }));

        // Create a listener for the ENTER key
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String action = textField.getText();
                // If the action is valid, handle it and clear the text field
                if (!action.isEmpty() && Base32.isValid(action)) {
                    actionHandler.accept(action);
                    textField.clear();
                }
            }
        });
        return textField;
    }
}
