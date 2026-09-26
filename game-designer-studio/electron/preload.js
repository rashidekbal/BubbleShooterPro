const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('gameStudioAPI', {
  selectProject: () => ipcRenderer.invoke('dialog:select-project'),
  scanProject: (projectPath) => ipcRenderer.invoke('project:scan', projectPath),
  loadLevel: (params) => ipcRenderer.invoke('level:load', params),
  saveLevel: (params) => ipcRenderer.invoke('level:save', params),
  saveLevelsBatch: (params) => ipcRenderer.invoke('levels:save-batch', params),
  loadWorlds: (params) => ipcRenderer.invoke('worlds:load', params),
  saveWorlds: (params) => ipcRenderer.invoke('worlds:save', params),
  loadImageBase64: (params) => ipcRenderer.invoke('image:load-base64', params),
  selectImage: (params) => ipcRenderer.invoke('dialog:select-image', params),
  copyImageToDrawable: (params) => ipcRenderer.invoke('image:copy-to-drawable', params),
  listDrawableImages: (params) => ipcRenderer.invoke('drawable:list-images', params)
});
