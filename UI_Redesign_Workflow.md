# 🎨 Holy Shot! — UI & Graphics Redesign Workflow
> **Solo Design Sprint** | Designer: Christel | Duration: 1 Day
> **Style:** Dark Horror Pixel Art | **Palette:** Void Purple · Toxic Green · Blood Red · Ghostly White

---

## 🧭 Overview

This document is your complete step-by-step guide for the single-day UI overhaul of *Holy Shot!*. You will be working with **Claude** as your coding assistant — you describe what you want, Claude writes the Java code. Your job is to **create art assets**, **give Claude visual direction**, and **test the result**.

Work through the phases **in order**. Don't skip ahead — assets must exist before screens can use them.

---

## ⚙️ Phase 0 — Setup (15 minutes)

Before touching anything:

```bash
# 1. Go to your project folder
cd /home/mirachi/CMSC137_PROJECT

# 2. Create a dedicated branch for the redesign (never work on main)
git checkout -b ui-redesign

# 3. Verify it compiled cleanly before you touch anything
javac -d out $(find src/main/java -name "*.java")
```

> ✅ Only proceed if it compiles. If not, fix existing errors first.

---

## 🖼️ Phase 1 — Asset Creation (3–4 Hours | Morning)

This is the longest phase. Do all asset creation FIRST so Phase 2 is just dropping them in.

### 1A. Tools You Need (Free)

| Tool | Purpose | Link |
|------|---------|------|
| **Piskel** | Pixel art editor + GIF export, browser-based | piskelapp.com |
| **Aseprite** | Best pixel art + sprite sheet tool (paid ~$20, worth it) | aseprite.org |
| **Libresprite** | Free fork of Aseprite | libresprite.github.io |
| **EZGif** | Optimize and resize GIF files | ezgif.com |
| **Itch.io Free Assets** | Royalty-free horror pixel art to remix | itch.io/game-assets/free/tag-horror |

> **Recommendation:** Use Piskel (free, no install) for quick sprites. Use Aseprite/Libresprite for anything complex with multiple frames.

---

### 1B. Asset Checklist & Sizes

Work through this list in order. Check each box as you finish.

#### 🎭 Enemy Sprites (In-game, Animated)

Each enemy needs **4 directional sprite sheets** (down, up, left, right) + an **idle animation GIF**.

> **Sprite sheet format:** Single PNG, frames side by side. 4 frames per direction. Each frame: **32×32 px**
> **Idle GIF:** 4-frame loop, exported from Piskel

| Asset | Filename | Frames | Notes |
|-------|----------|--------|-------|
| Aswang — sprite sheet | `aswang_sheet.png` | 4 dirs × 4 frames | Hunched silhouette, long claws |
| Tiktik — sprite sheet | `tiktik_sheet.png` | 4 dirs × 4 frames | Winged, tongue extended |
| Tikbalang — sprite sheet | `tikbalang_sheet.png` | 4 dirs × 4 frames | Horse skull head, tall |
| Aswang — idle GIF | `aswang_idle.gif` | 4-frame loop | Breathing / claw twitch |
| Tiktik — idle GIF | `tiktik_idle.gif` | 4-frame loop | Wings flutter slightly |
| Tikbalang — idle GIF | `tikbalang_idle.gif` | 4-frame loop | Head tilt / stomp |

---

#### 🧍 Player Sprites (In-game, Animated)

Each player needs **4 directional sprite sheets** in their color. Same format as enemies.

| Asset | Filename | Frames | Notes |
|-------|----------|--------|-------|
| Player 1 (Blue) | `player_blue_sheet.png` | 4 dirs × 4 frames | |
| Player 2 (Red) | `player_red_sheet.png` | 4 dirs × 4 frames | |
| Player 3 (Green) | `player_green_sheet.png` | 4 dirs × 4 frames | |
| Player 4 (Yellow) | `player_yellow_sheet.png` | 4 dirs × 4 frames | |

---

#### 🔫 Bullets

| Asset | Filename | Size | Notes |
|-------|----------|------|-------|
| Salt bullet | `bullet_salt.png` | 8×8 px | Small white pellet, slight glow |
| Holy water bullet | `bullet_holywater.png` | 8×8 px | Glowing blue-white droplet |

---

#### ✨ Power-Ups

| Asset | Filename | Size | Notes |
|-------|----------|------|-------|
| Speed boost | `powerup_speed.png` | 16×16 px | Lightning bolt, toxic green |
| Damage boost | `powerup_damage.png` | 16×16 px | Skull, blood red |
| Heal | `powerup_heal.png` | 16×16 px | Cross / heart, ghostly white |
| Attack speed | `powerup_atk.png` | 16×16 px | Clock + crosshair |

---

#### 🗺️ Tiles & Backgrounds

| Asset | Filename | Size | Notes |
|-------|----------|------|-------|
| Floor tile | `floor.png` | 32×32 px | Cracked stone/earth, dark purple-grey |
| Border/Wall tile | `border.png` | 32×32 px | Stone wall with moss or bone detail |
| Menu background | `bg_menu.png` | 800×640 px | Haunted scene or fog/mist, animated GIF preferred |
| Lobby background | `bg_lobby.png` | 800×640 px | Can reuse menu bg or darker variant |

---

#### 🎨 UI Panel Elements

| Asset | Filename | Size | Notes |
|-------|----------|------|-------|
| HP bar — frame | `hpbar_frame.png` | 100×16 px | Cracked frame, dark background |
| HP bar — fill | `hpbar_fill.png` | 96×12 px | Blood red gradient, 9-slice stretchable |
| Round banner | `round_banner.png` | 400×80 px | Scroll/banner style for "ROUND X" |
| Button — normal | `btn_normal.png` | 200×48 px | Stone/bone button |
| Button — hover | `btn_hover.png` | 200×48 px | Glowing red/green outline |

---

#### 🔤 Font Files

Download both fonts (free, Google Fonts) and save as TTF files in the resources folder:

| Font | Filename | Use |
|------|----------|-----|
| **Press Start 2P** | `PressStart2P-Regular.ttf` | Headers, titles, round counters |
| **VT323** | `VT323-Regular.ttf` | Body text, chat, player names, HUD details |

> Download from: fonts.google.com/specimen/Press+Start+2P and fonts.google.com/specimen/VT323

---

### 1C. AI Generation Prompts

Use these prompts in **Midjourney, DALL-E 3, Adobe Firefly, or Bing Image Creator**, then redraw/trace them in Piskel at the correct pixel resolution.

**General prefix to add to all prompts:**
> `pixel art, 32x32 sprite, dark horror style, Filipino folklore, transparent background, void purple and blood red palette, ghostly atmosphere, no anti-aliasing, crisp pixels`

---

**Enemy Prompts:**

```
Aswang sprite — pixel art, 32x32, Filipino aswang monster, hunched humanoid silhouette,
long black claws, hollow white eyes, dark void purple skin, walking pose facing down,
horror game sprite, transparent background, crisp pixels, no shading gradients
```

```
Tiktik sprite — pixel art, 32x32, Filipino manananggal / tiktik creature, severed upper
body with bat wings, long tongue, blood dripping, blood red and ghostly white palette,
horror game enemy sprite, facing right, transparent background
```

```
Tikbalang sprite — pixel art, 32x32, Filipino tikbalang, tall demonic horse-skull head
on humanoid body, glowing green eyes, dark armor, semi-boss enemy, intimidating stance,
horror game sprite sheet, transparent background
```

---

**Player Prompts (repeat for each color):**

```
Player character sprite — pixel art, 32x32, spirit hunter warrior, [BLUE/RED/GREEN/YELLOW]
color scheme, hooded cloak, holding salt-loaded crossbow pistol, Filipino mythology
aesthetic, horror game protagonist, facing downward, transparent background
```

---

**UI / Background Prompts:**

```
Game menu background — pixel art, 800x640, haunted Filipino barrio at night, fog rolling
in, glowing purple lanterns, twisted balete tree, blood moon, silhouettes of aswang
in background, deep void purple atmosphere, horror game title screen art
```

```
Floor tile — pixel art, 32x32 seamless tile, cracked stone floor, dark purple-grey,
dried blood stains, subtle bone fragments embedded in stone, horror dungeon, top-down view
```

```
Stone wall border tile — pixel art, 32x32 seamless, ancient stone wall, moss-covered,
etched skulls, ghostly glow seeping through cracks, dark purple and deep grey
```

---

### 1D. Sprite Sheet Layout Rules

When exporting your sprite sheets from Piskel/Aseprite, follow this exact layout so the Java code can auto-slice them:

```
player_blue_sheet.png layout (128 × 128 px total):

Row 0 (Y=0):   [Down-0] [Down-1] [Down-2] [Down-3]
Row 1 (Y=32):  [Up-0]   [Up-1]   [Up-2]   [Up-3]
Row 2 (Y=64):  [Left-0] [Left-1] [Left-2] [Left-3]
Row 3 (Y=96):  [Right-0][Right-1][Right-2][Right-3]

Each cell = 32×32 px
```

Save all assets to:
```
src/main/resources/assets/sprites/
src/main/resources/assets/tiles/
src/main/resources/assets/fonts/
src/main/resources/assets/audio/
```

---

## 💻 Phase 2 — Screen Redesign with Claude (3–4 Hours | Afternoon)

Do each screen one at a time. For each one:
1. Have your assets ready
2. Open `CLAUDE_PROMPT_UI.md` → paste it at the start of a **new Claude conversation**
3. Then describe what you want for that screen

### Screen Order (do in this sequence):

| # | Screen | Java File | Priority |
|---|--------|-----------|----------|
| 1 | **Main Menu** | `MainMenuScreen.java` | Highest — first impression |
| 2 | **HUD (In-game)** | `HUD.java` | High — players see this most |
| 3 | **Game Over** | `GameOverScreen.java` | High — emotional payoff |
| 4 | **Lobby** | `LobbyScreen.java` | Medium |
| 5 | **Pause** | `PauseScreen.java` | Medium — overlay only |

### How to talk to Claude for each screen:

Open a new Claude session, paste the CLAUDE_PROMPT_UI.md content first, then say something like:

> *"I want to redesign MainMenuScreen.java. The background should use bg_menu.gif. The title 'HOLY SHOT!' should use Press Start 2P font in blood red with a ghostly white drop shadow. There should be 3 buttons: HOST, JOIN, EXIT — styled using btn_normal.png and btn_hover.png. On hover, buttons glow green. The whole screen has a vignette darkening at the edges."*

Claude will write all the Java code. You test it, then tell Claude what to fix.

---

## 🧪 Phase 3 — Integration & Testing (1 Hour | Evening)

```bash
# Compile and check for errors
javac -d out $(find src/main/java -name "*.java")

# Run the server
java -cp out:src/main/resources com.shooter.server.GameServer

# Run a client (separate terminal)
java -cp out:src/main/resources com.shooter.client.GameClient
```

**Test checklist:**
- [ ] Main Menu loads with background art and styled buttons
- [ ] Hover effects work on buttons
- [ ] Lobby screen shows player names in correct font
- [ ] In-game HUD shows HP bars, round number, enemy count
- [ ] Pause overlay appears correctly (semi-transparent, not blocking gameplay view)
- [ ] Game Over screen shows stats in pixel font
- [ ] Player sprites change direction when moving
- [ ] Enemy sprites animate (idle GIF or sprite sheet cycle)
- [ ] No pink/magenta fallback sprites visible (means an asset didn't load)

---

## 💾 Phase 4 — Save Your Work (15 Minutes | End of Day)

```bash
git add .
git commit -m "UI redesign: dark horror pixel art theme — all screens + assets"
git push origin ui-redesign
```

---

## 🚨 Rules for the Day

1. **Never edit server-side files** — `GameServer.java`, `ClientHandler.java`, `GameManager.java`, etc.
2. **Never touch networking files** — `NetworkMessage.java`, `MessageType.java`, etc.
3. **Only edit** files in: `client/screens/`, `ui/`, `shared/util/AssetManager.java`, and `shared/util/Constants.java`
4. **Compile after every screen** — don't let errors pile up
5. **Commit often** — at least after finishing each screen

---

## 📁 Final Asset Folder Structure

```
src/main/resources/assets/
├── sprites/
│   ├── player_blue_sheet.png
│   ├── player_red_sheet.png
│   ├── player_green_sheet.png
│   ├── player_yellow_sheet.png
│   ├── aswang_sheet.png
│   ├── aswang_idle.gif
│   ├── tiktik_sheet.png
│   ├── tiktik_idle.gif
│   ├── tikbalang_sheet.png
│   ├── tikbalang_idle.gif
│   ├── bullet_salt.png
│   ├── bullet_holywater.png
│   ├── powerup_speed.png
│   ├── powerup_damage.png
│   ├── powerup_heal.png
│   └── powerup_atk.png
├── tiles/
│   ├── floor.png
│   └── border.png
├── ui/
│   ├── bg_menu.gif (or .png)
│   ├── bg_lobby.png
│   ├── hpbar_frame.png
│   ├── hpbar_fill.png
│   ├── round_banner.png
│   ├── btn_normal.png
│   └── btn_hover.png
├── fonts/
│   ├── PressStart2P-Regular.ttf
│   └── VT323-Regular.ttf
└── audio/
    ├── ambient.wav
    ├── shoot.wav
    ├── hit.wav
    └── powerup.wav
```
