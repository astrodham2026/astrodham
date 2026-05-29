import { useState, useRef, useCallback, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import {
  FileText,
  Download,
  Search,
  CloudOff,
  Activity,
  Eye,
  Edit3,
  X,
  ChevronLeft,
  ChevronRight,
  ZoomIn,
  ZoomOut,
  AlertCircle
} from 'lucide-react';
import { Document, Packer, Paragraph, TextRun } from 'docx';
import { saveAs } from 'file-saver';
import './App.css';

function App() {
  const [mode, setMode] = useState('editor'); // 'editor' or 'viewer'
  const [wordCount, setWordCount] = useState(0);
  const [charCount, setCharCount] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [document, setDocument] = useState(null);
  const [htmlContent, setHtmlContent] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [currentResultIndex, setCurrentResultIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [zoom, setZoom] = useState(100);
  const editorRef = useRef(null);
  const contentRef = useRef(null);
  const searchInputRef = useRef(null);

  // Open Word document
  const handleOpenFile = async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await window.electronAPI.openFile();

      if (result.canceled) {
        setLoading(false);
        return;
      }

      if (result.error) {
        setError(`Failed to open: ${result.error}`);
        setLoading(false);
        return;
      }

      setMode('viewer');
      setDocument({ name: result.fileName, path: result.filePath });
      setHtmlContent(result.html);
      setSearchQuery('');
      setSearchResults([]);
      setLoading(false);
    } catch (err) {
      setError(`Error: ${err.message}`);
      setLoading(false);
    }
  };

  // Switch to editor mode
  const startNewDocument = () => {
    setMode('editor');
    setDocument(null);
    setHtmlContent('');
    setSearchQuery('');
    setSearchResults([]);
  };

  const handleEditorChange = useCallback((value) => {
    if (!value) {
      setWordCount(0);
      setCharCount(0);
      return;
    }
    setCharCount(value.length);
    const words = value.trim().split(/\s+/).filter(w => w.length > 0).length;
    setWordCount(words);
  }, []);

  const handleEditorDidMount = (editor, monaco) => {
    editorRef.current = editor;
    editor.focus();

    monaco.editor.defineTheme('powerDark', {
      base: 'vs-dark',
      inherit: true,
      rules: [],
      colors: {
        'editor.background': '#12141c',
        'editor.lineHighlightBackground': '#1a1e2b',
        'editorCursor.foreground': '#3b82f6',
        'editor.selectionBackground': '#3b82f640',
      }
    });
    monaco.editor.setTheme('powerDark');
  };

  const triggerSearch = () => {
    if (editorRef.current) {
      editorRef.current.getAction('actions.find').run();
    }
  };

  const exportToDocx = async () => {
    if (!editorRef.current) return;

    setIsSaving(true);
    try {
      const content = editorRef.current.getValue();
      const lines = content.split(/\r?\n/);

      const paragraphs = lines.map(line => new Paragraph({
        children: [new TextRun({ text: line })],
        spacing: { after: 200 }
      }));

      const doc = new Document({
        sections: [{ properties: {}, children: paragraphs }],
      });

      const blob = await Packer.toBlob(doc);
      saveAs(blob, `Document_${new Date().toISOString().slice(0, 10)}.docx`);
    } catch (err) {
      console.error("Failed to export:", err);
      alert("Failed to export. Check console.");
    } finally {
      setIsSaving(false);
    }
  };

  // Viewer search
  const performSearch = useCallback(() => {
    if (!htmlContent || !searchQuery.trim()) {
      setSearchResults([]);
      return;
    }

    const results = [];
    const plainText = htmlContent.replace(/<[^>]*>/g, '');
    const query = searchQuery.toLowerCase();
    let index = 0;

    while (index <= plainText.length - query.length) {
      const foundIndex = plainText.toLowerCase().indexOf(query, index);
      if (foundIndex === -1) break;

      const start = Math.max(0, foundIndex - 30);
      const end = Math.min(plainText.length, foundIndex + query.length + 30);
      const context = (start > 0 ? '...' : '') + plainText.substring(start, end) + (end < plainText.length ? '...' : '');

      results.push({ index: foundIndex, context, query });
      index = foundIndex + 1;
    }

    setSearchResults(results);
    setCurrentResultIndex(results.length > 0 ? 0 : -1);
  }, [htmlContent, searchQuery]);

  useEffect(() => {
    const timer = setTimeout(performSearch, 200);
    return () => clearTimeout(timer);
  }, [performSearch]);

  const scrollToResult = (index) => {
    if (!contentRef.current) return;
    const highlights = contentRef.current.querySelectorAll('.search-highlight');
    if (highlights[index]) {
      highlights[index].scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  const goToNextResult = () => {
    if (searchResults.length === 0) return;
    const nextIndex = (currentResultIndex + 1) % searchResults.length;
    setCurrentResultIndex(nextIndex);
    scrollToResult(nextIndex);
  };

  const goToPrevResult = () => {
    if (searchResults.length === 0) return;
    const prevIndex = currentResultIndex === 0 ? searchResults.length - 1 : currentResultIndex - 1;
    setCurrentResultIndex(prevIndex);
    scrollToResult(prevIndex);
  };

  const clearSearch = () => {
    setSearchQuery('');
    setSearchResults([]);
    searchInputRef.current?.focus();
  };

  const getHighlightedHtml = () => {
    if (!searchQuery.trim()) return htmlContent;
    const escapedQuery = searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`(${escapedQuery})`, 'gi');
    return htmlContent.replace(regex, '<mark class="search-highlight">$1</mark>');
  };

  const increaseZoom = () => setZoom(prev => Math.min(prev + 10, 200));
  const decreaseZoom = () => setZoom(prev => Math.max(prev - 10, 50));

  return (
    <div className="app-container">
      {/* Header */}
      <nav className="top-navbar">
        <div className="logo-section">
          <FileText className="logo-icon" size={24} />
          <span>SUPERWORD</span>
          <div className="status-item" style={{ marginLeft: '12px', color: '#10b981', fontSize: '0.75rem', fontWeight: 600 }}>
            <CloudOff size={14} /> INFINITE MODE
          </div>
        </div>

        <div className="controls-section">
          <div className="mode-switch">
            <button
              className={`btn ${mode === 'editor' ? 'btn-primary' : ''}`}
              onClick={startNewDocument}
            >
              <Edit3 size={16} />
              <span>Create</span>
            </button>
            <button
              className={`btn ${mode === 'viewer' ? 'btn-primary' : ''}`}
              onClick={handleOpenFile}
            >
              <Eye size={16} />
              <span>Open Word</span>
            </button>
          </div>

          {mode === 'editor' && (
            <>
              <button className="btn" onClick={triggerSearch} title="Search (Ctrl+F)">
                <Search size={18} />
                <span>Find</span>
              </button>
              <button
                className="btn btn-primary"
                onClick={exportToDocx}
                disabled={isSaving}
              >
                {isSaving ? <Activity size={18} className="animate-spin" /> : <Download size={18} />}
                <span>{isSaving ? 'Saving...' : 'Export .docx'}</span>
              </button>
            </>
          )}
        </div>

        <div className="controls-section">
          {document && (
            <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{document.name}</span>
          )}
        </div>
      </nav>

      {/* Viewer Toolbar */}
      {mode === 'viewer' && document && (
        <div className="viewer-toolbar">
          <div className="search-container">
            <Search size={16} className="search-icon" />
            <input
              ref={searchInputRef}
              type="text"
              placeholder="Search in document..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
              style={{ background: 'transparent', border: 'none', color: '#f1f5f9', outline: 'none', width: '200px' }}
            />
            {searchQuery && (
              <button className="clear-btn" onClick={clearSearch}>
                <X size={14} />
              </button>
            )}
            {searchResults.length > 0 && (
              <>
                <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>
                  {currentResultIndex + 1} / {searchResults.length}
                </span>
                <button className="nav-btn" onClick={goToPrevResult}><ChevronLeft size={16} /></button>
                <button className="nav-btn" onClick={goToNextResult}><ChevronRight size={16} /></button>
              </>
            )}
          </div>
          <div className="controls-group">
            <button className="control-btn" onClick={decreaseZoom}><ZoomOut size={14} /></button>
            <span className="control-value">{zoom}%</span>
            <button className="control-btn" onClick={increaseZoom}><ZoomIn size={14} /></button>
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className="main-content">
        {mode === 'editor' && (
          <div className="editor-wrapper">
            <Editor
              defaultLanguage="plaintext"
              defaultValue=""
              theme="vs-dark"
              onChange={handleEditorChange}
              onMount={handleEditorDidMount}
              options={{
                wordWrap: "on",
                minimap: { enabled: true },
                fontSize: 16,
                lineNumbers: "on",
                renderLineHighlight: "all",
                scrollBeyondLastLine: true,
                automaticLayout: true,
                padding: { top: 20, bottom: 20 },
                unicodeHighlight: { ambiguousCharacters: false },
                folding: true,
                contextmenu: true
              }}
            />
          </div>
        )}

        {mode === 'viewer' && !loading && !error && document && (
          <div ref={contentRef} className="document-viewer" style={{ transform: `scale(${zoom / 100})`, transformOrigin: 'top center' }}>
            <div className="document-content" dangerouslySetInnerHTML={{ __html: getHighlightedHtml() }} />
          </div>
        )}

        {loading && (
          <div className="loading-screen">
            <div className="spinner"></div>
            <p>Loading document...</p>
          </div>
        )}

        {error && (
          <div className="error-screen">
            <AlertCircle size={48} />
            <h3>Error</h3>
            <p>{error}</p>
            <button className="retry-btn" onClick={handleOpenFile}>Try Again</button>
          </div>
        )}
      </main>

      {/* Status Bar */}
      <footer className="status-bar">
        <div className="status-item">
          <span>Status:</span>
          <span style={{ color: '#10b981', fontWeight: 600 }}>● {mode === 'editor' ? 'Editing Mode - Infinite Words' : 'Viewing Mode'}</span>
        </div>
        <div className="status-item" style={{ gap: '16px' }}>
          <span>Characters: <strong className="badge">{charCount.toLocaleString()}</strong></span>
          <span>Words: <strong className="badge">{wordCount.toLocaleString()}</strong></span>
          {mode === 'editor' && <span>Virtualization: <strong style={{ color: 'var(--accent)' }}>ACTIVE</strong></span>}
          {searchResults.length > 0 && <span>Found: <strong className="badge">{searchResults.length}</strong></span>}
        </div>
      </footer>
    </div>
  );
}

export default App;
