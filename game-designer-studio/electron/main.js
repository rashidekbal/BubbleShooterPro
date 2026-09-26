const { app, BrowserWindow, ipcMain, dialog, Menu } = require('electron');
const path = require('path');
const fs = require('fs').promises;
const fsSync = require('fs');

// Disable standard menu bar across application
Menu.setApplicationMenu(null);

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1200,
    minHeight: 760,
    backgroundColor: '#090d16',
    title: 'Bubble Shooter Pro - Game Designer Studio',
    icon: path.join(__dirname, '../assets/icon.png'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  mainWindow.removeMenu();
  mainWindow.setMenuBarVisibility(false);

  mainWindow.loadFile(path.join(__dirname, '../src/index.html'));

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// Helper to resolve paths inside Android project
function resolveGamePaths(baseDir) {
  const candidates = [
    baseDir,
    path.join(baseDir, 'BubbleShooterpro'),
    path.join(baseDir, 'app'),
    path.join(baseDir, 'BubbleShooterpro', 'app')
  ];

  for (const c of candidates) {
    const assetsCandidate = path.join(c, 'src', 'main', 'assets');
    if (fsSync.existsSync(assetsCandidate)) {
      return {
        root: c,
        assets: assetsCandidate,
        levels: path.join(assetsCandidate, 'levels'),
        worldsConfig: path.join(assetsCandidate, 'worlds_config.json'),
        drawable: path.join(c, 'src', 'main', 'res', 'drawable'),
        raw: path.join(c, 'src', 'main', 'res', 'raw')
      };
    }
    // Check direct assets directory
    const directAssets = path.join(c, 'assets');
    if (fsSync.existsSync(directAssets)) {
      return {
        root: c,
        assets: directAssets,
        levels: path.join(directAssets, 'levels'),
        worldsConfig: path.join(directAssets, 'worlds_config.json'),
        drawable: path.join(c, 'res', 'drawable'),
        raw: path.join(c, 'res', 'raw')
      };
    }
  }

  return null;
}

// IPC Handlers
ipcMain.handle('dialog:select-project', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Select Bubble Shooter Pro Game Project Folder',
    properties: ['openDirectory']
  });

  if (result.canceled || result.filePaths.length === 0) {
    return { canceled: true };
  }

  const selectedPath = result.filePaths[0];
  return { canceled: false, path: selectedPath };
});

ipcMain.handle('project:scan', async (event, projectPath) => {
  try {
    const resolved = resolveGamePaths(projectPath);
    if (!resolved) {
      return { success: false, error: 'Could not locate Android assets directory (assets/levels or assets/worlds_config.json)' };
    }

    let levelCount = 0;
    let levelFiles = [];
    if (fsSync.existsSync(resolved.levels)) {
      const files = await fs.readdir(resolved.levels);
      levelFiles = files.filter(f => f.startsWith('level_') && f.endsWith('.json')).sort();
      levelCount = levelFiles.length;
    }

    let worldsData = null;
    let worldCount = 0;
    if (fsSync.existsSync(resolved.worldsConfig)) {
      const raw = await fs.readFile(resolved.worldsConfig, 'utf-8');
      worldsData = JSON.parse(raw);
      worldCount = worldsData.worlds ? worldsData.worlds.length : 0;
    }

    return {
      success: true,
      projectPath,
      resolvedPaths: resolved,
      levelCount,
      worldCount,
      worldsData
    };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('level:load', async (event, { levelsDir, levelNumber }) => {
  try {
    const filename = levelNumber < 100 
      ? `level_${String(levelNumber).padStart(2, '0')}.json` 
      : `level_${levelNumber}.json`;
    const fullPath = path.join(levelsDir, filename);

    if (!fsSync.existsSync(fullPath)) {
      return { success: false, error: `File not found: ${filename}` };
    }

    const content = await fs.readFile(fullPath, 'utf-8');
    const data = JSON.parse(content);
    return { success: true, levelNumber, filename, fullPath, data };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('level:save', async (event, { levelsDir, levelNumber, data }) => {
  try {
    const filename = levelNumber < 100 
      ? `level_${String(levelNumber).padStart(2, '0')}.json` 
      : `level_${levelNumber}.json`;
    const fullPath = path.join(levelsDir, filename);

    // Ensure directory exists
    await fs.mkdir(levelsDir, { recursive: true });
    
    // Format JSON with 2 spaces
    const jsonStr = JSON.stringify(data, null, 2);
    await fs.writeFile(fullPath, jsonStr, 'utf-8');

    return { success: true, filename, fullPath, levelNumber };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('worlds:load', async (event, { worldsConfigFile }) => {
  try {
    if (!fsSync.existsSync(worldsConfigFile)) {
      return { success: false, error: 'worlds_config.json not found' };
    }
    const content = await fs.readFile(worldsConfigFile, 'utf-8');
    const data = JSON.parse(content);
    return { success: true, data };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('worlds:save', async (event, { worldsConfigFile, data }) => {
  try {
    const jsonStr = JSON.stringify(data, null, 2);
    await fs.writeFile(worldsConfigFile, jsonStr, 'utf-8');
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('image:load-base64', async (event, { drawableDir, imageName }) => {
  try {
    // Try imageName directly, or with .jpg, .png, .webp extensions
    const extensions = ['', '.jpg', '.png', '.webp', '.jpeg'];
    let foundPath = null;

    for (const ext of extensions) {
      const p = path.join(drawableDir, imageName + ext);
      if (fsSync.existsSync(p)) {
        foundPath = p;
        break;
      }
    }

    if (!foundPath) {
      return { success: false, error: `Image not found: ${imageName}` };
    }

    const buffer = await fs.readFile(foundPath);
    const ext = path.extname(foundPath).toLowerCase();
    const mime = ext === '.png' ? 'image/png' : ext === '.webp' ? 'image/webp' : 'image/jpeg';
    const base64Url = `data:${mime};base64,${buffer.toString('base64')}`;

    return { success: true, dataUrl: base64Url, path: foundPath };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

// Select Image Dialog
ipcMain.handle('dialog:select-image', async (event, { title = 'Select Background Image' } = {}) => {
  try {
    const result = await dialog.showOpenDialog(mainWindow, {
      title,
      filters: [
        { name: 'Image Files', extensions: ['jpg', 'jpeg', 'png', 'webp'] },
        { name: 'All Files', extensions: ['*'] }
      ],
      properties: ['openFile']
    });

    if (result.canceled || result.filePaths.length === 0) {
      return { canceled: true };
    }

    const filePath = result.filePaths[0];
    const buffer = await fs.readFile(filePath);
    const ext = path.extname(filePath).toLowerCase();
    const mime = ext === '.png' ? 'image/png' : ext === '.webp' ? 'image/webp' : 'image/jpeg';
    const base64Url = `data:${mime};base64,${buffer.toString('base64')}`;

    return { canceled: false, filePath, fileName: path.basename(filePath), dataUrl: base64Url };
  } catch (err) {
    return { canceled: true, error: err.message };
  }
});

// Copy Image into Android res/drawable folder
ipcMain.handle('image:copy-to-drawable', async (event, { drawableDir, sourcePath, targetBaseName }) => {
  try {
    if (!fsSync.existsSync(drawableDir)) {
      await fs.mkdir(drawableDir, { recursive: true });
    }

    const ext = path.extname(sourcePath).toLowerCase() || '.jpg';
    const finalName = targetBaseName.endsWith(ext) ? targetBaseName : `${targetBaseName}${ext}`;
    const destPath = path.join(drawableDir, finalName);

    await fs.copyFile(sourcePath, destPath);

    return { success: true, fileName: finalName, fullPath: destPath, baseDrawableName: path.parse(finalName).name };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

// List all background images in drawable folder and project
ipcMain.handle('drawable:list-images', async (event, { drawableDir, projectRoot }) => {
  try {
    const mapImages = [];
    const gameImages = [];
    const allImages = [];

    if (fsSync.existsSync(drawableDir)) {
      const files = await fs.readdir(drawableDir);
      for (const f of files) {
        const ext = path.extname(f).toLowerCase();
        if (['.jpg', '.jpeg', '.png', '.webp'].includes(ext)) {
          const base = path.parse(f).name;
          allImages.push({ name: base, file: f, path: path.join(drawableDir, f) });
          if (base.startsWith('bg_map_')) {
            mapImages.push({ name: base, file: f, path: path.join(drawableDir, f) });
          } else if (base.startsWith('bg_game_')) {
            gameImages.push({ name: base, file: f, path: path.join(drawableDir, f) });
          }
        }
      }
    }

    return { success: true, mapImages, gameImages, allImages };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

// Save multiple levels in batch
ipcMain.handle('levels:save-batch', async (event, { levelsDir, levelsList }) => {
  try {
    await fs.mkdir(levelsDir, { recursive: true });
    const saved = [];

    for (const item of levelsList) {
      const lvlNum = item.levelNumber || item.data.level;
      const filename = lvlNum < 100 
        ? `level_${String(lvlNum).padStart(2, '0')}.json` 
        : `level_${lvlNum}.json`;
      const fullPath = path.join(levelsDir, filename);
      const jsonStr = JSON.stringify(item.data, null, 2);
      await fs.writeFile(fullPath, jsonStr, 'utf-8');
      saved.push({ levelNumber: lvlNum, filename, fullPath });
    }

    return { success: true, count: saved.length, saved };
  } catch (err) {
    return { success: false, error: err.message };
  }
});
