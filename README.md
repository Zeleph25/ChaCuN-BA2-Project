# 🏆 Second Semester Project - EPFL

With my project partner Antoine Baside, we coded the second semester project of our first year at EPFL. This project is based on the board game "Hunters and Gatherers," derived from Carcassonne. Our version, named "Hunters and Gatherers in the Neolithic" (ChaCuN), differs slightly from the original.

## 📘 Introduction

The project aims to create an electronic version of the board game "Hunters and Gatherers." ChaCuN is designed for 2 to 5 players, where the goal is to build a prehistoric landscape by placing square tiles. The parts of the landscape (forests, rivers, etc.) can be occupied by hunters, gatherers, or fishermen to earn points.

# 🎮 Gameplay

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

- **Advanced Bot Logic**: Our bot can adjust its strategy based on the game's progress, making it more challenging and fun to play against.
- **Endgame Animation**: We implemented a dynamic endgame animation where the tiles shrink and change colors in waves, creating a suspenseful effect. The final screen displays the winner's score and color, or a multicolored screen if there are multiple winners.

These features add an extra layer of complexity and enjoyment to the game.
