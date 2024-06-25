package ch.epfl.chacun;

import java.util.*;

/**
 * Represents a text maker in French
 * @author Adam BEKKAR (379476)
 */
public final class TextMakerFr implements TextMaker {
    /** The map that associates the player colors to their names */
    private final Map<PlayerColor, String> playerNames;

    /**
     * Used to construct a TextMakerFr
     * @param playerNames The map of player colors to their names
     */
    public TextMakerFr(Map<PlayerColor, String> playerNames) {
        this.playerNames = playerNames;
    }

    // ----------------------- Overridden Methods ----------------------- //
    @Override
    public String playerName(PlayerColor playerColor) {
        return playerNames.getOrDefault(playerColor, null);
    }

    @Override
    public String points(int points) {
        Preconditions.checkArgument(points >= 0);
        return STR."\{points} point\{points > 1 ? "s" : ""}";
    }

    @Override
    public String playerClosedForestWithMenhir(PlayerColor player) {
        Preconditions.checkArgument(playerNames.containsKey(player));
        return STR."\{playerName(player)} a fermé une forêt contenant un menhir et peut donc placer une tuile menhir.";
    }

    @Override
    public String playersScoredForest(Set<PlayerColor> scorers, int points, int mushroomGroupCount, int tileCount) {
        Preconditions.checkArgument(scorers.stream().allMatch(playerNames::containsKey));
        return tallyPointsByMajority(scorers, points, "forêt", tileCount, mushroomGroupCount, 0,
                0, null, null);
    }

    @Override
    public String playersScoredRiver(Set<PlayerColor> scorers, int points, int fishCount, int tileCount) {
        Preconditions.checkArgument(scorers.stream().allMatch(playerNames::containsKey));
        return tallyPointsByMajority(scorers, points, "rivière", tileCount, 0, fishCount, 0,
                null, null);
    }

    @Override
    public String playerScoredHuntingTrap(PlayerColor scorer, int points, Map<Animal.Kind, Integer> animals) {
        Preconditions.checkArgument(playerNames.containsKey(scorer));

        Map<Animal.Kind, Integer> withoutTigers = new HashMap<>(animals);
        withoutTigers.remove(Animal.Kind.TIGER);
        return tallyPointsByClosing(scorer, points, "pré", nameOfSpecialPower(Zone.SpecialPower.HUNTING_TRAP),
                0, withoutTigers);
    }

    @Override
    public String playerScoredLogboat(PlayerColor scorer, int points, int lakeCount) {
        return tallyPointsByClosing(scorer, points, "réseau hydrographique",
                nameOfSpecialPower(Zone.SpecialPower.LOGBOAT), lakeCount, null);
    }

    @Override
    public String playersScoredMeadow(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals) {
        Map<Animal.Kind, Integer> withoutTigers = new HashMap<>(animals);
        withoutTigers.remove(Animal.Kind.TIGER);
        return tallyPointsByMajority(scorers, points, "pré", 0, 0, 0,
                0, withoutTigers, null);
    }

    @Override
    public String playersScoredRiverSystem(Set<PlayerColor> scorers, int points, int fishCount) {
        Preconditions.checkArgument(scorers.stream().allMatch(playerNames::containsKey));

        return tallyPointsByMajority(scorers, points, "réseau hydrographique", 0,
                0, fishCount, 0, null, null);
    }

    @Override
    public String playersScoredPitTrap(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals) {
        Preconditions.checkArgument(scorers.stream().allMatch(playerNames::containsKey));

        Map<Animal.Kind, Integer> withoutTigers = new HashMap<>(animals);
        withoutTigers.remove(Animal.Kind.TIGER);
        return tallyPointsByMajority(scorers, points, "pré", 0, 0,
                0, 0, withoutTigers, nameOfSpecialPower(Zone.SpecialPower.PIT_TRAP));
    }

    @Override
    public String playersScoredRaft(Set<PlayerColor> scorers, int points, int lakeCount) {
        Preconditions.checkArgument(scorers.stream().allMatch(playerNames::containsKey));
        return tallyPointsByMajority(scorers, points, "réseau hydrographique", 0, 0,
                0, lakeCount, null, nameOfSpecialPower(Zone.SpecialPower.RAFT));
    }

    @Override
    public String playersWon(Set<PlayerColor> winners, int points) {
        Preconditions.checkArgument(!winners.isEmpty() && winners.stream().allMatch(playerNames::containsKey));
        return STR."\{playersString(winners)} \{winners.size() > 1 ? "ont" : "a"} remporté la partie avec \{points(points)} !";
    }

    @Override
    public String clickToOccupy() {
        return "Cliquez sur le pion ou la hutte que vous désirez placer, ou ici pour ne pas en placer.";
    }

    @Override
    public String clickToUnoccupy() {
        return "Cliquez sur le pion que vous désirez reprendre, ou ici pour ne pas en reprendre.";
    }

    @Override
    public String waitForPlayer(String playerName) {
        return STR."Veuillez attendre que \{playerName} ait fini de jouer.";
    }

    @Override
    public String withPlacedTile(String playerName, PlacedTile placedTile) {
        return STR."\{playerName} a placé la tuile \{placedTile.id()} en \{placedTile.pos()}.";
    }

    @Override
    public String withOccupant(String playerName, Occupant occupant) {
        if (Objects.isNull(occupant)) return STR."\{playerName} a décidé de ne pas placer d'occupant.";
        return STR."\{playerName} a placé un\{occupant.kind() == Occupant.Kind.HUT ? "e hutte" : " pion"}"
                + STR." en \{occupant.zoneId()}.";
    }

    @Override
    public String withRetakePawn(String playerName, Occupant occupant) {
        if (Objects.isNull(occupant)) return STR."\{playerName} a décidé de ne pas reprendre un pion.";
        return STR."\{playerName} a repris son pion.";
    }
    // ----------------------- Overridden Methods ----------------------- //


    /**
     * Used to create the final text of a message declaring that a player has won points by closing a zone
     * @param player The player who has won the points
     * @param points The points won, if any
     * @param area The area that has been closed, if any
     * @param specialPower The special power used to close the area, if any
     * @param lakeCount The number of lakes in the river system, if any
     * @param animals The animals present in the meadow, if any
     * @return The final text of the message
     */
    private String tallyPointsByClosing(PlayerColor player, int points, String area, String specialPower,
                                        int lakeCount, Map<Animal.Kind, Integer> animals) {
        return new Builder().playersHasWonPoints(Set.of(player), points)
                .hasPlacedSpecialPowerTile(specialPower)
                .infoAboutSpecialArea(area, specialPower, lakeCount, animals)
                .build();
    }

    /**
     * Used to create the final text of a message declaring that a player
     * has won points by having majority occupants in an area
     * @param players The players who have won the points
     * @param points The points won
     * @param area The area in which the majority occupants are, if any
     * @param tileCount The number of tiles in the area, if any
     * @param mushroom The number of mushroom groups in the area, if any
     * @param fishCount The number of fish in the area, if any
     * @param lakeCount The number of lakes in the area, if any
     * @param animals The animals present in the area, if any
     * @param specialPower The special power, if any
     * @return The final text of the message declaring the points won by majority occupants
     */
    private String tallyPointsByMajority(Collection<PlayerColor> players, int points, String area, int tileCount,
                                         int mushroom, int fishCount, int lakeCount, Map<Animal.Kind, Integer> animals,
                                         String specialPower) {
        return new Builder().playersHasWonPoints(players, points)
                .majorityOccupants(players)
                .occupiedAreaTiles(area, tileCount)
                .contains(tileCount, mushroom, fishCount, lakeCount, animals, specialPower)
                .build();
    }

    /**
     * Represents the builder of the text of a message. Note that the methods of this class are used
     * to create the intermediate text of a message and that for correct behavior the methods must be
     * called in the correct order, which is the order in which they are declared in the TextMaker.Builder class
     */
    private final class Builder {
        /** The StringBuilder used to create the message */
        private final StringBuilder stringBuilder;

        /** Used ot construct a Builder */
        public Builder() {
            stringBuilder = new StringBuilder();
        }

        /**
         * Used to create the intermediate text of a message declaring that some players have won points
         * @param players The players who have won the points
         * @param points The points won
         * @return The intermediate text of the message declaring the points won by the players
         */
        private Builder playersHasWonPoints(Collection<PlayerColor> players, int points) {
            stringBuilder.append(STR."\{playersString(players)}\{players.size() > 1 ? " ont" : " a"} ")
                    .append(STR."remporté \{points(points)}");
            return this;
        }

        // ------------------------ Majority Occupants Part ------------------------ //
        /**
         * Used to create the intermediate text of a message declaring that a player has majority occupants in an area
         * @param players The players who have majority occupants in the area
         * @return The intermediate text of the message declaring the majority occupants
         */
        private Builder majorityOccupants(Collection<PlayerColor> players) {
            stringBuilder.append(STR." en tant qu'occupant·e\{players.size() > 1 ? "·s" : ""}")
                    .append(STR." majoritaire\{players.size() > 1 ? "s" : ""}");
            return this;
        }

        /**
         * Used to create the intermediate text of a message declaring the number of tiles in an area
         * @param area The area that has been occupied
         * @param tileCount The number of tiles in the area, if any
         * @return The intermediate text of the message declaring the number of tiles in the area
         */
        private Builder occupiedAreaTiles(String area, int tileCount) {
            boolean feminine = area.equals("forêt") || area.equals("rivière");
            String tile = STR."un\{feminine ? "e" : ""} \{area}";
            String composedOf = STR."\{tileCount > 0 ? STR." composé\{feminine ? "e" : ""} de"
                    + STR." \{tileCount} tuile\{tileCount > 1 ? "s" : ""}" : ""}";
            stringBuilder.append(STR." d'\{tile}\{composedOf}");
            return this;
        }

        /**
         * Used to create the intermediate text of a message declaring that an area contains certain elements
         * @param mushroomGroupCount The number of mushroom groups in the area, if any
         * @param fishCount The number of fish in the area, if any
         * @param lakeCount The number of lakes in the area, if any
         * @param animals The animals present in the area, if any
         * @param specialPower The special power used to close the area, if any
         * @return The intermediate text of the message declaring the elements in the area
         */
        private Builder contains(int tileCount, int mushroomGroupCount, int fishCount, int lakeCount, Map<Animal.Kind,
                Integer> animals, String specialPower) {
            // We use a StringJoiner instead of StringBuilders for the case
            // where two of the three elements are non-empty, if it exists
            String containsMessage = "";

            // Add the animals, fish and mushrooms to the message
            if (fishCount > 0) containsMessage += STR." \{tileCount > 0 ? "et " : ""}contenant \{fishCount} poisson\{fishCount > 1 ? "s" : ""}";
            else if (mushroomGroupCount > 0)
                containsMessage += STR." et de \{mushroomGroupCount} groupe\{mushroomGroupCount > 1 ? "s" : ""}"
                        + " de champignons";
            else if (Objects.equals(specialPower, nameOfSpecialPower(Zone.SpecialPower.RAFT))) {
                String withLakes = lakeCount > 0 ? STR." et \{lakeCount} lac\{lakeCount > 1 ? "s" : ""}" : "";
                containsMessage += STR." contenant le radeau\{withLakes}";
            }
            else if (Objects.equals(specialPower, nameOfSpecialPower(Zone.SpecialPower.PIT_TRAP))) {
                String withAnimalsAround = !animals.isEmpty() ? STR." entourée de \{animalsString(animals)}" : "";
                containsMessage += STR." contenant la grande fosse à pieux\{withAnimalsAround}";
            }
            else if (Objects.nonNull(animals) && !animals.isEmpty())
                containsMessage += STR." contenant \{animalsString(animals)}";

            stringBuilder.append(containsMessage);
            return this;
        }
        // ------------------------ Majority Occupants Part ------------------------ //

        // -------------------------- Special Power Part -------------------------- //
        /**
         * Used to create the intermediate text of a message declaring that a player
         * has placed a tile with special power
         * @param specialPower The special power of the tile, if any
         * @return The intermediate text of the message declaring the special power of the tile
         */
        private Builder hasPlacedSpecialPowerTile(String specialPower) {
            String pronoun = specialPower.equals(nameOfSpecialPower(Zone.SpecialPower.RAFT)) ? "le" : "la";
            stringBuilder.append(STR." en plaçant \{pronoun} \{specialPower}");
            return this;
        }

        /**
         * Used to create the final text of a message giving information about a special area
         * @param area The area in which the special tile has been placed
         * @param specialPower The special power of the tile, if any
         * @param lakeCount The number of lakes in the area, if any
         * @param animals The animals present in the area, if any
         * @return The intermediate text of the message giving information about the special area
         */
        private Builder infoAboutSpecialArea(String area, String specialPower, int lakeCount,
                                             Map<Animal.Kind, Integer> animals) {
            String pronoun = area.equals("pré") || area.equals("réseau hydrographique") ? "un" : "une";
            String message = STR." dans \{pronoun} \{area}";
            if (specialPower.equals(nameOfSpecialPower(Zone.SpecialPower.HUNTING_TRAP))) {
                String string = STR." dans lequel elle est entourée de \{animalsString(animals)}";
                message += STR."\{!animals.isEmpty() ? string : ""}";
            }
            else if (specialPower.equals(nameOfSpecialPower(Zone.SpecialPower.LOGBOAT)) && lakeCount > 0)
                message += STR." contenant \{lakeCount} lac\{lakeCount > 1 ? "s" : ""}";

            stringBuilder.append(message);
            return this;
        }
        // -------------------------- Special Power Part -------------------------- //

        /**
         * Used to create the final text of a message with a final point at the end
         * @return The final text of the message
         */
        private String build() {
            return stringBuilder.append(".").toString();
        }
    }

    // ------------------------ Utility Methods ------------------------ //
    /**
     * Used to get the players sorted in the correct order
     * @param players The players to order
     * @return The players sorted in the correct order
     */
    private List<PlayerColor> orderedPlayers(Collection<PlayerColor> players) {
        return PlayerColor.ALL.stream().filter(players::contains).toList();
    }

    /**
     * Used to order the animals sorted in the correct order
     * @param animals The animals to order
     * @return The animals sorted ordered in the correct order
     */
    private TreeMap<Animal.Kind, Integer> orderedAnimals(Map<Animal.Kind, Integer> animals) {
        TreeMap<Animal.Kind, Integer> orderedAnimals = new TreeMap<>(Comparator.comparing(Animal.Kind::ordinal));
        orderedAnimals.putAll(animals);
        return orderedAnimals;
    }

    /**
     * Used to create a string with the names of the players and the correct separators
     * @param players The players to create the string with
     * @return The string with the names of the players and the correct separators
     */
    private String playersString(Collection<PlayerColor> players) {
        List<PlayerColor> orderedPlayers = orderedPlayers(players);
        Iterator<PlayerColor> it = orderedPlayers.iterator();

        if (orderedPlayers.size() == 1) return playerName(it.next());
        else if (orderedPlayers.size() == 2) return STR."\{playerName(it.next())} et \{playerName(it.next())}";
        else {
            StringJoiner stringJoiner = new StringJoiner(", ", "", "");
            while (it.hasNext()) {
                PlayerColor next =  it.next();
                if (next.ordinal() == orderedPlayers.getLast().ordinal()) break;
                stringJoiner.add(playerName(next));
            }
            return STR."\{stringJoiner} et \{playerName(orderedPlayers.getLast())}";
        }
    }

    /**
     * Used to create a string with the names of the animals and the correct separators
     * @param animals The animals to create the string with
     * @return The string with the names of the animals and the correct separators
     */
    private String animalsString(Map<Animal.Kind, Integer> animals) {
        TreeMap<Animal.Kind, Integer> orderedAnimals = orderedAnimals(animals);
        Iterator<Map.Entry<Animal.Kind, Integer>> it = orderedAnimals.entrySet().iterator();

        if (orderedAnimals.size() == 1) {
            Map.Entry<Animal.Kind, Integer> entry = it.next();

            Animal.Kind animal = entry.getKey();
            Integer count = entry.getValue();
            return STR."\{count} \{count > 1 ? STR."\{nameOfAnimal(animal)}s" : nameOfAnimal(animal)}";
        }
        else if (orderedAnimals.size() == 2) {
            Map.Entry<Animal.Kind, Integer> entry = it.next();
            Animal.Kind firstAnimal = entry.getKey();
            Integer firstCount = entry.getValue();

            entry = it.next();
            Animal.Kind secondAnimal = entry.getKey();
            Integer secondCount = entry.getValue();

            return (STR."\{firstCount} \{firstCount > 1 ? STR."\{nameOfAnimal(firstAnimal)}s" : nameOfAnimal(firstAnimal)} et ") +
                    (STR."\{secondCount} \{secondCount > 1 ? STR."\{nameOfAnimal(secondAnimal)}s" : nameOfAnimal(secondAnimal)}");
        }
        else {
            // Create the message with the animals
            StringJoiner stringJoiner = new StringJoiner(", ", "", "");
            while (it.hasNext()) {
                Map.Entry<Animal.Kind, Integer> entry = it.next();
                if (entry.getKey() == orderedAnimals.lastEntry().getKey()) break;

                Animal.Kind animal = entry.getKey();
                Integer count = entry.getValue();

                stringJoiner.add(STR."\{count} \{count > 1 ? STR."\{nameOfAnimal(animal)}s" : nameOfAnimal(animal)}");
            }
            Map.Entry<Animal.Kind, Integer> entry = orderedAnimals.lastEntry();
            Animal.Kind animal = entry.getKey();
            Integer count = entry.getValue();

            return stringJoiner + STR." et \{count} \{count > 1 ? STR."\{nameOfAnimal(animal)}s" : nameOfAnimal(animal)}";
        }
    }

    /**
     * Used to get the name of the animal in French from its kind
     * @param animal The kind of the animal
     * @return The name of the animal in French
     */
    private String nameOfAnimal(Animal.Kind animal) {
        // The names of the animals that can appear in a message in French
        Map<Animal.Kind, String> animalNames = Map.of(
                Animal.Kind.MAMMOTH, "mammouth",
                Animal.Kind.AUROCHS, "auroch",
                Animal.Kind.DEER, "cerf"
        );

        return animalNames.get(animal);
    }

    /**
     * Used to get the name of the special power in French from its kind
     * @param specialPower The kind of the special power
     * @return The name of the special power in French
     */
    private String nameOfSpecialPower(Zone.SpecialPower specialPower) {
        // The names of the special powers that can appear in a message in French
        Map<Zone.SpecialPower, String> specialPowerNames = Map.of(
                Zone.SpecialPower.LOGBOAT, "pirogue",
                Zone.SpecialPower.HUNTING_TRAP, "fosse à pieux",
                Zone.SpecialPower.PIT_TRAP, "grande fosse à pieux",
                Zone.SpecialPower.RAFT, "radeau"
        );

        return specialPowerNames.get(specialPower);
    }
    // ------------------------ Utility Methods ------------------------ //
}