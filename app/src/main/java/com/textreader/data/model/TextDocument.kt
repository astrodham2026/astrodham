package com.textreader.data.model

import java.io.File

data class TextDocument(
    val id: String,
    val title: String,
    val content: String,
    val fileName: String,
    val createdAt: Long,
    val modifiedAt: Long
) {
    companion object {
        fun fromFile(file: File, content: String): TextDocument {
            return TextDocument(
                id = file.nameWithoutExtension,
                title = file.nameWithoutExtension,
                content = content,
                fileName = file.name,
                createdAt = file.parentFile?.let {
                    File(it, ".${file.nameWithoutExtension}.meta").takeIf { m -> m.exists() }
                        ?.readText()?.split(",")?.getOrNull(0)?.toLongOrNull()
                } ?: file.lastModified(),
                modifiedAt = file.lastModified()
            )
        }
    }
}
