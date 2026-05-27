# CLAUDE_PROMPT — Holy Shot! UI Redesign Session
> Paste this ENTIRE file at the start of every new Claude conversation during the UI redesign sprint.

---

## Who You Are Talking To

You are helping **Christel**, the UI/Graphics designer for the game *Holy Shot!*. She is redesigning all visual elements for a 1-day solo sprint. She will describe what she wants visually — **you write all the Java code**. Do not ask her to write code herself. She will test it and give you feedback.

---

## About the Project

**Holy Shot!** is a 2D top-down multiplayer co-op PvE shooter built in **Java** using **Java Swing** and **Graphics2D** for all rendering. It is NOT a web app. There is no game engine — everything is custom-built.

- Up to 4 players cooperate to survive 5 rounds of enemy waves
- **Theme:** Elite spirit-cleansing hunters battling Filipino folklore monsters in haunted arenas
- **Enemies:** Aswang (melee), Tiktik (ranged), Tikbalang (semi-boss)
- **Power-ups:** Speed, Damage, Heal, Attack Speed

---

## Visual Style (CRITICAL — Follow This for ALL Code)

| Property | Value |
|----------|-------|
| **Art style** | Dark horror pixel art |
| **Color palette** | Deep void purple `#1A0A2E`, Toxic green `#39FF14`, Blood red `#8B0000` / `#CC0000`, Ghostly white `#E8E8F0` |
| **Accent glow colors** | Purple glow `#7B2FBE`, Green glow `#00FF88` |
| **Background default** | Near-black `#0D0D1A` |
| **Font — Headers/Titles** | Press Start 2P (TTF file at `src/main/resources/assets/fonts/PressStart2P-Regular.ttf`) |
| **Font — Body/HUD/Names** | VT323 (TTF file at `src/main/resources/assets/fonts/VT323-Regular.ttf`) |
| **Font fallback** | `new Font("Monospaced", Font.BOLD, size)` if TTF not found |
| **Rendering quality** | Always enable: `g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)` (pixel art should stay crisp, NO anti-aliasing) |

### Color Constants to Use in Code

```java
// Paste these at the top of any screen file you write
private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
private static final Color DARK_BG      = new Color(0x0D, 0x0D, 0x1A);
private static final Color PURPLE_GLOW  = new Color(0x7B, 0x2F, 0xBE);
private static final Color GREEN_GLOW   = new Color(0x00, 0xFF, 0x88);
```

---

## Architecture — How the Code is Structured

### File Locations (only edit these during UI redesign)

```
src/main/java/com/shooter/
├── client/screens/          ← EDIT THESE (all screen files)
│   ├── Screen.java          ← Interface, DO NOT CHANGE
│   ├── MainMenuScreen.java  ← Title screen, HOST/JOIN/EXIT buttons
│   ├── LobbyScreen.java     ← Connected players, ready toggle, host starts
│   ├── GameScreen.java      ← Wraps gameplay, calls Renderer and HUD
│   ├── PauseScreen.java     ← Semi-transparent overlay, any player can pause
│   └── GameOverScreen.java  ← Post-game: kills per player, rounds cleared
│
├── ui/
│   ├── HUD.java             ← EDIT THIS — HP bars, round counter, enemy count
│   └── ChatPanel.java       ← leave alone unless asked
│
└── shared/util/
    ├── AssetManager.java    ← EDIT THIS — add new image/font/GIF loaders
    └── Constants.java       ← EDIT THIS — add new size/layout constants
```

### Files You Must NEVER Touch

```
server/GameServer.java
server/ClientHandler.java
server/GameManager.java
server/EntityManager.java
server/EnemySpawner.java
server/RoundManager.java
network/NetworkMessage.java
network/MessageType.java
network/InputSnapshot.java
network/LobbyState.java
network/GameOverStats.java
shared/model/Player.java       (fields only, not rendering)
shared/model/Enemy.java
shared/model/Bullet.java
shared/model/PowerUp.java
shared/model/GameState.java
shared/logic/CollisionDetector.java
shared/logic/RoundConfig.java
```

---

## Screen Interface Contract

Every screen implements this interface. Never break these method signatures:

```java
public interface Screen {
    void update();                    // called every game tick
    void render(Graphics2D g2d);      // called every render frame
    void onEnter();                   // called when screen becomes active
    void onExit();                    // called when screen is leaving
    void handleInput(KeyEvent e);     // optional — add if screen needs input
}
```

---

## Asset System — How Images Load

All images are loaded through `AssetManager`. The pattern is:

```java
// In AssetManager.java — load an image:
public static BufferedImage load(String relativePath) {
    // loads from src/main/resources/ root
    // returns a magenta 32x32 fallback image if file not found
}

// In AssetManager.java — load a font:
public static Font loadFont(String relativePath, float size) {
    // loads TTF from src/main/resources/assets/fonts/
    // returns Font("Monospaced", BOLD, size) as fallback
}
```

### Sprite Sheet Slicing Pattern

All character sprite sheets follow this layout (128×128 px, 4 directions × 4 frames, each frame 32×32):

```
Row 0 (Y=0):   DOWN  frames [0][1][2][3]
Row 1 (Y=32):  UP    frames [0][1][2][3]
Row 2 (Y=64):  LEFT  frames [0][1][2][3]
Row 3 (Y=96):  RIGHT frames [0][1][2][3]
```

When writing code to slice a sprite sheet:

```java
// Extract frame: direction (0=down, 1=up, 2=left, 3=right), frame index (0-3)
BufferedImage frame = spriteSheet.getSubimage(
    frameIndex * 32,      // X
    directionRow * 32,    // Y
    32, 32                // width, height
);
```

### Animated GIF for UI Screens

For animated GIF backgrounds (menu, lobby), use this pattern in `render()`:

```java
// Declare at class level:
private ImageIcon animatedGif;

// In onEnter() or constructor:
URL gifUrl = getClass().getResource("/assets/ui/bg_menu.gif");
animatedGif = new ImageIcon(gifUrl);

// In render(Graphics2D g2d):
animatedGif.getImage().flush(); // allows frame cycling with observer
g2d.drawImage(animatedGif.getImage(), 0, 0, panelWidth, panelHeight, panel);
// 'panel' is the JPanel passed in as ImageObserver for GIF animation to work
```

---

## How Screens Receive the Game Panel (Important!)

`ScreenManager` manages which screen is active. Each screen is constructed with access to the main `GamePanel` (a `JPanel`). Use this reference as the `ImageObserver` when drawing GIFs.

Each screen's constructor typically looks like:
```java
public MainMenuScreen(GamePanel panel, ScreenManager screenManager, GameClient client) {
    this.panel = panel;
    this.screenManager = screenManager;
    this.client = client;
}
```

---

## Drawing Techniques Claude Should Use

### Drop Shadow Text (Blood Red Text with Purple Shadow)
```java
private void drawShadowText(Graphics2D g2d, String text, int x, int y, Font font, Color textColor, Color shadowColor) {
    g2d.setFont(font);
    g2d.setColor(shadowColor);
    g2d.drawString(text, x + 3, y + 3);  // shadow offset
    g2d.setColor(textColor);
    g2d.drawString(text, x, y);
}
```

### Glowing Effect (Toxic Green Glow Around HP Bar)
```java
private void drawGlow(Graphics2D g2d, Shape shape, Color glowColor, int radius) {
    g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 60));
    for (int i = radius; i > 0; i--) {
        g2d.setStroke(new BasicStroke(i * 2));
        g2d.draw(shape);
    }
    g2d.setStroke(new BasicStroke(1));
}
```

### Vignette Effect (Darken Screen Edges)
```java
private void drawVignette(Graphics2D g2d, int w, int h) {
    RadialGradientPaint vignette = new RadialGradientPaint(
        w / 2f, h / 2f,
        Math.max(w, h) / 1.5f,
        new float[]{0f, 1f},
        new Color[]{new Color(0,0,0,0), new Color(0,0,0,200)}
    );
    g2d.setPaint(vignette);
    g2d.fillRect(0, 0, w, h);
}
```

### Semi-Transparent Dark Panel (For Pause Overlay, Popups)
```java
private void drawDarkPanel(Graphics2D g2d, int x, int y, int w, int h, int alpha) {
    g2d.setColor(new Color(0, 0, 0, alpha)); // alpha: 0-255
    g2d.fillRoundRect(x, y, w, h, 16, 16);
    g2d.setColor(new Color(0x7B, 0x2F, 0xBE, 180)); // purple border
    g2d.setStroke(new BasicStroke(2));
    g2d.drawRoundRect(x, y, w, h, 16, 16);
}
```

### Flickering Text Effect (Horror Atmosphere)
```java
private float flickerAlpha = 1.0f;
private int flickerTimer = 0;

// In update():
flickerTimer++;
if (flickerTimer % 8 == 0) {
    flickerAlpha = 0.7f + (float)(Math.random() * 0.3f);
}

// In render():
g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flickerAlpha));
// draw text here
g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // reset
```

---

## Animation System (Sprite Sheet Cycling)

For walking/moving sprites in the game, use this pattern in any renderer that draws players or enemies:

```java
private int animFrame = 0;
private int animTimer = 0;
private static final int ANIM_SPEED = 8; // ticks per frame

// In update():
animTimer++;
if (animTimer >= ANIM_SPEED) {
    animFrame = (animFrame + 1) % 4;
    animTimer = 0;
}

// Direction mapping from shared Direction enum:
int dirRow; // 0=DOWN, 1=UP, 2=LEFT, 3=RIGHT
switch (player.getDirection()) {
    case DOWN:  dirRow = 0; break;
    case UP:    dirRow = 1; break;
    case LEFT:  dirRow = 2; break;
    case RIGHT: dirRow = 3; break;
    default:    dirRow = 0;
}

BufferedImage frame = spriteSheet.getSubimage(animFrame * 32, dirRow * 32, 32, 32);
g2d.drawImage(frame, player.getX(), player.getY(), 32, 32, null);
```

---

## Game Window Size

```java
// From Constants.java:
PANEL_WIDTH  = 800   // pixels
PANEL_HEIGHT = 640   // pixels
TILE_SIZE    = 32    // pixels
```

---

## How to Start Every Claude Session

1. Paste this entire file's content at the start of chat
2. Then say which screen you are working on
3. Describe the visual result you want (colors, layout, effects, animations)
4. Claude will write the complete Java file for you
5. Test it, report what looks wrong, Claude fixes it

---

## Example Prompt Template (Copy and Adapt)

> "I want to redesign **[SCREEN NAME]** found in **[FILE PATH]**. 
> 
> Here is what I want:
> - Background: [describe — image, color, animated GIF, particles]
> - Title/header: [font size, color, position, effects like flicker or shadow]
> - Buttons/elements: [list each UI element and what it should look like]
> - Hover effects: [what happens when mouse is over a button]
> - Animations: [anything that moves, pulses, flickers]
> - Any other details: [...]
> 
> Please write the complete updated Java file for [FILE NAME]."

---

## Known Working Asset Paths (once files are placed)

```java
// Backgrounds
"/assets/ui/bg_menu.gif"
"/assets/ui/bg_lobby.png"

// Sprite sheets (in-game characters)
"/assets/sprites/player_blue_sheet.png"
"/assets/sprites/aswang_sheet.png"

// Idle GIFs (enemy idle animation on screens)
"/assets/sprites/aswang_idle.gif"

// UI elements
"/assets/ui/hpbar_frame.png"
"/assets/ui/hpbar_fill.png"
"/assets/ui/btn_normal.png"
"/assets/ui/btn_hover.png"
"/assets/ui/round_banner.png"

// Fonts
"/assets/fonts/PressStart2P-Regular.ttf"
"/assets/fonts/VT323-Regular.ttf"
```

---

## If an Asset Is Missing

The game has a fallback: missing images render as a **magenta (hot pink) square**. If you see pink/magenta in testing, the asset path is wrong or the file isn't placed correctly.

---

## Final Reminder

- Christel only edits: `client/screens/`, `ui/HUD.java`, `shared/util/AssetManager.java`
- She never touches: server files, network files, shared model/logic files
- After every screen is done: `javac -d out $(find src/main/java -name "*.java")`
- Commit after each screen: `git add . && git commit -m "redesign: [screen name]"`
