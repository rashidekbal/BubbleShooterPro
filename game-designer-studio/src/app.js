// Game Designer Studio - Client Application Logic

const COLOR_MAP = {
  "R": { name: "Red", class: "bubble-R", emoji: "🔴" },
  "G": { name: "Green", class: "bubble-G", emoji: "🟢" },
  "B": { name: "Blue", class: "bubble-B", emoji: "🔵" },
  "Y": { name: "Yellow", class: "bubble-Y", emoji: "🟡" },
  "P": { name: "Purple", class: "bubble-P", emoji: "🟣" },
  "O": { name: "Orange", class: "bubble-O", emoji: "🟠" },
  "C": { name: "Cyan", class: "bubble-C", emoji: "🩵" },
  "*": { name: "Rainbow", class: "bubble-STAR", emoji: "🌈" },
  "X": { name: "Bomb", class: "bubble-X", emoji: "💣" },
  "L": { name: "Lightning", class: "bubble-L", emoji: "⚡" },
  "F": { name: "Fireball", class: "bubble-F", emoji: "🔥" },
  "S": { name: "Stone", class: "bubble-S", emoji: "🪨" },
  "T": { name: "Target", class: "bubble-T", emoji: "🎯" },
  ".": { name: "Empty", class: "bubble-EMPTY", emoji: "✕" }
};

const CODE_TO_NAME = {
  "R": "RED",
  "G": "GREEN",
  "B": "BLUE",
  "Y": "YELLOW",
  "P": "PURPLE",
  "O": "ORANGE",
  "C": "CYAN"
};

const NAME_TO_CODE = {
  "RED": "R",
  "GREEN": "G",
  "BLUE": "B",
  "YELLOW": "Y",
  "PURPLE": "P",
  "ORANGE": "O",
  "CYAN": "C"
};

// Preset Patterns Library (14 Distinct Patterns)
const PRESET_PATTERNS = [
  {
    id: "diamond_core",
    name: "Diamond Core",
    icon: "💎",
    category: "Geometric",
    desc: "A concentrated diamond fortress with dual-color concentric layers.",
    rows: [
      "...RRR...",
      "..RRRR..",
      ".RRBBRR.",
      ".RBBBBR.",
      "..RBRB..",
      "...RR..."
    ]
  },
  {
    id: "rainbow_arch",
    name: "Rainbow Waves",
    icon: "🌈",
    category: "Waves",
    desc: "Vibrant sweeping diagonal bands of 5 colors.",
    rows: [
      "RRGGYYBBP",
      "RGGYYBBP",
      "GGYYBBPPR",
      "GYYBBPPR",
      "YYBBPPRRG",
      "YBBPPRRG"
    ]
  },
  {
    id: "heart_emblem",
    name: "Pixel Heart",
    icon: "❤️",
    category: "Whimsical",
    desc: "Iconic heart motif surrounded by supporting anchor colors.",
    rows: [
      ".RR...RR.",
      "RRRR.RRRR",
      "RRRRRRRRR",
      "RRRRRRRR",
      ".RRRRRR.",
      "..RRRR..",
      "...RR..."
    ]
  },
  {
    id: "crown_fort",
    name: "Crown Fortress",
    icon: "👑",
    category: "Tactical",
    desc: "Imperial castle crown with heavy corner towers and central gem.",
    rows: [
      "Y..YYY..Y",
      "YY.YYY.YY",
      "YYYYYYYYY",
      "YYYYYYYY",
      ".YY*B*YY.",
      "..YYBYY.."
    ]
  },
  {
    id: "spiral_galaxy",
    name: "Spiral Yin-Yang",
    icon: "🌀",
    category: "Whimsical",
    desc: "Interlocking dual-color spiral coils meeting in the center.",
    rows: [
      "BBBB.YYYY",
      "BBB..YYY",
      "BB.B.Y.YY",
      "B.BB.YY.Y",
      "YY.Y.B.BB",
      "YYYY.BBBB"
    ]
  },
  {
    id: "thunderbolt",
    name: "Thunderbolt",
    icon: "⚡",
    category: "Tactical",
    desc: "Dynamic zigzag lightning crackling through the board.",
    rows: [
      "...YYY...",
      "..YYYY..",
      "...YYY..",
      "....YY..",
      "...YYY...",
      "..YYYY..",
      "...YY..."
    ]
  },
  {
    id: "bullseye_target",
    name: "Bullseye Target",
    icon: "🎯",
    category: "Geometric",
    desc: "Concentric rings with a high-value star bomb at the epicenter.",
    rows: [
      "RRRRRRRRR",
      "RBBBBBBR",
      "RB.YY.BR",
      "RB.X.BR",
      "RB.YY.BR",
      "RBBBBBBR",
      "RRRRRRRRR"
    ]
  },
  {
    id: "space_invader",
    name: "Space Alien",
    icon: "🛸",
    category: "Whimsical",
    desc: "Retro pixel alien invader with flashing wildcard eyes.",
    rows: [
      "..G...G..",
      "...GGG...",
      "..GGGGG..",
      ".G*GGG*G.",
      ".GGGGGGG.",
      "..G.G.G..",
      ".G.....G."
    ]
  },
  {
    id: "flower_blossom",
    name: "Cherry Blossom",
    icon: "🌸",
    category: "Whimsical",
    desc: "Delicate floral petals surrounding a golden sun core.",
    rows: [
      "..PP.PP..",
      ".PPPPPPP.",
      "PPPPYPPPP",
      ".PPYYYPP.",
      "PPPPYPPPP",
      ".PPPPPPP.",
      "..PP.PP.."
    ]
  },
  {
    id: "honeycomb_mesh",
    name: "Honeycomb Mesh",
    icon: "🐝",
    category: "Geometric",
    desc: "Hexagonal open cells mimicking natural beehive architecture.",
    rows: [
      "Y.Y.Y.Y.Y",
      ".Y.Y.Y.Y.",
      "Y.Y.Y.Y.Y",
      ".Y.Y.Y.Y.",
      "Y.Y.Y.Y.Y",
      ".Y.Y.Y.Y."
    ]
  },
  {
    id: "castle_wall",
    name: "Castle Battlement",
    icon: "🧱",
    category: "Tactical",
    desc: "Stone obstacles guarding high-value colored clusters inside pockets.",
    rows: [
      "S.S.S.S.S",
      "SSSSSSSS",
      "SRR.S.BBS",
      "SRR.BBS",
      "S.R.S.B.S",
      "..S...S.."
    ]
  },
  {
    id: "pinecone_tree",
    name: "Pinecone Tree",
    icon: "🌲",
    category: "Whimsical",
    desc: "Festive evergreen tree cascading downwards to a sturdy trunk.",
    rows: [
      "....G....",
      "...GGG...",
      "..GGGGG..",
      ".GGGGGGG.",
      "..GGGGG..",
      "...GGG...",
      "....O...."
    ]
  },
  {
    id: "crossroads_x",
    name: "Crossroads X",
    icon: "⚔️",
    category: "Tactical",
    desc: "Intersecting diagonal colored beams crossing at a bomb nexus.",
    rows: [
      "R.......B",
      ".R.....B.",
      "..R...B..",
      "...R.B...",
      "....X....",
      "...B.R...",
      "..B...R..",
      ".B.....R.",
      "B.......R"
    ]
  },
  {
    id: "cascade_stream",
    name: "Cascade Droplets",
    icon: "🌊",
    category: "Waves",
    desc: "Gentle waterfalls of alternating blue, cyan and purple bubbles.",
    rows: [
      "BCBCBCBCB",
      "CBCBCBCB",
      "PPCCBBPP",
      "PCCBBP",
      ".P.C.B.P.",
      "..C.B.."
    ]
  }
];

// World Name Suggestions
const WORLD_NAME_SUGGESTIONS = [
  "Starlight Sanctuary", "Crystal Cavern", "Sunset Ridge", "Sakura Blossom Valley",
  "Whispering Woods", "Sunflower Valley", "Emerald Glade", "Pinecone Peak",
  "Tropical Palm Atoll", "Tulip Terrace", "Willow Marsh Glade", "Fairy Mushroom Haven",
  "Orchid Cloud Garden", "Lavender Mist", "Bamboo Sanctuary", "Butterfly Haven",
  "Rainbow Highlands", "Amber Canyon", "Opal Oasis", "Glacial Springs",
  "Dragonfly Lagoon", "Mystic Moors", "Coral Cove", "Nebula Pass",
  "Sunstone Cliffs", "Enchanted Grove", "Golden Meadow", "Silver Cascade"
];

// Application State
const appState = {
  projectPath: 'D:\\projects\\bubble shooter pro\\BubbleShooterpro',
  resolvedPaths: null,
  levelCount: 0,
  worldCount: 0,
  worldsData: null,
  currentTab: 'level-designer',

  // Level Designer State
  levelDesigner: {
    levelNumber: 1,
    shots: 20,
    activeColor: 'R',
    activeTool: 'brush', // 'brush', 'fill', 'eraser'
    mirrorMode: false,
    isMouseDown: false,
    isDirty: false,
    colors: ['RED', 'GREEN', 'BLUE', 'YELLOW', 'PURPLE', 'ORANGE'],
    objective: { type: 'CLEAR_ALL', target: 0 },
    starThresholds: [2000, 4500, 8000],
    rows: [
      "RRBBYBBRR",
      "RBYRRYBR",
      "BYRRRRRYB",
      "BRRRRRRB",
      ".YRRRRRY.",
      ".YBYYBY."
    ],
    analysis: null
  },

  // Pin Positioner State
  pinPositioner: {
    selectedWorldId: 1,
    selectedType: 'node', // 'node' or 'gift'
    selectedNodeIndex: 0,
    selectedGiftIndex: 0,
    zoomScale: 1.0,
    isDragging: false,
    isDirty: false,
    dragType: 'node', // 'node' or 'gift'
    dragIndex: -1
  },

  // Create World State
  createWorldModalState: {
    mapImageFile: null,
    gameImageFile: null
  },

  // Edit World State
  editWorldModalState: {
    worldNumber: 1,
    mapImageFile: null,
    gameImageFile: null
  }
};

// Autosave debounce timer
let levelAutosaveTimer = null;
let pinAutosaveTimer = null;

// DOM Elements
const inputProjectPath = document.getElementById('inputProjectPath');
const btnBrowseProject = document.getElementById('btnBrowseProject');
const btnRescanProject = document.getElementById('btnRescanProject');
const txtProjectStatus = document.getElementById('txtProjectStatus');
const badgeProjectStatus = document.getElementById('badgeProjectStatus');
const lblWorldCount = document.getElementById('lblWorldCount');
const lblLevelCount = document.getElementById('lblLevelCount');

// Tabs
const tabBtnLevelDesigner = document.getElementById('tabBtnLevelDesigner');
const tabBtnPinPositioner = document.getElementById('tabBtnPinPositioner');
const tabBtnWorldsExplorer = document.getElementById('tabBtnWorldsExplorer');
const viewLevelDesigner = document.getElementById('viewLevelDesigner');
const viewPinPositioner = document.getElementById('viewPinPositioner');
const viewWorldsExplorer = document.getElementById('viewWorldsExplorer');

// Level Designer Elements
const inputLevelNum = document.getElementById('inputLevelNum');
const btnPrevLevel = document.getElementById('btnPrevLevel');
const btnNextLevel = document.getElementById('btnNextLevel');
const btnLoadLevelDirect = document.getElementById('btnLoadLevelDirect');
const btnSaveLevelDirect = document.getElementById('btnSaveLevelDirect');
const badgeLevelAutosave = document.getElementById('badgeLevelAutosave');
const hexContainer = document.getElementById('hexContainer');
const rowBadgesContainer = document.getElementById('rowBadgesContainer');
const txtHoverInfo = document.getElementById('txtHoverInfo');
const txtBoardDimensions = document.getElementById('txtBoardDimensions');
const lblTotalBubbles = document.getElementById('lblTotalBubbles');
const pillsColorBreakdown = document.getElementById('pillsColorBreakdown');
const lblConnectionStatus = document.getElementById('lblConnectionStatus');
const badgeCurrentWorld = document.getElementById('badgeCurrentWorld');
const inputShots = document.getElementById('inputShots');
const selectObjectiveType = document.getElementById('selectObjectiveType');
const boxObjectiveTarget = document.getElementById('boxObjectiveTarget');
const inputObjectiveTarget = document.getElementById('inputObjectiveTarget');
const boxObjectiveColor = document.getElementById('boxObjectiveColor');
const selectObjectiveColor = document.getElementById('selectObjectiveColor');
const btnAutoColors = document.getElementById('btnAutoColors');
const btnAutoStars = document.getElementById('btnAutoStars');
const inputStar1 = document.getElementById('inputStar1');
const inputStar2 = document.getElementById('inputStar2');
const inputStar3 = document.getElementById('inputStar3');

// Analyzer HUD Elements
const badgeComplexityRating = document.getElementById('badgeComplexityRating');
const lblMinBallsRequired = document.getElementById('lblMinBallsRequired');
const lblRealisticShots = document.getElementById('lblRealisticShots');
const lblShotBalanceVerdict = document.getElementById('lblShotBalanceVerdict');
const btnAutoBalance = document.getElementById('btnAutoBalance');
const btnOpenAnalyzerModal = document.getElementById('btnOpenAnalyzerModal');

// Pin Positioner Elements
const selectPinWorld = document.getElementById('selectPinWorld');
const btnPrevWorld = document.getElementById('btnPrevWorld');
const btnNextWorld = document.getElementById('btnNextWorld');
const btnFloatingPrevWorld = document.getElementById('btnFloatingPrevWorld');
const btnFloatingNextWorld = document.getElementById('btnFloatingNextWorld');
const btnSavePinsDirect = document.getElementById('btnSavePinsDirect');
const badgePinAutosave = document.getElementById('badgePinAutosave');
const btnOpenEditWorldModal = document.getElementById('btnOpenEditWorldModal');
const lblWorldLevelRange = document.getElementById('lblWorldLevelRange');
const lblWorldDrawableName = document.getElementById('lblWorldDrawableName');
const lblWorldGameDrawableName = document.getElementById('lblWorldGameDrawableName');
const badgeSelectedPinLevel = document.getElementById('badgeSelectedPinLevel');
const inputPinX = document.getElementById('inputPinX');
const inputPinY = document.getElementById('inputPinY');
const btnZoomIn = document.getElementById('btnZoomIn');
const btnZoomOut = document.getElementById('btnZoomOut');
const btnZoomFit = document.getElementById('btnZoomFit');
const lblZoomLevel = document.getElementById('lblZoomLevel');
const listWorldNodes = document.getElementById('listWorldNodes');
const listWorldGifts = document.getElementById('listWorldGifts');
const boxGiftDetails = document.getElementById('boxGiftDetails');
const inputGiftName = document.getElementById('inputGiftName');
const inputGiftOffset = document.getElementById('inputGiftOffset');
const inputGiftCoins = document.getElementById('inputGiftCoins');
const mapWrapper = document.getElementById('mapWrapper');
const imgMapBackground = document.getElementById('imgMapBackground');
const pinsOverlay = document.getElementById('pinsOverlay');

// Worlds Explorer Elements
const gridWorldsList = document.getElementById('gridWorldsList');
const inputSearchWorlds = document.getElementById('inputSearchWorlds');
const btnOpenCreateWorldModal = document.getElementById('btnOpenCreateWorldModal');
const badgeWorldsCount = document.getElementById('badgeWorldsCount');

// Modal Elements
const modalRawJson = document.getElementById('modalRawJson');
const btnOpenRawJson = document.getElementById('btnOpenRawJson');
const btnCloseRawJson = document.getElementById('btnCloseRawJson');
const txtRawJsonContent = document.getElementById('txtRawJsonContent');
const btnCopyRawJson = document.getElementById('btnCopyRawJson');
const toastNotification = document.getElementById('toastNotification');

// Shortcuts Modal Elements
const modalShortcuts = document.getElementById('modalShortcuts');
const btnOpenShortcutsModal = document.getElementById('btnOpenShortcutsModal');
const btnCloseShortcuts = document.getElementById('btnCloseShortcuts');
const btnCloseShortcutsBtn = document.getElementById('btnCloseShortcutsBtn');

// Create New Level Modal Elements
const btnOpenNewLevelModal = document.getElementById('btnOpenNewLevelModal');
const modalNewLevel = document.getElementById('modalNewLevel');
const btnCloseNewLevel = document.getElementById('btnCloseNewLevel');
const btnCancelNewLevel = document.getElementById('btnCancelNewLevel');
const btnConfirmCreateLevel = document.getElementById('btnConfirmCreateLevel');
const inputNewLevelNum = document.getElementById('inputNewLevelNum');
const lblNewLevelWorldHint = document.getElementById('lblNewLevelWorldHint');
const selectNewLevelTemplate = document.getElementById('selectNewLevelTemplate');
const inputNewLevelShots = document.getElementById('inputNewLevelShots');
const selectNewLevelObjective = document.getElementById('selectNewLevelObjective');

// Create New World Modal Elements
const modalCreateWorld = document.getElementById('modalCreateWorld');
const btnCloseCreateWorld = document.getElementById('btnCloseCreateWorld');
const btnCancelCreateWorld = document.getElementById('btnCancelCreateWorld');
const btnConfirmCreateWorld = document.getElementById('btnConfirmCreateWorld');
const inputNewWorldNumber = document.getElementById('inputNewWorldNumber');
const inputNewWorldName = document.getElementById('inputNewWorldName');
const inputNewWorldSubtitle = document.getElementById('inputNewWorldSubtitle');
const btnRandomWorldName = document.getElementById('btnRandomWorldName');
const selectNewWorldMapBg = document.getElementById('selectNewWorldMapBg');
const selectNewWorldGameBg = document.getElementById('selectNewWorldGameBg');
const btnBrowseMapImg = document.getElementById('btnBrowseMapImg');
const btnBrowseGameBg = document.getElementById('btnBrowseGameBg');
const previewNewWorldMapBg = document.getElementById('previewNewWorldMapBg');
const previewNewWorldGameBg = document.getElementById('previewNewWorldGameBg');
const txtNoMapPreview = document.getElementById('txtNoMapPreview');
const txtNoGamePreview = document.getElementById('txtNoGamePreview');
const chkAutoGenerateLevels = document.getElementById('chkAutoGenerateLevels');
const inputNewWorldLevelsCount = document.getElementById('inputNewWorldLevelsCount');
const selectNewWorldProgression = document.getElementById('selectNewWorldProgression');
const selectNewWorldColors = document.getElementById('selectNewWorldColors');
const lblNewWorldNumBadge = document.getElementById('lblNewWorldNumBadge');

// Edit Existing World Modal Elements
const modalEditExistingWorld = document.getElementById('modalEditExistingWorld');
const btnCloseEditWorld = document.getElementById('btnCloseEditWorld');
const btnCancelEditWorld = document.getElementById('btnCancelEditWorld');
const btnConfirmEditWorld = document.getElementById('btnConfirmEditWorld');
const lblEditWorldNumBadge = document.getElementById('lblEditWorldNumBadge');
const btnRandomEditWorldName = document.getElementById('btnRandomEditWorldName');
const inputEditWorldName = document.getElementById('inputEditWorldName');
const inputEditWorldSubtitle = document.getElementById('inputEditWorldSubtitle');
const btnBrowseEditMapImg = document.getElementById('btnBrowseEditMapImg');
const btnBrowseEditGameBg = document.getElementById('btnBrowseEditGameBg');
const selectEditWorldMapBg = document.getElementById('selectEditWorldMapBg');
const selectEditWorldGameBg = document.getElementById('selectEditWorldGameBg');
const previewEditWorldMapBg = document.getElementById('previewEditWorldMapBg');
const previewEditWorldGameBg = document.getElementById('previewEditWorldGameBg');
const txtNoEditMapPreview = document.getElementById('txtNoEditMapPreview');
const txtNoEditGamePreview = document.getElementById('txtNoEditGamePreview');

// Level Analyzer Modal Elements
const modalLevelAnalyzer = document.getElementById('modalLevelAnalyzer');
const btnCloseAnalyzerModal = document.getElementById('btnCloseAnalyzerModal');
const btnCloseAnalyzerModalBtn = document.getElementById('btnCloseAnalyzerModalBtn');
const btnApplyAutoBalanceFromModal = document.getElementById('btnApplyAutoBalanceFromModal');
const lblAnalyzerLevelBadge = document.getElementById('lblAnalyzerLevelBadge');
const lblModalComplexityBadge = document.getElementById('lblModalComplexityBadge');
const lblModalMinBalls = document.getElementById('lblModalMinBalls');
const lblModalRealisticShots = document.getElementById('lblModalRealisticShots');
const lblModalShotsMargin = document.getElementById('lblModalShotsMargin');
const lblModalComplexityPercent = document.getElementById('lblModalComplexityPercent');
const barComplexityProgress = document.getElementById('barComplexityProgress');
const lblModalTotalBubbles = document.getElementById('lblModalTotalBubbles');
const lblModalColorCount = document.getElementById('lblModalColorCount');
const lblModalClusterCount = document.getElementById('lblModalClusterCount');
const lblModalCeilingAnchors = document.getElementById('lblModalCeilingAnchors');
const lblModalHangingCount = document.getElementById('lblModalHangingCount');
const lblModalSpecialCount = document.getElementById('lblModalSpecialCount');
const boxSolverInsightText = document.getElementById('boxSolverInsightText');
const lblRecShots = document.getElementById('lblRecShots');
const lblRecStars = document.getElementById('lblRecStars');

// Preset Patterns Elements
const quickPatternsList = document.getElementById('quickPatternsList');
const btnOpenPatternsModal = document.getElementById('btnOpenPatternsModal');
const modalPresetPatterns = document.getElementById('modalPresetPatterns');
const btnClosePatternsModal = document.getElementById('btnClosePatternsModal');
const btnClosePatternsModalBtn = document.getElementById('btnClosePatternsModalBtn');
const gridFullPatternsCatalog = document.getElementById('gridFullPatternsCatalog');


// ============================================================================
// INITIALIZATION
// ============================================================================
async function initApp() {
  setupEventListeners();
  renderQuickPatternsList();
  renderFullPatternsCatalog();

  // Auto-scan default project
  await scanAndConnectProject(appState.projectPath);
}

function setupEventListeners() {
  // Global Mouse tracking for drawing
  window.addEventListener('mousedown', () => appState.levelDesigner.isMouseDown = true);
  window.addEventListener('mouseup', () => {
    appState.levelDesigner.isMouseDown = false;
    if (appState.pinPositioner.isDragging) {
      appState.pinPositioner.isDragging = false;
      triggerPinAutosave();
    }
  });

  // Autosave on window exit / reload
  window.addEventListener('beforeunload', () => {
    if (appState.levelDesigner.isDirty) saveActiveLevel(true);
    if (appState.pinPositioner.isDirty) saveActiveWorldPins(true);
  });

  // Global Keyboard Shortcuts
  window.addEventListener('keydown', (e) => {
    // Escape key closes any open modal
    if (e.key === 'Escape') {
      const openModals = [
        modalRawJson, modalNewLevel, modalCreateWorld, 
        modalEditExistingWorld, modalLevelAnalyzer, modalPresetPatterns, modalShortcuts
      ];
      openModals.forEach(m => {
        if (m && !m.classList.contains('hidden')) m.classList.add('hidden');
      });
      return;
    }

    // Ctrl+S / Cmd+S save
    if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S')) {
      e.preventDefault();
      if (appState.currentTab === 'level-designer') {
        saveActiveLevel(false);
      } else if (appState.currentTab === 'pin-positioner') {
        saveActiveWorldPins(false);
      }
      return;
    }

    // If user is focused on an input, textarea, or select, do not intercept
    const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
    if (activeTag === 'input' || activeTag === 'textarea' || activeTag === 'select') {
      return;
    }

    // If any modal is currently visible/open, do not intercept
    const allModals = [
      modalRawJson, modalNewLevel, modalCreateWorld, 
      modalEditExistingWorld, modalLevelAnalyzer, modalPresetPatterns, modalShortcuts
    ];
    if (allModals.some(m => m && !m.classList.contains('hidden'))) {
      return;
    }

    // Level / World Left-Right Navigation
    if (e.key === 'ArrowRight' || e.key === 'Right') {
      if (appState.currentTab === 'level-designer') {
        e.preventDefault();
        changeLevel(1);
      } else if (appState.currentTab === 'pin-positioner') {
        e.preventDefault();
        changeWorld(1);
      }
      return;
    } else if (e.key === 'ArrowLeft' || e.key === 'Left') {
      if (appState.currentTab === 'level-designer') {
        e.preventDefault();
        changeLevel(-1);
      } else if (appState.currentTab === 'pin-positioner') {
        e.preventDefault();
        changeWorld(-1);
      }
      return;
    }

    // Level Designer Hotkeys
    if (appState.currentTab === 'level-designer') {
      const key = e.key.toLowerCase();
      if (key === 'b') {
        setLevelTool('brush');
      } else if (key === 'g') {
        setLevelTool('fill');
      } else if (key === 'e') {
        setLevelTool('eraser');
      } else if (key === 'm') {
        toggleLevelMirror();
      } else if (key === '1') {
        selectPaletteColorByCode('R');
      } else if (key === '2') {
        selectPaletteColorByCode('G');
      } else if (key === '3') {
        selectPaletteColorByCode('B');
      } else if (key === '4') {
        selectPaletteColorByCode('Y');
      } else if (key === '5') {
        selectPaletteColorByCode('P');
      } else if (key === '6') {
        selectPaletteColorByCode('O');
      } else if (key === '7') {
        selectPaletteColorByCode('C');
      } else if (key === '0' || key === 'delete' || key === 'backspace') {
        selectPaletteColorByCode('.');
      }
    }
  });

  // Project Folder Selector
  btnBrowseProject.addEventListener('click', onBrowseProject);
  btnRescanProject.addEventListener('click', () => scanAndConnectProject(appState.projectPath));

  // Tabs Navigation
  tabBtnLevelDesigner.addEventListener('click', () => switchTab('level-designer'));
  tabBtnPinPositioner.addEventListener('click', () => switchTab('pin-positioner'));
  tabBtnWorldsExplorer.addEventListener('click', () => switchTab('worlds-explorer'));

  // Shortcuts Modal Events
  if (btnOpenShortcutsModal) btnOpenShortcutsModal.addEventListener('click', () => modalShortcuts.classList.remove('hidden'));
  if (btnCloseShortcuts) btnCloseShortcuts.addEventListener('click', () => modalShortcuts.classList.add('hidden'));
  if (btnCloseShortcutsBtn) btnCloseShortcutsBtn.addEventListener('click', () => modalShortcuts.classList.add('hidden'));

  // Campaign Search Filter
  if (inputSearchWorlds) {
    inputSearchWorlds.addEventListener('input', () => {
      const term = inputSearchWorlds.value.trim().toLowerCase();
      const cards = gridWorldsList.querySelectorAll('.world-card-item');
      cards.forEach(card => {
        const text = card.textContent.toLowerCase();
        if (!term || text.includes(term)) {
          card.classList.remove('hidden');
        } else {
          card.classList.add('hidden');
        }
      });
    });
  }

  // Create New Level Modal Events
  if (btnOpenNewLevelModal) btnOpenNewLevelModal.addEventListener('click', openNewLevelModal);
  if (btnCloseNewLevel) btnCloseNewLevel.addEventListener('click', () => modalNewLevel.classList.add('hidden'));
  if (btnCancelNewLevel) btnCancelNewLevel.addEventListener('click', () => modalNewLevel.classList.add('hidden'));
  if (btnConfirmCreateLevel) btnConfirmCreateLevel.addEventListener('click', handleCreateNewLevel);
  if (inputNewLevelNum) inputNewLevelNum.addEventListener('input', updateNewLevelWorldHint);

  // Create New World Modal Events
  if (btnOpenCreateWorldModal) btnOpenCreateWorldModal.addEventListener('click', openCreateWorldModal);
  if (btnCloseCreateWorld) btnCloseCreateWorld.addEventListener('click', () => modalCreateWorld.classList.add('hidden'));
  if (btnCancelCreateWorld) btnCancelCreateWorld.addEventListener('click', () => modalCreateWorld.classList.add('hidden'));
  if (btnConfirmCreateWorld) btnConfirmCreateWorld.addEventListener('click', handleCreateNewWorld);
  if (btnRandomWorldName) btnRandomWorldName.addEventListener('click', randomizeNewWorldName);
  if (btnBrowseMapImg) btnBrowseMapImg.addEventListener('click', onBrowseNewWorldMapImage);
  if (btnBrowseGameBg) btnBrowseGameBg.addEventListener('click', onBrowseNewWorldGameImage);
  if (selectNewWorldMapBg) selectNewWorldMapBg.addEventListener('change', updateNewWorldMapPreview);
  if (selectNewWorldGameBg) selectNewWorldGameBg.addEventListener('change', updateNewWorldGamePreview);
  if (inputNewWorldNumber) inputNewWorldNumber.addEventListener('input', () => {
    const num = parseInt(inputNewWorldNumber.value) || 1;
    if (lblNewWorldNumBadge) lblNewWorldNumBadge.textContent = `World ${num}`;
  });

  // Edit Existing World Modal Events
  if (btnOpenEditWorldModal) btnOpenEditWorldModal.addEventListener('click', () => openEditWorldModal(appState.pinPositioner.selectedWorldId));
  if (btnCloseEditWorld) btnCloseEditWorld.addEventListener('click', () => modalEditExistingWorld.classList.add('hidden'));
  if (btnCancelEditWorld) btnCancelEditWorld.addEventListener('click', () => modalEditExistingWorld.classList.add('hidden'));
  if (btnConfirmEditWorld) btnConfirmEditWorld.addEventListener('click', handleConfirmEditWorld);
  if (btnRandomEditWorldName) btnRandomEditWorldName.addEventListener('click', randomizeEditWorldName);
  if (btnBrowseEditMapImg) btnBrowseEditMapImg.addEventListener('click', onBrowseEditWorldMapImage);
  if (btnBrowseEditGameBg) btnBrowseEditGameBg.addEventListener('click', onBrowseEditWorldGameImage);
  if (selectEditWorldMapBg) selectEditWorldMapBg.addEventListener('change', updateEditWorldMapPreview);
  if (selectEditWorldGameBg) selectEditWorldGameBg.addEventListener('change', updateEditWorldGamePreview);

  // Level Analyzer Modal Events
  if (btnOpenAnalyzerModal) btnOpenAnalyzerModal.addEventListener('click', openLevelAnalyzerModal);
  if (btnCloseAnalyzerModal) btnCloseAnalyzerModal.addEventListener('click', () => modalLevelAnalyzer.classList.add('hidden'));
  if (btnCloseAnalyzerModalBtn) btnCloseAnalyzerModalBtn.addEventListener('click', () => modalLevelAnalyzer.classList.add('hidden'));
  if (btnAutoBalance) btnAutoBalance.addEventListener('click', autoBalanceLevelParameters);
  if (btnApplyAutoBalanceFromModal) btnApplyAutoBalanceFromModal.addEventListener('click', () => {
    autoBalanceLevelParameters();
    modalLevelAnalyzer.classList.add('hidden');
  });

  const selectTuningEl = document.getElementById('selectTuningDifficulty');
  if (selectTuningEl) {
    selectTuningEl.addEventListener('change', () => {
      updateLevelAnalysisHUD();
    });
  }

  // Preset Patterns Modal Events
  if (btnOpenPatternsModal) btnOpenPatternsModal.addEventListener('click', () => modalPresetPatterns.classList.remove('hidden'));
  if (btnClosePatternsModal) btnClosePatternsModal.addEventListener('click', () => modalPresetPatterns.classList.add('hidden'));
  if (btnClosePatternsModalBtn) btnClosePatternsModalBtn.addEventListener('click', () => modalPresetPatterns.classList.add('hidden'));

  // Level Designer Palette & Tools
  document.querySelectorAll('.palette-item').forEach(btn => {
    btn.addEventListener('click', () => {
      const code = btn.dataset.code;
      selectPaletteColorByCode(code);
    });
  });

  document.getElementById('toolBrush').addEventListener('click', () => setLevelTool('brush'));
  document.getElementById('toolFill').addEventListener('click', () => setLevelTool('fill'));
  document.getElementById('toolEraser').addEventListener('click', () => setLevelTool('eraser'));
  document.getElementById('toolMirror').addEventListener('click', toggleLevelMirror);

  // Level Row operations
  document.getElementById('btnAddRowTop').addEventListener('click', addRowTop);
  document.getElementById('btnAddRowBottom').addEventListener('click', addRowBottom);
  document.getElementById('btnRemoveRowTop').addEventListener('click', removeRowTop);
  document.getElementById('btnRemoveRowBottom').addEventListener('click', removeRowBottom);
  document.getElementById('btnClearBoard').addEventListener('click', clearBoard);
  document.getElementById('btnRandomize').addEventListener('click', randomizeBoard);

  document.getElementById('btnShiftUp2').addEventListener('click', () => shiftRows(2, -1));
  document.getElementById('btnShiftDown2').addEventListener('click', () => shiftRows(2, 1));
  document.getElementById('btnShiftLeft').addEventListener('click', () => shiftCols(-1));
  document.getElementById('btnShiftRight').addEventListener('click', () => shiftCols(1));

  // Level Navigation & Save
  btnPrevLevel.addEventListener('click', () => changeLevel(-1));
  btnNextLevel.addEventListener('click', () => changeLevel(1));
  btnLoadLevelDirect.addEventListener('click', async () => {
    await saveActiveLevel(true);
    await loadLevel(parseInt(inputLevelNum.value) || 1);
  });
  btnSaveLevelDirect.addEventListener('click', () => saveActiveLevel(false));

  // Level Config Inputs
  inputShots.addEventListener('input', () => {
    appState.levelDesigner.shots = parseInt(inputShots.value) || 20;
    appState.levelDesigner.isDirty = true;
    updateLevelAnalysisHUD();
    triggerLevelAutosave();
  });
  selectObjectiveType.addEventListener('change', () => {
    onObjectiveTypeChange();
    appState.levelDesigner.isDirty = true;
    updateLevelAnalysisHUD();
    triggerLevelAutosave();
  });
  selectObjectiveColor.addEventListener('change', () => {
    appState.levelDesigner.objective.color = selectObjectiveColor.value;
    appState.levelDesigner.isDirty = true;
    triggerLevelAutosave();
  });
  inputObjectiveTarget.addEventListener('input', () => {
    appState.levelDesigner.objective.target = parseInt(inputObjectiveTarget.value) || 0;
    appState.levelDesigner.isDirty = true;
    triggerLevelAutosave();
  });

  // Color Checkbox Changes
  ["RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE"].forEach(c => {
    const el = document.getElementById("chkCol" + c);
    if (el) {
      el.addEventListener('change', () => {
        syncColorsFromCheckboxes();
        appState.levelDesigner.isDirty = true;
        updateLevelAnalysisHUD();
        triggerLevelAutosave();
      });
    }
  });

  btnAutoColors.addEventListener('click', () => autoDetectLevelColors(false));
  btnAutoStars.addEventListener('click', autoCalculateLevelStars);

  // Raw JSON Modal
  btnOpenRawJson.addEventListener('click', openRawJsonModal);
  btnCloseRawJson.addEventListener('click', () => modalRawJson.classList.add('hidden'));
  btnCopyRawJson.addEventListener('click', copyRawJson);

  // Pin Positioner Controls
  selectPinWorld.addEventListener('change', onWorldSelectChange);
  if (btnPrevWorld) btnPrevWorld.addEventListener('click', () => changeWorld(-1));
  if (btnNextWorld) btnNextWorld.addEventListener('click', () => changeWorld(1));
  if (btnFloatingPrevWorld) btnFloatingPrevWorld.addEventListener('click', () => changeWorld(-1));
  if (btnFloatingNextWorld) btnFloatingNextWorld.addEventListener('click', () => changeWorld(1));
  btnSavePinsDirect.addEventListener('click', () => saveActiveWorldPins(false));
  inputPinX.addEventListener('input', () => {
    onPinCoordinateInput();
    triggerPinAutosave();
  });
  inputPinY.addEventListener('input', () => {
    onPinCoordinateInput();
    triggerPinAutosave();
  });
  if (inputGiftName) inputGiftName.addEventListener('input', () => {
    onGiftDetailsInput();
    triggerPinAutosave();
  });
  if (inputGiftOffset) inputGiftOffset.addEventListener('input', () => {
    onGiftDetailsInput();
    triggerPinAutosave();
  });
  if (inputGiftCoins) inputGiftCoins.addEventListener('input', () => {
    onGiftDetailsInput();
    triggerPinAutosave();
  });

  btnZoomIn.addEventListener('click', () => adjustPinZoom(0.15));
  btnZoomOut.addEventListener('click', () => adjustPinZoom(-0.15));
  btnZoomFit.addEventListener('click', () => adjustPinZoom(0, true));

  // Draggable Pin Overlay Events
  pinsOverlay.addEventListener('mousemove', onPinsOverlayMouseMove);

  // Drag & Drop for Preset Patterns on Hex Board Container
  setupBoardDragAndDrop();
}

// ============================================================================
// AUTOSAVE ENGINE
// ============================================================================
function triggerLevelAutosave() {
  if (levelAutosaveTimer) clearTimeout(levelAutosaveTimer);
  levelAutosaveTimer = setTimeout(() => {
    saveActiveLevel(true);
  }, 1200);
}

function triggerPinAutosave() {
  if (pinAutosaveTimer) clearTimeout(pinAutosaveTimer);
  pinAutosaveTimer = setTimeout(() => {
    saveActiveWorldPins(true);
  }, 1000);
}

function showAutosavePill(customText = "⚡ Auto-Saved") {
  if (badgeLevelAutosave) {
    badgeLevelAutosave.textContent = customText;
    badgeLevelAutosave.className = "text-[11px] text-emerald-300 font-mono flex items-center gap-1 bg-emerald-900/80 px-2.5 py-0.5 rounded border border-emerald-500/80 shadow-lg shadow-emerald-950/60 transition-all duration-300 opacity-100 scale-105";
    setTimeout(() => {
      badgeLevelAutosave.className = "text-[11px] text-emerald-400 font-mono flex items-center gap-1 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800/60 transition-all duration-300 opacity-80 scale-100";
    }, 1800);
  }
}

function showPinAutosavePill(customText = "⚡ Auto-Saved") {
  if (badgePinAutosave) {
    badgePinAutosave.textContent = customText;
    badgePinAutosave.className = "text-[10px] text-emerald-300 font-mono bg-emerald-900/80 px-2.5 py-0.5 rounded border border-emerald-500/80 shadow-lg shadow-emerald-950/60 transition-all duration-300 opacity-100 scale-105";
    setTimeout(() => {
      badgePinAutosave.className = "text-[10px] text-emerald-400 font-mono bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800/60 transition-all duration-300 opacity-80 scale-100";
    }, 1800);
  }
}

// ============================================================================
// PROJECT SCANNING & DIRECTORY CONNECTION
// ============================================================================
async function onBrowseProject() {
  if (!window.gameStudioAPI) {
    showToast("Electron API not available (Browser mode)", true);
    return;
  }
  const res = await window.gameStudioAPI.selectProject();
  if (!res.canceled && res.path) {
    await scanAndConnectProject(res.path);
  }
}

async function scanAndConnectProject(targetPath) {
  inputProjectPath.value = targetPath;
  appState.projectPath = targetPath;

  if (!window.gameStudioAPI) {
    showToast("Running in preview mode", false);
    return;
  }

  const result = await window.gameStudioAPI.scanProject(targetPath);
  if (!result.success) {
    badgeProjectStatus.className = "flex items-center gap-1.5 bg-red-950/60 border border-red-800/60 px-2.5 py-1 rounded-lg text-red-300 text-xs font-mono";
    txtProjectStatus.textContent = "Not Found";
    showToast(`Error: ${result.error}`, true);
    return;
  }

  appState.resolvedPaths = result.resolvedPaths;
  appState.levelCount = result.levelCount;
  appState.worldCount = result.worldCount;
  appState.worldsData = result.worldsData;

  badgeProjectStatus.className = "flex items-center gap-1.5 bg-emerald-950/60 border border-emerald-800/60 px-2.5 py-1 rounded-lg text-emerald-300 text-xs font-mono";
  txtProjectStatus.textContent = "Connected";
  lblWorldCount.textContent = result.worldCount;
  lblLevelCount.textContent = result.levelCount;
  if (badgeWorldsCount) badgeWorldsCount.textContent = `${result.worldCount} Total Worlds`;

  // Populate Pin Positioner World Selector
  populateWorldSelector();
  // Populate Campaign Explorer Grid
  renderCampaignWorldsGrid();
  // Load Level 1 into designer
  await loadLevel(1);
  // Load World 1 into pin positioner
  await loadWorldForPinPositioner(1);

  showToast(`Project Connected: ${result.worldCount} Worlds, ${result.levelCount} Levels`);
}

// ============================================================================
// TAB NAVIGATION
// ============================================================================
async function switchTab(tabId) {
  // Autosave when switching away
  if (appState.currentTab === 'level-designer' && appState.levelDesigner.isDirty) {
    await saveActiveLevel(true);
  } else if (appState.currentTab === 'pin-positioner' && appState.pinPositioner.isDirty) {
    await saveActiveWorldPins(true);
  }

  appState.currentTab = tabId;

  [tabBtnLevelDesigner, tabBtnPinPositioner, tabBtnWorldsExplorer].forEach(b => {
    b.className = "tab-btn px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition text-slate-400 hover:text-slate-200 hover:bg-slate-800/60";
  });

  viewLevelDesigner.classList.add('hidden');
  viewPinPositioner.classList.add('hidden');
  viewWorldsExplorer.classList.add('hidden');

  if (tabId === 'level-designer') {
    tabBtnLevelDesigner.className = "tab-btn px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition bg-indigo-600 text-white shadow";
    viewLevelDesigner.classList.remove('hidden');
    renderLevelBoard();
  } else if (tabId === 'pin-positioner') {
    tabBtnPinPositioner.className = "tab-btn px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition bg-indigo-600 text-white shadow";
    viewPinPositioner.classList.remove('hidden');
    loadWorldForPinPositioner(appState.pinPositioner.selectedWorldId);
  } else if (tabId === 'worlds-explorer') {
    tabBtnWorldsExplorer.className = "tab-btn px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition bg-indigo-600 text-white shadow";
    viewWorldsExplorer.classList.remove('hidden');
    renderCampaignWorldsGrid();
  }
}

// ============================================================================
// MODULE 1: VISUAL LEVEL DESIGNER
// ============================================================================
function selectPaletteColorByCode(code) {
  const btn = document.querySelector(`.palette-item[data-code="${code}"]`);
  document.querySelectorAll('.palette-item').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');

  appState.levelDesigner.activeColor = code;

  // Auto-detect & ensure this color is in shooter playable colors
  if (CODE_TO_NAME[code]) {
    const colName = CODE_TO_NAME[code];
    const chk = document.getElementById("chkCol" + colName);
    if (chk && !chk.checked) {
      chk.checked = true;
      syncColorsFromCheckboxes();
      showAutosavePill("Color " + colName + " Enabled");
    }
  }

  if (code === '.') {
    setLevelTool('eraser');
  } else {
    setLevelTool('brush');
  }
}

function setLevelTool(tool) {
  appState.levelDesigner.activeTool = tool;
  const bBrush = document.getElementById('toolBrush');
  const bFill = document.getElementById('toolFill');
  const bEraser = document.getElementById('toolEraser');

  [bBrush, bFill, bEraser].forEach(b => {
    b.className = "studio-btn p-1.5 rounded-md text-slate-400 hover:text-white hover:bg-studio-hover flex flex-col items-center gap-0.5";
  });

  if (tool === 'brush') bBrush.className = "studio-btn p-1.5 rounded-md bg-indigo-600 text-white flex flex-col items-center gap-0.5 shadow-sm";
  if (tool === 'fill') bFill.className = "studio-btn p-1.5 rounded-md bg-indigo-600 text-white flex flex-col items-center gap-0.5 shadow-sm";
  if (tool === 'eraser') bEraser.className = "studio-btn p-1.5 rounded-md bg-indigo-600 text-white flex flex-col items-center gap-0.5 shadow-sm";
}

function toggleLevelMirror() {
  appState.levelDesigner.mirrorMode = !appState.levelDesigner.mirrorMode;
  const b = document.getElementById('toolMirror');
  const txt = document.getElementById('txtMirror');
  if (appState.levelDesigner.mirrorMode) {
    b.className = "w-full studio-btn bg-amber-600 text-white py-1 text-[11px] gap-1.5 shadow-sm";
    txt.textContent = "Mirror: ON";
  } else {
    b.className = "w-full studio-btn studio-btn-secondary py-1 text-[11px] gap-1.5";
    txt.textContent = "Mirror: OFF";
  }
}

function renderLevelBoard() {
  hexContainer.innerHTML = "";
  rowBadgesContainer.innerHTML = "";

  const connectedSet = computeConnectedBubbles();
  let floatingCount = 0;

  appState.levelDesigner.rows.forEach((rowStr, rIdx) => {
    const rowEl = document.createElement("div");
    const isEven = rIdx % 2 === 0;
    const maxCols = isEven ? 9 : 8;

    rowEl.className = "hex-row " + (isEven ? "hex-row-even" : "hex-row-odd");
    rowEl.dataset.row = rIdx;

    // Ensure strict 9/8 parity lengths
    while (rowStr.length < maxCols) rowStr += ".";
    appState.levelDesigner.rows[rIdx] = rowStr.substring(0, maxCols);

    // Row Parity Badge
    const badgeEl = document.createElement("div");
    badgeEl.className = `h-[38.1px] flex items-center justify-end font-mono text-[10px] font-bold ${
      isEven ? "text-emerald-400" : "text-amber-400"
    }`;
    badgeEl.innerHTML = `<span class="px-1.5 py-0.5 rounded bg-slate-800 border ${
      isEven ? "border-emerald-700/60" : "border-amber-700/60"
    }">R${rIdx} (${maxCols})</span>`;
    badgeEl.title = `Row ${rIdx}: ${isEven ? "EVEN (9 Cols, Full Width)" : "ODD (8 Cols, Indented 22px)"}`;
    rowBadgesContainer.appendChild(badgeEl);

    // Cells
    for (let cIdx = 0; cIdx < maxCols; cIdx++) {
      const char = appState.levelDesigner.rows[rIdx][cIdx] || ".";
      const isConnected = (char === ".") || connectedSet.has(`${rIdx},${cIdx}`);
      if (!isConnected) floatingCount++;

      const cellEl = createBubbleElement(char, rIdx, cIdx, isConnected);
      rowEl.appendChild(cellEl);
    }

    hexContainer.appendChild(rowEl);
  });

  updateLevelStats(floatingCount);
  txtBoardDimensions.textContent = `9 Columns • ${appState.levelDesigner.rows.length} Rows`;

  const worldNum = Math.floor((appState.levelDesigner.levelNumber - 1) / 10) + 1;
  badgeCurrentWorld.textContent = `World ${worldNum}`;

  // Automatically analyze complexity & solver metrics
  updateLevelAnalysisHUD();
}

function createBubbleElement(char, rIdx, cIdx, isConnected) {
  const el = document.createElement("div");
  const info = COLOR_MAP[char] || COLOR_MAP["."];

  el.className = "bubble " + info.class;
  if (!isConnected) el.classList.add("floating-warning");
  el.dataset.row = rIdx;
  el.dataset.col = cIdx;
  el.dataset.code = char;
  el.title = `${info.name} (${char})`;

  if (char === ".") el.textContent = "✕";

  el.addEventListener("mousedown", (e) => {
    if (e.button === 2) {
      paintCell(rIdx, cIdx, ".");
    } else {
      handleCellAction(rIdx, cIdx);
    }
  });

  el.addEventListener("mouseenter", () => {
    highlightHexNeighbors(rIdx, cIdx);
    updateHoverHUD(rIdx, cIdx, char);
    if (appState.levelDesigner.isMouseDown) {
      handleCellAction(rIdx, cIdx);
    }
  });

  el.addEventListener("mouseleave", clearHexNeighborHighlights);

  return el;
}

function updateHoverHUD(r, c, char) {
  const isEven = r % 2 === 0;
  const info = COLOR_MAP[char] || COLOR_MAP["."];
  const centerX = isEven ? (22 + c * 44) : (44 + c * 44);
  const centerY = Math.round(22 + r * 38.1);
  txtHoverInfo.innerHTML = `Row <strong class="${isEven ? 'text-emerald-300' : 'text-amber-300'}">${r}</strong> [${isEven ? 'EVEN: 9 Cols' : 'ODD: 8 Cols'}], Col <strong>${c}</strong> &bull; Center: X=<strong>${centerX}px</strong>, Y=<strong>${centerY}px</strong> &bull; Bubble: <span class="font-bold text-white">${info.emoji} ${info.name}</span>`;
}

function highlightHexNeighbors(r, c) {
  clearHexNeighborHighlights();
  const neighbors = getHexNeighbors(r, c);
  neighbors.forEach(([nr, nc]) => {
    const cell = hexContainer.querySelector(`.bubble[data-row="${nr}"][data-col="${nc}"]`);
    if (cell) cell.classList.add("neighbor-highlight");
  });
}

function clearHexNeighborHighlights() {
  hexContainer.querySelectorAll(".neighbor-highlight").forEach(el => el.classList.remove("neighbor-highlight"));
}

function handleCellAction(r, c) {
  if (appState.levelDesigner.activeTool === "eraser") {
    paintCell(r, c, ".");
  } else if (appState.levelDesigner.activeTool === "fill") {
    floodFill(r, c, appState.levelDesigner.activeColor);
  } else {
    paintCell(r, c, appState.levelDesigner.activeColor);
  }
}

function paintCell(r, c, code) {
  const maxCols = (r % 2 === 0) ? 9 : 8;
  if (r < 0 || r >= appState.levelDesigner.rows.length || c < 0 || c >= maxCols) return;

  let rowStr = appState.levelDesigner.rows[r];
  rowStr = rowStr.substring(0, c) + code + rowStr.substring(c + 1);
  appState.levelDesigner.rows[r] = rowStr;

  if (appState.levelDesigner.mirrorMode) {
    const mirrorC = maxCols - 1 - c;
    if (mirrorC !== c) {
      rowStr = appState.levelDesigner.rows[r];
      rowStr = rowStr.substring(0, mirrorC) + code + rowStr.substring(mirrorC + 1);
      appState.levelDesigner.rows[r] = rowStr;
    }
  }

  appState.levelDesigner.isDirty = true;
  autoDetectLevelColors(true);
  renderLevelBoard();
  triggerLevelAutosave();
}

function floodFill(startR, startC, targetColor) {
  const sourceColor = appState.levelDesigner.rows[startR][startC];
  if (sourceColor === targetColor) return;

  const visited = new Set();
  const queue = [[startR, startC]];

  while (queue.length > 0) {
    const [r, c] = queue.shift();
    const key = `${r},${c}`;
    if (visited.has(key)) continue;
    visited.add(key);

    const maxCols = (r % 2 === 0) ? 9 : 8;
    if (r < 0 || r >= appState.levelDesigner.rows.length || c < 0 || c >= maxCols) continue;
    if (appState.levelDesigner.rows[r][c] !== sourceColor) continue;

    let rowStr = appState.levelDesigner.rows[r];
    appState.levelDesigner.rows[r] = rowStr.substring(0, c) + targetColor + rowStr.substring(c + 1);

    const neighbors = getHexNeighbors(r, c);
    for (const [nr, nc] of neighbors) {
      queue.push([nr, nc]);
    }
  }

  appState.levelDesigner.isDirty = true;
  autoDetectLevelColors(true);
  renderLevelBoard();
  triggerLevelAutosave();
}

function getHexNeighbors(r, c) {
  const isEven = (r % 2 === 0);
  let neighbors;
  if (isEven) {
    neighbors = [
      [r, c - 1], [r, c + 1],
      [r - 1, c - 1], [r - 1, c],
      [r + 1, c - 1], [r + 1, c]
    ];
  } else {
    neighbors = [
      [r, c - 1], [r, c + 1],
      [r - 1, c], [r - 1, c + 1],
      [r + 1, c], [r + 1, c + 1]
    ];
  }

  return neighbors.filter(([nr, nc]) => {
    if (nr < 0 || nr >= appState.levelDesigner.rows.length) return false;
    const maxCols = (nr % 2 === 0) ? 9 : 8;
    return nc >= 0 && nc < maxCols;
  });
}

function computeConnectedBubbles() {
  const connected = new Set();
  const queue = [];

  if (appState.levelDesigner.rows.length > 0) {
    const row0 = appState.levelDesigner.rows[0];
    for (let c = 0; c < row0.length; c++) {
      if (row0[c] !== ".") {
        connected.add(`0,${c}`);
        queue.push([0, c]);
      }
    }
  }

  while (queue.length > 0) {
    const [r, c] = queue.shift();
    const neighbors = getHexNeighbors(r, c);
    for (const [nr, nc] of neighbors) {
      const key = `${nr},${nc}`;
      if (!connected.has(key) && appState.levelDesigner.rows[nr] && appState.levelDesigner.rows[nr][nc] && appState.levelDesigner.rows[nr][nc] !== ".") {
        connected.add(key);
        queue.push([nr, nc]);
      }
    }
  }

  return connected;
}

// Row Operations
function addRowTop() {
  const adjusted = [".".repeat(9)];
  for (let i = 0; i < appState.levelDesigner.rows.length; i++) {
    const targetLen = ((i + 1) % 2 === 0) ? 9 : 8;
    let str = appState.levelDesigner.rows[i];
    str = (str + ".".repeat(targetLen)).substring(0, targetLen);
    adjusted.push(str);
  }
  appState.levelDesigner.rows = adjusted;
  appState.levelDesigner.isDirty = true;
  renderLevelBoard();
  triggerLevelAutosave();
}

function addRowBottom() {
  const isEven = (appState.levelDesigner.rows.length % 2 === 0);
  const cols = isEven ? 9 : 8;
  appState.levelDesigner.rows.push(".".repeat(cols));
  appState.levelDesigner.isDirty = true;
  renderLevelBoard();
  triggerLevelAutosave();
}

function removeRowTop() {
  if (appState.levelDesigner.rows.length > 1) {
    appState.levelDesigner.rows.shift();
    appState.levelDesigner.rows = appState.levelDesigner.rows.map((row, idx) => {
      const targetLen = (idx % 2 === 0) ? 9 : 8;
      return (row + ".".repeat(targetLen)).substring(0, targetLen);
    });
    appState.levelDesigner.isDirty = true;
    autoDetectLevelColors(true);
    renderLevelBoard();
    triggerLevelAutosave();
  }
}

function removeRowBottom() {
  if (appState.levelDesigner.rows.length > 1) {
    appState.levelDesigner.rows.pop();
    appState.levelDesigner.isDirty = true;
    autoDetectLevelColors(true);
    renderLevelBoard();
    triggerLevelAutosave();
  }
}

function clearBoard() {
  if (!confirm("Clear all bubbles on this board?")) return;
  appState.levelDesigner.rows = appState.levelDesigner.rows.map((row, idx) => ".".repeat(idx % 2 === 0 ? 9 : 8));
  appState.levelDesigner.isDirty = true;
  autoDetectLevelColors(true);
  renderLevelBoard();
  triggerLevelAutosave();
}

function randomizeBoard() {
  const activeChoices = appState.levelDesigner.colors.map(c => NAME_TO_CODE[c] || "R");

  appState.levelDesigner.rows = appState.levelDesigner.rows.map((row, idx) => {
    const cols = (idx % 2 === 0) ? 9 : 8;
    let str = "";
    for (let c = 0; c < cols; c++) {
      if (idx < 6 && Math.random() < 0.82) {
        str += activeChoices[Math.floor(Math.random() * activeChoices.length)];
      } else {
        str += ".";
      }
    }
    return str;
  });
  appState.levelDesigner.isDirty = true;
  autoDetectLevelColors(true);
  renderLevelBoard();
  triggerLevelAutosave();
}

function shiftRows(count, direction) {
  if (count === 2) {
    if (direction === -1) {
      if (appState.levelDesigner.rows.length > 2) {
        appState.levelDesigner.rows.shift();
        appState.levelDesigner.rows.shift();
        appState.levelDesigner.rows.push(".".repeat((appState.levelDesigner.rows.length % 2 === 0) ? 9 : 8));
        appState.levelDesigner.rows.push(".".repeat((appState.levelDesigner.rows.length % 2 === 0) ? 9 : 8));
      }
    } else {
      appState.levelDesigner.rows.unshift(".".repeat(8));
      appState.levelDesigner.rows.unshift(".".repeat(9));
      appState.levelDesigner.rows = appState.levelDesigner.rows.map((row, idx) => {
        const cols = (idx % 2 === 0) ? 9 : 8;
        return (row + ".".repeat(cols)).substring(0, cols);
      });
    }
  }
  appState.levelDesigner.isDirty = true;
  renderLevelBoard();
  triggerLevelAutosave();
}

function shiftCols(direction) {
  appState.levelDesigner.rows = appState.levelDesigner.rows.map((row, idx) => {
    const maxCols = (idx % 2 === 0) ? 9 : 8;
    if (direction === 1) {
      return ("." + row).substring(0, maxCols);
    } else {
      return (row.substring(1) + ".");
    }
  });
  appState.levelDesigner.isDirty = true;
  renderLevelBoard();
  triggerLevelAutosave();
}

function updateLevelStats(floatingCount = 0) {
  let total = 0;
  const counts = {};

  appState.levelDesigner.rows.forEach(r => {
    for (let ch of r) {
      if (ch !== ".") {
        total++;
        counts[ch] = (counts[ch] || 0) + 1;
      }
    }
  });

  lblTotalBubbles.textContent = total;

  pillsColorBreakdown.innerHTML = "";
  Object.keys(counts).sort().forEach(code => {
    const info = COLOR_MAP[code] || { name: code, emoji: "●" };
    const badge = document.createElement("span");
    badge.className = "px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300 flex items-center gap-1";
    badge.innerHTML = `<span>${info.emoji}</span><span>${counts[code]}</span>`;
    pillsColorBreakdown.appendChild(badge);
  });

  if (floatingCount > 0) {
    lblConnectionStatus.className = "flex items-center gap-1.5 text-amber-400 font-bold text-[11px]";
    lblConnectionStatus.innerHTML = `<span>⚠️ ${floatingCount} unattached bubble${floatingCount > 1 ? 's' : ''} (will drop on start)</span>`;
  } else {
    lblConnectionStatus.className = "flex items-center gap-1.5 text-emerald-400 font-medium text-[11px]";
    lblConnectionStatus.innerHTML = `<span>✓ All bubbles properly anchored to ceiling</span>`;
  }
}

// Level Load & Autosave
async function changeLevel(delta) {
  const cur = parseInt(inputLevelNum.value) || 1;
  const next = Math.max(1, Math.min(appState.levelCount || 999, cur + delta));
  if (cur === next) return;

  // 1. Auto-save current active level silently
  await saveActiveLevel(true);

  // 2. Switch and load next level
  inputLevelNum.value = next;
  await loadLevel(next);
}

async function loadLevel(levelNum) {
  appState.levelDesigner.levelNumber = levelNum;
  inputLevelNum.value = levelNum;

  if (!window.gameStudioAPI || !appState.resolvedPaths) {
    renderLevelBoard();
    return;
  }

  const res = await window.gameStudioAPI.loadLevel({
    levelsDir: appState.resolvedPaths.levels,
    levelNumber: levelNum
  });

  if (!res.success) {
    showToast(`Level ${levelNum} not found, blank template loaded`, true);
    appState.levelDesigner.rows = [
      ".........",
      "........",
      ".........",
      "........",
      ".........",
      "........"
    ];
    renderLevelBoard();
    return;
  }

  const data = res.data;
  appState.levelDesigner.shots = data.shots || 20;
  inputShots.value = appState.levelDesigner.shots;

  if (data.colors) {
    appState.levelDesigner.colors = data.colors;
    ["RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE"].forEach(c => {
      const el = document.getElementById("chkCol" + c);
      if (el) el.checked = data.colors.includes(c);
    });
  }

  if (data.objective) {
    appState.levelDesigner.objective = data.objective;
    selectObjectiveType.value = data.objective.type || "CLEAR_ALL";
    onObjectiveTypeChange();
    if (data.objective.target) inputObjectiveTarget.value = data.objective.target;
    if (data.objective.color) selectObjectiveColor.value = data.objective.color;
  }

  if (data.starThresholds && data.starThresholds.length >= 3) {
    appState.levelDesigner.starThresholds = data.starThresholds;
    inputStar1.value = data.starThresholds[0];
    inputStar2.value = data.starThresholds[1];
    inputStar3.value = data.starThresholds[2];
  }

  if (data.rows && Array.isArray(data.rows)) {
    appState.levelDesigner.rows = data.rows.map((row, idx) => {
      const expected = (idx % 2 === 0) ? 9 : 8;
      return (row + ".".repeat(expected)).substring(0, expected);
    });
  }

  appState.levelDesigner.isDirty = false;
  autoDetectLevelColors(true);
  renderLevelBoard();
  showToast(`Loaded Level ${levelNum}`);
}

async function saveActiveLevel(silent = false) {
  if (!window.gameStudioAPI || !appState.resolvedPaths) {
    if (!silent) showToast("Cannot save: No active project connection", true);
    return;
  }

  syncColorsFromCheckboxes();

  const sanitizedRows = appState.levelDesigner.rows.map((r, idx) => {
    const expected = (idx % 2 === 0) ? 9 : 8;
    return (r + ".".repeat(expected)).substring(0, expected);
  });

  const levelData = {
    level: appState.levelDesigner.levelNumber,
    shots: parseInt(inputShots.value) || appState.levelDesigner.shots,
    colors: appState.levelDesigner.colors,
    objective: {
      type: selectObjectiveType.value,
      target: parseInt(inputObjectiveTarget.value) || 0
    },
    starThresholds: [
      parseInt(inputStar1.value) || appState.levelDesigner.starThresholds[0],
      parseInt(inputStar2.value) || appState.levelDesigner.starThresholds[1],
      parseInt(inputStar3.value) || appState.levelDesigner.starThresholds[2]
    ],
    rows: sanitizedRows
  };

  if (selectObjectiveType.value === "POP_COLOR") {
    levelData.objective.color = selectObjectiveColor.value;
  }

  const res = await window.gameStudioAPI.saveLevel({
    levelsDir: appState.resolvedPaths.levels,
    levelNumber: appState.levelDesigner.levelNumber,
    data: levelData
  });

  if (res.success) {
    appState.levelDesigner.isDirty = false;
    if (silent) {
      showAutosavePill();
    } else {
      showToast(`✓ Saved Level ${res.levelNumber} directly to disk!`);
    }
  } else {
    if (!silent) showToast(`Save failed: ${res.error}`, true);
  }
}

function onObjectiveTypeChange() {
  const type = selectObjectiveType.value;
  appState.levelDesigner.objective.type = type;

  if (type === "CLEAR_ALL") {
    boxObjectiveTarget.classList.add("hidden");
    boxObjectiveColor.classList.add("hidden");
  } else if (type === "SCORE_TARGET") {
    boxObjectiveTarget.classList.remove("hidden");
    boxObjectiveColor.classList.add("hidden");
    document.getElementById("lblObjectiveTarget").textContent = "Target Score (Points)";
  } else if (type === "POP_COLOR") {
    boxObjectiveTarget.classList.remove("hidden");
    boxObjectiveColor.classList.remove("hidden");
    document.getElementById("lblObjectiveTarget").textContent = "Number of Bubbles to Pop";
  } else if (type === "DROP_COUNT") {
    boxObjectiveTarget.classList.remove("hidden");
    boxObjectiveColor.classList.add("hidden");
    document.getElementById("lblObjectiveTarget").textContent = "Number of Bubbles to Drop";
  }
}

function syncColorsFromCheckboxes() {
  const selected = [];
  ["RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE"].forEach(c => {
    const el = document.getElementById("chkCol" + c);
    if (el && el.checked) selected.push(c);
  });
  appState.levelDesigner.colors = selected.length > 0 ? selected : ["RED", "BLUE", "YELLOW"];
}

// Real-Time Color Auto-Detection
function autoDetectLevelColors(silent = true) {
  const found = new Set();
  appState.levelDesigner.rows.forEach(r => {
    for (let ch of r) {
      if (CODE_TO_NAME[ch]) found.add(CODE_TO_NAME[ch]);
    }
  });

  // Also include the currently active palette color if applicable
  const activeColorName = CODE_TO_NAME[appState.levelDesigner.activeColor];
  if (activeColorName) found.add(activeColorName);

  // If board is empty, default to at least RED, BLUE, YELLOW
  if (found.size === 0) {
    found.add("RED");
    found.add("BLUE");
    found.add("YELLOW");
  }

  ["RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE"].forEach(c => {
    const el = document.getElementById("chkCol" + c);
    if (el) el.checked = found.has(c);
  });

  syncColorsFromCheckboxes();
  if (!silent) showToast("Colors auto-detected from board!");
}

function autoCalculateLevelStars() {
  let bubbleCount = 0;
  appState.levelDesigner.rows.forEach(r => {
    for (let ch of r) if (ch !== ".") bubbleCount++;
  });

  const baseScore = bubbleCount * 100 + (parseInt(inputShots.value) || 20) * 150;
  const s1 = Math.round(baseScore * 0.85);
  const s2 = Math.round(baseScore * 1.85);
  const s3 = Math.round(baseScore * 3.2);

  inputStar1.value = s1;
  inputStar2.value = s2;
  inputStar3.value = s3;
  appState.levelDesigner.isDirty = true;
  triggerLevelAutosave();
  showToast("Star thresholds auto-calculated!");
}

function openRawJsonModal() {
  syncColorsFromCheckboxes();
  const sanitizedRows = appState.levelDesigner.rows.map((r, idx) => {
    const expected = (idx % 2 === 0) ? 9 : 8;
    return (r + ".".repeat(expected)).substring(0, expected);
  });

  const obj = {
    level: appState.levelDesigner.levelNumber,
    shots: parseInt(inputShots.value) || appState.levelDesigner.shots,
    colors: appState.levelDesigner.colors,
    objective: {
      type: selectObjectiveType.value,
      target: parseInt(inputObjectiveTarget.value) || 0
    },
    starThresholds: [
      parseInt(inputStar1.value) || 2000,
      parseInt(inputStar2.value) || 4500,
      parseInt(inputStar3.value) || 8000
    ],
    rows: sanitizedRows
  };

  txtRawJsonContent.value = JSON.stringify(obj, null, 2);
  document.getElementById('lblModalLevelFilename').textContent = `level_${appState.levelDesigner.levelNumber}.json`;
  modalRawJson.classList.remove('hidden');
}

function copyRawJson() {
  navigator.clipboard.writeText(txtRawJsonContent.value).then(() => {
    showToast("Level JSON copied to clipboard!");
  });
}

// ============================================================================
// PRESET PATTERNS ENGINE (DRAG & DROP & CLICK TO STAMP)
// ============================================================================
function renderQuickPatternsList() {
  if (!quickPatternsList) return;
  quickPatternsList.innerHTML = "";

  // Show top 8 quick patterns in sidebar
  const quickList = PRESET_PATTERNS.slice(0, 8);
  quickList.forEach(pat => {
    const chip = document.createElement("div");
    chip.className = "p-1.5 rounded-lg bg-studio-bg border border-studio-border hover:border-indigo-500/60 flex items-center justify-between cursor-grab active:cursor-grabbing hover:bg-studio-hover transition group select-none";
    chip.draggable = true;
    chip.dataset.patternId = pat.id;

    chip.innerHTML = `
      <div class="flex items-center gap-1.5 truncate">
        <span class="text-sm">${pat.icon}</span>
        <span class="text-[10px] font-semibold text-slate-200 group-hover:text-indigo-300 truncate">${pat.name}</span>
      </div>
      <button class="btn-stamp-quick text-[10px] text-slate-400 hover:text-white px-1 hover:bg-indigo-600 rounded transition" title="Stamp into board">
        ➕
      </button>
    `;

    chip.addEventListener('dragstart', (e) => {
      e.dataTransfer.setData('text/plain', pat.id);
      e.dataTransfer.effectAllowed = 'copy';
    });

    chip.querySelector('.btn-stamp-quick').addEventListener('click', (e) => {
      e.stopPropagation();
      applyPresetPattern(pat.id, 0, 0, false);
    });

    quickPatternsList.appendChild(chip);
  });
}

function renderFullPatternsCatalog() {
  if (!gridFullPatternsCatalog) return;
  gridFullPatternsCatalog.innerHTML = "";

  PRESET_PATTERNS.forEach(pat => {
    const card = document.createElement("div");
    card.className = "studio-card p-3 flex flex-col justify-between space-y-3 hover:border-indigo-500/70 transition shadow-md group";
    card.draggable = true;
    card.dataset.patternId = pat.id;

    // Mini visual grid preview
    let previewHtml = `<div class="bg-studio-bg p-2 rounded-lg border border-studio-border flex flex-col items-center space-y-0.5 overflow-hidden scale-90 origin-top">`;
    pat.rows.forEach((r, rIdx) => {
      const isEven = rIdx % 2 === 0;
      previewHtml += `<div class="flex items-center ${isEven ? '' : 'pl-2'} space-x-0.5">`;
      for (let ch of r) {
        let colorClass = "bg-slate-800";
        if (ch === "R") colorClass = "bg-red-500 shadow-sm shadow-red-500/50";
        else if (ch === "G") colorClass = "bg-emerald-500 shadow-sm shadow-emerald-500/50";
        else if (ch === "B") colorClass = "bg-sky-500 shadow-sm shadow-sky-500/50";
        else if (ch === "Y") colorClass = "bg-amber-400 shadow-sm shadow-amber-400/50";
        else if (ch === "P") colorClass = "bg-purple-500 shadow-sm shadow-purple-500/50";
        else if (ch === "O") colorClass = "bg-orange-500 shadow-sm shadow-orange-500/50";
        else if (ch === "C") colorClass = "bg-cyan-400 shadow-sm shadow-cyan-400/50";
        else if (ch === "*") colorClass = "bg-pink-400 animate-pulse";
        else if (ch === "X") colorClass = "bg-rose-700";
        else if (ch === "S") colorClass = "bg-slate-600";
        previewHtml += `<span class="w-2.5 h-2.5 rounded-full ${colorClass} inline-block"></span>`;
      }
      previewHtml += `</div>`;
    });
    previewHtml += `</div>`;

    card.innerHTML = `
      <div>
        <div class="flex items-center justify-between mb-1">
          <span class="font-bold text-white text-xs flex items-center gap-1.5">
            <span class="text-base">${pat.icon}</span>
            <span>${pat.name}</span>
          </span>
          <span class="text-[9px] bg-indigo-500/15 text-indigo-300 border border-indigo-500/30 px-1.5 py-0.5 rounded font-mono">${pat.category}</span>
        </div>
        <p class="text-[10px] text-slate-400 leading-tight mb-2">${pat.desc}</p>
        ${previewHtml}
      </div>

      <div class="grid grid-cols-2 gap-1.5 pt-1">
        <button class="btn-stamp-top studio-btn studio-btn-secondary py-1 text-[10px] font-semibold" title="Stamp at top">
          📥 Stamp Top
        </button>
        <button class="btn-replace-all studio-btn studio-btn-primary py-1 text-[10px] font-semibold" title="Replace whole board">
          ✨ Replace Board
        </button>
      </div>
    `;

    card.addEventListener('dragstart', (e) => {
      e.dataTransfer.setData('text/plain', pat.id);
      e.dataTransfer.effectAllowed = 'copy';
    });

    card.querySelector('.btn-stamp-top').addEventListener('click', () => {
      applyPresetPattern(pat.id, 0, 0, false);
      modalPresetPatterns.classList.add('hidden');
    });

    card.querySelector('.btn-replace-all').addEventListener('click', () => {
      applyPresetPattern(pat.id, 0, 0, true);
      modalPresetPatterns.classList.add('hidden');
    });

    gridFullPatternsCatalog.appendChild(card);
  });
}

function setupBoardDragAndDrop() {
  const boardEl = document.getElementById('boardViewport');
  if (!boardEl) return;

  boardEl.addEventListener('dragover', (e) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
  });

  boardEl.addEventListener('drop', (e) => {
    e.preventDefault();
    const patternId = e.dataTransfer.getData('text/plain');
    if (!patternId) return;

    // Determine target row based on drop coordinate
    let targetRow = 0;
    const targetBubble = e.target.closest('.bubble');
    if (targetBubble && targetBubble.dataset.row) {
      targetRow = parseInt(targetBubble.dataset.row) || 0;
    }

    applyPresetPattern(patternId, targetRow, 0, false);
  });
}

function applyPresetPattern(patternId, startRow = 0, startCol = 0, replaceEntireBoard = false) {
  const pat = PRESET_PATTERNS.find(p => p.id === patternId);
  if (!pat) return;

  if (replaceEntireBoard) {
    appState.levelDesigner.rows = pat.rows.map((row, idx) => {
      const expected = (idx % 2 === 0) ? 9 : 8;
      return (row + ".".repeat(expected)).substring(0, expected);
    });
  } else {
    // Stamp pattern starting at startRow
    while (appState.levelDesigner.rows.length < startRow + pat.rows.length) {
      const isEven = appState.levelDesigner.rows.length % 2 === 0;
      appState.levelDesigner.rows.push(".".repeat(isEven ? 9 : 8));
    }

    pat.rows.forEach((pRow, pIdx) => {
      const actualRow = startRow + pIdx;
      const isEven = actualRow % 2 === 0;
      const maxCols = isEven ? 9 : 8;
      let existingRow = appState.levelDesigner.rows[actualRow] || ".".repeat(maxCols);

      for (let c = 0; c < pRow.length && c < maxCols; c++) {
        const char = pRow[c];
        if (char !== ".") {
          existingRow = existingRow.substring(0, c) + char + existingRow.substring(c + 1);
        }
      }
      appState.levelDesigner.rows[actualRow] = existingRow.substring(0, maxCols);
    });
  }

  appState.levelDesigner.isDirty = true;
  autoDetectLevelColors(true);
  renderLevelBoard();
  triggerLevelAutosave();
  showToast(`Applied preset pattern "${pat.name}"!`);
}

// ============================================================================
// LEVEL COMPLEXITY & SOLVER ANALYZER
// ============================================================================
function analyzeLevelComplexityAndSolver() {
  const rows = appState.levelDesigner.rows;
  const currentShots = parseInt(inputShots.value) || 20;
  let totalBubbles = 0;
  const colorCounts = {};
  const specialCounts = { bomb: 0, lightning: 0, fireball: 0, star: 0, stone: 0, target: 0 };
  let ceilingAnchorsCount = 0;

  // 1. Scan Board
  rows.forEach((r, rIdx) => {
    for (let c = 0; c < r.length; c++) {
      const ch = r[c];
      if (ch !== ".") {
        totalBubbles++;
        if (CODE_TO_NAME[ch]) {
          colorCounts[ch] = (colorCounts[ch] || 0) + 1;
        } else if (ch === "X") specialCounts.bomb++;
        else if (ch === "L") specialCounts.lightning++;
        else if (ch === "F") specialCounts.fireball++;
        else if (ch === "*") specialCounts.star++;
        else if (ch === "S") specialCounts.stone++;
        else if (ch === "T") specialCounts.target++;

        if (rIdx === 0) ceilingAnchorsCount++;
      }
    }
  });

  const distinctColorsCount = Object.keys(colorCounts).length;

  // 2. Connected Color Clusters Decomposition
  const visited = new Set();
  const clusters = [];

  for (let r = 0; r < rows.length; r++) {
    for (let c = 0; c < rows[r].length; c++) {
      const ch = rows[r][c];
      const key = `${r},${c}`;
      if (ch !== "." && !visited.has(key)) {
        // BFS cluster
        const cluster = { color: ch, cells: [] };
        const q = [[r, c]];
        visited.add(key);

        while (q.length > 0) {
          const [cr, cc] = q.shift();
          cluster.cells.push([cr, cc]);

          const neighbors = getHexNeighbors(cr, cc);
          for (const [nr, nc] of neighbors) {
            const nKey = `${nr},${nc}`;
            if (!visited.has(nKey) && rows[nr] && rows[nr][nc] === ch) {
              visited.add(nKey);
              q.push([nr, nc]);
            }
          }
        }
        clusters.push(cluster);
      }
    }
  }

  // 3. Hanging / Drop Candidates calculation
  const connectedToCeiling = computeConnectedBubbles();
  let hangingCount = 0;
  rows.forEach((r, rIdx) => {
    for (let c = 0; c < r.length; c++) {
      if (r[c] !== "." && !connectedToCeiling.has(`${rIdx},${c}`)) {
        hangingCount++;
      }
    }
  });

  // 4. Mathematical Solver Simulation (Min Balls Estimation)
  // Large clusters (size >= 3) can be popped in 1 shot
  // Small clusters (size 1-2) need 1 shot matching
  // Drops save shots (approx 1 shot saved per 4-5 dropped bubbles)
  let theoreticalMinShots = 0;
  clusters.forEach(cl => {
    if (cl.color === "S") return; // Stones don't pop directly
    if (cl.cells.length >= 3) {
      theoreticalMinShots += 1;
    } else {
      theoreticalMinShots += 1;
    }
  });

  // Avalanche drop bonus reduction
  const dropBonusShots = Math.floor(hangingCount / 4);
  const powerupBonusShots = specialCounts.bomb * 2 + specialCounts.lightning * 2 + specialCounts.fireball * 2 + specialCounts.star;
  theoreticalMinShots = Math.max(3, theoreticalMinShots - dropBonusShots - powerupBonusShots);

  // Get selected tuning mode ('tight', 'standard', 'casual')
  const selectTuningEl = document.getElementById('selectTuningDifficulty');
  const tuningMode = selectTuningEl ? selectTuningEl.value : 'tight';

  // Target shots based on chosen tuning profile
  let targetShots = 14;
  if (tuningMode === 'tight') {
    // Tight & Challenging: requires precision, zero wasted shots, and smart bank shots
    targetShots = Math.max(8, Math.min(22, Math.round(theoreticalMinShots * 1.10 + 1)));
  } else if (tuningMode === 'standard') {
    // Standard balanced margin
    targetShots = Math.max(11, Math.min(26, Math.round(theoreticalMinShots * 1.25 + 2)));
  } else {
    // Casual / Relaxed
    targetShots = Math.max(15, Math.min(32, Math.round(theoreticalMinShots * 1.50 + 4)));
  }

  // 5. Complexity Index Calculation (0 to 100)
  let complexityScore = 0;
  // Bubble volume (0-30 pts)
  complexityScore += Math.min(30, Math.round(totalBubbles * 0.45));
  // Color variety (0-30 pts)
  complexityScore += Math.min(30, distinctColorsCount * 5.5);
  // Cluster fragmentation (0-20 pts)
  complexityScore += Math.min(20, Math.round(clusters.length * 1.2));
  // Hazards (Stones)
  complexityScore += Math.min(10, specialCounts.stone * 2.5);
  // Ceiling weakness bonus
  if (ceilingAnchorsCount <= 3 && totalBubbles > 15) complexityScore += 10;

  const complexityIndex = Math.max(5, Math.min(100, Math.round(complexityScore)));

  // Difficulty Rating Label
  let rating = "NORMAL";
  let ratingClass = "bg-amber-900/70 text-amber-300 border-amber-500/50";
  if (complexityIndex < 25) {
    rating = "CASUAL / EASY";
    ratingClass = "bg-emerald-900/70 text-emerald-300 border-emerald-500/50";
  } else if (complexityIndex < 45) {
    rating = "EASY";
    ratingClass = "bg-teal-900/70 text-teal-300 border-teal-500/50";
  } else if (complexityIndex < 65) {
    rating = "NORMAL";
    ratingClass = "bg-amber-900/70 text-amber-300 border-amber-500/50";
  } else if (complexityIndex < 80) {
    rating = "HARD";
    ratingClass = "bg-orange-900/70 text-orange-300 border-orange-500/50";
  } else if (complexityIndex < 90) {
    rating = "EXPERT";
    ratingClass = "bg-rose-900/70 text-rose-300 border-rose-500/50";
  } else {
    rating = "MASTER / NIGHTMARE";
    ratingClass = "bg-purple-900/70 text-purple-300 border-purple-500/50";
  }

  // Balance Verdict
  const margin = currentShots - targetShots;
  let verdict = "Challenging & Tight";
  let verdictClass = "text-emerald-400";
  if (currentShots < theoreticalMinShots) {
    verdict = `⚠️ Impossible (${currentShots} < Min ${theoreticalMinShots})`;
    verdictClass = "text-red-400 font-bold";
  } else if (currentShots < targetShots - 1) {
    verdict = `⚡ Hardcore / Strict (${currentShots} Shots)`;
    verdictClass = "text-amber-400 font-bold";
  } else if (Math.abs(margin) <= 2) {
    verdict = `🔥 Tight & Challenging (${currentShots} Shots)`;
    verdictClass = "text-emerald-400 font-bold";
  } else if (margin > 5) {
    verdict = `🎈 Too Easy / Over-budget (${currentShots} Shots)`;
    verdictClass = "text-sky-400 font-bold";
  } else {
    verdict = `✅ Moderate Balance (${currentShots} Shots)`;
    verdictClass = "text-teal-400 font-semibold";
  }

  // Recommended calibration
  const recShots = targetShots;
  const basePoints = totalBubbles * 100 + recShots * 150;
  const recStars = [
    Math.round(basePoints * 0.90),
    Math.round(basePoints * 1.90),
    Math.round(basePoints * 3.30)
  ];

  // Insight diagnostic text
  let insightText = `Level contains <strong>${totalBubbles} bubbles</strong> split across <strong>${clusters.length} color clusters</strong> with <strong>${distinctColorsCount} active colors</strong>.<br/>`;
  insightText += `• Theoretical minimum perfect solver balls: <strong>${theoreticalMinShots}</strong><br/>`;
  insightText += `• Target shots for <em>${tuningMode.toUpperCase()}</em> difficulty: <strong>${targetShots} shots</strong> (Current: ${currentShots}).`;

  return {
    totalBubbles,
    distinctColorsCount,
    clustersCount: clusters.length,
    ceilingAnchorsCount,
    hangingCount,
    specialCount: Object.values(specialCounts).reduce((a, b) => a + b, 0),
    theoreticalMinShots,
    realisticShots: targetShots,
    complexityIndex,
    rating,
    ratingClass,
    margin,
    verdict,
    verdictClass,
    recShots,
    recStars,
    insightText
  };
}

function updateLevelAnalysisHUD() {
  const analysis = analyzeLevelComplexityAndSolver();
  appState.levelDesigner.analysis = analysis;

  if (badgeComplexityRating) {
    badgeComplexityRating.textContent = `${analysis.rating} (${analysis.complexityIndex}/100)`;
    badgeComplexityRating.className = `px-2 py-0.5 rounded-full text-[10px] font-bold border ${analysis.ratingClass}`;
  }

  if (lblMinBallsRequired) lblMinBallsRequired.textContent = `🎯 ${analysis.theoreticalMinShots} Balls`;
  if (lblRealisticShots) lblRealisticShots.textContent = `🎱 ${analysis.realisticShots} Shots`;
  if (lblShotBalanceVerdict) {
    lblShotBalanceVerdict.textContent = analysis.verdict;
    lblShotBalanceVerdict.className = `font-semibold ${analysis.verdictClass}`;
  }
}

function openLevelAnalyzerModal() {
  const analysis = analyzeLevelComplexityAndSolver();
  appState.levelDesigner.analysis = analysis;

  lblAnalyzerLevelBadge.textContent = `Level ${appState.levelDesigner.levelNumber}`;
  lblModalComplexityBadge.textContent = `${analysis.rating} (${analysis.complexityIndex}/100)`;
  lblModalComplexityBadge.className = `font-bold text-sm block mt-0.5 ${analysis.verdictClass}`;
  lblModalMinBalls.textContent = `🎯 ${analysis.theoreticalMinShots} Balls`;
  lblModalRealisticShots.textContent = `🎱 ${analysis.realisticShots} Shots`;
  lblModalShotsMargin.textContent = analysis.margin >= 0 ? `+${analysis.margin} Margin` : `${analysis.margin} Strict`;
  lblModalComplexityPercent.textContent = `${analysis.complexityIndex} / 100`;
  barComplexityProgress.style.width = `${analysis.complexityIndex}%`;

  lblModalTotalBubbles.textContent = analysis.totalBubbles;
  lblModalColorCount.textContent = analysis.distinctColorsCount;
  lblModalClusterCount.textContent = analysis.clustersCount;
  lblModalCeilingAnchors.textContent = analysis.ceilingAnchorsCount;
  lblModalHangingCount.textContent = analysis.hangingCount;
  lblModalSpecialCount.textContent = analysis.specialCount;

  boxSolverInsightText.innerHTML = analysis.insightText;
  lblRecShots.textContent = `${analysis.recShots} Shots`;
  lblRecStars.textContent = `${analysis.recStars[0]} / ${analysis.recStars[1]} / ${analysis.recStars[2]}`;

  modalLevelAnalyzer.classList.remove('hidden');
}

function autoBalanceLevelParameters() {
  const analysis = analyzeLevelComplexityAndSolver();
  inputShots.value = analysis.recShots;
  appState.levelDesigner.shots = analysis.recShots;

  inputStar1.value = analysis.recStars[0];
  inputStar2.value = analysis.recStars[1];
  inputStar3.value = analysis.recStars[2];
  appState.levelDesigner.starThresholds = analysis.recStars;

  appState.levelDesigner.isDirty = true;
  updateLevelAnalysisHUD();
  triggerLevelAutosave();
  showToast(`⚡ Tuned to ${analysis.recShots} Shots & calibrated Stars!`);
}

// ============================================================================
// MODULE 2: SAGA MAP PIN POSITIONER
// ============================================================================
function populateWorldSelector() {
  selectPinWorld.innerHTML = "";
  if (!appState.worldsData || !appState.worldsData.worlds) return;

  appState.worldsData.worlds.forEach(w => {
    const worldNum = w.worldNumber || w.id;
    const levels = w.levels || w.nodes || [];
    const startLevel = levels.length > 0 ? levels[0].level : ((worldNum - 1) * 10 + 1);
    const endLevel = levels.length > 0 ? levels[levels.length - 1].level : (worldNum * 10);
    const opt = document.createElement("option");
    opt.value = worldNum;
    opt.textContent = `World ${worldNum}: ${w.name} (Lvl ${startLevel}-${endLevel})`;
    selectPinWorld.appendChild(opt);
  });
}

async function onWorldSelectChange() {
  // Autosave previous world before switching
  await saveActiveWorldPins(true);
  const worldId = parseInt(selectPinWorld.value) || 1;
  await loadWorldForPinPositioner(worldId);
}

async function changeWorld(delta) {
  if (!appState.worldsData || !appState.worldsData.worlds || appState.worldsData.worlds.length === 0) return;
  // Autosave current pins before changing
  await saveActiveWorldPins(true);

  const currentWorld = appState.pinPositioner.selectedWorldId || 1;
  const totalWorlds = appState.worldsData.worlds.length;
  let nextWorld = currentWorld + delta;
  if (nextWorld < 1) nextWorld = totalWorlds;
  if (nextWorld > totalWorlds) nextWorld = 1;
  await loadWorldForPinPositioner(nextWorld);
}

async function loadWorldForPinPositioner(worldId) {
  appState.pinPositioner.selectedWorldId = worldId;
  selectPinWorld.value = worldId;

  if (!appState.worldsData || !appState.worldsData.worlds) return;
  const world = appState.worldsData.worlds.find(w => (w.worldNumber === worldId || w.id === worldId));
  if (!world) return;

  const levels = world.levels || world.nodes || [];
  const startLevel = levels.length > 0 ? levels[0].level : ((worldId - 1) * 10 + 1);
  const endLevel = levels.length > 0 ? levels[levels.length - 1].level : (worldId * 10);

  lblWorldLevelRange.textContent = `${startLevel} – ${endLevel}`;
  const bgName = world.mapBackground || `bg_map_world_${worldId}`;
  lblWorldDrawableName.textContent = bgName;

  // Ensure every world has 2 gifts
  if (!world.gifts || world.gifts.length < 2) {
    if (!world.gifts) world.gifts = [];
    if (world.gifts.length === 0) {
      world.gifts.push({
        giftIndex: 1,
        name: "Mid-World Chest",
        x: 0.337,
        y: 0.3547,
        requiredLevelOffset: 5,
        rewardCoins: 100
      });
    }
    if (world.gifts.length === 1) {
      world.gifts.push({
        giftIndex: 2,
        name: "Castle Gate Chest",
        x: 0.5583,
        y: 0.1331,
        requiredLevelOffset: 10,
        rewardCoins: 200
      });
    }
  }

  // Load World Map Background as base64
  if (window.gameStudioAPI && appState.resolvedPaths) {
    const imgRes = await window.gameStudioAPI.loadImageBase64({
      drawableDir: appState.resolvedPaths.drawable,
      imageName: bgName
    });
    if (imgRes && imgRes.success) {
      imgMapBackground.src = imgRes.dataUrl;
    } else {
      imgMapBackground.src = "";
    }
  }

  renderPinsOverlay(world);
  renderWorldNodesList(world);
  renderWorldGiftsList(world);
  selectPinNode(0);
}

function renderPinsOverlay(world) {
  pinsOverlay.innerHTML = "";
  const nodes = world.levels || world.nodes;

  // 1. Render Level Pins
  if (nodes && nodes.length) {
    nodes.forEach((node, idx) => {
      const pinEl = document.createElement("div");
      pinEl.className = "level-pin pin-completed";
      pinEl.dataset.type = "node";
      pinEl.dataset.index = idx;
      pinEl.textContent = node.level;
      const pctX = node.x > 1.0 ? node.x : (node.x * 100);
      const pctY = node.y > 1.0 ? node.y : (node.y * 100);
      pinEl.style.left = `${pctX}%`;
      pinEl.style.top = `${pctY}%`;

      pinEl.addEventListener("mousedown", (e) => {
        e.stopPropagation();
        selectPinNode(idx);
        appState.pinPositioner.isDragging = true;
        appState.pinPositioner.dragType = 'node';
        appState.pinPositioner.dragIndex = idx;
      });

      pinsOverlay.appendChild(pinEl);
    });
  }

  // 2. Render 2 Gift Chest Pins
  if (world.gifts && world.gifts.length) {
    world.gifts.forEach((gift, gIdx) => {
      const giftEl = document.createElement("div");
      giftEl.className = "gift-pin";
      giftEl.dataset.type = "gift";
      giftEl.dataset.index = gIdx;
      giftEl.title = `🎁 Gift ${gift.giftIndex}: ${gift.name} (Offset +${gift.requiredLevelOffset}, ${gift.rewardCoins} coins)`;

      giftEl.innerHTML = `<img src="../assets/chest_gift.svg" alt="Chest" />`;
      const pctX = gift.x > 1.0 ? gift.x : (gift.x * 100);
      const pctY = gift.y > 1.0 ? gift.y : (gift.y * 100);
      giftEl.style.left = `${pctX}%`;
      giftEl.style.top = `${pctY}%`;

      giftEl.addEventListener("mousedown", (e) => {
        e.stopPropagation();
        selectGiftNode(gIdx);
        appState.pinPositioner.isDragging = true;
        appState.pinPositioner.dragType = 'gift';
        appState.pinPositioner.dragIndex = gIdx;
      });

      pinsOverlay.appendChild(giftEl);
    });
  }
}

function selectPinNode(nodeIdx) {
  appState.pinPositioner.selectedType = 'node';
  appState.pinPositioner.selectedNodeIndex = nodeIdx;
  if (boxGiftDetails) boxGiftDetails.classList.add('hidden');

  const world = appState.worldsData.worlds.find(w => (w.worldNumber === appState.pinPositioner.selectedWorldId || w.id === appState.pinPositioner.selectedWorldId));
  const nodes = world ? (world.levels || world.nodes) : null;
  if (!world || !nodes || !nodes[nodeIdx]) return;

  const node = nodes[nodeIdx];
  badgeSelectedPinLevel.className = "bg-amber-900/60 text-amber-300 px-2 py-0.5 rounded font-mono text-xs font-bold";
  badgeSelectedPinLevel.textContent = `Level ${node.level}`;
  const pctX = node.x > 1.0 ? node.x : (node.x * 100);
  const pctY = node.y > 1.0 ? node.y : (node.y * 100);
  inputPinX.value = pctX.toFixed(1);
  inputPinY.value = pctY.toFixed(1);

  // Update pin visual selection
  pinsOverlay.querySelectorAll('.level-pin, .gift-pin').forEach(p => p.classList.remove('pin-selected'));
  if (listWorldNodes) listWorldNodes.querySelectorAll('.node-row').forEach(r => r.classList.remove('bg-indigo-900/60', 'border-indigo-600'));
  if (listWorldGifts) listWorldGifts.querySelectorAll('.gift-row').forEach(r => r.classList.remove('bg-amber-900/60', 'border-amber-500'));

  const activePin = pinsOverlay.querySelector(`.level-pin[data-index="${nodeIdx}"]`);
  if (activePin) activePin.classList.add('pin-selected');

  if (listWorldNodes) {
    const activeRow = listWorldNodes.querySelector(`.node-row[data-index="${nodeIdx}"]`);
    if (activeRow) activeRow.classList.add('bg-indigo-900/60', 'border-indigo-600');
  }
}

function selectGiftNode(giftIdx) {
  appState.pinPositioner.selectedType = 'gift';
  appState.pinPositioner.selectedGiftIndex = giftIdx;
  if (boxGiftDetails) boxGiftDetails.classList.remove('hidden');

  const world = appState.worldsData.worlds.find(w => (w.worldNumber === appState.pinPositioner.selectedWorldId || w.id === appState.pinPositioner.selectedWorldId));
  if (!world || !world.gifts || !world.gifts[giftIdx]) return;

  const gift = world.gifts[giftIdx];
  badgeSelectedPinLevel.className = "bg-pink-900/60 text-pink-300 px-2 py-0.5 rounded font-mono text-xs font-bold";
  badgeSelectedPinLevel.textContent = `🎁 Gift ${gift.giftIndex}: ${gift.name}`;

  const pctX = gift.x > 1.0 ? gift.x : (gift.x * 100);
  const pctY = gift.y > 1.0 ? gift.y : (gift.y * 100);
  inputPinX.value = pctX.toFixed(1);
  inputPinY.value = pctY.toFixed(1);

  if (inputGiftName) inputGiftName.value = gift.name || `Gift ${gift.giftIndex}`;
  if (inputGiftOffset) inputGiftOffset.value = gift.requiredLevelOffset || (gift.giftIndex === 1 ? 5 : 10);
  if (inputGiftCoins) inputGiftCoins.value = gift.rewardCoins || (gift.giftIndex === 1 ? 100 : 200);

  // Update pin visual selection
  pinsOverlay.querySelectorAll('.level-pin, .gift-pin').forEach(p => p.classList.remove('pin-selected'));
  if (listWorldNodes) listWorldNodes.querySelectorAll('.node-row').forEach(r => r.classList.remove('bg-indigo-900/60', 'border-indigo-600'));
  if (listWorldGifts) listWorldGifts.querySelectorAll('.gift-row').forEach(r => r.classList.remove('bg-amber-900/60', 'border-amber-500'));

  const activePin = pinsOverlay.querySelector(`.gift-pin[data-index="${giftIdx}"]`);
  if (activePin) activePin.classList.add('pin-selected');

  if (listWorldGifts) {
    const activeRow = listWorldGifts.querySelector(`.gift-row[data-index="${giftIdx}"]`);
    if (activeRow) activeRow.classList.add('bg-amber-900/60', 'border-amber-500');
  }
}

function onPinCoordinateInput() {
  const world = appState.worldsData.worlds.find(w => (w.worldNumber === appState.pinPositioner.selectedWorldId || w.id === appState.pinPositioner.selectedWorldId));
  if (!world) return;

  const pctX = Math.max(1, Math.min(99, parseFloat(inputPinX.value) || 0));
  const pctY = Math.max(1, Math.min(99, parseFloat(inputPinY.value) || 0));
  const floatX = parseFloat((pctX / 100).toFixed(4));
  const floatY = parseFloat((pctY / 100).toFixed(4));

  appState.pinPositioner.isDirty = true;

  if (appState.pinPositioner.selectedType === 'gift') {
    const gIdx = appState.pinPositioner.selectedGiftIndex;
    if (world.gifts && world.gifts[gIdx]) {
      world.gifts[gIdx].x = floatX;
      world.gifts[gIdx].y = floatY;
      const pinEl = pinsOverlay.querySelector(`.gift-pin[data-index="${gIdx}"]`);
      if (pinEl) {
        pinEl.style.left = `${pctX}%`;
        pinEl.style.top = `${pctY}%`;
      }
      renderWorldGiftsList(world);
    }
  } else {
    const idx = appState.pinPositioner.selectedNodeIndex;
    const nodes = world.levels || world.nodes;
    if (nodes && nodes[idx]) {
      nodes[idx].x = floatX;
      nodes[idx].y = floatY;
      const pinEl = pinsOverlay.querySelector(`.level-pin[data-index="${idx}"]`);
      if (pinEl) {
        pinEl.style.left = `${pctX}%`;
        pinEl.style.top = `${pctY}%`;
      }
      renderWorldNodesList(world);
    }
  }
}

function onPinsOverlayMouseMove(e) {
  if (!appState.pinPositioner.isDragging || appState.pinPositioner.dragIndex < 0) return;

  const rect = pinsOverlay.getBoundingClientRect();
  const rawX = ((e.clientX - rect.left) / rect.width) * 100;
  const rawY = ((e.clientY - rect.top) / rect.height) * 100;

  const pctX = Math.max(2, Math.min(98, parseFloat(rawX.toFixed(1))));
  const pctY = Math.max(2, Math.min(98, parseFloat(rawY.toFixed(1))));
  const floatX = parseFloat((pctX / 100).toFixed(4));
  const floatY = parseFloat((pctY / 100).toFixed(4));

  const world = appState.worldsData.worlds.find(w => (w.worldNumber === appState.pinPositioner.selectedWorldId || w.id === appState.pinPositioner.selectedWorldId));
  if (!world) return;

  appState.pinPositioner.isDirty = true;

  if (appState.pinPositioner.dragType === 'gift') {
    const gIdx = appState.pinPositioner.dragIndex;
    if (world.gifts && world.gifts[gIdx]) {
      world.gifts[gIdx].x = floatX;
      world.gifts[gIdx].y = floatY;
      const pinEl = pinsOverlay.querySelector(`.gift-pin[data-index="${gIdx}"]`);
      if (pinEl) {
        pinEl.style.left = `${pctX}%`;
        pinEl.style.top = `${pctY}%`;
      }
      inputPinX.value = pctX;
      inputPinY.value = pctY;
    }
  } else {
    const idx = appState.pinPositioner.dragIndex;
    const nodes = world.levels || world.nodes;
    if (nodes && nodes[idx]) {
      nodes[idx].x = floatX;
      nodes[idx].y = floatY;
      const pinEl = pinsOverlay.querySelector(`.level-pin[data-index="${idx}"]`);
      if (pinEl) {
        pinEl.style.left = `${pctX}%`;
        pinEl.style.top = `${pctY}%`;
      }
      inputPinX.value = pctX;
      inputPinY.value = pctY;
    }
  }
}

function onGiftDetailsInput() {
  if (appState.pinPositioner.selectedType !== 'gift') return;
  const world = appState.worldsData.worlds.find(w => (w.worldNumber === appState.pinPositioner.selectedWorldId || w.id === appState.pinPositioner.selectedWorldId));
  const gIdx = appState.pinPositioner.selectedGiftIndex;
  if (!world || !world.gifts || !world.gifts[gIdx]) return;

  world.gifts[gIdx].name = inputGiftName.value.trim() || `Gift ${world.gifts[gIdx].giftIndex}`;
  world.gifts[gIdx].requiredLevelOffset = parseInt(inputGiftOffset.value) || 5;
  world.gifts[gIdx].rewardCoins = parseInt(inputGiftCoins.value) || 100;

  badgeSelectedPinLevel.textContent = `🎁 Gift ${world.gifts[gIdx].giftIndex}: ${world.gifts[gIdx].name}`;
  appState.pinPositioner.isDirty = true;
  renderWorldGiftsList(world);
}

function renderWorldNodesList(world) {
  listWorldNodes.innerHTML = "";
  const nodes = world.levels || world.nodes;
  if (!nodes) return;

  nodes.forEach((node, idx) => {
    const row = document.createElement("div");
    row.className = "node-row p-1.5 rounded bg-slate-800 border border-slate-700/80 flex items-center justify-between cursor-pointer hover:bg-slate-700 transition";
    row.dataset.index = idx;
    const pctX = node.x > 1.0 ? node.x : (node.x * 100);
    const pctY = node.y > 1.0 ? node.y : (node.y * 100);
    row.innerHTML = `
      <span class="text-amber-300 font-bold">Lvl ${node.level}</span>
      <span class="text-slate-400">X: ${pctX.toFixed(1)}%</span>
      <span class="text-slate-400">Y: ${pctY.toFixed(1)}%</span>
    `;
    row.addEventListener("click", () => selectPinNode(idx));
    listWorldNodes.appendChild(row);
  });
}

function renderWorldGiftsList(world) {
  if (!listWorldGifts) return;
  listWorldGifts.innerHTML = "";
  if (!world.gifts) return;

  world.gifts.forEach((gift, idx) => {
    const row = document.createElement("div");
    row.className = `gift-row p-1.5 rounded bg-slate-800/90 border border-amber-500/40 flex items-center justify-between cursor-pointer hover:bg-slate-700 transition ${
      appState.pinPositioner.selectedType === 'gift' && appState.pinPositioner.selectedGiftIndex === idx ? 'bg-amber-900/60 border-amber-500' : ''
    }`;
    row.dataset.index = idx;
    const pctX = gift.x > 1.0 ? gift.x : (gift.x * 100);
    const pctY = gift.y > 1.0 ? gift.y : (gift.y * 100);
    row.innerHTML = `
      <div class="flex items-center gap-1.5">
        <span class="text-base">🎁</span>
        <div>
          <span class="text-amber-300 font-bold block leading-tight">Gift ${gift.giftIndex}: ${gift.name}</span>
          <span class="text-[10px] text-slate-400 font-normal">+${gift.requiredLevelOffset} lvls • ${gift.rewardCoins} coins</span>
        </div>
      </div>
      <div class="text-right text-[10px]">
        <span class="text-slate-400 block font-mono">X: ${pctX.toFixed(1)}%</span>
        <span class="text-slate-400 block font-mono">Y: ${pctY.toFixed(1)}%</span>
      </div>
    `;
    row.addEventListener("click", () => selectGiftNode(idx));
    listWorldGifts.appendChild(row);
  });
}

function adjustPinZoom(delta, fit = false) {
  if (fit) {
    appState.pinPositioner.zoomScale = 1.0;
  } else {
    appState.pinPositioner.zoomScale = Math.max(0.6, Math.min(2.0, appState.pinPositioner.zoomScale + delta));
  }

  lblZoomLevel.textContent = `${Math.round(appState.pinPositioner.zoomScale * 100)}%`;
  const baseW = 450;
  const baseH = 800;
  imgMapBackground.style.width = `${baseW * appState.pinPositioner.zoomScale}px`;
  imgMapBackground.style.height = `${baseH * appState.pinPositioner.zoomScale}px`;
}

async function saveActiveWorldPins(silent = false) {
  if (!window.gameStudioAPI || !appState.resolvedPaths || !appState.worldsData) {
    if (!silent) showToast("Cannot save: No project connection", true);
    return;
  }

  const res = await window.gameStudioAPI.saveWorlds({
    worldsConfigFile: appState.resolvedPaths.worldsConfig,
    data: appState.worldsData
  });

  if (res.success) {
    appState.pinPositioner.isDirty = false;
    if (silent) {
      showPinAutosavePill();
    } else {
      showToast("✓ Saved all map pin coordinates to worlds_config.json!");
    }
  } else {
    if (!silent) showToast(`Failed to save pins: ${res.error}`, true);
  }
}

// ============================================================================
// MODULE 3: CREATE NEW WORLD & AUTO-GENERATE LEVELS
// ============================================================================
async function openCreateWorldModal() {
  const highestWorld = appState.worldsData && appState.worldsData.worlds
    ? appState.worldsData.worlds.reduce((max, w) => Math.max(max, w.worldNumber || w.id || 0), 0)
    : (appState.worldCount || 49);

  const nextWorldNum = highestWorld + 1;
  inputNewWorldNumber.value = nextWorldNum;
  if (lblNewWorldNumBadge) lblNewWorldNumBadge.textContent = `World ${nextWorldNum}`;

  randomizeNewWorldName();
  appState.createWorldModalState.mapImageFile = null;
  appState.createWorldModalState.gameImageFile = null;

  // Populate background options from drawable directory
  await populateNewWorldBackgroundOptions(nextWorldNum);

  modalCreateWorld.classList.remove('hidden');
}

function randomizeNewWorldName() {
  const chosen = WORLD_NAME_SUGGESTIONS[Math.floor(Math.random() * WORLD_NAME_SUGGESTIONS.length)];
  const worldNum = parseInt(inputNewWorldNumber.value) || (appState.worldCount + 1);
  inputNewWorldName.value = chosen;
  inputNewWorldSubtitle.value = `WORLD ${worldNum} • ${chosen.toUpperCase()}`;
}

async function populateNewWorldBackgroundOptions(targetWorldNum) {
  selectNewWorldMapBg.innerHTML = "";
  selectNewWorldGameBg.innerHTML = "";

  let mapOptions = [`bg_map_world_${targetWorldNum}`];
  let gameOptions = [`bg_game_world_${targetWorldNum}`];

  if (window.gameStudioAPI && appState.resolvedPaths) {
    const res = await window.gameStudioAPI.listDrawableImages({
      drawableDir: appState.resolvedPaths.drawable
    });
    if (res && res.success) {
      if (res.mapImages && res.mapImages.length) {
        mapOptions = [...new Set([`bg_map_world_${targetWorldNum}`, ...res.mapImages.map(m => m.name)])];
      }
      if (res.gameImages && res.gameImages.length) {
        gameOptions = [...new Set([`bg_game_world_${targetWorldNum}`, ...res.gameImages.map(g => g.name)])];
      }
    }
  }

  mapOptions.forEach(opt => {
    const el = document.createElement("option");
    el.value = opt;
    el.textContent = opt;
    selectNewWorldMapBg.appendChild(el);
  });

  gameOptions.forEach(opt => {
    const el = document.createElement("option");
    el.value = opt;
    el.textContent = opt;
    selectNewWorldGameBg.appendChild(el);
  });

  updateNewWorldMapPreview();
  updateNewWorldGamePreview();
}

async function onBrowseNewWorldMapImage() {
  if (!window.gameStudioAPI) return;
  const res = await window.gameStudioAPI.selectImage({ title: "Select World Saga Map Background" });
  if (!res.canceled && res.filePath) {
    appState.createWorldModalState.mapImageFile = res;
    previewNewWorldMapBg.src = res.dataUrl;
    previewNewWorldMapBg.classList.remove('hidden');
    txtNoMapPreview.classList.add('hidden');

    // Add to dropdown and select
    const opt = document.createElement("option");
    opt.value = res.fileName;
    opt.textContent = `📂 ${res.fileName} (Custom)`;
    opt.selected = true;
    selectNewWorldMapBg.prepend(opt);
  }
}

async function onBrowseNewWorldGameImage() {
  if (!window.gameStudioAPI) return;
  const res = await window.gameStudioAPI.selectImage({ title: "Select World Gameplay Background" });
  if (!res.canceled && res.filePath) {
    appState.createWorldModalState.gameImageFile = res;
    previewNewWorldGameBg.src = res.dataUrl;
    previewNewWorldGameBg.classList.remove('hidden');
    txtNoGamePreview.classList.add('hidden');

    // Add to dropdown and select
    const opt = document.createElement("option");
    opt.value = res.fileName;
    opt.textContent = `📂 ${res.fileName} (Custom)`;
    opt.selected = true;
    selectNewWorldGameBg.prepend(opt);
  }
}

async function updateNewWorldMapPreview() {
  const selected = selectNewWorldMapBg.value;
  if (!selected) return;

  if (appState.createWorldModalState.mapImageFile && selectNewWorldMapBg.selectedOptions[0].textContent.includes('Custom')) {
    previewNewWorldMapBg.src = appState.createWorldModalState.mapImageFile.dataUrl;
    previewNewWorldMapBg.classList.remove('hidden');
    txtNoMapPreview.classList.add('hidden');
    return;
  }

  if (window.gameStudioAPI && appState.resolvedPaths) {
    const res = await window.gameStudioAPI.loadImageBase64({
      drawableDir: appState.resolvedPaths.drawable,
      imageName: selected
    });
    if (res && res.success) {
      previewNewWorldMapBg.src = res.dataUrl;
      previewNewWorldMapBg.classList.remove('hidden');
      txtNoMapPreview.classList.add('hidden');
    } else {
      previewNewWorldMapBg.classList.add('hidden');
      txtNoMapPreview.classList.remove('hidden');
    }
  }
}

async function updateNewWorldGamePreview() {
  const selected = selectNewWorldGameBg.value;
  if (!selected) return;

  if (appState.createWorldModalState.gameImageFile && selectNewWorldGameBg.selectedOptions[0].textContent.includes('Custom')) {
    previewNewWorldGameBg.src = appState.createWorldModalState.gameImageFile.dataUrl;
    previewNewWorldGameBg.classList.remove('hidden');
    txtNoGamePreview.classList.add('hidden');
    return;
  }

  if (window.gameStudioAPI && appState.resolvedPaths) {
    const res = await window.gameStudioAPI.loadImageBase64({
      drawableDir: appState.resolvedPaths.drawable,
      imageName: selected
    });
    if (res && res.success) {
      previewNewWorldGameBg.src = res.dataUrl;
      previewNewWorldGameBg.classList.remove('hidden');
      txtNoGamePreview.classList.add('hidden');
    } else {
      previewNewWorldGameBg.classList.add('hidden');
      txtNoGamePreview.classList.remove('hidden');
    }
  }
}

async function handleCreateNewWorld() {
  const worldNum = parseInt(inputNewWorldNumber.value);
  if (!worldNum || worldNum < 1) {
    showToast("Please enter a valid World Number", true);
    return;
  }

  const worldName = inputNewWorldName.value.trim() || `World ${worldNum}`;
  const subtitle = inputNewWorldSubtitle.value.trim() || `WORLD ${worldNum} • ${worldName.toUpperCase()}`;
  const levelsCount = parseInt(inputNewWorldLevelsCount.value) || 10;
  const colorVariety = parseInt(selectNewWorldColors.value) || 4;
  const progression = selectNewWorldProgression.value || "balanced";

  let mapBgName = selectNewWorldMapBg.value || `bg_map_world_${worldNum}`;
  let gameBgName = selectNewWorldGameBg.value || `bg_game_world_${worldNum}`;

  // If user selected custom image files from disk, copy them to res/drawable
  if (window.gameStudioAPI && appState.resolvedPaths) {
    if (appState.createWorldModalState.mapImageFile) {
      const copyRes = await window.gameStudioAPI.copyImageToDrawable({
        drawableDir: appState.resolvedPaths.drawable,
        sourcePath: appState.createWorldModalState.mapImageFile.filePath,
        targetBaseName: `bg_map_world_${worldNum}`
      });
      if (copyRes.success) mapBgName = copyRes.baseDrawableName;
    }

    if (appState.createWorldModalState.gameImageFile) {
      const copyRes = await window.gameStudioAPI.copyImageToDrawable({
        drawableDir: appState.resolvedPaths.drawable,
        sourcePath: appState.createWorldModalState.gameImageFile.filePath,
        targetBaseName: `bg_game_world_${worldNum}`
      });
      if (copyRes.success) gameBgName = copyRes.baseDrawableName;
    }
  }

  // Calculate start level number for this world
  const startLevelNum = (worldNum - 1) * 10 + 1;
  const endLevelNum = startLevelNum + levelsCount - 1;

  // Auto-generate S-curve Pin Path Coordinates (ascending vertically from bottom y=0.82 to top y=0.16)
  const generatedNodes = [];
  for (let i = 0; i < levelsCount; i++) {
    const lvl = startLevelNum + i;
    const progress = i / (levelsCount - 1); // 0.0 to 1.0
    // S-curve Y ascends from 0.82 down to 0.16
    const yVal = parseFloat((0.82 - (progress * 0.66)).toFixed(4));
    // S-curve X oscillates between 0.28 and 0.72
    const xVal = parseFloat((0.50 + Math.sin(progress * Math.PI * 2.5) * 0.22).toFixed(4));

    generatedNodes.push({
      level: lvl,
      x: xVal,
      y: yVal
    });
  }

  // Auto-generate 2 Mystery Gift Chests
  const generatedGifts = [
    {
      giftIndex: 1,
      name: "Mid-World Chest",
      x: parseFloat((0.36 + Math.sin(0.45 * Math.PI * 2.5) * 0.20).toFixed(4)),
      y: 0.48,
      requiredLevelOffset: 5,
      rewardCoins: 100
    },
    {
      giftIndex: 2,
      name: "Summit Gate Chest",
      x: parseFloat((0.54 + Math.sin(0.9 * Math.PI * 2.5) * 0.18).toFixed(4)),
      y: 0.14,
      requiredLevelOffset: levelsCount,
      rewardCoins: 250
    }
  ];

  // Auto-generate Level Files if checked
  if (chkAutoGenerateLevels.checked) {
    const generatedLevelsList = [];
    const allColors = ["RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE"];
    const activeColorPalette = allColors.slice(0, Math.min(6, Math.max(3, colorVariety)));

    for (let i = 0; i < levelsCount; i++) {
      const lvl = startLevelNum + i;
      const progress = i / (levelsCount - 1);
      const shotsAllowed = Math.max(16, Math.round(28 - progress * 10));

      // Choose objective variety
      let objType = "CLEAR_ALL";
      let objTarget = 0;
      let objColor = undefined;

      if (i % 4 === 1) {
        objType = "SCORE_TARGET";
        objTarget = 5000 + i * 400;
      } else if (i % 4 === 2) {
        objType = "POP_COLOR";
        objColor = activeColorPalette[i % activeColorPalette.length];
        objTarget = 12 + i;
      } else if (i % 4 === 3) {
        objType = "DROP_COUNT";
        objTarget = 8 + i;
      }

      // Choose pattern based on index & progression
      const patternIdx = (worldNum + i) % PRESET_PATTERNS.length;
      const pat = PRESET_PATTERNS[patternIdx];
      const rows = pat.rows.map((r, idx) => {
        const expected = (idx % 2 === 0) ? 9 : 8;
        return (r + ".".repeat(expected)).substring(0, expected);
      });

      const baseScore = 2400 + i * 350;
      const levelData = {
        level: lvl,
        shots: shotsAllowed,
        colors: activeColorPalette,
        objective: {
          type: objType,
          target: objTarget
        },
        starThresholds: [
          Math.round(baseScore * 0.85),
          Math.round(baseScore * 1.85),
          Math.round(baseScore * 3.20)
        ],
        rows: rows
      };

      if (objColor) levelData.objective.color = objColor;

      generatedLevelsList.push({
        levelNumber: lvl,
        data: levelData
      });
    }

    // Batch save all levels
    if (window.gameStudioAPI && appState.resolvedPaths) {
      await window.gameStudioAPI.saveLevelsBatch({
        levelsDir: appState.resolvedPaths.levels,
        levelsList: generatedLevelsList
      });
    }
  }

  // Create or Update World in worlds_config.json
  const newWorldObj = {
    worldNumber: worldNum,
    name: worldName,
    subtitle: subtitle,
    mapBackground: mapBgName,
    gameBackground: gameBgName,
    levelsCount: levelsCount,
    levels: generatedNodes,
    gifts: generatedGifts
  };

  if (!appState.worldsData) appState.worldsData = { worlds: [] };
  if (!appState.worldsData.worlds) appState.worldsData.worlds = [];

  const existingIdx = appState.worldsData.worlds.findIndex(w => (w.worldNumber === worldNum || w.id === worldNum));
  if (existingIdx >= 0) {
    appState.worldsData.worlds[existingIdx] = newWorldObj;
  } else {
    appState.worldsData.worlds.push(newWorldObj);
  }

  appState.worldsData.worlds.sort((a, b) => (a.worldNumber || a.id) - (b.worldNumber || b.id));

  // Save worlds_config.json
  if (window.gameStudioAPI && appState.resolvedPaths) {
    await window.gameStudioAPI.saveWorlds({
      worldsConfigFile: appState.resolvedPaths.worldsConfig,
      data: appState.worldsData
    });
  }

  // Update App State
  appState.worldCount = appState.worldsData.worlds.length;
  appState.levelCount = Math.max(appState.levelCount, endLevelNum);
  lblWorldCount.textContent = appState.worldCount;
  lblLevelCount.textContent = appState.levelCount;
  if (badgeWorldsCount) badgeWorldsCount.textContent = `${appState.worldCount} Total Worlds`;

  populateWorldSelector();
  renderCampaignWorldsGrid();
  modalCreateWorld.classList.add('hidden');

  // Switch to Pin Positioner for the new world
  switchTab('pin-positioner');
  await loadWorldForPinPositioner(worldNum);

  showToast(`🚀 Successfully created World ${worldNum}: ${worldName} with ${levelsCount} levels & pin path!`);
}

// ============================================================================
// CREATE NEW LEVEL FEATURE
// ============================================================================
function openNewLevelModal() {
  const nextLevel = (appState.levelCount || 490) + 1;
  inputNewLevelNum.value = nextLevel;
  inputNewLevelShots.value = 20;
  selectNewLevelObjective.value = "CLEAR_ALL";
  selectNewLevelTemplate.value = "blank";
  updateNewLevelWorldHint();
  modalNewLevel.classList.remove('hidden');
}

function updateNewLevelWorldHint() {
  const lvl = parseInt(inputNewLevelNum.value) || 1;
  const worldNum = Math.floor((lvl - 1) / 10) + 1;
  lblNewLevelWorldHint.textContent = `World ${worldNum}`;
}

async function handleCreateNewLevel() {
  const newLevelNum = parseInt(inputNewLevelNum.value);
  if (!newLevelNum || newLevelNum < 1) {
    showToast("Please enter a valid level number", true);
    return;
  }

  const shots = parseInt(inputNewLevelShots.value) || 20;
  const objectiveType = selectNewLevelObjective.value || "CLEAR_ALL";
  const template = selectNewLevelTemplate.value;

  let rows = [];
  let colors = ["RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE"];

  if (template === "clone") {
    rows = [...appState.levelDesigner.rows];
    colors = [...appState.levelDesigner.colors];
  } else if (template === "diamond") {
    rows = [
      "...RRR...",
      "..RRRR..",
      ".RRBBRR.",
      ".RBBBBR.",
      "..RBRB..",
      "...RR..."
    ];
  } else if (template === "waves") {
    rows = [
      "RRGGYYBBP",
      "RGGYYBBP",
      "GGYYBBPPR",
      "GYYBBPPR",
      "YYBBPPRRG",
      "YBBPPRRG"
    ];
  } else {
    // Blank grid - 8 empty rows
    rows = [
      ".........",
      "........",
      ".........",
      "........",
      ".........",
      "........",
      ".........",
      "........"
    ];
  }

  const sanitizedRows = rows.map((r, idx) => {
    const expected = (idx % 2 === 0) ? 9 : 8;
    return (r + ".".repeat(expected)).substring(0, expected);
  });

  const levelData = {
    level: newLevelNum,
    shots: shots,
    colors: colors,
    objective: {
      type: objectiveType,
      target: objectiveType === "SCORE_TARGET" ? 5000 : 0
    },
    starThresholds: [2000, 4500, 8000],
    rows: sanitizedRows
  };

  if (!window.gameStudioAPI || !appState.resolvedPaths) {
    appState.levelDesigner.levelNumber = newLevelNum;
    appState.levelDesigner.shots = shots;
    appState.levelDesigner.colors = colors;
    appState.levelDesigner.rows = sanitizedRows;
    renderLevelBoard();
    modalNewLevel.classList.add('hidden');
    showToast(`Level ${newLevelNum} created (Preview mode)`);
    return;
  }

  // 1. Save level file directly to assets/levels/
  const saveRes = await window.gameStudioAPI.saveLevel({
    levelsDir: appState.resolvedPaths.levels,
    levelNumber: newLevelNum,
    data: levelData
  });

  if (!saveRes.success) {
    showToast(`Failed to create level: ${saveRes.error}`, true);
    return;
  }

  // 2. Ensure level is registered in worlds_config.json
  if (appState.worldsData && appState.worldsData.worlds) {
    const targetWorldNum = Math.floor((newLevelNum - 1) / 10) + 1;
    let targetWorld = appState.worldsData.worlds.find(w => (w.worldNumber === targetWorldNum || w.id === targetWorldNum));

    if (!targetWorld) {
      targetWorld = {
        worldNumber: targetWorldNum,
        name: `World ${targetWorldNum}`,
        subtitle: `WORLD ${targetWorldNum}`,
        mapBackground: `bg_map_world_${targetWorldNum}`,
        gameBackground: `bg_game_world_${targetWorldNum}`,
        levelsCount: 10,
        levels: []
      };
      appState.worldsData.worlds.push(targetWorld);
    }

    if (!targetWorld.levels) targetWorld.levels = targetWorld.nodes || [];
    const exists = targetWorld.levels.some(l => l.level === newLevelNum);
    if (!exists) {
      const idxInWorld = (newLevelNum - 1) % 10;
      const defaultY = Math.max(0.15, Math.min(0.85, 0.80 - (idxInWorld * 0.065)));
      const defaultX = 0.35 + (idxInWorld % 2 === 0 ? 0.15 : -0.10);
      targetWorld.levels.push({
        level: newLevelNum,
        x: parseFloat(defaultX.toFixed(4)),
        y: parseFloat(defaultY.toFixed(4))
      });
      targetWorld.levels.sort((a, b) => a.level - b.level);
    }

    // Save updated worlds_config.json
    await window.gameStudioAPI.saveWorlds({
      worldsConfigFile: appState.resolvedPaths.worldsConfig,
      data: appState.worldsData
    });
  }

  // 3. Update level counts and refresh
  appState.levelCount = Math.max(appState.levelCount, newLevelNum);
  lblLevelCount.textContent = appState.levelCount;
  populateWorldSelector();
  modalNewLevel.classList.add('hidden');

  // 4. Switch to designer and load new level
  switchTab('level-designer');
  await loadLevel(newLevelNum);
  showToast(`✓ Successfully created and opened Level ${newLevelNum}!`);
}

// ============================================================================
// MODULE 3B: EDIT EXISTING WORLD & BACKGROUNDS
// ============================================================================
async function openEditWorldModal(worldNum) {
  const world = appState.worldsData && appState.worldsData.worlds
    ? appState.worldsData.worlds.find(w => (w.worldNumber === worldNum || w.id === worldNum))
    : null;

  if (!world) {
    showToast(`World ${worldNum} not found in project config`, true);
    return;
  }

  appState.editWorldModalState.worldNumber = worldNum;
  appState.editWorldModalState.mapImageFile = null;
  appState.editWorldModalState.gameImageFile = null;

  lblEditWorldNumBadge.textContent = `World ${worldNum}`;
  inputEditWorldName.value = world.name || `World ${worldNum}`;
  inputEditWorldSubtitle.value = world.subtitle || `WORLD ${worldNum} • ${(world.name || '').toUpperCase()}`;

  const currentMapBg = world.mapBackground || `bg_map_world_${worldNum}`;
  const currentGameBg = world.gameBackground || `bg_game_world_${worldNum}`;

  await populateEditWorldBackgroundOptions(worldNum, currentMapBg, currentGameBg);

  modalEditExistingWorld.classList.remove('hidden');
}

function randomizeEditWorldName() {
  const chosen = WORLD_NAME_SUGGESTIONS[Math.floor(Math.random() * WORLD_NAME_SUGGESTIONS.length)];
  const worldNum = appState.editWorldModalState.worldNumber || 1;
  inputEditWorldName.value = chosen;
  inputEditWorldSubtitle.value = `WORLD ${worldNum} • ${chosen.toUpperCase()}`;
}

async function populateEditWorldBackgroundOptions(targetWorldNum, currentMapBg, currentGameBg) {
  selectEditWorldMapBg.innerHTML = "";
  selectEditWorldGameBg.innerHTML = "";

  let mapOptions = [currentMapBg, `bg_map_world_${targetWorldNum}`];
  let gameOptions = [currentGameBg, `bg_game_world_${targetWorldNum}`];

  if (window.gameStudioAPI && appState.resolvedPaths) {
    const res = await window.gameStudioAPI.listDrawableImages({
      drawableDir: appState.resolvedPaths.drawable
    });
    if (res && res.success) {
      if (res.mapImages && res.mapImages.length) {
        mapOptions = [...new Set([currentMapBg, ...res.mapImages.map(m => m.name)])];
      }
      if (res.gameImages && res.gameImages.length) {
        gameOptions = [...new Set([currentGameBg, ...res.gameImages.map(g => g.name)])];
      }
    }
  }

  mapOptions.forEach(opt => {
    const el = document.createElement("option");
    el.value = opt;
    el.textContent = opt;
    if (opt === currentMapBg) el.selected = true;
    selectEditWorldMapBg.appendChild(el);
  });

  gameOptions.forEach(opt => {
    const el = document.createElement("option");
    el.value = opt;
    el.textContent = opt;
    if (opt === currentGameBg) el.selected = true;
    selectEditWorldGameBg.appendChild(el);
  });

  updateEditWorldMapPreview();
  updateEditWorldGamePreview();
}

async function onBrowseEditWorldMapImage() {
  if (!window.gameStudioAPI) return;
  const res = await window.gameStudioAPI.selectImage({ title: "Select Saga Map Background for World " + appState.editWorldModalState.worldNumber });
  if (!res.canceled && res.filePath) {
    appState.editWorldModalState.mapImageFile = res;
    previewEditWorldMapBg.src = res.dataUrl;
    previewEditWorldMapBg.classList.remove('hidden');
    txtNoEditMapPreview.classList.add('hidden');

    const opt = document.createElement("option");
    opt.value = res.fileName;
    opt.textContent = `📂 ${res.fileName} (Custom)`;
    opt.selected = true;
    selectEditWorldMapBg.prepend(opt);
  }
}

async function onBrowseEditWorldGameImage() {
  if (!window.gameStudioAPI) return;
  const res = await window.gameStudioAPI.selectImage({ title: "Select Gameplay Background for World " + appState.editWorldModalState.worldNumber });
  if (!res.canceled && res.filePath) {
    appState.editWorldModalState.gameImageFile = res;
    previewEditWorldGameBg.src = res.dataUrl;
    previewEditWorldGameBg.classList.remove('hidden');
    txtNoEditGamePreview.classList.add('hidden');

    const opt = document.createElement("option");
    opt.value = res.fileName;
    opt.textContent = `📂 ${res.fileName} (Custom)`;
    opt.selected = true;
    selectEditWorldGameBg.prepend(opt);
  }
}

async function updateEditWorldMapPreview() {
  const selected = selectEditWorldMapBg.value;
  if (!selected) return;

  if (appState.editWorldModalState.mapImageFile && selectEditWorldMapBg.selectedOptions[0]?.textContent.includes('Custom')) {
    previewEditWorldMapBg.src = appState.editWorldModalState.mapImageFile.dataUrl;
    previewEditWorldMapBg.classList.remove('hidden');
    txtNoEditMapPreview.classList.add('hidden');
    return;
  }

  if (window.gameStudioAPI && appState.resolvedPaths) {
    const res = await window.gameStudioAPI.loadImageBase64({
      drawableDir: appState.resolvedPaths.drawable,
      imageName: selected
    });
    if (res && res.success) {
      previewEditWorldMapBg.src = res.dataUrl;
      previewEditWorldMapBg.classList.remove('hidden');
      txtNoEditMapPreview.classList.add('hidden');
    } else {
      previewEditWorldMapBg.classList.add('hidden');
      txtNoEditMapPreview.classList.remove('hidden');
    }
  }
}

async function updateEditWorldGamePreview() {
  const selected = selectEditWorldGameBg.value;
  if (!selected) return;

  if (appState.editWorldModalState.gameImageFile && selectEditWorldGameBg.selectedOptions[0]?.textContent.includes('Custom')) {
    previewEditWorldGameBg.src = appState.editWorldModalState.gameImageFile.dataUrl;
    previewEditWorldGameBg.classList.remove('hidden');
    txtNoEditGamePreview.classList.add('hidden');
    return;
  }

  if (window.gameStudioAPI && appState.resolvedPaths) {
    const res = await window.gameStudioAPI.loadImageBase64({
      drawableDir: appState.resolvedPaths.drawable,
      imageName: selected
    });
    if (res && res.success) {
      previewEditWorldGameBg.src = res.dataUrl;
      previewEditWorldGameBg.classList.remove('hidden');
      txtNoEditGamePreview.classList.add('hidden');
    } else {
      previewEditWorldGameBg.classList.add('hidden');
      txtNoEditGamePreview.classList.remove('hidden');
    }
  }
}

async function handleConfirmEditWorld() {
  const worldNum = appState.editWorldModalState.worldNumber;
  if (!worldNum || !appState.worldsData || !appState.worldsData.worlds) return;

  const world = appState.worldsData.worlds.find(w => (w.worldNumber === worldNum || w.id === worldNum));
  if (!world) {
    showToast(`World ${worldNum} not found`, true);
    return;
  }

  const worldName = inputEditWorldName.value.trim() || `World ${worldNum}`;
  const subtitle = inputEditWorldSubtitle.value.trim() || `WORLD ${worldNum} • ${worldName.toUpperCase()}`;

  let mapBgName = selectEditWorldMapBg.value || world.mapBackground || `bg_map_world_${worldNum}`;
  let gameBgName = selectEditWorldGameBg.value || world.gameBackground || `bg_game_world_${worldNum}`;

  // If custom disk images were picked, copy them to res/drawable
  if (window.gameStudioAPI && appState.resolvedPaths) {
    if (appState.editWorldModalState.mapImageFile) {
      const copyRes = await window.gameStudioAPI.copyImageToDrawable({
        drawableDir: appState.resolvedPaths.drawable,
        sourcePath: appState.editWorldModalState.mapImageFile.filePath,
        targetBaseName: `bg_map_world_${worldNum}`
      });
      if (copyRes.success) mapBgName = copyRes.baseDrawableName;
    }

    if (appState.editWorldModalState.gameImageFile) {
      const copyRes = await window.gameStudioAPI.copyImageToDrawable({
        drawableDir: appState.resolvedPaths.drawable,
        sourcePath: appState.editWorldModalState.gameImageFile.filePath,
        targetBaseName: `bg_game_world_${worldNum}`
      });
      if (copyRes.success) gameBgName = copyRes.baseDrawableName;
    }
  }

  // Update in-memory world
  world.name = worldName;
  world.subtitle = subtitle;
  world.mapBackground = mapBgName;
  world.gameBackground = gameBgName;

  // Persist worlds_config.json
  if (window.gameStudioAPI && appState.resolvedPaths) {
    await window.gameStudioAPI.saveWorlds({
      worldsConfigFile: appState.resolvedPaths.worldsConfig,
      data: appState.worldsData
    });
  }

  modalEditExistingWorld.classList.add('hidden');
  populateWorldSelector();
  renderCampaignWorldsGrid();

  // If this world is active in pin positioner, refresh it
  if (appState.pinPositioner.selectedWorldId === worldNum) {
    await loadWorldForPinPositioner(worldNum);
  }

  showToast(`✓ Saved World ${worldNum} changes: "${worldName}"!`);
}

// ============================================================================
// MODULE 4: CAMPAIGN WORLDS EXPLORER
// ============================================================================
async function renderCampaignWorldsGrid() {
  gridWorldsList.innerHTML = "";
  if (!appState.worldsData || !appState.worldsData.worlds) return;

  for (const w of appState.worldsData.worlds) {
    const worldNum = w.worldNumber || w.id;
    const levels = w.levels || w.nodes || [];
    const startLevel = levels.length > 0 ? levels[0].level : ((worldNum - 1) * 10 + 1);
    const endLevel = levels.length > 0 ? levels[levels.length - 1].level : (worldNum * 10);
    const bgName = w.mapBackground || `bg_map_world_${worldNum}`;

    const card = document.createElement("div");
    card.className = "world-card-item studio-card p-3 flex flex-col space-y-2.5 shadow-md hover:border-indigo-500/60 transition group";

    card.innerHTML = `
      <div class="flex items-center justify-between">
        <span class="font-bold text-white text-xs">World ${worldNum}</span>
        <span class="text-[10px] bg-indigo-500/15 text-indigo-300 px-2 py-0.5 rounded-md border border-indigo-500/30 font-mono">Lvls ${startLevel}-${endLevel}</span>
      </div>
      <div class="text-slate-200 font-semibold text-xs truncate">${w.name}</div>
      <div class="h-28 rounded-lg overflow-hidden bg-studio-bg border border-studio-border relative flex items-center justify-center">
        <img id="thumbWorld${worldNum}" class="w-full h-full object-cover group-hover:scale-105 transition duration-300" />
        <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent flex items-end p-2">
          <span class="text-[10px] text-amber-300 font-mono font-bold">${levels.length} Nodes Placed</span>
        </div>
      </div>
      <div class="grid grid-cols-3 gap-1 pt-1">
        <button class="btn-edit-pins studio-btn studio-btn-secondary py-1 text-[10px] font-semibold" data-world="${worldNum}" title="Open in Map Pin Positioner">
          🎯 Pins
        </button>
        <button class="btn-edit-lvl studio-btn studio-btn-secondary py-1 text-[10px] font-semibold" data-level="${startLevel}" title="Edit Levels in Designer">
          🔮 Lvl ${startLevel}
        </button>
        <button class="btn-edit-world studio-btn studio-btn-secondary py-1 text-[10px] font-semibold" data-world="${worldNum}" title="Edit World Info & Backgrounds">
          ⚙️ Edit
        </button>
      </div>
    `;

    card.querySelector('.btn-edit-pins').addEventListener('click', () => {
      switchTab('pin-positioner');
      loadWorldForPinPositioner(worldNum);
    });

    card.querySelector('.btn-edit-lvl').addEventListener('click', () => {
      switchTab('level-designer');
      loadLevel(startLevel);
    });

    card.querySelector('.btn-edit-world').addEventListener('click', () => {
      openEditWorldModal(worldNum);
    });

    gridWorldsList.appendChild(card);

    // Asynchronously load thumbnail
    if (window.gameStudioAPI && appState.resolvedPaths) {
      window.gameStudioAPI.loadImageBase64({
        drawableDir: appState.resolvedPaths.drawable,
        imageName: bgName
      }).then(res => {
        if (res && res.success) {
          const img = document.getElementById(`thumbWorld${worldNum}`);
          if (img) img.src = res.dataUrl;
        }
      });
    }
  }
}

// ============================================================================
// TOAST NOTIFICATIONS
// ============================================================================
function showToast(msg, isError = false) {
  toastNotification.textContent = msg;
  toastNotification.className = `fixed bottom-6 left-1/2 -translate-x-1/2 ${
    isError ? 'bg-red-600' : 'bg-emerald-600'
  } text-white font-semibold text-xs px-5 py-2.5 rounded-xl shadow-2xl z-50 transition transform duration-200 opacity-100`;

  setTimeout(() => {
    toastNotification.classList.remove('opacity-100');
    toastNotification.classList.add('opacity-0');
  }, 2600);
}

// Start application
initApp();
