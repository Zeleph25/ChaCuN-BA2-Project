package ch.epfl.chacun;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents the decks of tiles used during the game.
 * @param startTiles The first tile(s) of the game
 * @param normalTiles The tiles that will be played during the game
 * @param menhirTiles The tiles that will be played during the game and contain a menhir
 * @author Antoine Bastide (375407)
 */
public record TileDecks(List<Tile> startTiles, List<Tile> normalTiles, List<Tile> menhirTiles) {
    /**
     * Used to construct a deck of tiles
     * @param startTiles The first tile(s) of the game
     * @param normalTiles The tiles that will be played during the game
     * @param menhirTiles The tiles that will be played during the game and contain a menhir
     */
    public TileDecks {
        // Make the parameters unmodifiable
        startTiles = List.copyOf(startTiles);
        normalTiles = List.copyOf(normalTiles);
        menhirTiles = List.copyOf(menhirTiles);
    }

    /**
     * Used to get the size of the deck of tiles of a certain kind
     * @param kind the kind of the tiles
     * @return The number of tiles of the kind
     */
    public int deckSize(Tile.Kind kind) {
        return switch (kind) {
            case START -> startTiles.size();
            case NORMAL -> normalTiles.size();
            case MENHIR -> menhirTiles.size();
        };
    }

    /**
     * Used to get the top tile of the deck of a certain kind
     * @param kind the kind of the tiles
     * @return The top tile of the deck
     */
    public Tile topTile(Tile.Kind kind) {
        return switch (kind) {
            case START -> !startTiles.isEmpty() ? startTiles.getFirst() : null;
            case NORMAL -> !normalTiles.isEmpty() ? normalTiles.getFirst() : null;
            case MENHIR -> !menhirTiles.isEmpty() ? menhirTiles.getFirst() : null;
        };
    }

    /**
     * Used to get a new deck without the top tile of the deck of a certain kind
     * @param kind the kind of the tiles
     * @return a new deck with the top tile of the deck drawn
     * @throws IllegalArgumentException If the deck of the kind is empty
     */
    public TileDecks withTopTileDrawn(Tile.Kind kind) {
        // Check if the deck we want to draw from is empty
        Preconditions.checkArgument(Objects.nonNull(topTile(kind)));
        // Draw the top tile and return the new deck
        return switch (kind) {
            case START -> new TileDecks(startTiles.subList(1, startTiles.size()), normalTiles, menhirTiles);
            case NORMAL -> new TileDecks(startTiles, normalTiles.subList(1, normalTiles.size()), menhirTiles);
            case MENHIR -> new TileDecks(startTiles, normalTiles, menhirTiles.subList(1, menhirTiles.size()));
        };
    }

    /**
     * Used to get a new deck without the top tiles of the deck of a certain kind drawn until a certain condition is met
     * @param kind the kind of the tiles
     * @param predicate the condition to stop drawing the top tiles
     * @return A new deck with the top tiles of the deck drawn until the predicate is true
     */
    public TileDecks withTopTileDrawnUntil(Tile.Kind kind, Predicate<Tile> predicate) {
        // Draw the top tiles until the predicate is true
        List<Tile> newTiles = (switch (kind) {
            case START -> startTiles;
            case NORMAL -> normalTiles;
            case MENHIR -> menhirTiles;
        }).stream().dropWhile(predicate.negate()).toList();
        return new TileDecks(kind == Tile.Kind.START ? newTiles : startTiles,
                             kind == Tile.Kind.NORMAL ? newTiles : normalTiles,
                             kind == Tile.Kind.MENHIR ? newTiles : menhirTiles);
    }
}
