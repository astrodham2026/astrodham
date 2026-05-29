import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';

// --- OFFICIAL VITE + MONACO WORKER INTEGRATION ---
// Tell Vite to compile and inline the Monaco worker script, preventing "Unexpected Usage" crashes
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';

self.MonacoEnvironment = {
  getWorker(_, label) {
    return new editorWorker(); // Return Vite-bundled offline worker instance
  }
};

// Attach the fully loaded static Monaco instance to the React loader
loader.config({ monaco });

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
