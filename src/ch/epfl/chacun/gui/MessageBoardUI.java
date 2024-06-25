package ch.epfl.chacun.gui;

import ch.epfl.chacun.MessageBoard;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Set;

/**
 * Represents the GUI for the message board
 * @author Adam BEKKAR (379476)
 */
public class MessageBoardUI {
    /** Private constructor to prevent instantiation */
    private MessageBoardUI() {}

    /**
     * Used to create the GUI for the message board
     * @param observableMessage The observable message board
     * @param tilesIds The set of tile ids
     */
    public static Node create(ObservableValue<List<MessageBoard.Message>> observableMessage,
                              ObjectProperty<Set<Integer>> tilesIds) {
        // Create a vertical container and add the stylesheet, and id to it
        VBox verticalContainer = new VBox();
        verticalContainer.getStylesheets().add("message-board.css");
        verticalContainer.setId("message-board");
        verticalContainer.setPrefWidth(ImageLoader.LARGE_TILE_FIT_SIZE);
        verticalContainer.setPrefWidth(ImageLoader.LARGE_TILE_FIT_SIZE);

        // Create a scroll pane and add it to the vertical container
        ScrollPane root = new ScrollPane(verticalContainer);

        // Add a listener to the observable message board
        observableMessage.addListener((_, oldMessages, newMessages) -> {
            // We assume that the new messages are composed of the old messages and new messages
            newMessages.stream().skip(oldMessages.size()).forEach(message -> {
                // Create a new text, set the wrapping width and add it to the vertical container
                Text text = new Text(message.text());
                text.setWrappingWidth(ImageLoader.LARGE_TILE_FIT_SIZE);
                text.setOnMouseEntered(_ -> tilesIds.setValue(message.tileIds()));
                text.setOnMouseExited(_ -> tilesIds.setValue(Set.of()));

                verticalContainer.getChildren().add(text);
            });


            // After messages are added, scroll to the bottom
            root.layout();
            root.setVvalue(1);
        });

        return root;
    }
}
