

**CMSC 137**

*Computer Networks*

**Project \#2: 2D Multiplayer Shooter**

Development & Workflow Document

Milestone 1: Single-Player Version (Completed)

**Milestone 2: Networked Multiplayer \+ Asset & Screen Integration**

Second Semester AY 2025–2026

Due: Milestone 1 — May 1  |  Milestone 2 — May 15

# **1\. Project Overview**

A pixelated 2D multiplayer PvE shooter inspired by Journey of the Prairie King from Stardew Valley. Four players cooperate in a top-down arena to survive 5 rounds of increasingly difficult enemy waves.

## **1.1 Game Summary**

| Field | Details |
| ----- | ----- |
| Game Type | 2D top-down PvE co-op shooter |
| Inspiration | Journey of the Prairie King (Stardew Valley) |
| Platform | Java desktop application |
| Players | 4 (cooperative, same abilities) |
| Rounds | 5, increasing difficulty |
| Win Condition | Eliminate all enemies per round |
| Bonus Feature | In-game chat overlay |

## **1.2 Course Constraints**

* Must implement a custom game loop (no full game engines)

* 2D graphics/sprite libraries require instructor approval

* Project tracking via GitHub (commits, branches, pull requests)

* Milestone 1 must be testable by a single player

* 3 members per group

* Must accommodate at least 4 players

## **1.3 Key Mechanics**

**MULTIPLAYER MECHANICS** 

* 4 up to 4 players simultaneously

* All players share same abilities and objectives

* Each player has independent HP and can revive after death

* Team-based survival

**PLAYER MECHANICS**

* Movement: WASD (per player)

* Shooting: direction-based

* Unlimited ammo with cooldown

* Revive system: player respawns at center but loses all power ups

**ENEMY SYSTEM**

* Enemies spawn from arena edges

* Will target nearest player

* Scale difficulty per round

**ROUND SYSTEM**

| Round | Difficulty |
| :---- | :---- |
| R1 | Basic enemies |
| R2 | Introduces ranged |
| R3 | Semi-boss appears |
| R4 | Heavy pressure |
| R5 | Final wave |

**POWER-UP SYSTEM**

* Dropped by enemies

* Can be picked up by any player

* Effects apply per player

* Types: boost attack speed, boost damage, recover HP, increase movement speed

## **1.4 Game Concept**

* Title: “Holy Shot\!”

* Genre: 2D Top-Down PvE Cooperative Shooter

* Platform: Java Desktop Application (Custom Game Looping, Swing Rendering)

* Players: Designed for 4 players

* Core Objective: A team of **four spirit hunters** must survive and cleanse waves of supernatural enemies using holy weapons. 

* Win Condition: All players collectively eliminate enemies in each round

* Lose Condition: All players are defeated (or exceeded revive limit)

* Theme: Fantasy. Players are elite members of a **spirit-cleansing unit**, trained to combat entities invisible to ordinary people. 

* Setting: Haunted arenas (church ruins, cemeteries, cursed grounds), Enemies emerge from all directions 

* Style: Pixel-art, top-down. Dark \+ glowing holy visuals 

**GAME SCREENS**

1. Main Menu  
   1. Start Game  
   2. Multiplayer (Host/Join)  
   3. Settings  
   4. Exit  
2. Lobby Screen  
   1. Shows connected players  
   2. Ready System (players must all press “Ready” to have the option to start the game)  
   3. Host Starts the game  
3. Game Screen (HUD)  
   1. Display   
      1. Player HP (your player)  
      2. Other players HP bars  
      3. Round number  
      4. Number of enemies killed  
      5. Total number of enemies in the round  
4. Game Over Screen  
   1. Team Result  
   2. Stats:  
      1. Total Enemies killed per round  
      2. Rounds cleared  
      3. Back to Lobby   
5. Pause Menu  
   1. Resume (this will pause the whole game, even other multiplayer)  
   2. Exit Game (this will exit the whole game, even other multiplayer)

**VISUAL ASSETS**

1. Player Sprites  
   1. P1: Blue  
   2. P2: Red  
   3. P3: Green  
   4. P4: Yellow  
2. Enemy Types  
   1. Melee-weaponed Spirit (Filipino Aswang Character with Long Nails as Melee weapon)  
   2. Ranged Spirit (Filipino Tiktik-manananggal Character with long tongue as ranged-attack weapon)  
   3. Semi-boss Entity (Filipino Tikbalang with cigarette punch attack as close-ranged attack)  
3. Projectiles (Player Attack Bullets)  
   1. Holy Water (when upgraded)  
4. Power Ups:  
   1. Speed  
   2. Damage  
   3. Heal  
   4. Attack Speed  
5. Environment   
   1. Arena-based map  
   2. edge \-spawn points

**EFFECTS**

* Hit Flash

* Death Fade

* Power-up glow

**AUDIO**

* Ambient music

* Shooting sounds

* Hit sounds

# **2\. Team & Roles**

## **2.1 Member Assignments (Updated for Milestone 2)**

| Member | Role |
| ----- | ----- |
| Christel | Member A | 
| Sophia | Member B |
| Geastin | Member C |

## **2.2 Shared Responsibilities**

* All members: code review via GitHub pull requests before merging

* All members: write Javadoc comments on public methods

* All members: participate in daily check-in (10 min standup or group chat)

* Project leader: manages GitHub milestones and resolves merge conflicts

# **3\. System Architecture**

## **3.1 Class Overview**

The project is divided into five layers. Each layer depends only on layers below it.

| Layer | Classes | Responsibility |
| ----- | ----- | ----- |
| Core Loop | GameLoop, GameState | Fixed-timestep update/render cycle, phase management |
| Managers | EntityManager, RoundManager, EnemySpawner | Coordinate game objects and round progression |
| Entities | Player, Enemy, Bullet, PowerUp | Game object logic and state |
| Subtypes | MeleeEnemy, RangedEnemy, SemiBossEnemy | Enemy-specific behavior and attributes |
| Utilities | Renderer, InputHandler, HUD, CollisionDetector | Cross-cutting concerns: drawing, input, physics |

## **3.2 Agreed Interfaces (Day 1 Contract)**

Before splitting work, all members must agree on these method signatures:

| Entity | getX(), getY(), getWidth(), getHeight(), getHP(), update(), render(Graphics2D g) |
| :---: | :---- |
| **Player** | shoot(direction), revive(), applyPowerUp(PowerUp p), getFacingDirection() |
| **Enemy** | moveToward(float x, float y), takeDamage(int dmg), isDead(), dropPowerUp() |
| **Collision** | checkBulletEnemyCollision(List\<Bullet\>, List\<Enemy\>), checkEnemyPlayerCollision(List\<Enemy\>, Player) |

## **3.3 Round Configuration**

| Round | Melee | Ranged | Semi-Boss | Enemy HP | Notes |
| ----- | ----- | ----- | ----- | ----- | ----- |
| R1 | 20 | 0 | 0 | 1 | Tutorial feel |
| R2 | 30 | 10 | 0 | 3 | Ranged introduced |
| R3 | 40 | 20 | 3 | 10 | Semi-boss appears |
| R4 | 50 | 30 | 10 | 20 | High pressure |
| R5 | 60 | 40 | 20 | 50 | Final wave |

# **4\. Milestone 1 Development Plan**

Target: A working single-player game covering Round 1 fully and Rounds 2–5 structurally. Due April 27–May 1\.

## **4.1 Day 1 — Independent Foundations**

| Goal | Each member builds their isolated module stub. No integration yet. |
| :---: | :---- |

### **Member A Tasks**

- [x] ~~Create Java Swing window (JFrame \+ JPanel) with a black background~~

- [x] ~~Implement GameLoop using System.nanoTime() at 60fps target~~

- [x] ~~Implement InputHandler using KeyListener (track held keys in a Set\<Integer\>)~~

- [x] ~~Draw a placeholder 32x32 white rectangle as the player on screen~~

- [x] ~~Player moves with WASD using InputHandler~~

End-of-day check: Window opens, player box moves smoothly, console prints "tick" 60 times per second.

### **Member B Tasks**

- [x] ~~Write Entity abstract class with x, y, width, height, hp, update(), render()~~

- [x] ~~Write Player extends Entity: direction enum, shoot() creating a Bullet, cooldown timer~~

- [x] ~~Write Bullet: direction vector, moves 5px/tick, isExpired() when out of bounds~~

- [x] ~~Write Enemy abstract: moveToward(x,y), takeDamage(), isDead()~~

- [x] ~~Write MeleeEnemy: moves toward player, deals contact damage~~

End-of-day check: In a standalone test panel, pressing Space fires a bullet that travels across the screen.

### **Member C Tasks**

- [x] ~~Write EntityManager with List\<Enemy\>, List\<Bullet\>, List\<PowerUp\>; iterates update/render~~

- [x] ~~Write EnemySpawner: given round number, creates correct enemy list at random edge positions~~

- [x] ~~Write RoundManager: holds current round, triggers spawn, detects enemyCount \== 0~~

- [x] ~~Write PowerUp base class and AttackSpeedPowerUp with apply(Player p)~~

End-of-day check: Console prints 20 MeleeEnemy (x,y) coordinates all at the arena edges.

  **End of Day 1: Hold a 20-minute sync. Create Main.java that wires A’s loop to B’s Player and C’s EntityManager. Agree on any interface changes before Day 2\.**

## **4.2 Day 2 — Integration**

| Goal | Full playable Round 1: enemies appear, player fights, round ends on clear. |
| :---: | :---- |

- [x] ~~Morning: Plug Player and Bullet into Renderer. Player appears on screen, shoots visible projectiles.~~

- [x] ~~Midday: Plug EntityManager into GameLoop. Enemies appear at edges and walk toward center.~~

- [x] ~~Early afternoon: Wire CollisionDetector. Bullets kill enemies; enemy contact reduces player HP.~~

- [x] ~~Late afternoon: Wire RoundManager. Round 1 ends when all enemies are dead; brief pause, then Round 2 spawns.~~

- [x] ~~Evening: Add HUD overlay (HP bar, round number, enemy count remaining).~~

End-of-day check: Full Round 1 playable from spawn to clear. HUD updates in real time.

## **4.3 Day 3 — Polish & Testing**

| Goal | All 5 rounds functional, power-ups working, revive tested, demo-ready. |
| :---: | :---- |

- [x] ~~Morning: Power-ups drop from dead enemies (random chance). Player walks over them to collect. At least HP and Attack Speed work.~~

- [x] ~~Midday: Test all 5 round configs. Verify enemy counts and HP values match the design spec.~~

- [x] ~~Early afternoon: Player revive mechanic — on HP zero, respawn at area where there are least enemies, clear power-up list, brief invincibility frames.~~

- [x] ~~Late afternoon: Game over screen after max deaths. Round clear transition screen.~~

- [x] ~~Evening: Sprite assets replacing placeholder rectangles. Bug fix pass. Record demo for submission.~~

# **5\. Milestone 2 Development Plan — Networked Multiplayer \+ Assets & Screens** 

**Target**: A fully functional 4-player networked multiplayer version with synchronized gameplay, lobby system, complete screen flow, sprite/audio integration, and stress-tested server authority.

**Due May 11–13. Begin only after Milestone 1 is fully working.**

## **5.1 Day 1 — Networking Foundations**

| Goal | Establish the multiplayer networking backbone and screen architecture. No gameplay synchronization yet. |
| :---: | :---- |

### **Member A Tasks (Networking Core / Engine)** 

- [ ] Create `GameServer.java`  
* Open `ServerSocket` on port `5000`  
* Accept up to 4 client connections  
* Print connection logs in console  
- [ ] Update `GameClient.java`  
* Connect to server using TCP socket  
* Send simple ping/test packet  
- [ ] Create `ClientHandler.java`  
* One thread per connected player  
* Maintain `ObjectInputStream` and `ObjectOutputStream`  
- [ ] Create `NetworkMessage.java`  
* Serializable message packet  
* Contains:  
  * `MessageType`  
  * `playerId`  
  * `payload`  
  * `timestamp`  
- [ ] Create `MessageType.java`  
* Add:  
  * `CONNECT`  
  * `CONNECTED`  
  * `DISCONNECT`  
  * `PING`  
  * `ERROR`

**End-of-day check**: Server accepts multiple clients. Console prints: 

Player connected: 0  
Player connected: 1 

---

### **Member B Tasks (Multiplayer Entities / Rendering)** 

- [ ] Update `Player.java`  
* Add `playerId`  
* Add multiplayer-safe serialization support  
- [ ] Update `GameState.java`  
* Ensure serializable support  
* Store:  
  * all players  
  * bullets  
  * enemies  
  * Power-ups  
- [ ] Create placeholder multiplayer rendering  
      * Render all connected players  
      * Different colors per player  
- [ ] Add interpolation-safe position updating  
      * Client-side rendering only  
      * No gameplay authority

**End-of-day check**: Two local clients display two separate player rectangles. 

---

### **Member C Tasks (Screens / Systems / Assets)** 

- [ ] Create `ScreenManager.java`  
      * Handles active screen switching  
- [ ] Create `Screen.java`  
      * Interface:  
        * update()  
        * render(Graphics2D g)  
        * onEnter()  
        * onExit()  
- [ ] Create:  
      * `MainMenuScreen.java`  
      * `LobbyScreen.java`  
- [ ] Create `AssetManager.java`  
      * Load sprites once at startup  
      * Magenta fallback image if missing  
- [ ] Create `AudioManager.java`  
      * Load WAV files safely  
      * Stub-safe if assets missing

**End-of-day check**: Main Menu appears. Lobby screen switches correctly. Missing sprites display magenta placeholders instead of crashing. 

---

**End of Day 1: Hold a 30-minute integration sync: confirm packet structure, confirm serialization format, confirm screen flow, agree on networking rules before synchronization work** 

## 

## 

## 

## 

## **4.2 Day 2 — State Synchronization** 

| Goal | Synchronize player movement and game state across all clients. |
| :---: | :---- |

**Member A Tasks**

1. Implement server tick loop at `20 ticks/sec`  
2. Broadcast `GameState` to all clients  
3. Handle disconnect cleanup safely  
4. Prevent server crashes on socket errors

**End-of-day check:** Server continuously broadcasts synchronized player positions.

---

**Member B Tasks**

5. Create `InputSnapshot.java`  
   * WASD state  
   * facing direction  
   * shooting flag  
6. Client sends input snapshots to server  
7. Server updates:  
   * player movement  
   * player direction  
8. Clients render authoritative positions only

**End-of-day check:** All clients see all players moving in real time.

---

**Member C Tasks**

9. Update `LobbyScreen.java`  
   * Connected player list  
   * Ready toggle  
   * Host start button  
10. Create `LobbyState.java`  
    * Serializable lobby information  
11. Synchronize lobby ready status across all clients  
12. Integrate sprite loading into multiplayer rendering

**End-of-day check:** Lobby updates in real time when players connect or toggle ready.

---

**End of Day 2: Run first 4-player local test: verify no crashes, verify synchronized movement, verify correct player IDs**

## 

## 

## 

## **4.3 Day 3 — Gameplay Synchronization** 

| Goal | Synchronize bullets, enemies, collisions, HP, rounds, and power-ups. |
| :---: | :---- |

**Member A Tasks**

- [ ] Broadcast synchronized bullets  
- [ ] Synchronize pause system  
- [ ] Synchronize Game Over events

**End-of-day check**: Pause freezes all clients simultaneously.  
---

**Member B Tasks**

- [ ] Move all collision logic server-side  
- [ ] Synchronize:  
      * bullets  
      * enemy movement  
      * enemy deaths  
      * player HP  
- [ ] Clients render received entity states only

**End-of-day check:** Enemy deaths and bullet collisions appear correctly on all clients.  
---

**Member C Tasks**

- [ ] Synchronize:  
      * rounds  
      * enemy spawning  
      * power-up spawning  
      * power-up collection  
- [ ] Create:  
      * `PauseScreen.java`  
      * `GameOverScreen.java`  
- [ ] Integrate:  
      * round transitions  
      * synchronized HUD updates

**End-of-day check:** All clients advance rounds simultaneously with synchronized enemy counts.

---

**End of Day 3: Record gameplay clips for testing and presentation backup.** 

## 

## 

## 

## **4.4 Day 4 — Assets, Audio, and Polish** 

| Goal | Replace placeholders with final assets and improve presentation quality.  |
| :---: | :---- |

**Member A Tasks**

- [ ] Add:  
      * Main Menu polish  
      * Pause Menu polish  
      * Screen transitions  
- [ ] Improve rendering performance

**End-of-day check:** Screen flow fully functional from Main Menu → Lobby → Game → Pause → Game Over.

---

**Member B Tasks**

- [ ] Replace rectangle placeholders with:  
      * player sprites  
      * enemy sprites  
      * bullet sprites  
- [ ] Add:  
      * hit flash  
      * death fade

**End-of-day check:** Gameplay fully sprite-based.

---

**Member C Tasks**

- [ ] Integrate:  
      * ambient music  
      * shooting SFX  
      * hit sounds  
      * power-up sounds  
- [ ] Add:  
      * floor tiles  
      * border tiles  
- [ ] Finalize HUD:  
      * HP bars  
      * round counter  
      * enemy count  
      * power-up indicators

**End-of-day check:** Game visually resembles final project presentation quality.

---

**End of Day 4: Record gameplay clips for testing and presentation backup.**  

## **4.5 Day 5 — Stress Testing & Bug Fixing**  

| Goal | Stabilize multiplayer gameplay and remove major desyncs.  |
| :---: | :---- |

**Team Tasks**

- [ ] Run 4-player stress tests  
- [ ] Test:  
      * disconnect handling  
      * revive synchronization  
      * pause synchronization  
      * round synchronization  
      * power-up synchronization  
- [ ] Fix:  
      * desync bugs  
      * null pointer crashes  
      * duplicate packet handling  
      * socket cleanup issues  
- [ ] Verify clean repository build from fresh clone

---

**End of Day 5: No critical desyncs, stable 4-player gameplay, all milestone checklist items functional**

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## **4.5 Day 6 — In-Game Chat Overlay** 

| Goal | Add multiplayer chat system if core requirements are stable.  |
| :---: | :---- |

**Team Tasks**

- [ ] Create `ChatMessage.java`  
      * sender  
      * text  
      * timestamp  
- [ ] Create `ChatPanel.java`  
      * scrollable overlay  
      * input field  
- [ ] Server relays chat packets to all clients  
- [ ] Press `T` to toggle chat overlay

---

**End of Day 6: Players can send synchronized messages in lobby and in-game.** 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# **6\. Asset Checklist**

## **6.1 Visual Assets**

| Asset | Description | Priority | Status |
| ----- | ----- | :---: | :---: |
| Player sprite (4 directions) | Facing up/down/left/right, or 1 rotated | **M1 Must** | **Pending** |
| Melee enemy sprite | Simple walking sprite, 1–2 frames | **M1 Must** | **Pending** |
| Ranged enemy sprite | Visually distinct from melee | **M1 Must** | **Pending** |
| Semi-boss sprite | Larger, more imposing design | **M1 Must** | **Pending** |
| Player & enemy bullet sprites | Distinct colors; rectangle fallback OK | **Day 2** | **Pending** |
| 4 power-up icons | ATK, DMG, HP, SPD — color circles OK for M1 | **Day 3** | **Pending** |
| Arena floor tile \+ border tile | Repeating ground texture | **Day 1** | **Pending** |

## **6.2 Recommended Free Asset Sources**

| Source | What’s Available | License |
| ----- | ----- | ----- |
| kenney.nl | Roguelike/RPG Pack, Tiny Dungeon — best fit for this project | CC0 (no attribution needed) |
| opengameart.org | Sprites, music, SFX, wide variety | CC0 / CC-BY |
| itch.io/game-assets | Pixel art packs, characters, tilesets | Varies — check each |
| freesound.org | SFX and ambient loops | Varies |
| pixabay.com | Music and SFX | Royalty-free |

# **7\. Development Workflow**

## **7.1 GitHub Branching Strategy**

| Branch | Purpose |
| ----- | ----- |
| main | Stable, presentation-ready code only. Never commit directly here. |
| dev | Integration branch. All features merged here first. |
| feature/engine | Member A’s work: game loop, renderer, input |
| feature/entities | Member B’s work: player, enemies, collision |
| feature/systems | Member C’s work: rounds, spawning, power-ups |
| feature/networking | Milestone 2: all network code |
| feature/chat | Bonus: chat overlay |

## **7.2 Commit & PR Rules**

* Commit message format: \[type\] short description

* Types: feat, fix, refactor, docs, assets, test

* Example: feat: add bullet cooldown timer to Player

* Never push directly to main or dev

* Open a Pull Request to dev when a feature is ready

* At least 1 other member must review and approve before merging

* Resolve merge conflicts locally before pushing

## **7.3 Daily Standup Checklist**

Each member answers three questions, either in the group chat or in a quick 10-minute call:

- [ ] What did I finish since the last check-in?

- [ ] What am I working on today?

- [ ] Is anything blocking me?

*Good luck\! Prioritize Milestones 1 and 2\. The bonus chat only counts if both milestones are complete.*

