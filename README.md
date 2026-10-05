# Bubble Shooter Pro

A high-performance, production-ready arcade puzzle game for Android, paired with a custom desktop Level Designer & Campaign Studio.

---

## Quick Navigation
- 📖 **[Complete Technical Architecture & Operation Manual](DOCUMENTATION.md)** — Complete end-to-end documentation covering hexagonal mathematics, physics engine, game loop, dynamic shot selection, booster mechanics, schemas, and developer recipes.
- 📐 **[Level Design Guide](LEVEL_DESIGN_GUIDE.md)** — Gameplay pacing, world progression, token definitions, and balancing guidelines.
- 🤖 **[Level Design Rules for AI](LEVEL_DESIGN_RULES_FOR_AI.md)** — Strict placement rules and constraints for procedural generation.
- 🎨 **[400 Worlds Prompt Encyclopedia](400_WORLDS_PROMPT_ENCYCLOPEDIA.md)** — Biome art prompts and visual world database.
- 📜 **[Workspace Rules (GEMINI.md)](GEMINI.md)** — Mandatory development rules, including automatic documentation synchronization.

---

## Project Structure
- **`BubbleShooterpro/`**: Android Studio project (Java, Android SDK, Gradle, custom SurfaceView/Canvas 60 FPS renderer, custom raycasting physics, Google Play In-App Billing, Google AdMob, Google Play Games Cloud Save, 2-column Shop Grid, Reward Center with daily diamonds, and non-intrusive Star Milestones).
- **`game-designer-studio/`**: Desktop Electron app for visual hex level design, deep solver complexity analysis, and saga map pin positioning.
- **`Game Designer Studio.exe`**: Pre-built portable Windows desktop level editor executable.
- **`worlds/`**: High-resolution world map and in-game background artwork for 400 biomes.

---

## Quick Start Commands

### Android Game Client
```powershell
cd BubbleShooterpro

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug

# Build release bundle (AAB)
.\gradlew.bat bundleRelease
```

### Desktop Game Designer Studio
```powershell
cd game-designer-studio

# Run in dev mode
npm start

# Package standalone Windows portable .exe
cmd.exe /c "npm run dist:win"
```
Or directly double-click `Game Designer Studio.exe` in the root folder.
