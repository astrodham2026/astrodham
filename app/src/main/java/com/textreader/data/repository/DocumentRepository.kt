package com.textreader.data.repository

import android.content.Context
import com.textreader.data.model.TextDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DocumentRepository(private val context: Context) {

    private val documentsDir: File
        get() = File(context.filesDir, "documents").also {
            if (!it.exists()) it.mkdirs()
        }

    suspend fun getAllDocuments(): List<TextDocument> = withContext(Dispatchers.IO) {
        documentsDir.listFiles { file ->
            file.isFile && file.extension == "txt"
        }?.map { file ->
            try {
                val content = file.readText()
                TextDocument.fromFile(file, content)
            } catch (e: Exception) {
                null
            }
        }?.filterNotNull()?.sortedByDescending { it.modifiedAt } ?: emptyList()
    }

    suspend fun getDocument(id: String): TextDocument? = withContext(Dispatchers.IO) {
        val file = File(documentsDir, "$id.txt")
        if (file.exists()) {
            try {
                val content = file.readText()
                TextDocument.fromFile(file, content)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    suspend fun saveDocument(title: String, content: String): TextDocument = withContext(Dispatchers.IO) {
        val id = if (title.isBlank()) UUID.randomUUID().toString() else title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val uniqueId = ensureUniqueId(id)
        val file = File(documentsDir, "$uniqueId.txt")

        val existingDoc = if (id != uniqueId && file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                ""
            }
        } else ""

        file.writeText(if (existingDoc.isNotEmpty() && !content.contains(existingDoc)) {
            if (existingDoc.isNotBlank()) content + "\n" + existingDoc else content
        } else content)

        saveMetadata(uniqueId, System.currentTimeMillis())

        TextDocument.fromFile(file, file.readText()).copy(title = uniqueId)
    }

    suspend fun updateDocument(id: String, newContent: String): TextDocument? = withContext(Dispatchers.IO) {
        val file = File(documentsDir, "$id.txt")
        if (file.exists()) {
            file.writeText(newContent)
            saveMetadata(id, System.currentTimeMillis())
            TextDocument.fromFile(file, newContent)
        } else null
    }

    suspend fun deleteDocument(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(documentsDir, "$id.txt")
        val metaFile = File(documentsDir, ".$id.meta")
        val deleted = file.delete()
        metaFile.delete()
        deleted
    }

    suspend fun searchDocuments(query: String): List<TextDocument> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            getAllDocuments()
        } else {
            val lowerQuery = query.lowercase()
            documentsDir.listFiles { file ->
                file.isFile && file.extension == "txt"
            }?.mapNotNull { file ->
                try {
                    val content = file.readText()
                    if (file.nameWithoutExtension.lowercase().contains(lowerQuery) ||
                        content.lowercase().contains(lowerQuery)) {
                        TextDocument.fromFile(file, content)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }?.sortedByDescending { it.modifiedAt } ?: emptyList()
        }
    }

    private fun ensureUniqueId(id: String): String {
        var uniqueId = id
        var counter = 1
        while (File(documentsDir, "$uniqueId.txt").exists()) {
            uniqueId = "${id}_$counter"
            counter++
        }
        return uniqueId
    }

    private fun saveMetadata(id: String, timestamp: Long) {
        val metaFile = File(documentsDir, ".$id.meta")
        metaFile.writeText("$timestamp")
    }
}
