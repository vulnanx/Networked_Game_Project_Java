# Holy Shot! — Milestone 2 Development Plan
## CMSC 137 | Networked Multiplayer + Asset & Screen Integration
**Due: May 11–13 | Sprint Window: May 4–13**

---

## 0. Pre-Sprint Gate — Before Writing a Single Line of M2 Code

Complete all of these before May 4. If any item is unchecked, fix it first.

| Check | Item | Owner |
|-------|------|-------|
| ☐ | Milestone 1 fully passes all Definition of Done criteria | All |
| ☐ | All M1 code is merged into `dev` (no open feature branches with unmerged work) | Leader |
| ☐ | `main` is tagged `v1.0-milestone1` as a stable snapshot | Leader |
| ☐ | `feature/networking` branch is created from `dev` | Leader |
| ☐ | `feature/assets-screens` branch is created from `dev` | Leader |
| ☐ | All sprite asset files are collected and placed in `src/main/resources/assets/sprites/` | C |
| ☐ | All tile/background asset files are placed in `src/main/resources/assets/tiles/` | C |
| ☐ | Team agrees on the final `NetworkMessage` packet schema (see Section 3.2) | All |

> **Rule:** `feature/networking` and `feature/assets-screens` are developed in parallel. They merge into `dev` independently when each is ready. Never merge them into each other.

---

## 1. Sprint Overview

| Date | Focus | End-of-Day Goal |
|------|-------|-----------------|
| **May 4** | Networking scaffold + Asset pipeline + Screen stubs | Server accepts connections; 4 placeholder sprites load; all screen classes exist |
| **May 5** | State sync + Sprite integration in-game | All 4 players visible and moving on every client |
| **May 6** | Action sync + Lobby & Main Menu screens | Shooting, bullet hits, and enemy deaths replicated; Lobby screen functional |
| **May 7** | Full 5-round networked loop + Game Over & Pause screens | Complete game playable over LAN; all screens wired |
| **May 8** | 4-player stress test + Bug fix pass | No critical desyncs; game feels smooth |
| **May 11–13** | Final polish, bonus chat, presentation prep | Submission-ready build |

---

## 2. Member Roles for Milestone 2

| Member | M1 Role | M2 Primary Track | M2 Secondary Support |
|--------|---------|-----------------|----------------------|
| **A (Geastin)** | Engine Lead | Networking — `GameServer`, `GameClient`, message loop | Main Menu & Game Over screens |
| **B (Sophia)** | Entity Lead | Networking — entity state sync (players, bullets, enemies) | Asset integration (sprite rendering) |
| **C (Christel)** | Systems Lead | Networking — round/power-up sync + Lobby screen | Pause screen; asset pipeline setup |

---

## 3. Networking Architecture Reference

### 3.1 Model: Authoritative Server

```
[Client A] ──SEND input──▶ [GameServer] ──BROADCAST GameState──▶ [Client A]
[Client B] ──SEND input──▶      │                               ──▶ [Client B]
[Client C] ──SEND input──▶      │                               ──▶ [Client C]
[Client D] ──SEND input──▶      │                               ──▶ [Client D]
                           Server owns ALL state.
                           Clients only send input + render what server tells them.
```

- Server runs the game loop at **20 ticks/sec** (not 60 — network doesn't need 60fps updates).
- Clients still render at **60fps** locally, using the last received `GameState` snapshot.
- Clients **never** move their own player directly. They send `MOVE` to the server; the server moves the player and broadcasts back.

### 3.2 NetworkMessage Packet Schema

All messages are `NetworkMessage` objects, serialized with Java's `ObjectOutputStream`.

```java
public class NetworkMessage implements Serializable {
    public MessageType type;    // Enum — what kind of message
    public int playerId;        // Which player sent/is targeted (0–3)
    public Object payload;      // Cast based on type (see table below)
    public long timestamp;      // System.currentTimeMillis() at send time
}
```

| `MessageType` | Direction | Payload Type | Description |
|---------------|-----------|-------------|-------------|
| `CONNECT` | C → S | `String` (player name) | Player joins server |
| `CONNECTED` | S → C | `Integer` (assigned playerId) | Server confirms join + assigns ID |
| `LOBBY_STATE` | S → C | `LobbyState` (names, ready flags) | Broadcast on any lobby change |
| `PLAYER_READY` | C → S | `null` | Player toggles ready status |
| `GAME_START` | S → C | `null` | Server tells all clients to enter game |
| `INPUT` | C → S | `InputSnapshot` (keys held + facing) | Client sends held input every tick |
| `GAME_STATE` | S → C | `GameState` | Full authoritative snapshot at 20 ticks/sec |
| `ROUND_START` | S → C | `Integer` (round number) | New round beginning |
| `ROUND_CLEAR` | S → C | `Integer` (round number) | All enemies dead, brief pause |
| `GAME_OVER` | S → C | `GameOverStats` | All players dead — game ended |
| `PAUSE` | C → S / S → C | `Boolean` (paused?) | Any player can pause; server relays |
| `CHAT` | C → S / S → C | `ChatMessage` | Bonus feature |
| `DISCONNECT` | C → S | `null` | Clean disconnect signal |

**`InputSnapshot` fields:**
```java
public class InputSnapshot implements Serializable {
    public boolean up, down, left, right;   // WASD held
    public boolean shooting;                // Space/mouse held
    public Direction facing;                // Last non-neutral direction
}
```

### 3.3 Server Tick Loop

The server's game loop runs independently of any client. It:
1. Collects all pending `InputSnapshot` messages from each client's queue.
2. Applies inputs → moves players, fires bullets.
3. Runs enemy AI (move toward nearest player).
4. Runs collision detection.
5. Runs round logic (check win condition, spawn next round).
6. Broadcasts the full `GameState` to all clients.
7. Sleeps to maintain 20 ticks/sec.

```java
// Server game loop skeleton — GameManager.java
long NS_PER_TICK = 1_000_000_000L / 20;
long lastTime = System.nanoTime();
while (running) {
    long now = System.nanoTime();
    if (now - lastTime >= NS_PER_TICK) {
        processAllInputs();   // drain each ClientHandler's input queue
        update();             // move entities, collisions, round logic
        broadcastGameState(); // send GameState to all clients
        lastTime = now;
    }
}
```

---

## 4. Asset & Screen Architecture Reference

### 4.1 AssetManager (load once at startup)

```java
public class AssetManager {
    // Key = asset name string, Value = loaded BufferedImage
    private static final Map<String, BufferedImage> sprites = new HashMap<>();

    public static void loadAll() {
        // Players
        load("player_blue",   "/assets/sprites/player_blue.png");
        load("player_red",    "/assets/sprites/player_red.png");
        load("player_green",  "/assets/sprites/player_green.png");
        load("player_yellow", "/assets/sprites/player_yellow.png");
        // Enemies
        load("enemy_melee",    "/assets/sprites/aswang.png");
        load("enemy_ranged",   "/assets/sprites/tiktik.png");
        load("enemy_semiboss", "/assets/sprites/tikbalang.png");
        // Bullets
        load("bullet_salt",       "/assets/sprites/bullet_salt.png");
        load("bullet_holywater",  "/assets/sprites/bullet_holywater.png");
        // Power-ups
        load("powerup_speed",  "/assets/sprites/powerup_speed.png");
        load("powerup_damage", "/assets/sprites/powerup_damage.png");
        load("powerup_heal",   "/assets/sprites/powerup_heal.png");
        load("powerup_atk",    "/assets/sprites/powerup_atk.png");
        // Tiles
        load("tile_floor",  "/assets/tiles/floor.png");
        load("tile_border", "/assets/tiles/border.png");
    }

    public static BufferedImage get(String key) { return sprites.get(key); }

    private static void load(String key, String path) {
        try {
            sprites.put(key, ImageIO.read(AssetManager.class.getResourceAsStream(path)));
        } catch (Exception e) {
            System.err.println("[AssetManager] Missing: " + path + " — using fallback.");
            // Fallback: 32x32 magenta rectangle so the game still runs
            BufferedImage fallback = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
            Graphics g = fallback.getGraphics();
            g.setColor(Color.MAGENTA); g.fillRect(0, 0, 32, 32); g.dispose();
            sprites.put(key, fallback);
        }
    }
}
```

> **Rule:** `AssetManager.loadAll()` is called **once** in `GameClient.main()` before the game loop starts. Never call `ImageIO.read()` inside any `render()` method.

### 4.2 Screen Flow

```
[MainMenuScreen]
      │
      ├─── Host Game ──▶ [LobbyScreen (host mode)] ──▶ [GameScreen] ──▶ [GameOverScreen] ──▶ [LobbyScreen]
      │
      └─── Join Game ──▶ [LobbyScreen (join mode)] ──▶ [GameScreen] ──▶ [GameOverScreen] ──▶ [LobbyScreen]
                                                              │
                                                        ESC key pressed
                                                              │
                                                        [PauseScreen overlay]
                                                              │
                                                         Resume / Exit
```

All screens implement a common interface:

```java
public interface Screen {
    void update();
    void render(Graphics2D g);
    void onEnter();   // called once when switching TO this screen
    void onExit();    // called once when switching AWAY from this screen
}
```

`GamePanel` holds a `currentScreen` reference and delegates `update()` and `render()` to it each tick.

---

## 5. Day-by-Day Sprint Plan

---

### DAY 1 — May 4: Scaffold Everything, Break Nothing

**Goal:** Server accepts connections. Asset pipeline loads without crashing. All five screen classes exist with placeholder rendering. No integration yet — just foundations.

---

#### Member A (Geastin) — Networking Scaffold

**Task 1 — `GameServer.java`**
- Create a `ServerSocket` on port `5000`.
- Loop: `accept()` up to 4 connections. Each accepted socket spawns one `ClientHandler` thread.
- Store all `ClientHandler` instances in a `List<ClientHandler> clients`.
- Print to console when each player connects: `"Player 0 connected from /127.0.0.1"`.
- After 4 connections, print `"Lobby full — waiting for ready signals."`

**Task 2 — `ClientHandler.java`**
- Each `ClientHandler` has an `ObjectInputStream in` and `ObjectOutputStream out`.
- Runs a loop: `readObject()` → cast to `NetworkMessage` → place in a `BlockingQueue<NetworkMessage> inputQueue`.
- Exposes `sendMessage(NetworkMessage msg)` — writes to `out` (synchronized).
- On `IOException`, mark player as disconnected and notify `GameServer`.

**Task 3 — `GameClient.java` connection**
- On launch: prompt for host IP (or default `localhost`) and connect `Socket` to port `5000`.
- Open `ObjectOutputStream out` and `ObjectInputStream in` (in that order — Java serialization deadlock caveat: always open `out` first).
- Send a `CONNECT` message with player name.
- Receive `CONNECTED` message — store `myPlayerId`.
- Print `"Connected as Player [id]"` to console.

**End-of-Day A Check:** Run server, run 4 clients on localhost. Console shows all 4 connections confirmed with assigned IDs.

---

#### Member B (Sophia) — Asset Pipeline

**Task 4 — `AssetManager.java`** *(copy template from Section 4.1)*
- Implement `loadAll()` with magenta fallback for every missing sprite.
- Call `AssetManager.loadAll()` at the top of `GameClient.main()`.
- Test: run client; no `NullPointerException` on sprite access; console shows which assets are missing vs. loaded.

**Task 5 — Sprite rendering in `Renderer.java`**
- Replace all colored-rectangle placeholder draws with `AssetManager.get(key)` calls.
- Player draw: use `"player_blue"`, `"player_red"`, `"player_green"`, `"player_yellow"` by `playerId`.
- Enemy draw: check `enemy.getType()` → select `"enemy_melee"`, `"enemy_ranged"`, or `"enemy_semiboss"`.
- Bullet draw: `"bullet_salt"` by default; `"bullet_holywater"` if player has damage power-up active.
- Power-up draw: match `powerUp.getType()` to the four icon keys.
- Arena: tile the floor with `"tile_floor"` (32×32 tiles filling the arena), border with `"tile_border"`.

**Task 6 — Direction-aware sprite rendering**
- If sprites have 4-direction variants (e.g., `player_blue_up.png`, `player_blue_down.png`), load all variants.
- In `Renderer`, pass `player.getFacingDirection()` to select the correct sprite key.
- If sprites are single-direction, use `Graphics2D.rotate()` around the sprite center instead.

**End-of-Day B Check:** Run the single-player M1 build — all game objects render with real sprites (or visible magenta fallback placeholders) instead of colored rectangles.

---

#### Member C (Christel) — Screen Stubs + Lobby Data Classes

**Task 7 — Create all five screen stub classes**

Each file goes in `src/main/java/com/shooter/client/screens/`:

```java
// MainMenuScreen.java — stub
public class MainMenuScreen implements Screen {
    public void onEnter() {}
    public void update() {}
    public void render(Graphics2D g) {
        g.setColor(Color.BLACK); g.fillRect(0, 0, 800, 600);
        g.setColor(Color.WHITE); g.drawString("HOLY SHOT! — Main Menu", 300, 280);
        g.drawString("[H] Host   [J] Join   [Q] Quit", 300, 320);
    }
    public void onExit() {}
}
```

Create identical stubs for: `LobbyScreen`, `GameScreen` (wraps existing game logic), `PauseScreen`, `GameOverScreen`. Each just draws a labeled black screen for now.

**Task 8 — Wire `ScreenManager` into `GamePanel`**

```java
public class ScreenManager {
    private Screen current;
    public void switchTo(Screen next) {
        if (current != null) current.onExit();
        current = next;
        current.onEnter();
    }
    public void update() { if (current != null) current.update(); }
    public void render(Graphics2D g) { if (current != null) current.render(g); }
}
```

- `GamePanel` holds a `ScreenManager screenManager`.
- `GamePanel.update()` calls `screenManager.update()`.
- `GamePanel.paintComponent()` calls `screenManager.render(g)`.
- On startup, call `screenManager.switchTo(new MainMenuScreen(...))`.

**Task 9 — Data classes for lobby and game-over**

```java
public class LobbyState implements Serializable {
    public String[] playerNames = new String[4];  // null = slot empty
    public boolean[] readyFlags = new boolean[4];
    public int hostId;
}

public class GameOverStats implements Serializable {
    public int[] killsPerPlayer = new int[4];
    public int[] killsPerRound  = new int[5];
    public int roundsCleared;
    public boolean teamWon;
}
```

**End-of-Day C Check:** Launch the client — it shows the Main Menu stub screen. Pressing H or J switches to the Lobby stub. Pressing Escape from game shows Pause stub. All screen transitions work without crashing.

---

**End-of-Day 1 Sync (20 min, all members):**
- Demo: 4 clients connect to server (A's work).
- Demo: single-player game with real sprites (B's work).
- Demo: screen switching working on client (C's work).
- Agree on any `NetworkMessage` field changes before Day 2.
- Create a `v2-day1` tag on `dev` as a restore point.

---

### DAY 2 — May 5: Players See Each Other

**Goal:** All 4 players appear on every client's screen and move in real time. The game is still mechanically M1 (single-player logic on the server), but visually multiplayer.

---

#### Member A — Server Game Loop + State Broadcast

**Task 10 — `GameManager.java` server-side game loop**
- Implement the 20 ticks/sec loop (see Section 3.3).
- `processAllInputs()`: for each `ClientHandler`, drain its `inputQueue`, apply the latest `InputSnapshot` to the corresponding `Player` in `GameState`.
- `update()`: call existing `EntityManager.update()`, `CollisionDetector` checks, `RoundManager` logic — all already written in M1, just running server-side now.
- `broadcastGameState()`: serialize the current `GameState` and call `clientHandler.sendMessage(msg)` for each connected client.

**Task 11 — `GameClient.java` receive loop**
- Start a background thread: continuously `readObject()` from the server input stream.
- On receiving `GAME_STATE`: store it as `volatile GameState latestState` in `GameClient`.
- `GamePanel.render()` reads `latestState` each frame — it renders whatever the server last sent.

**Task 12 — Input sending loop**
- In `GameClient`, every 50ms (20 times/sec): read current keys from `InputHandler`, build an `InputSnapshot`, send `INPUT` message to server.

**End-of-Day A Check:** 4 clients connected. Move one client's player with WASD — all 4 clients see that player move.

---

#### Member B — Multi-Player Rendering

**Task 13 — `Renderer` renders all 4 players from `GameState`**
- `GameState.players` is a `Player[4]`. In `Renderer.renderPlayers()`, iterate all 4 slots.
- Skip null/disconnected slots.
- Draw each player's sprite using their `playerId` for color: 0=blue, 1=red, 2=green, 3=yellow.
- Draw each other player's HP bar above their sprite (smaller than the HUD's own HP bar).
- Draw a small name tag above each player.

**Task 14 — Distinguish "my player" from others**
- `GameClient` knows `myPlayerId`. Pass it to `Renderer`.
- My player: render normally.
- Other players: render at 80% opacity (`AlphaComposite`) or with a subtle outline to differentiate.
- Camera/view: if the arena fits the screen, no scrolling needed. If it doesn't, center view on `myPlayer`.

**Task 15 — Smooth interpolation (optional but recommended)**
- Network updates arrive at 20hz but rendering is 60fps. Without interpolation, players will stutter.
- Simple fix: store the `previousGameState` and `currentGameState`. Each render frame, lerp entity positions by `alpha = timeSinceLastStateUpdate / (1000/20)`.
- This makes movement look smooth even at 20 tick/sec server rate.

**End-of-Day B Check:** All 4 players visible, correct color sprites, HP bars above their heads, smooth movement.

---

#### Member C — Lobby Screen (Functional)

**Task 16 — `LobbyScreen` with real player list**
- `LobbyScreen` receives `LobbyState` updates (pushed from server) and renders them.
- Display: 4 player slots. Each slot shows: player name (or "Waiting..."), a colored player icon, and a "READY ✓" badge if `readyFlags[i]` is true.
- Keyboard: Press `R` to toggle ready. Send `PLAYER_READY` message to server.
- Host only: a "START GAME" button appears when all connected players are ready. Pressing `Enter` sends a `GAME_START` signal to server.

**Task 17 — Server-side lobby logic**
- `GameServer` tracks `LobbyState`.
- On receiving `PLAYER_READY` from client i: toggle `readyFlags[i]`, broadcast updated `LobbyState` to all clients.
- On receiving `GAME_START` from host client: verify all ready flags set, then call `GameManager.startGame()`, broadcast `GAME_START` to all clients.
- Clients receiving `GAME_START`: `screenManager.switchTo(new GameScreen(...))`.

**End-of-Day C Check:** 4 clients in lobby. Each presses R — all other clients see the ready indicator update in real time. Host presses Enter → all clients transition to the game screen.

---

**End-of-Day 2 Sync:** Live multiplayer movement demo. Tag `v2-day2`.

---

### DAY 3 — May 6: Actions Are Synchronized

**Goal:** Shooting, bullet travel, bullet-enemy collisions, enemy deaths, and HP damage all replicate correctly on every client. Enemies attack and kill players. Main Menu screen is polished.

---

#### Member A — Action Sync & Main Menu Screen

**Task 18 — Shooting sync**
- Client: when `InputSnapshot.shooting == true` AND player's cooldown permits (tracked server-side), server creates a `Bullet` and adds it to `EntityManager`.
- `GameState` already contains `List<Bullet>` — clients render them from the broadcast state.
- Verify: fire on Client 1 → bullet appears on Clients 2, 3, 4.

**Task 19 — Main Menu Screen (full implementation)**

Layout: dark background with a haunted-arena-style illustration (can be a static image or drawn with Graphics2D shapes). Four menu items with keyboard navigation:

```
        ✦ HOLY SHOT! ✦
    A spirit-cleansing co-op shooter

    ▶  HOST GAME
       JOIN GAME
       SETTINGS (placeholder)
       EXIT

    Use ↑↓ to navigate, ENTER to select
```

- Arrow keys move a cursor indicator between options.
- Selecting HOST: connect to `localhost:5000`, join lobby as host.
- Selecting JOIN: show an IP input dialog (use `JOptionPane.showInputDialog()`), connect to entered IP.
- Selecting EXIT: `System.exit(0)`.

---

#### Member B — Enemy & Bullet Death Sync

**Task 20 — Enemy death replication**
- Server: `CollisionDetector` kills an enemy → `EntityManager.removeEnemy(enemy)`.
- Since `GameState` is broadcast every tick, removed enemies simply disappear from all clients' next state update.
- Add a **death effect queue**: when server removes an enemy, it adds a `DeathEffect{x, y, type}` to `GameState.pendingEffects`. Clients render a one-time fade/flash at that position and clear the list.

**Task 21 — Player HP damage sync**
- Enemy contact reduces player HP server-side (already written in M1).
- `GameState.players[i].hp` is broadcast to all clients.
- `HUD.java`: update to render all 4 players' HP bars (not just the local player's).
- When `players[i].hp <= 0`: server triggers revive logic (already written in M1), broadcasts `PLAYER_REVIVED` effect.

**Task 22 — Hit flash effects**
- Client-side only (no need to sync effects, only state).
- When a player's HP decreases between two consecutive `GameState` snapshots: flash that player's sprite white for 200ms.
- When an enemy is hit: flash it white for 100ms.

---

#### Member C — Power-Up Sync & Round Sync

**Task 23 — Power-up sync**
- `GameState` includes `List<PowerUp> activePowerUps` (those on the ground).
- Server: enemy death → random chance → add `PowerUp` to `EntityManager`.
- Client walks over a `PowerUp` → server detects overlap in `CollisionDetector` → applies effect to `Player`, removes `PowerUp` from ground list.
- All clients see the power-up disappear when any player picks it up.
- HUD: show active power-up icons for the local player (timers if power-ups are temporary).

**Task 24 — Round transition sync**
- Server: `RoundManager` detects `enemyCount == 0` → broadcast `ROUND_CLEAR{roundNumber}`.
- All clients: show a "ROUND CLEAR!" overlay for 2 seconds, then freeze input.
- Server: after 3-second pause, broadcast `ROUND_START{nextRound}`, call `EnemySpawner` for next round.
- All clients: dismiss overlay, resume rendering new enemies.
- If `roundNumber > 5`: broadcast `GAME_OVER` with `GameOverStats`.

**End-of-Day 3 Sync:** Full combat demo — 4 players shoot enemies, enemies die on all screens, rounds advance, power-ups sync. Tag `v2-day3`.

---

### DAY 4 — May 7: Full Game + Remaining Screens

**Goal:** All 5 rounds complete in multiplayer. Game Over and Pause screens functional. The game can be played start-to-finish by 4 players with no crashes.

---

#### Member A — Pause Screen + Disconnect Handling

**Task 25 — Pause Screen**
- Any client presses `ESC` → sends `PAUSE{true}` to server.
- Server broadcasts `PAUSE{true}` to all clients → all `GamePanel`s freeze their `update()` call.
- Pause overlay draws on top of the frozen game screen:

```
          ║ PAUSED ║
      [R] Resume
      [X] Exit Game
    Paused by: Player 2
```

- `[R]` Resume: sending client sends `PAUSE{false}` → server broadcasts → all clients unpause.
- `[X]` Exit: terminate all connections, return all clients to Main Menu.

**Task 26 — Disconnect handling**
- If a client disconnects mid-game: `ClientHandler` catches `IOException` → notifies `GameServer`.
- `GameServer` removes that player from `GameState` (mark slot as disconnected).
- Remaining clients see that player disappear on next `GameState` broadcast.
- If only 1 player remains connected: server can either continue (solo) or trigger `GAME_OVER`.
- If host disconnects: broadcast a `DISCONNECT` notice and return all other clients to Main Menu.

---

#### Member B — Game Over Screen

**Task 27 — `GameOverScreen` (full implementation)**

Receives `GameOverStats` from the server. Renders a post-game summary:

```
    ╔══════════════════════════════╗
    ║      MISSION COMPLETE        ║   (or TEAM ELIMINATED)
    ╠══════════════════════════════╣
    ║  Rounds Cleared:  3 / 5      ║
    ╠═══════════╦══════════════════╣
    ║  Player   ║  Total Kills     ║
    ║  P1 (You) ║      47          ║
    ║  P2       ║      31          ║
    ║  P3       ║      28          ║
    ║  P4       ║      19          ║
    ╠══════════════════════════════╣
    ║  [ENTER] Return to Lobby     ║
    ╚══════════════════════════════╝
```

- `teamWon == true`: play a victory sound (if audio implemented), green banner.
- `teamWon == false`: red banner, "TEAM ELIMINATED" header.
- Pressing ENTER: `screenManager.switchTo(new LobbyScreen(...))`, reconnect to server in lobby state.

---

#### Member C — All 5 Rounds Integration Test

**Task 28 — Full 5-round networked run-through**

Run a complete game (2+ players minimum) and verify each round:

| Round | Expected Enemies | Verify |
|-------|-----------------|--------|
| R1 | 20 Melee, HP 1 | Enemies spawn at edges, walk toward players, die in 1 hit |
| R2 | 30 Melee + 10 Ranged, HP 3 | Ranged enemies fire projectiles at players |
| R3 | 40 Melee + 20 Ranged + 3 Semi-Boss, HP 10 | Semi-boss has distinct behavior (close-range punch) |
| R4 | 50 Melee + 30 Ranged + 10 Semi-Boss, HP 20 | Noticeable difficulty spike |
| R5 | 60 Melee + 40 Ranged + 20 Semi-Boss, HP 50 | All enemy types; chaos |

Log any desync bugs found (enemy alive on one client but dead on another, round counter mismatch, etc.) and create GitHub Issues for each.

**Task 29 — Round-clear and game-over stats collection**

Server-side: track `killsPerPlayer[4]` (increment in `CollisionDetector` when a bullet kills an enemy — use the bullet's `ownerId` to attribute the kill). Track `killsPerRound[5]`. Populate `GameOverStats` before broadcasting `GAME_OVER`.

**End-of-Day 4 Sync:** Play full 5-round game. All screens functional. List all remaining bugs. Tag `v2-day4`.

---

### DAY 5 — May 8: Stress Test & Bug Fix Pass

**Goal:** Game is stable under real 4-player load. No critical desyncs. Submission candidate.

---

#### All Members — 4-Player Stress Test Protocol

Run two full 4-player sessions. For each session, one member takes notes:

**Checklist to verify during each session:**

| Test | Expected | Pass? |
|------|----------|-------|
| All 4 clients see same enemy positions | Positions match within 1 tile | ☐ |
| Killing an enemy on one client removes it on all | Disappears within 1 server tick (50ms) | ☐ |
| Round advances simultaneously on all clients | Round counter syncs | ☐ |
| Power-up pickup removes it for all players | Pickup is not duplicated | ☐ |
| Player death and revive visible to all | Revive position syncs | ☐ |
| Pause freezes all clients | No client continues updating | ☐ |
| Game Over shows same stats on all clients | Kill counts match | ☐ |
| Disconnecting 1 player mid-game | No crash on remaining clients | ☐ |
| Late joiner sees lobby correctly | Lobby state received immediately | ☐ |

**Known common networking bugs and fixes:**

| Bug | Likely Cause | Fix |
|-----|-------------|-----|
| Enemies jitter between positions | Client interpolating between old + new state with wrong alpha | Fix lerp timing |
| Enemy alive on one client, dead on another | CollisionDetector running on client AND server | Remove all collision logic from client render path |
| Round doesn't advance on one client | `ROUND_CLEAR` message dropped or not handled | Add message handler case for `ROUND_CLEAR` |
| `StreamCorruptedException` on connect | `ObjectInputStream` opened before `ObjectOutputStream` | Always open `out` first on both ends |
| Game freezes after player disconnect | Server thread blocking on dead socket | Wrap `clientHandler.sendMessage()` in try-catch; skip dead clients |

---

#### Member A — Latency Handling

**Task 30 — Dead reckoning for bullet positions**
- Bullets move fast (5px/tick × 60 ticks = 300px/sec). At 20 server ticks/sec, bullet positions can jump 15px between updates — noticeable.
- Client-side: after receiving a `GameState`, continue moving bullets locally at their known velocity between server updates. When the next `GameState` arrives, snap to server position.
- This eliminates bullet stuttering without adding complexity to the server.

---

#### Member B — Visual Polish Pass

**Task 31 — Effect refinements**
- Death fade: when an enemy dies, play a 300ms fade-out on the client (client-side effect triggered by `pendingEffects`).
- Power-up glow: draw a pulsing colored circle behind power-up sprites.
- Round clear flash: brief white screen flash on `ROUND_CLEAR`.
- Player revive: 1 second of invincibility frames (flashing sprite) after revive.

---

#### Member C — Audio Integration (if assets available)

**Task 32 — `AudioManager.java`**

```java
public class AudioManager {
    public static void play(String clipName) { /* load + play wav clip */ }
    public static void playMusic(String trackName) { /* loop background music */ }
    public static void stopMusic() { }
}
```

- Load audio files from `/assets/audio/`.
- Trigger `play("shoot")` in `Renderer` when a bullet is created.
- Trigger `play("hit")` on enemy death effect.
- Trigger `play("powerup")` on power-up pickup.
- `playMusic("ambient")` on game start; `stopMusic()` on game over.
- Use `javax.sound.sampled` (no external library needed).

If audio assets are not ready: leave `AudioManager` as a stub with empty methods so no code needs to change when assets arrive.

---

### MAY 11–13 — Final Polish & Bonus Chat

#### Bonus: In-Game Chat (implement only if all above is complete)

**Data classes:**
```java
public class ChatMessage implements Serializable {
    public int senderId;
    public String senderName;
    public String text;
    public long timestamp;
}
```

**`ChatPanel.java` (overlay):**
- Press `T` to toggle visibility.
- Shows last 8 messages in a semi-transparent dark box in the bottom-left corner.
- Text input field at the bottom when open.
- Press `Enter` to send (server relays to all clients as `CHAT` message).
- Chat is available in both lobby and in-game.

**Server relay:**
- On receiving `CHAT` from any client: broadcast `CHAT` to all other clients.
- No server-side storage needed.

#### Pre-Presentation Checklist

| Task | Owner | Done? |
|------|-------|-------|
| `main` branch updated from `dev` (clean merge) | Leader | ☐ |
| Game compiles from a clean clone with one command | All | ☐ |
| README updated with Milestone 2 run instructions | A | ☐ |
| All Javadoc comments on public methods | All | ☐ |
| GitHub Issues for all known bugs are closed or labeled "known" | Leader | ☐ |
| Demo video recorded (2–3 minutes, 4 players) | C | ☐ |
| Peer evaluation forms completed | All | ☐ |

---

## 6. Definition of Done — Milestone 2

All criteria must pass before tagging `v2.0-milestone2` on `main`.

| Acceptance Criteria | Pass? | Tester |
|---------------------|-------|--------|
| Server accepts 4 TCP connections on port 5000 | ☐ | A |
| Lobby shows all connected players and ready status | ☐ | C |
| Host can start game when all players ready | ☐ | C |
| All 4 players see each other moving in real time | ☐ | B |
| Shooting is visible on all clients | ☐ | A |
| Bullet-enemy collision removes enemy on all clients | ☐ | B |
| Enemy death syncs to all clients within 1 tick | ☐ | B |
| Player HP damage syncs across clients | ☐ | B |
| Power-up pickup removes it from ground for all clients | ☐ | C |
| All 5 rounds advance and sync correctly | ☐ | C |
| Round clear transition displayed on all clients | ☐ | C |
| Player revive visible to all clients | ☐ | B |
| Pause freezes all clients simultaneously | ☐ | A |
| Game Over screen shows correct stats on all clients | ☐ | B |
| Disconnecting 1 player does not crash the game | ☐ | A |
| All sprites replaced (no magenta placeholders remain) | ☐ | B |
| Main Menu → Lobby → Game → Game Over → Lobby flow works | ☐ | C |
| Game compiles and runs from a clean repo clone | ☐ | All |

---

## 7. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Serialization `StreamCorruptedException` on connect | High | Blocks all networking | Always open `ObjectOutputStream` before `ObjectInputStream` on both ends |
| Desync accumulates over many rounds | Medium | Game unplayable | Server is sole authority — clients never mutate state locally |
| Sprites not ready by Day 2 | Medium | Visual only | Magenta fallback in `AssetManager` keeps game runnable |
| 4-player test impossible (only 3 members) | High | Can't fully test | Test with 3 clients + 1 dummy bot client OR use 2 machines + 1 laptop |
| Merge conflict between `feature/networking` and `feature/assets-screens` | Low | Hours lost | Branches modify different files — networking touches server/network classes, assets touch renderer/screens |
| Audio assets unavailable | Medium | Minor | `AudioManager` stub — no code breaks if audio missing |

---

## 8. File Creation Checklist — New Files for M2

All new files go in these packages:

```
src/main/java/com/shooter/
├── server/
│   ├── GameServer.java          (new)
│   ├── ClientHandler.java       (new)
│   └── GameManager.java         (modify — add server game loop)
├── network/
│   ├── NetworkMessage.java      (new)
│   ├── MessageType.java         (new)
│   ├── InputSnapshot.java       (new)
│   ├── LobbyState.java          (new)
│   └── GameOverStats.java       (new)
├── client/
│   ├── GameClient.java          (modify — add networking)
│   ├── screens/
│   │   ├── Screen.java          (new — interface)
│   │   ├── MainMenuScreen.java  (new)
│   │   ├── LobbyScreen.java     (new)
│   │   ├── GameScreen.java      (new — wraps existing game)
│   │   ├── PauseScreen.java     (new)
│   │   └── GameOverScreen.java  (new)
│   └── ScreenManager.java       (new)
└── shared/
    └── util/
        ├── AssetManager.java    (new)
        └── AudioManager.java    (new)
```

---

*Good luck on Milestone 2. Keep commits small, keep the server authoritative, and test with real connections as early as Day 1. The networking bugs you find on Day 2 are easier to fix than the ones you find on May 11.*
