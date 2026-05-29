# Text Reader - Specification Document

## 1. Project Overview

**Project Name:** Text Reader
**Type:** Native Android Application (Kotlin + Jetpack Compose)
**Core Functionality:** A local-first, offline text file manager that allows users to create, save, search, and read unlimited .txt files with full-text search capabilities.

---

## 2. Technology Stack & Choices

| Component | Choice |
|-----------|--------|
| **Language** | Kotlin 1.9.x |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Architecture** | MVVM + Clean Architecture |
| **State Management** | Kotlin StateFlow + ViewModel |
| **Dependency Injection** | Manual (no external DI library for simplicity) |
| **Data Storage** | Internal App Storage (text files on device) |
| **File Format** | Plain .txt files |
| **Async** | Kotlin Coroutines |

---

## 3. Feature List

### Core Features
1. **Create Text Files** - Add button opens editor with unlimited text input, save with filename
2. **List All Documents** - Scrollable list showing all saved .txt files with title and preview
3. **Full-Text Search** - Search bar filters files containing the search query in filename or content
4. **Read Documents** - Tap file to open and read full content in read-only view
5. **Edit Documents** - Modify existing files and save changes
6. **Delete Documents** - Remove unwanted files with confirmation
7. **Infinite Documents** - No artificial limits, supports thousands of files

### UI Features
1. **Home Screen** - File list with search bar at top, add button in top-left
2. **Add/Edit Screen** - Full-screen text editor with save action
3. **Reader Screen** - Clean reading view with edit action
4. **Search** - Real-time filtering as user types
5. **Empty State** - Friendly message when no files exist

---

## 4. UI/UX Design Direction

### Visual Style
- **Design System:** Material Design 3 (Material You)
- **Theme:** Clean, minimalist reading-focused interface
- **Typography:** System default fonts optimized for readability

### Color Scheme
- **Primary:** Deep Blue (#1976D2)
- **Background:** White/Light Gray
- **Surface:** White
- **On Surface:** Dark text for readability
- **Accent:** Teal for action buttons

### Layout Approach
- **Navigation:** Single-activity with Compose Navigation
- **Home:** Top search bar + FAB for adding files
- **File List:** LazyColumn with card-style items
- **Editor:** Full-screen text field with floating save button
- **Reader:** Scrollable text with top app bar actions

### Interactions
- Search bar: Real-time filtering (no submit button needed)
- FAB: Navigate to create new file
- List item tap: Open file for reading
- Long press: Show delete option
- Swipe: Alternative delete gesture
