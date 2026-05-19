# CMSC 137 — Project: Holy Shot!
> A 2D Multiplayer Co-op Shooter | Inspired by *Journey of the Prairie King* (Stardew Valley)

## Overview
A pixelated 2D top-down PvE co-op shooter built in Java.  
Up to four players cooperate to survive 5 rounds of increasingly difficult supernatural enemy waves.  
Built from scratch with a custom game loop — no full game engines used.

**Theme:** Players are elite spirit-cleansing hunters battling supernatural Filipino folklore entities across haunted arenas.

---

## Project Structure

```
shooter-game/
├── src/main/java/com/shooter/
│   ├── client/                         # CLIENT-SIDE ONLY — rendering, input, screen management
│   │   ├── GameClient.java             Entry point for players joining the game
│   │   ├── GamePanel.java              Main Swing canvas — delegates to ScreenManager
│   │   ├── InputHandler.java           Keyboard input — tracks held keys and facing direction
│   │   ├── ScreenManager.java          Switches between game screens (Menu, Lobby, Game, etc.)
│   │   │
│   │   └── screens/                    # One class per game screen
│   │       ├── Screen.java             Interface: update(), render(Graphics2D), onEnter(), onExit()
│   │       ├── MainMenuScreen.java     Title screen — Host / Join / Exit
│   │       ├── LobbyScreen.java        Shows connected players, ready toggle, host starts game
│   │       ├── GameScreen.java         Wraps the active game — delegates to Renderer and HUD
│   │       ├── PauseScreen.java        Overlay — any player can pause; freezes all clients
│   │       └── GameOverScreen.java     Post-game stats — kills per player, rounds cleared
│   │
│   ├── server/                         # SERVER-SIDE ONLY — authority over all game state
│   │   ├── GameServer.java             Accepts up to 4 TCP connections on port 5000
│   │   ├── ClientHandler.java          One thread per player — reads input, sends state
│   │   ├── GameManager.java            Server game loop at 20 ticks/sec, broadcasts GameState
│   │   ├── EntityManager.java          Stores active enemies, bullets, and power-ups
│   │   ├── EnemySpawner.java           Creates enemies at arena edges per round config
│   │   └── RoundManager.java           Tracks round number, triggers spawns, detects round clear
│   │
│   ├── network/                        # SHARED — all message types sent over TCP
│   │   ├── NetworkMessage.java         Serializable packet: type, playerId, payload, timestamp
│   │   ├── MessageType.java            Enum: CONNECT, CONNECTED, INPUT, GAME_STATE, ROUND_START,
│   │   │                               ROUND_CLEAR, GAME_OVER, PAUSE, CHAT, DISCONNECT, etc.
│   │   ├── InputSnapshot.java          Client → Server: held WASD keys + facing direction
│   │   ├── LobbyState.java             Server → Client: player names + ready flags (Serializable)
│   │   └── GameOverStats.java          Server → Client: kills per player/round, rounds cleared
│   │
│   ├── shared/
│   │   ├── model/                      # SHARED — plain data objects (no game logic)
│   │   │   ├── Player.java             Position, HP, direction, power-up state, playerId (0–3)
│   │   │   ├── Enemy.java              Type (MELEE/RANGED/SEMIBOSS), position, HP
│   │   │   ├── Bullet.java             Position, direction, damage, ownerId
│   │   │   ├── PowerUp.java            Type (SPEED/DAMAGE/HEAL/ATK_SPEED) and position
│   │   │   └── GameState.java          Full snapshot: Player[4], enemies, bullets, powerups, effects
│   │   │
│   │   ├── logic/                      # SHARED — pure game rules, no networking or rendering
│   │   │   ├── CollisionDetector.java  AABB collision checks (bullet-enemy, enemy-player, pickup)
│   │   │   └── RoundConfig.java        Enemy counts and HP values per round (R1–R5)
│   │   │
│   │   └── util/                       # SHARED — helpers and constants
│   │       ├── Constants.java          All magic numbers (port, tick rate, arena size, etc.)
│   │       ├── Direction.java          Enum: UP, DOWN, LEFT, RIGHT
│   │       ├── AssetManager.java       Loads all sprites/tiles once at startup; magenta fallback
│   │       └── AudioManager.java       Loads and plays WAV clips; stub-safe if assets missing
│   │
│   └── ui/                             # CLIENT-SIDE — HUD and overlay rendering
│       ├── HUD.java                    HP bars (all 4 players), round counter, enemy count, power-up icons
│       ├── ChatPanel.java              Scrollable chat overlay (bonus) — press T to toggle
│       └── GameScreen.java             Enum: LOBBY, GAME, PAUSE, GAMEOVER (legacy, replaced by screens/)
│
├── src/main/resources/assets/
│   ├── sprites/
│   │   ├── player_blue.png             P1 sprite (4-direction or single + rotated)
│   │   ├── player_red.png              P2 sprite
│   │   ├── player_green.png            P3 sprite
│   │   ├── player_yellow.png           P4 sprite
│   │   ├── aswang.png                  Melee enemy — Filipino Aswang with long nails
│   │   ├── tiktik.png                  Ranged enemy — Tiktik/Manananggal with tongue attack
│   │   ├── tikbalang.png               Semi-boss — Tikbalang with close-range punch
│   │   ├── bullet_salt.png             Default player projectile
│   │   ├── bullet_holywater.png        Upgraded projectile (damage power-up active)
│   │   ├── powerup_speed.png           Movement speed boost icon
│   │   ├── powerup_damage.png          Damage boost icon
│   │   ├── powerup_heal.png            HP recovery icon
│   │   └── powerup_atk.png             Attack speed boost icon
│   ├── tiles/
│   │   ├── floor.png                   Repeating 32×32 arena floor tile
│   │   └── border.png                  Arena border/wall tile
│   └── audio/
│       ├── ambient.wav                 Looping background music
│       ├── shoot.wav                   Bullet fire sound effect
│       ├── hit.wav                     Enemy hit / death sound
│       └── powerup.wav                 Power-up pickup sound
│
└── docs/
```

---

## How to Run

### Prerequisites
- Java 17 or higher
- Maven (optional, for building)

### Build the Project
```bash
# Compile all Java files into the 'out' directory
javac -d out $(find src/main/java -name "*.java")
```

### Milestone 1 — Single Player (no server needed)
```bash
java -cp out com.shooter.client.GameClient
```

### Milestone 2 — Networked Multiplayer

**Step 1: Start the Server (run once — on the host machine)**
```bash
java -cp out com.shooter.server.GameServer
```
> Server listens on port **5000**. Console confirms each player connection.

**Step 2: Start Clients (run once per player — on each machine)**
```bash
java -cp out com.shooter.client.GameClient
```
> On launch: select **Host Game** (connects to `localhost`) or **Join Game** (enter host's IP address).  
> All 4 players must be in the Lobby before the host can start the game.

**Local testing (all on one machine):**
```bash
# Terminal 1 — Server
java -cp out:src/main/resources com.shooter.server.GameServer

# Terminals 2–5 — Clients (run separately)
java -cp out:src/main/resources com.shooter.client.GameClient
```

## Team

| Members |
|--------|
| Mirano, Christel| 
| Garcia, Sophia Ysabel | 
| Castillo, Geastin|