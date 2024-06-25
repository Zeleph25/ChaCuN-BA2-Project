package ch.epfl.chacun;

/**
 * Represents the four types of partitions: Forest, Meadow, River and Water
 * @param forests The partition of the forests
 * @param meadows The partition of the meadows
 * @param rivers The partition of the rivers
 * @param riverSystems The partition of the river systems (rivers and lakes that are connected to each other)
 * @author Adam Bekkar (379476)
 */
public record ZonePartitions(ZonePartition<Zone.Forest> forests, ZonePartition<Zone.Meadow> meadows,
                             ZonePartition<Zone.River> rivers, ZonePartition<Zone.Water> riverSystems) {
    /**Empty ZonePartitions */
    public final static ZonePartitions EMPTY = new ZonePartitions(
            new ZonePartition<>(), new ZonePartition<>(),
            new ZonePartition<>(), new ZonePartition<>()
    );

    /** Represents the builder of the ZonePartitions */
    public final static class Builder {
        /** The maximum number of zones */
        private static final int MAX_ZONES = 10;
        /** The builder for the forest partition */
        private final ZonePartition.Builder<Zone.Forest> forests;
        /** The builder for the meadow partition */
        private final ZonePartition.Builder<Zone.Meadow> meadows;
        /** The builder for the river partition */
        private final ZonePartition.Builder<Zone.River> rivers;
        /** The builder for the river system partition */
        private final ZonePartition.Builder<Zone.Water> riverSystems;

        /** Used to construct the builder of a ZonePartitions */
        public Builder(ZonePartitions initial) {
            forests = new ZonePartition.Builder<>(initial.forests());
            meadows = new ZonePartition.Builder<>(initial.meadows());
            rivers = new ZonePartition.Builder<>(initial.rivers());
            riverSystems = new ZonePartition.Builder<>(initial.riverSystems());
        }

        /**
         * Used to add a tile to the game by adding its zones to the partitions with the
         * correct number of open connections and by connecting the rivers to their lakes if there are any
         * @param tile The tile to add to the partitions
         */
        public void addTile(Tile tile) {
            int[] openConnexions = new int[MAX_ZONES];

            // Count the number of open connexions for each zone of id i
            // and add it to the position i of the openConnexions array
            for (TileSide side : tile.sides())
                for (Zone zone : side.zones()) {
                    openConnexions[zone.localId()]++;
                    if (zone instanceof Zone.River river && river.hasLake()) {
                        openConnexions[river.lake().localId()]++;
                        openConnexions[river.localId()]++;
                    }
                }

            // Add the zones to the different partitions
            for (Zone zone : tile.zones())
                switch (zone) {
                    case Zone.Forest forest -> forests.addSingleton(forest, openConnexions[zone.localId()]);
                    case Zone.Meadow meadow -> meadows.addSingleton(meadow, openConnexions[zone.localId()]);
                    case Zone.Lake lake -> riverSystems.addSingleton(lake, openConnexions[zone.localId()]);
                    case Zone.River river -> {
                        riverSystems.addSingleton(river, openConnexions[zone.localId()]);
                        rivers.addSingleton(river, openConnexions[zone.localId()] + (river.hasLake() ? -1 : 0));
                    }
                }

            // If there is a river with a lake, connect the river and the lake in the river system partitions
            for (Zone zone : tile.zones())
                if (zone instanceof Zone.River river && river.hasLake())
                    riverSystems.union(river, river.lake());
        }

        /**
         * Used to connect two sides of a tile by connecting the areas that contain them
         * @param s1 the first side to connect
         * @param s2 the second side to connect
         * @throws IllegalArgumentException If the sides are not of the same kind
         */
        public void connectSides(TileSide s1, TileSide s2) {
            // Check if the sides are of the same kind then connect the zones that contain them
            // If the sides are not of the same kind, throw an exception
            switch (s1) {
                case TileSide.Forest(Zone.Forest forest1)
                        when s2 instanceof TileSide.Forest(Zone.Forest forest2) ->
                        forests.union(forest1, forest2);
                case TileSide.Meadow(Zone.Meadow meadow1)
                        when s2 instanceof TileSide.Meadow(Zone.Meadow meadow2) ->
                        meadows.union(meadow1, meadow2);
                case TileSide.River(Zone.Meadow m11, Zone.River river1, Zone.Meadow m12)
                        when s2 instanceof TileSide.River(Zone.Meadow m21, Zone.River river2, Zone.Meadow m22) -> {
                    // Connect the areas that contain the rivers and the meadows that encircle them
                    meadows.union(m11, m22);
                    rivers.union(river1, river2);
                    meadows.union(m12, m21);

                    // Connect the river system areas that contain river1 and river2
                    riverSystems.union(river1, river2);
                }
                default -> throw new IllegalArgumentException();
            }
        }

        /**
         * Used to add an initial occupant (of the specified kind that belongs to the specified player
         * and zone) to the area that contains the zone
         * @param player The color of the player that owns the occupant
         * @param oK The kind of the occupant
         * @param occupiedZone The zone where the new area of the occupant is
         * @throws IllegalArgumentException If the occupant cannot occupy the given zone of the tile
         */
        public void addInitialOccupant(PlayerColor player, Occupant.Kind oK, Zone occupiedZone) {
            // Check if the occupant can occupy the given zone of the tile. If so, add an
            // initial occupant to the area that contains the zone, or throw an exception
            switch (occupiedZone) {
                case Zone.Forest forest when oK == Occupant.Kind.PAWN -> forests.addInitialOccupant(forest, player);
                case Zone.Meadow meadow when oK == Occupant.Kind.PAWN -> meadows.addInitialOccupant(meadow, player);
                case Zone.River river when oK == Occupant.Kind.PAWN -> rivers.addInitialOccupant(river, player);
                case Zone.Water water when oK == Occupant.Kind.HUT -> riverSystems.addInitialOccupant(water, player);
                default -> throw new IllegalArgumentException();
            }
        }

        /**
         * Used to remove a pawn of the specified color from the area that contains the zone
         * @param player The color of the player that owns the pawn
         * @param occupiedZone The zone where the pawn is
         * @throws IllegalArgumentException If the pawn cannot be removed from the given zone of the tile
         * (it's the case if the zone is a lake because it can have a hut but no pawn)
         */
        public void removePawn(PlayerColor player, Zone occupiedZone) {
            // Check the kind of the zone then remove the pawn from the corresponding area of the builder
            switch (occupiedZone) {
                case Zone.Forest forest -> forests.removeOccupant(forest, player);
                case Zone.Meadow meadow -> meadows.removeOccupant(meadow, player);
                case Zone.River river -> rivers.removeOccupant(river, player);
                default -> throw new IllegalArgumentException();
            }
        }

        /** Used to remove all the occupants from the forest area */
        public void clearGatherers(Area<Zone.Forest> forest) {
            forests.removeAllOccupantsOf(forest);
        }

        /** Used to remove all the occupants from the river area */
        public void clearFishers(Area<Zone.River> river) {
            rivers.removeAllOccupantsOf(river);
        }

        /** Used to build the ZonePartitions */
        public ZonePartitions build() {
            // Build each partition and return the ZonePartitions
            return new ZonePartitions(forests.build(), meadows.build(), rivers.build(), riverSystems.build());
        }
    }
}