import { app, BrowserWindow, ipcMain, dialog } from 'electron';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import mammoth from 'mammoth';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const isDev = !app.isPackaged;

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 900,
    minHeight: 700,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    },
    title: 'PowerWord Viewer - All Languages Supported',
    backgroundColor: '#0f172a',
    show: false
  });

  if (isDev) {
    mainWindow.loadFile('index.html');
  } else {
    mainWindow.loadFile(path.join(__dirname, 'dist', 'index.html'));
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.setTitle('PowerWord Viewer - Open a Word Document to Begin');
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

ipcMain.handle('open-file-dialog', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile'],
    filters: [
      { name: 'Word Documents', extensions: ['docx', 'doc'] },
      { name: 'All Files', extensions: ['*'] }
    ]
  });

  if (result.canceled) {
    return { canceled: true };
  }

  const filePath = result.filePaths[0];
  const fileName = path.basename(filePath);

  try {
    const buffer = fs.readFileSync(filePath);
    const mammothResult = await mammoth.convertToHtml({ buffer });

    return {
      canceled: false,
      fileName,
      filePath,
      html: mammothResult.value,
      messages: mammothResult.messages
    };
  } catch (error) {
    return {
      canceled: false,
      error: error.message,
      fileName,
      filePath
    };
  }
});

ipcMain.handle('read-file', async (_event, filePath) => {
  try {
    const buffer = fs.readFileSync(filePath);
    const mammothResult = await mammoth.convertToHtml({ buffer });
    const fileName = path.basename(filePath);

    return {
      success: true,
      fileName,
      filePath,
      html: mammothResult.value,
      messages: mammothResult.messages
    };
  } catch (error) {
    return {
      success: false,
      error: error.message
    };
  }
});
