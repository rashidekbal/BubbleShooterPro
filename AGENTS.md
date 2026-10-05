# Antigravity Workspace Rules & Guidelines — Bubble Shooter Pro

## Mandatory Documentation Maintenance Rule

> **CRITICAL REQUIREMENT**: Every time any changes, bug fixes, features, or architectural modifications are made to this project, **the documentation MUST be updated** immediately within the same turn/task.

### Scope & Enforced Actions
Whenever modifications are made to:
- **Android Game Client (`BubbleShooterpro/`)**:
  - State machine, game loop, rendering cycle, touch events
  - Bubble types, colors, physics, raycasting, wall bounce, snap coordinates
  - Dynamic shot selector, candidate calculations, launcher bubble sanitization
  - Objectives, star scoring formulas, scoring/combos, sound effects
  - Economy, monetization, AdMob mediation, IAP, cloud save
- **Game Designer Studio (`game-designer-studio/`)**:
  - Canvas editor, hex board drawing, palette options, tools (brush, fill, eraser)
  - Solver and mathematical complexity analyzer
  - Pin positioner, world explorer, campaign generator
  - Electron main process, preload IPC APIs, build/packaging
- **Level & World Data (`assets/levels/`, `assets/worlds_config.json`)**:
  - Level JSON schemas, row tokens, color lists, shots allowances, star thresholds
  - World definitions, biomes, pin coordinates

### Documentation Files to Maintain
1. **[`DOCUMENTATION.md`](file:///d:/projects/bubble%20shooter%20pro/DOCUMENTATION.md)**:
   - Primary single source of truth for complete technical architecture, data schemas, API interactions, and maintenance runbooks.
   - Update architecture diagrams, mathematical formulas, color tables, booster tables, and recipes whenever code behavior changes.
2. **[`README.md`](file:///d:/projects/bubble%20shooter%20pro/README.md)**:
   - High-level overview, quick start commands, repository structure, and cross-references.
3. **[`LEVEL_DESIGN_GUIDE.md`](file:///d:/projects/bubble%20shooter%20pro/LEVEL_DESIGN_GUIDE.md)** & **[`LEVEL_DESIGN_RULES_FOR_AI.md`](file:///d:/projects/bubble%20shooter%20pro/LEVEL_DESIGN_RULES_FOR_AI.md)**:
   - Update if design constraints, token definitions, or balancing rules evolve.

### Verification Checklist Before Finishing Any Task
- [ ] Code compiles and tests pass (`.\gradlew.bat testDebugUnitTest`).
- [ ] Any new or modified constants/enums (e.g. colors, boosters) are documented in [`DOCUMENTATION.md`](file:///d:/projects/bubble%20shooter%20pro/DOCUMENTATION.md).
- [ ] All developer recipes and CLI commands remain tested and valid.
