package com.textreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.textreader.data.model.TextDocument
import com.textreader.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentListState(
    val documents: List<TextDocument> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class EditorState(
    val document: TextDocument? = null,
    val title: String = "",
    val content: String = "",
    val isNewDocument: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

data class ReaderState(
    val document: TextDocument? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)

    private val _listState = MutableStateFlow(DocumentListState())
    val listState: StateFlow<DocumentListState> = _listState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _readerState = MutableStateFlow(ReaderState())
    val readerState: StateFlow<ReaderState> = _readerState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            try {
                val documents = if (_listState.value.searchQuery.isBlank()) {
                    repository.getAllDocuments()
                } else {
                    repository.searchDocuments(_listState.value.searchQuery)
                }
                _listState.value = _listState.value.copy(
                    documents = documents,
                    isLoading = false
                )
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load documents"
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _listState.value = _listState.value.copy(searchQuery = query)
        viewModelScope.launch {
            try {
                val documents = repository.searchDocuments(query)
                _listState.value = _listState.value.copy(documents = documents)
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(error = e.message)
            }
        }
    }

    fun initNewDocument() {
        _editorState.value = EditorState(
            isNewDocument = true,
            title = "",
            content = ""
        )
    }

    fun initEditDocument(documentId: String) {
        viewModelScope.launch {
            _editorState.value = _editorState.value.copy(isLoading = true)
            try {
                val document = repository.getDocument(documentId)
                _editorState.value = EditorState(
                    document = document,
                    title = document?.title ?: "",
                    content = document?.content ?: "",
                    isNewDocument = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                _editorState.value = _editorState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _editorState.value = _editorState.value.copy(title = title)
    }

    fun onContentChange(content: String) {
        _editorState.value = _editorState.value.copy(content = content)
    }

    fun saveDocument() {
        viewModelScope.launch {
            val state = _editorState.value
            if (state.content.isBlank()) {
                _editorState.value = state.copy(error = "Content cannot be empty")
                return@launch
            }

            _editorState.value = state.copy(isSaving = true, error = null)
            try {
                val savedDoc = if (state.isNewDocument) {
                    repository.saveDocument(state.title.ifBlank { "Untitled" }, state.content)
                } else {
                    state.document?.let { doc ->
                        repository.updateDocument(doc.id, state.content)
                    } ?: repository.saveDocument(state.title.ifBlank { "Untitled" }, state.content)
                }
                _editorState.value = state.copy(
                    document = savedDoc,
                    isSaving = false,
                    saveSuccess = true
                )
                loadDocuments()
            } catch (e: Exception) {
                _editorState.value = state.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save document"
                )
            }
        }
    }

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _readerState.value = ReaderState(isLoading = true)
            try {
                val document = repository.getDocument(documentId)
                _readerState.value = ReaderState(
                    document = document,
                    isLoading = false
                )
            } catch (e: Exception) {
                _readerState.value = ReaderState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDocument(documentId)
                loadDocuments()
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(error = e.message)
            }
        }
    }

    fun resetEditorState() {
        _editorState.value = EditorState()
    }

    fun clearSaveSuccess() {
        _editorState.value = _editorState.value.copy(saveSuccess = false)
    }
}
