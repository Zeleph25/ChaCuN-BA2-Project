package ch.epfl.chacun;

import java.util.*;

/**
 * Represents the message board
 * @param textMaker The text maker used to create the messages
 * @param messages The messages on the message board
 * @author Antoine Bastide (375407)
 */
public record MessageBoard(TextMaker textMaker, List<Message> messages) {
    /**
     * Used to construct a message board
     * @param textMaker The text maker used to create the messages
     * @param messages The messages on the message board
     */
    public MessageBoard {
        messages = List.copyOf(messages);
    }

    /**
     * Used to create a message board with a new message
     * @param message The message to add to the message board
     */
    private MessageBoard withMessage(Message message) {
        List<Message> messages = new ArrayList<>(this.messages);
        messages.add(message);
        return new MessageBoard(textMaker, messages);
    }

    /**
     * Used to count the number of animals in a meadow area
     * @param animals The animals in the meadow area
     * @return The number of animals in the meadow area
     */
    private Map<Animal.Kind, Integer> animalCountMap(Set<Animal> animals) {
        Map<Animal.Kind, Integer> animalCount = new HashMap<>();
        animals.stream().map(Animal::kind)
                        .forEach(kind -> animalCount.merge(kind, 1, Integer::sum));
        return animalCount;
    }

    /**
     * Used to find the points scored by a player in a meadow area
     * @param animalCountMap The map that contains the number of each animal in the meadow area
     * @return The points scored by the player in the meadow area
     */
    private int findMeadowPoints(Map<Animal.Kind, Integer> animalCountMap) {
        return Points.forMeadow(animalCountMap.getOrDefault(Animal.Kind.MAMMOTH, 0),
                                animalCountMap.getOrDefault(Animal.Kind.AUROCHS, 0),
                                animalCountMap.getOrDefault(Animal.Kind.DEER, 0));
    }

    /**
     * Used to send a message map that contains the points scored by each player
     * @return The message map that contains the points scored by each player
     */
    public Map<PlayerColor, Integer> points() {
        // Create the message map and fill it with 0 points for each player
        Map<PlayerColor, Integer> messageMap = new HashMap<>();
        PlayerColor.ALL.forEach(c -> messageMap.put(c, 0));

        // Convert the list of messages to a map of points scored by each player
        messages.forEach(m -> m.scorers().forEach(s -> messageMap.merge(s, m.points(), Integer::sum)));
        messageMap.values().removeIf(v -> v == 0);
        return Map.copyOf(messageMap);
    }

    /**
     * Used to create a message declaring that the majority occupants
     * of a forest have received points for closing it
     * @param forest The forest that has been closed
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredForest(Area<Zone.Forest> forest) {
        if (!forest.isOccupied()) return this;

        // Compute the needed values to create the message
        Set<PlayerColor> majorityOccupants = forest.majorityOccupants();
        Set<Integer> tileIds = forest.tileIds();
        int mushroomCount = Area.mushroomGroupCount(forest);
        int points = Points.forClosedForest(tileIds.size(), mushroomCount);
        String text = textMaker.playersScoredForest(majorityOccupants, points, mushroomCount, tileIds.size());

        // Create the message and add it to the message board
        return withMessage(new Message(text, points, majorityOccupants, tileIds));
    }

    /**
     * Used to create a message declaring that a player has closed a forest with a menhir
     * @param player The player that has closed the forest
     * @param forest The forest that has been closed
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withClosedForestWithMenhir(PlayerColor player, Area<Zone.Forest> forest) {
        return withMessage(new Message(textMaker.playerClosedForestWithMenhir(player), 0, Set.of(), forest.tileIds()));
    }

    /**
     * Used to create a message declaring that a player has closed a river
     * @param river The river that has been closed
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredRiver(Area<Zone.River> river) {
        if (!river.isOccupied()) return this;

        // Compute the needed values to create the message
        Set<PlayerColor> majorityOccupants = river.majorityOccupants();
        Set<Integer> tileIds = river.tileIds();
        int fishCount = Area.riverFishCount(river);
        int points = Points.forClosedRiver(tileIds.size(), fishCount);
        String text = textMaker.playersScoredRiver(majorityOccupants, points, fishCount, tileIds.size());

        // Create the message and add it to the message board
        return withMessage(new Message(text, points, majorityOccupants, tileIds));
    }

    /**
     * Used to create a message declaring that a player has placed a hunting trap
     * @param scorer The player that has placed the hunting trap
     * @param adjacentMeadow The meadow area that the hunting trap is adjacent to
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredHuntingTrap(PlayerColor scorer, Area<Zone.Meadow> adjacentMeadow, Set<Animal> cancelledAnimals) {
        // Find all the animals in the meadow and count them (do not count the cancelled animals)
        Set<Animal> animals = Area.animals(adjacentMeadow, cancelledAnimals);
        Map<Animal.Kind, Integer> animalCountMap = animalCountMap(animals);
        int points = findMeadowPoints(animalCountMap);
        if (points <= 0) return this;

        // Create the message and add it to the message board
        String text = textMaker.playerScoredHuntingTrap(scorer, points, animalCountMap);
        return withMessage(new Message(text, points, Set.of(scorer), adjacentMeadow.tileIds()));
    }

    /**
     * Used to create a message declaring that a player has placed a logboat
     * @param scorer The player that has placed the logboat
     * @param riverSystem The river system area that the logboat is in
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredLogboat(PlayerColor scorer, Area<Zone.Water> riverSystem) {
        // Find the number of lakes in the river system
        int lakeCount = Area.lakeCount(riverSystem);
        int points = Points.forLogboat(lakeCount);
        // Create the message and add it to the message board
        return withMessage(new Message(textMaker.playerScoredLogboat(scorer, points, lakeCount), points, Set.of(scorer),
                riverSystem.tileIds()));
    }

    /**
     * Used to create a message declaring that a player has closed a meadow
     * @param meadow The meadow that has been closed
     * @param cancelledAnimals The animals that are cancelled out
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredMeadow(Area<Zone.Meadow> meadow, Set<Animal> cancelledAnimals) {
        // Find all the animals in the meadow and count them (do not count the cancelled animals)
        Set<Animal> animals = Area.animals(meadow, cancelledAnimals);
        Map<Animal.Kind, Integer> animalCountMap = animalCountMap(animals);
        int points = findMeadowPoints(animalCountMap);
        if (!meadow.isOccupied() || points <= 0) return this;

        // Create the message and add it to the message board
        String text = textMaker.playersScoredMeadow(meadow.majorityOccupants(), points, animalCountMap);
        return withMessage(new Message(text, points, meadow.majorityOccupants(), meadow.tileIds()));
    }

    /**
     * Used to create a message declaring that a player has closed a river system
     * @param riverSystem The river system that has been closed
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredRiverSystem(Area<Zone.Water> riverSystem) {
        // Find the number of fish in the river system and count the points
        int fishCount = Area.riverSystemFishCount(riverSystem);
        int points = Points.forRiverSystem(fishCount);
        if (!riverSystem.isOccupied() || points <= 0) return this;

        // Create the message and add it to the message board
        String text = textMaker.playersScoredRiverSystem(riverSystem.majorityOccupants(), points, fishCount);
        return withMessage(new Message(text, points, riverSystem.majorityOccupants(), riverSystem.tileIds()));
    }

    /**
     * Used to create a message declaring that a player has placed a pit trap
     * @param adjacentMeadow The meadow area that the pit trap is adjacent to
     * @param cancelledAnimals The animals that are cancelled out
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredPitTrap(Area<Zone.Meadow> adjacentMeadow, Set<Animal> cancelledAnimals) {
        // Find all the animals in the meadow and count them (do not count the cancelled animals)
        Map<Animal.Kind, Integer> animalCountMap = animalCountMap(Area.animals(adjacentMeadow, cancelledAnimals));
        int points = findMeadowPoints(animalCountMap);
        if (points <= 0 || !adjacentMeadow.isOccupied()) return this;

        // Create the message and add it to the message board
        String text = textMaker.playersScoredPitTrap(adjacentMeadow.majorityOccupants(), points, animalCountMap);
        return withMessage(new Message(text, points, adjacentMeadow.majorityOccupants(), adjacentMeadow.tileIds()));
    }

    /**
     * Used to create a message declaring that a player has placed a raft
     * @param riverSystem The river system area that the raft is in
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withScoredRaft(Area<Zone.Water> riverSystem) {
        int points = Points.forRaft(Area.lakeCount(riverSystem));
        if (!riverSystem.isOccupied() || points <= 0) return this;

        // Create the message and add it to the message board
        String text = textMaker.playersScoredRaft(riverSystem.majorityOccupants(), points, Area.lakeCount(riverSystem));
        return withMessage(new Message(text, points, riverSystem.majorityOccupants(), riverSystem.tileIds()));
    }

    /**
     * Used to create a message declaring that one or more players have won
     * @param winners The players that have won
     * @param points The number of points the players have scored
     * @return The updated message board with the new message added to it
     */
    public MessageBoard withWinners(Set<PlayerColor> winners, int points) {
        return withMessage(new Message(textMaker.playersWon(winners, points), 0, Set.of(), Set.of()));
    }

    /**
     * Represents a message on the message board
     * @param text The text of the message
     * @param points The number of points the player has scored
     * @param scorers The players that have scored the points
     * @param tileIds The ids of the tiles that make up the area
     * @author Antoine Bastide (375407)
     */
    public record Message(String text, int points, Set<PlayerColor> scorers, Set<Integer> tileIds) {
        /**
         * Used to construct a message on the message board
         * @param text The text of the message
         * @param points The number of points the players have scored
         * @param scorers The players that have scored the points
         * @param tileIds The id of the tiles that are concerned by the message
         */
        public Message {
            // Make sure the arguments are valid
            Objects.requireNonNull(text);
            Preconditions.checkArgument(points >= 0);

            // Make sure the sets are immutable
            scorers = Set.copyOf(scorers);
            tileIds = Set.copyOf(tileIds);
        }
    }
}
