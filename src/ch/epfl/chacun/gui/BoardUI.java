package ch.epfl.chacun.gui;

import ch.epfl.chacun.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents the GUI for the board
 * @author Antoine Bastide (375407)
 */
public class BoardUI {
    /** The map containing all the previously generated images */
    private static final Map<Integer, Image> cachedImages = createEmptyImageMap();
    /** The map containing all the generated veils */
    private static final Map<Color, ColorInput> cachedVeilColors = createVeilColors();
    /** The map containing all the previously generated veils */
    private static final Map<Group, Blend> cachedVeils = new HashMap<>();

    /** The map containing all the previously generated occupants */
    private static Map<Occupant, Node> cachedOccupants;

    /**
     * Used to create the empty image map
     * @return The map containing the empty image
     */
    private static Map<Integer, Image> createEmptyImageMap() {
        Map<Integer, Image> map = new HashMap<>();
        WritableImage emptyTileImage = new WritableImage(1, 1);
        emptyTileImage.getPixelWriter().setColor(0, 0, Color.gray(0.98));
        map.put(-1, emptyTileImage);
        return map;
    }

    /**
     * Used to create the color input map for the colors
     * @return The map containing all the color inputs
     */
    public static Map<Color, ColorInput> getColorColorInputMap(double size) {
        Map<Color, ColorInput> map = new HashMap<>();
        Set<Color> colors = PlayerColor.ALL.stream().map(ColorMap::fillColor).collect(Collectors.toSet());
        colors.addAll(Set.of(Color.BLACK, Color.WHITE));
        for (Color color : colors) {
            ColorInput input = new ColorInput(0, 0, 1, 1, color);
            input.setWidth(size);
            input.setHeight(size);
            map.put(color, input);
        }
        return map;
    }
    /**
     * Used to create the veil colors
     * @return The map containing all the veil colors
     */
    private static Map<Color, ColorInput> createVeilColors() {
        return getColorColorInputMap(ImageLoader.NORMAL_TILE_FIT_SIZE);
    }

    /** Private constructor to prevent instantiation */
    private BoardUI() {}

    /**
     * Used to create the GUI for the board
     * @param reach The reach of the board
     * @param gameState The current game state
     * @param tileRotation The current tile rotation
     * @param visibleOccupants The visible occupants
     * @param tilesInEvidence The ids of the tiles in evidence
     * @param rotateTile The function to rotate the tile
     * @param placeTile The function to place the tile
     * @param selectOccupant The function to select an occupant
     * @return The GUI for the board
     */
    public static Node create(int reach, ObservableValue<GameState> gameState, ObservableValue<Rotation> tileRotation,
                              ObservableValue<Set<Occupant>> visibleOccupants,
                              ObservableValue<Set<Integer>> tilesInEvidence, Consumer<Rotation> rotateTile,
                              Consumer<Pos> placeTile, Consumer<Occupant> selectOccupant,
                              ObservableValue<Boolean> correctPlayer) {
        cachedOccupants = new HashMap<>();

        // Create the grid and scroll pane
        GridPane grid = new GridPane();
        ScrollPane scrollPane = new ScrollPane(grid);

        // Apply the stylesheet and ids
        scrollPane.getStylesheets().add("board.css");
        scrollPane.setId("board-scroll-pane");
        grid.setId("board-grid");

        // Create the groups for the tiles and occupants
        for (int i = -reach; i <= reach; i++) {
            for (int j = -reach; j <= reach; j++) {
                // Create the tile group and image view
                ImageView tileView = new ImageView();
                tileView.setFitWidth(ImageLoader.NORMAL_TILE_FIT_SIZE);
                tileView.setFitHeight(ImageLoader.NORMAL_TILE_FIT_SIZE);

                Group tileGroup = new Group(tileView);
                Pos pos = new Pos(i, j);

                ObservableValue<PlacedTile> placedTile = gameState.map(s -> s.board().tileAt(pos));
                ObservableValue<Tile> tile = gameState.map(GameState::tileToPlace);
                ObservableValue<Boolean> isInsertionPosition = gameState.map(s ->
                        s.board().insertionPositions()).map(p -> p.contains(pos));
                ObservableValue<Boolean> hoverProperty = tileGroup.hoverProperty();

                ObjectBinding<CellData> cellData = Bindings.createObjectBinding(() -> {
                    // Get the values
                    GameState gameStateV = gameState.getValue();
                    Set<Integer> tilesInEvidenceV = tilesInEvidence.getValue();
                    Tile tileV = tile.getValue();
                    PlacedTile placedTileV = placedTile.getValue();
                    Boolean correctPlayerV = correctPlayer.getValue();
                    Boolean isInsertionPositionV = isInsertionPosition.getValue();
                    Boolean hover = hoverProperty.getValue();

                    // Create the helper variables
                    PlayerColor currentPlayer = gameStateV.currentPlayer();
                    boolean nextActionIsCorrect = gameStateV.nextAction() == GameState.Action.PLACE_TILE;
                    boolean groupHasTile = Objects.nonNull(placedTile.getValue());
                    boolean nullTile = tileV == null;
                    PlacedTile pt = nullTile ? null : new PlacedTile(tileV, currentPlayer, tileRotation.getValue(), pos);
                    boolean canAddTile = !nullTile && gameStateV.board().canAddTile(pt);
                    boolean couldPlaceTile = !nullTile && gameStateV.board().couldPlaceTile(tileV, pos);

                    // Get the color of the cell
                    Color color = Color.TRANSPARENT;
                    // If the tile is not in evidence, show it as black
                    if (!tilesInEvidenceV.isEmpty() && groupHasTile && !tilesInEvidenceV.contains(placedTileV.id()))
                        color = Color.BLACK;
                    // If the group has no tile and is in the insertion positions, try and show the fringe
                    if (correctPlayerV && !groupHasTile && nextActionIsCorrect && isInsertionPositionV) {
                        // If the tile is not hovered show the fringe
                        if (!hover && couldPlaceTile)
                            color = ColorMap.fillColor(Objects.requireNonNull(currentPlayer));
                        // If the tile is hovered and cannot be placed, white it out
                        else color = couldPlaceTile && !canAddTile ? Color.WHITE : Color.TRANSPARENT;
                    }

                    // Find the image
                    Image image;
                    boolean correctVeil = color != Color.BLACK;
                    // Reset the tile view to the correct tile if needed
                    if (groupHasTile)
                        image = cachedImages.computeIfAbsent(placedTileV.id(),
                                _ -> ImageLoader.normalImageForTile(placedTileV.tile()));
                    // Set the tile view to the next tile
                    else if (correctPlayerV && tilesInEvidenceV.isEmpty() && nextActionIsCorrect && correctVeil &&
                            hover && couldPlaceTile && isInsertionPositionV)
                        image = cachedImages.computeIfAbsent(tileV.id(), _ -> ImageLoader.normalImageForTile(tileV));
                    // Reset the tile view to the empty tile
                    else image = cachedImages.get(-1);

                    return new CellData(image, tileRotation.getValue(), color);
                }, placedTile, tilesInEvidence, tileRotation, hoverProperty, isInsertionPosition, tile, correctPlayer);

                tileGroup.effectProperty().bind(cellData.map(_ ->
                    cachedVeils.computeIfAbsent(tileGroup, _ -> {
                        Blend blend = new Blend(BlendMode.SRC_OVER);
                        blend.setOpacity(0.5);
                        blend.topInputProperty().bind(cellData.map(CellData::color)
                                .map(cachedVeilColors::get));
                        return blend;
                    }))
                );

                // Boolean to track if animation is running for this cell
                BooleanProperty animWasExecuted = new SimpleBooleanProperty(false);

                // Bind the image and rotation
                tileView.imageProperty().bind(cellData.map(c -> {
                    if (gameState.getValue().nextAction() == GameState.Action.END_GAME && !animWasExecuted.get()) {
                        animWasExecuted.set(true);
                        createFadeOutTimeline(tileView).play();
                    }
                    return c.image();
                }));

                tileGroup.rotateProperty().bind(cellData.map(d -> d.rotation.degreesCW()));

                placedTile.addListener((o, oldValue, newValue) -> {
                    if (Objects.isNull(newValue) || newValue.equals(oldValue)) return;

                    // Create the animal UI if needed
                    for (Zone.Meadow z : newValue.meadowZones()) {
                        for (Animal animal : z.animals()) {
                            // Create the animal view
                            ImageView animalView = new ImageView();
                            animalView.setFitHeight(ImageLoader.MARKER_FIT_SIZE);
                            animalView.setFitWidth(ImageLoader.MARKER_FIT_SIZE);
                            animalView.setId(STR."marker_\{animal.id()}");
                            animalView.getStyleClass().add("marker");

                            animalView.visibleProperty().bind(o.map(_ -> {
                                if (gameState.getValue().nextAction() == GameState.Action.END_GAME) return false;
                                else return gameState.getValue().board().cancelledAnimals().contains(animal);
                            }));

                            if (!tileGroup.getChildren().contains(animalView))
                                tileGroup.getChildren().add(animalView);
                        }
                    }

                    // Create the occupant UI if needed, and cache it
                    for (Occupant occupant : newValue.potentialOccupants()) {
                        cachedOccupants.computeIfAbsent(occupant, _ -> {
                            // Create the occupant view
                            Node occupantView = Icon.newFor(newValue.placer(), occupant);
                            occupantView.setId(STR."\{occupant.kind().toString().toLowerCase()}_\{occupant.zoneId()}");
                            occupantView.setRotate(tileRotation.getValue().negated().degreesCW());

                            // Handle the click event
                            occupantView.setOnMouseClicked(e -> {
                                if (e.getButton() == MouseButton.PRIMARY) selectOccupant.accept(occupant);
                            });
                            occupantView.rotateProperty().bind(tileGroup.rotateProperty().negate());
                            occupantView.visibleProperty().bind(visibleOccupants.map(s -> {
                                if (gameState.getValue().nextAction() == GameState.Action.END_GAME) return false;
                                else return s.contains(occupant);
                            }));
                            tileGroup.getChildren().add(occupantView);

                            return occupantView;
                        });
                    }
                    tileGroup.rotateProperty().unbind();
                });

                tileGroup.setOnMouseClicked(e -> {
                    // Check if we are in the right state to place the tile and if the position is valid
                    if (gameState.getValue().nextAction() != GameState.Action.PLACE_TILE ||
                            !gameState.getValue().board().insertionPositions().contains(pos) ||
                            !e.isStillSincePress())
                        return;

                    // Place the tile on left click and rotate it on right-click
                    if (e.getButton() == MouseButton.PRIMARY) placeTile.accept(pos);
                    else if (e.getButton() == MouseButton.SECONDARY) {
                        if (e.isAltDown()) rotateTile.accept(Rotation.RIGHT);
                        else rotateTile.accept(Rotation.LEFT);
                    }
                });

                grid.add(tileGroup, i + reach, j + reach);
            }
        }

        scrollPane.setHvalue(.5);
        scrollPane.setVvalue(.5);

        return scrollPane;
    }

    private static Timeline createFadeOutTimeline(Node node) {
        Timeline timeline = new Timeline();
        for (int index = 1; index <= 4; index++) {
            double opacity = 1.0 - (index * 1.0 / 4);
            Duration time = Duration.millis(index * 100);
            timeline.getKeyFrames().add(new KeyFrame(time, new KeyValue(node.opacityProperty(), opacity)));
        }
        return timeline;
    }

    /**
     * Represents the json of a cell
     * @param image The image of the cell
     * @param rotation The rotation of the image
     * @param color The color of the cell
     */
    private record CellData(Image image, Rotation rotation, Color color) {}
}
