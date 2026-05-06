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
    ├── Milestone2_Development_Plan.md  Day-by-day M2 sprint plan with per-member tasks
    ├── Workflow_and_Development.pdf    Full project workflow document (updated for M2)
    └── Project_Specifications.pdf      Original course spec sheet
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
java -cp out com.shooter.server.GameServer

# Terminals 2–5 — Clients (run separately)
java -cp out com.shooter.client.GameClient
```

---

## Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, demo-ready only. Never commit here directly. |
| `dev` | Integration branch. All features merge here first. |
| `feature/systems` | ✅ Milestone 1 — single-player, rounds, spawning, power-ups, HUD |
| `feature/networking` | Milestone 2 — `GameServer`, `ClientHandler`, `GameManager` server loop, all `network/` classes |
| `feature/assets-screens` | Milestone 2 — `AssetManager`, all `screens/`, `AudioManager`, sprite integration in Renderer |
| `feature/chat` | Bonus — `ChatPanel`, `ChatMessage`, server relay logic |

**Pull Request rule:** At least 1 teammate must review and approve before merging into `dev`.  
**Merge order for M2:** `feature/networking` and `feature/assets-screens` merge into `dev` independently. Never merge them into each other. Merge both into `dev` before tagging a release on `main`.

**Tags:**
- `v1.0-milestone1` — stable M1 snapshot (already tagged)
- `v2.0-milestone2` — target tag for submission

---

## Commit Message Format
```
[type] short description

Types: feat, fix, refactor, docs, assets, test

Examples:
  feat: add GameServer TCP accept loop
  feat: add LobbyScreen ready-toggle UI
  fix: ObjectOutputStream opened before ObjectInputStream to prevent deadlock
  assets: add aswang melee enemy sprite
  refactor: move collision logic to server-side only
  test: verify 4-player round sync in local stress test
```

---

## Milestone Checklist

### ✅ Milestone 1 — Single Player (Completed)
- [x] Game window opens and runs at 60fps
- [x] Player moves with WASD
- [x] Player shoots in facing direction with cooldown
- [x] 20 Melee enemies spawn at edges (Round 1)
- [x] Bullets kill enemies; enemies reduce player HP
- [x] Round ends when all enemies eliminated
- [x] At least 1 power-up drops and applies effect
- [x] HUD shows HP, round, enemy count
- [x] Player revives on death (power-ups reset)
- [x] All 5 rounds spawn correct enemy counts and HP

### Milestone 2 — Networked Multiplayer (Due May 11–13)

**Networking**
- [ ] Server accepts 4 TCP connections on port 5000
- [ ] Lobby shows connected players and ready status in real time
- [ ] Host can start game when all players are ready
- [ ] All 4 players see each other moving in real time
- [ ] Shooting is visible on all clients simultaneously
- [ ] Bullet-enemy collision removes enemy on all clients
- [ ] Player HP damage syncs across all clients
- [ ] Power-up pickup removes it from the ground for all clients
- [ ] All 5 rounds advance and sync correctly across clients
- [ ] Round clear transition displayed on all clients simultaneously
- [ ] Player revive is visible to all clients
- [ ] Pause freezes all clients simultaneously (any player can pause)
- [ ] Game Over screen shows correct stats on all clients
- [ ] Disconnecting 1 player mid-game does not crash remaining clients

**Assets & Screens**
- [ ] `AssetManager` loads all sprites at startup (magenta fallback for missing files)
- [ ] All game objects render with real sprites (no colored-rectangle placeholders)
- [ ] Arena renders with floor tiles and border tiles
- [ ] Main Menu screen — Host / Join / Exit options functional
- [ ] Lobby screen — player list, ready toggle, host start button
- [ ] Pause screen — Resume / Exit, visible to all clients
- [ ] Game Over screen — team result, kills per player, rounds cleared
- [ ] Audio plays for shooting, hits, power-up pickups, and ambient music

**Quality**
- [ ] No critical desyncs during 4-player stress test
- [ ] Game compiles and runs from a clean repo clone

### Bonus
- [ ] In-game chat overlay (press T to toggle, works in lobby and in-game)

---

## Network Architecture (Quick Reference)

```
[Client 0] ──INPUT──▶ [GameServer :5000] ──GAME_STATE──▶ [Client 0]
[Client 1] ──INPUT──▶       │            ──GAME_STATE──▶ [Client 1]
[Client 2] ──INPUT──▶       │            ──GAME_STATE──▶ [Client 2]
[Client 3] ──INPUT──▶       │            ──GAME_STATE──▶ [Client 3]
                      Server is the sole
                      authority on state.
                      Broadcasts at 20 ticks/sec.
                      Clients render at 60fps.
```

**Key rule:** Clients never mutate game state locally. They send `InputSnapshot` to the server; the server moves entities, resolves collisions, and broadcasts the authoritative `GameState` to all clients.

---

## Team

| Member | Role | M1 Responsibility | M2 Primary Track |
|--------|------|------------------|-----------------|
| Geastin | Engine Lead | Game loop, rendering, input, HUD | `GameServer`, `GameClient` networking, `MainMenuScreen`, `PauseScreen` |
| Sophia | Entity Lead | Player, enemies, bullets, collision | Entity state sync, multi-player rendering, `GameOverScreen`, sprite integration |
| Christel | Systems Lead | Rounds, spawning, power-ups | Round/power-up sync, `LobbyScreen`, `AssetManager`, `AudioManager` |