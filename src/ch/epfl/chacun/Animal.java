package ch.epfl.chacun;

/**
 * Represents an animal
 * @param id The id of the animal
 * @param kind The kind of the animal
 * @author Adam BEKKAR (379476)
 */
public record Animal(int id, Kind kind) {
    /** The enum of the different types of animals the game has */
    public enum Kind { MAMMOTH, AUROCHS, DEER, TIGER }

    /**
     * Used to get the id of the tile where the animal is
     * @return The id of the tile where the animal is
     */
    public int tileId() {
       return Zone.tileId(id / 10);
    }
}