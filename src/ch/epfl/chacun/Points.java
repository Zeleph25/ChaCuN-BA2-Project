package ch.epfl.chacun;

/**
 * Represents points in different situations of the game
 * @author Antoine Bastide (375407)
 */
public final class Points {
    /** The minimum number of tiles needed to gain points */
    private static final int MIN_TILE_COUNT = 2;
    /** The minimum number of lakes needed to gain points */
    private static final int MIN_LAKE_COUNT = 1;
    /** The minimum number of features needed to gain points */
    private static final int MIN_FEATURE_COUNT = 0;

    /** Private constructor to prevent instantiation */
    private Points() {}

    /**
     * Used to compute the points for a closed forest
     * @param tileCount The number of tiles in the forest
     * @param mushroomGroupCount The number of mushroom groups in the forest
     * @return The number of points gained
     * @throws IllegalArgumentException If the tile count or the mushroom group count is less than the minimum
     */
    public static int forClosedForest(int tileCount, int mushroomGroupCount) {
        // Make sure all the arguments are valid
        Preconditions.checkArgument(tileCount >= MIN_TILE_COUNT && mushroomGroupCount >= MIN_FEATURE_COUNT);

        // Return the correct number of points
        return tileCount * 2 + mushroomGroupCount * 3;
    }

    /**
     * Used to compute the points for a closed river
     * @param tileCount The number of tiles in the river
     * @param fishCount The number of fish in the river
     * @return The number of points gained
     * @throws IllegalArgumentException If the tile count or the fish count is less than the minimum
     */
    public static int forClosedRiver(int tileCount, int fishCount) {
        // Make sure all the arguments are valid
        Preconditions.checkArgument(tileCount >= MIN_TILE_COUNT && fishCount >= MIN_FEATURE_COUNT);

        // Return the correct number of points
        return tileCount + fishCount;
    }

    /**
     * Used to compute the points for a closed meadow
     * @param mammothCount The number of mammoths in the meadow
     * @param aurochCount The number of aurochs in the meadow
     * @param deerCount The number of deers in the meadow
     * @return The number of points gained
     * @throws IllegalArgumentException If the mammoth count, auroch count or deer count is less than the minimum
     */
    public static int forMeadow(int mammothCount, int aurochCount, int deerCount) {
        // Make sure all the arguments are valid
        Preconditions.checkArgument(mammothCount >= MIN_FEATURE_COUNT);
        Preconditions.checkArgument(aurochCount >= MIN_FEATURE_COUNT);
        Preconditions.checkArgument(deerCount >= MIN_FEATURE_COUNT);

        // Return the correct number of points
        return mammothCount * 3 + aurochCount * 2 + deerCount;
    }

    /**
     * Used to compute the points for a closed lake
     * @param fishCount The number of fish in the lake
     * @return The number of points gained
     * @throws IllegalArgumentException If the fish count is less than the minimum
     */
    public static int forRiverSystem(int fishCount) {
        // Make sure all the arguments are valid
        Preconditions.checkArgument(fishCount >= MIN_FEATURE_COUNT);

        // Return the correct number of points
        return fishCount;
    }

    /**
     * Used to compute the points when a log boat is placed in a lake
     * @param lakeCount The number of lake tiles
     * @return The number of points gained
     * @throws IllegalArgumentException If the lake count is less than the minimum
     */
    public static int forLogboat(int lakeCount) {
        // Make sure all the arguments are valid
        Preconditions.checkArgument(lakeCount >= MIN_LAKE_COUNT);

        // Return the correct number of points
        return lakeCount * 2;
    }

    /**
     * Used to compute the points when a raft is placed in a lake
     * @param lakeCount The number of lake tiles
     * @return The number of points gained
     * @throws IllegalArgumentException If the lake count is less than the minimum
     */
    public static int forRaft(int lakeCount) {
        // Make sure all the arguments are valid
        Preconditions.checkArgument(lakeCount >= MIN_LAKE_COUNT);

        // Return the correct number of points
        return lakeCount;
    }
}
