package com.visionscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.visionscan.model.ScanDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanRepository(context: Context) {

    private val resolver = context.contentResolver
    private val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    suspend fun documents(): List<ScanDocument> = withContext(Dispatchers.IO) {
        directory.listFiles { file -> file.isFile && file.extension == EXTENSION }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { it.toDocument() }
    }

    suspend fun import(source: Uri): ScanDocument = withContext(Dispatchers.IO) {
        val target = uniqueFile(defaultName())
        val input = requireNotNull(resolver.openInputStream(source))
        input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
        target.toDocument()
    }

    suspend fun rename(document: ScanDocument, newName: String): ScanDocument? = withContext(Dispatchers.IO) {
        val sanitized = sanitize(newName)
        if (sanitized.isEmpty()) return@withContext null
        if (sanitized == document.name) return@withContext document
        val target = File(directory, "$sanitized.$EXTENSION")
        if (target.exists() || !document.file.renameTo(target)) return@withContext null
        target.toDocument()
    }

    suspend fun delete(document: ScanDocument): Boolean = withContext(Dispatchers.IO) {
        document.file.delete()
    }

    suspend fun thumbnail(document: ScanDocument, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            openRenderer(document.file) { renderer ->
                if (renderer.pageCount == 0) return@openRenderer null
                renderer.openPage(0).use { page ->
                    val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }.getOrNull()
    }

    private fun File.toDocument() = ScanDocument(
        file = this,
        name = nameWithoutExtension,
        pageCount = runCatching { openRenderer(this) { it.pageCount } }.getOrDefault(0),
        sizeBytes = length(),
        modifiedAt = lastModified()
    )

    private inline fun <T> openRenderer(file: File, block: (PdfRenderer) -> T): T =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use(block)
        }

    private fun uniqueFile(baseName: String): File {
        var candidate = File(directory, "$baseName.$EXTENSION")
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$baseName ($index).$EXTENSION")
            index++
        }
        return candidate
    }

    private fun defaultName(): String =
        "Scan " + SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US).format(Date())

    private fun sanitize(name: String): String =
        name.trim().replace(INVALID_CHARACTERS, "_").take(MAX_NAME_LENGTH)

    private companion object {
        const val DIRECTORY = "scans"
        const val EXTENSION = "pdf"
        const val MAX_NAME_LENGTH = 100
        val INVALID_CHARACTERS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    }
}
