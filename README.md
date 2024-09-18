# 🏆 Second Semester Project - EPFL

With my project partner Antoine Baside, we coded the second semester project of our first year at EPFL. This project is based on the board game "Hunters and Gatherers," derived from Carcassonne. Our version, named "Hunters and Gatherers in the Neolithic" (ChaCuN), differs slightly from the original.

## 📘 Introduction

The project aims to create an electronic version of the board game "Hunters and Gatherers." ChaCuN is designed for 2 to 5 players, where the goal is to build a prehistoric landscape by placing square tiles. The parts of the landscape (forests, rivers, etc.) can be occupied by hunters, gatherers, or fishermen to earn points.

## 🎮 Gameplay

Players draw a tile, place it to form a continuous landscape, and can occupy tiles with pawns or huts to score points. A game begins with a starting tile placed at the center of the play area. Players then place adjacent tiles to continue the landscape. Some tiles allow for additional plays or have special powers.

## 🏡 Occupation and Points

Players can occupy the tiles with pawns (hunters, gatherers, fishermen) or huts. Points are scored based on the completion of landscape elements (forests, rivers) or at the end of the game for meadows and water networks. Occupants enable players to score points and are returned to players once points are tallied.

## 🖥️ User Interface

### Objective
Enhance ChaCuN's graphical interface by developing components for displaying the game board.

### Game Board Layout
- **Location**: Left side of the interface, occupying the largest portion.
- **Structure**: 25x25 grid of square cells.
  - **Cell States**: 
    - Empty
    - Containing a tile with potential occupants
  - **Tile Display**: Non-highlighted tiles are dimmed when others are highlighted.

### Fringe Cells
- **Definition**: Empty cells adjacent to a tile, relevant for tile placement.
- **Appearance**: Colored with the current player's color.
- **Hover Effect**: Shows the image of the tile to be placed.
- **Placement Validation**: Invalid placements are veiled in white.
- **Tile Manipulation**:
  - **Right-click**: Rotates tile counterclockwise (clockwise with Alt key).
  - **Left-click**: Places the tile.

### Post-Placement Actions
- **Occupant Display**: Potential occupants appear on placed tiles.
- **Interaction**: 
  - Click on an occupant to place it.
  - Click on a pawn to retrieve it when placing a shaman tile.

### Special Cases
- **Canceled Animals**: Mark images of canceled animals to count them only once.

---
### Additional Features

The entire cohort had to code this project in pairs, but it was possible to code an additional part beyond this common section, some examples of which I will provide.

## Bot

The Bot makes decisions based on its difficulty level and adaptive strategy. Levels range from BABY to IMPOSSIBLE, determining the number of moves the Bot can foresee. The BABY bot plays randomly. Adaptive strategies (EARLY, MID, LATE) adjust decisions as the game progresses. The Bot evaluates free pawns and huts, board occupants, and placed tiles. Early game (EARLY) focuses on rapid expansion, while late game (LATE) optimizes points and limits opponent opportunities. This allows the Bot to play flexibly and effectively.

### Key Methods
- **play**: Executes the best action based on the game state and adaptive strategy.
- **playRandomAction**: Plays a random action (BABY level).
- **simulateGame**: Simulates the game over several moves to determine the best action.

## Endgame Animation

The endgame animation provides a dynamic conclusion. Board tiles shrink, allowing more to be displayed, and change colors multiple times per second in waves from the top-left to bottom-right. This movement slows down, creating suspense. Finally, the screen displays the winner's score in their color. If multiple players win, the screen becomes multicolored.

## Last Move Highlight

This component highlights the last action and its impact on the board. Visible only during the game, it helps track actions when the board becomes large. A specific class was created to simplify the message board for this feature.

## JSON Parser

We reused a JSON parser written in the first semester to implement the next two extensions, as they return JSON strings. Java does not have a built-in JSON parser.

The parser includes:
- **JSONValue**: The base of the parser, containing a string value.
- **JSONArray**: Extends JSONValue, representing a list of JSONValues.
- **JSONObject**: Extends JSONValue, representing a map with string keys and JSONValue values.
- **JSONUtils**: Contains utility methods for the parser.
- **JSONParser**: Reads/writes JSON to files.

## Remote Play

To implement remote play, we used a Realtime Database. This choice made connecting to a game easier and added features on top of remote play, including:
- Rejoining a game if accidentally disconnected.
- Replacing a player with a bot if they leave the game.

We used HTTPS get, put, and delete requests via a class named `Database`. These requests return `Response` objects, containing the response body and status code, simplifying response handling in our code.

## Authentication

We extended `Database` with an `Authentication` class to create, log in, log out, and delete accounts. An account requires a username and password, creating a unique player ID (UUID). This ID allows automatic reconnection, rejoining games, and changing the game host if necessary.

## Interface

We added several interfaces for user ergonomics, including scenes for:
- General authentication (create or log in to an account).
- Account creation and login.
- Main menu with buttons to access other scenes.
- Joining a game, displaying existing games with status and refresh button.
- Creating a game, asking for a game name.
- Displaying game rules.
- Viewing player statistics.
- Waiting room, where the host can set the number of cards and bot difficulty if needed.
- Creating a game against a bot, similar to the waiting room, without database communication.


These features add an extra layer of complexity and enjoyment to the game.

Little Note : This project was completed during the second semester of my first year at EPFL. I prefer to leave the project as it is, to have a reference point for my skill level during this time. Please refrain from judging the quality of the code.
