package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.data.models.PdfFileMetaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PdfExtractor(private val context: Context) {

    suspend fun getMetaData(uri: Uri, isQuestionsPdf: Boolean): PdfFileMetaData = withContext(Dispatchers.IO) {
        var fileName = "document.pdf"
        var fileSize = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "document.pdf"
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val formattedSize = when {
            fileSize > 1024 * 1024 -> String.format("%.2f MB", fileSize / (1024.0 * 1024.0))
            fileSize > 1024 -> String.format("%.1f KB", fileSize / 1024.0)
            fileSize > 0 -> "$fileSize B"
            else -> "PDF Document"
        }

        PdfFileMetaData(
            uriString = uri.toString(),
            fileName = fileName,
            fileSizeFormatted = formattedSize,
            isQuestionsPdf = isQuestionsPdf
        )
    }

    suspend fun renderPdfPagesToBase64Images(uri: Uri, maxPages: Int = 12): List<String> = withContext(Dispatchers.IO) {
        val base64Images = mutableListOf<String>()
        var pfd: ParcelFileDescriptor? = null
        var tempFile: File? = null

        try {
            // Copy InputStream to a temp file to get a Seekable ParcelFileDescriptor
            tempFile = File.createTempFile("pdf_render_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(pfd)
            val totalPages = minOf(pdfRenderer.pageCount, maxPages)

            for (i in 0 until totalPages) {
                val page = pdfRenderer.openPage(i)
                // Render at good resolution (1080px width)
                val scale = 1080f / page.width.toFloat()
                val targetWidth = 1080
                val targetHeight = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                // Fill white background before rendering
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Compress to JPEG Base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                base64Images.add(base64Str)
                bitmap.recycle()
            }

            pdfRenderer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                pfd?.close()
                tempFile?.delete()
            } catch (ignored: Exception) {}
        }

        base64Images
    }

    suspend fun readPdfTextSimple(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext ""
            inputStream.close()
            // Try extracting plain printable ASCII/UTF-8 strings from stream
            val textBuilder = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                if (b in 32..126 || b == 10 || b == 13 || b == 9) {
                    textBuilder.append(b.toChar())
                } else if (textBuilder.isNotEmpty() && textBuilder.last() != ' ') {
                    textBuilder.append(' ')
                }
                i++
            }
            textBuilder.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
