# CMSC 137 — Project #2: 2D Multiplayer Shooter
> Inspired by *Journey of the Prairie King* (Stardew Valley)

## Overview
A pixelated 2D top-down PvE co-op shooter built in Java.  
Four players cooperate to survive 5 rounds of increasingly difficult enemy waves.  
Built from scratch with a custom game loop — no full game engines used.

---

## Project Structure

```
shooter-game/
├── src/main/java/com/shooter/
│   ├── client/             # CLIENT-SIDE ONLY — rendering, input, local game loop
│   │   ├── GameClient.java         Entry point for players joining the game
│   │   ├── GamePanel.java          The main Swing canvas (draws everything)
│   │   └── InputHandler.java       Keyboard input — tracks which keys are held
│   │
│   ├── server/             # SERVER-SIDE ONLY — authority over game state
│   │   ├── GameServer.java         Entry point for the host — starts the server
│   │   ├── ClientHandler.java      One thread per connected player
│   │   ├── GameManager.java        Core game loop, round logic, state authority
│   │   ├── EntityManager.java      Stores active enemies, bullets, and power-ups
│   │   ├── EnemySpawner.java       Creates enemies for each round at arena edges
│   │   └── RoundManager.java       Tracks current round and advances waves
│   │
│   ├── network/            # SHARED — message types sent between client and server
│   │   ├── Message.java            Base class / envelope for all network messages
│   │   ├── MessageType.java        Enum of every message type (MOVE, SHOOT, etc.)
│   │   └── MessageHandler.java     Parses incoming messages and routes them
│   │
│   ├── shared/
│   │   ├── model/          # SHARED — plain data objects (no game logic here)
│   │   │   ├── Player.java         Player state: position, HP, direction, power-ups
│   │   │   ├── Enemy.java          Enemy state: type, position, HP
│   │   │   ├── Bullet.java         Bullet state: position, direction, damage
│   │   │   ├── PowerUp.java        Power-up type and position
│   │   │   └── GameState.java      Snapshot of everything: players, enemies, round
│   │   │
│   │   ├── logic/          # SHARED — pure game rules, no networking or rendering
│   │   │   ├── CollisionDetector.java   AABB collision checks
│   │   │   └── RoundConfig.java         Enemy counts and HP per round
│   │   │
│   │   └── util/           # SHARED — helpers and constants
│   │       ├── Constants.java      All magic numbers in one place
│   │       └── Direction.java      Enum: UP, DOWN, LEFT, RIGHT
│   │
│   └── ui/                 # CLIENT-SIDE — HUD and screen overlays
│       ├── HUD.java                Draws HP bar, round counter, enemy count
│       └── GameScreen.java         Enum for which screen is showing (LOBBY, GAME, etc.)
│
├── src/main/resources/assets/
│   ├── sprites/            # PNG sprite files (player, enemies, bullets, power-ups)
│   ├── tiles/              # Floor and border tile PNGs
│   └── audio/              # SFX and music (optional for Milestone 1)
│
└── docs/                   # Notes, diagrams, and planning documents
```

---

## How to Run

### Prerequisites
- Java 17 or higher
- Maven (optional, for building)

### Build the project
```bash
# Compile all Java files into the 'out' directory
javac -d out $(find src/main/java -name "*.java")
```

### Start the Server (Host)
```bash
java -cp out com.shooter.server.GameServer
```

### Start a Client (Player)
```bash
java -cp out com.shooter.client.GameClient
```
> For Milestone 1, run GameClient directly — no server needed yet.

---

## Branching Strategy

| Branch             | Purpose                                      |
|--------------------|----------------------------------------------|
| `main`             | Stable, demo-ready only. Never commit here directly. |
| `dev`              | Integration branch. All features merge here first. |
| `feature/systems`  | Milestone 1 — single player, rounds, spawning, power-ups, HUD      |
| `feature/networking` | Milestone 2 — all network code             |

**Pull Request rule:** At least 1 teammate must review before merging into `dev`.

---

## Commit Message Format
```
[type] short description

Types: feat, fix, refactor, docs, assets, test

Examples:
  feat: add bullet cooldown to Player
  fix: collision not detecting enemy edge case
  assets: add melee enemy sprite sheet
```

---

## Milestone Checklist

### Milestone 1 — Single Player (Due Apr 27–May 1)
- [x] Game window opens and runs at 60fps
- [x] Player moves with WASD
- [x] Player shoots in facing direction with cooldown
- [x] 20 Melee enemies spawn at edges (Round 1)
- [x] Bullets kill enemies; enemies reduce player HP
- [x] Round ends when all enemies eliminated
- [x] At least 1 power-up drops and applies effect
- [x] HUD shows HP, round, enemy count
- [x] Player revives on death (power-ups reset)

### Milestone 2 — Networked (Due May 11–13)
- [ ] Server accepts 4 TCP connections
- [ ] Players see each other moving in real time
- [ ] Shooting and enemy deaths synced across clients
- [ ] All 5 rounds work in multiplayer
- [ ] Lobby screen with host/join

### Bonus
- [ ] In-game chat overlay (press T to toggle)