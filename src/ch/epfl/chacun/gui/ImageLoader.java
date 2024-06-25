package ch.epfl.chacun.gui;

import ch.epfl.chacun.Tile;
import javafx.scene.image.Image;

/**
 * Used to load images to the game
 * @author Antoine Bastide (375407)
 */
public class ImageLoader {
    /** Size of large tiles in pixels */
    public final static int LARGE_TILE_PIXEL_SIZE = 512;
    /** Size of large tiles in the game */
    public final static int LARGE_TILE_FIT_SIZE = 256;
    /** Size of normal tiles in pixels */
    public final static int NORMAL_TILE_PIXEL_SIZE = 256;
    /** Size of normal tiles in the game */
    public final static int NORMAL_TILE_FIT_SIZE = 128;
    /** Size of the marker */
    public final static int MARKER_PIXEL_SIZE = 96;
    /** Size of the marker in the game */
    public final static int MARKER_FIT_SIZE = 48;

    /** Private constructor to prevent instantiation */
    private ImageLoader() {}

    /**
     * Used to load the normal image of a tile
     * @param tile The tile to load the image from
     * @return The image of the tile
     */
    public static Image normalImageForTile(Tile tile) {
        return getImage(tile, false);
    }

    /**
     * Used to load the large image of a tile
     * @param tile The tile to load the image from
     * @return The image of the tile
     */
    public static Image largeImageForTile(Tile tile) {
        return getImage(tile, true);
    }

    /**
     * Used to load the image of a tile
     * @param tile The tile to load the image from
     * @param large Whether the image should be large or not
     * @return The image of the tile
     */
    private static Image getImage(Tile tile, boolean large) {
        return new Image(
                STR."/\{large ? LARGE_TILE_PIXEL_SIZE : NORMAL_TILE_PIXEL_SIZE}/" +
                STR."\{tile.id() < 10 ? "0" : ""}\{tile.id()}.jpg"
        );
    }
}
