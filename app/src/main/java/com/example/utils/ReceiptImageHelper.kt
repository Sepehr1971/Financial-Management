package com.example.utils

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseEntity
import java.io.File
import java.io.FileOutputStream

/**
 * Manages receipt photo files stored privately in app internal storage (filesDir/receipts).
 * Compatible with Android Photo Picker (zero storage permission) and Camera TakePicture via FileProvider.
 */
object ReceiptImageHelper {

    private fun getReceiptsDir(context: Context): File {
        return File(context.filesDir, "receipts").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Creates a new destination File and FileProvider content Uri for Camera capture.
     */
    fun createCameraCaptureTarget(context: Context): Pair<File, Uri> {
        val dir = getReceiptsDir(context)
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    /**
     * Copies a gallery-selected URI (from PickVisualMedia) into app-private storage so access
     * persists permanently without requiring READ_EXTERNAL_STORAGE / READ_MEDIA_IMAGES.
     */
    fun copyUriToInternalReceiptFile(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getReceiptsDir(context)
            val destFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            if (destFile.exists() && destFile.length() > 0L) {
                destFile.absolutePath
            } else {
                destFile.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteReceiptFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val f = File(path)
            if (f.exists()) f.delete()
        } catch (_: Exception) {
        }
    }

    fun exists(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            File(path).exists()
        } catch (_: Exception) {
            false
        }
    }

    fun buildExpenseShareText(expense: ExpenseEntity): String {
        val cleanTitle = expense.title.replace(Regex("[\\r\\n]+"), " ").trim()
        val cleanNote = expense.note.replace(Regex("[\\r\\n]+"), " ").trim()
        return buildString {
            appendLine("📌 هزینه: $cleanTitle")
            appendLine("💰 مبلغ: ${PersianDateHelper.formatCurrency(expense.amount)}")
            if (expense.shamsiDateString.isNotBlank()) {
                appendLine("📅 تاریخ: ${PersianDateHelper.toEnglishDigits(expense.shamsiDateString)}")
            }
            val folder = expense.customFolder?.trim().orEmpty()
            if (folder.isNotBlank()) {
                appendLine("📁 دسته‌بندی: $folder")
            }
            if (expense.isAnnual) {
                appendLine("🗓️ نوع: تعهد سالانه")
            }
            if (cleanNote.isNotBlank()) {
                appendLine("📝 توضیحات: $cleanNote")
            }
        }.trim()
    }

    /**
     * Shares an expense's full details text together with its attached receipt photo via Android Sharesheet.
     */
    fun shareExpenseWithReceipt(context: Context, expense: ExpenseEntity) {
        val shareText = buildExpenseShareText(expense)
        val imageFile = expense.receiptImagePath?.takeIf { it.isNotBlank() }?.let { File(it) }

        try {
            val sendIntent = if (imageFile != null && imageFile.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, expense.title)
                    clipData = ClipData.newRawUri(expense.title, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Toast.makeText(
                    context,
                    "تصویر رسید در حافظه یافت نشد؛ متن هزینه اشتراک‌گذاری می‌شود",
                    Toast.LENGTH_SHORT
                ).show()
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, expense.title)
                }
            }

            val chooser = Intent.createChooser(sendIntent, "اشتراک‌گذاری هزینه و رسید")
            if (context !is Activity) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "خطا در اشتراک‌گذاری هزینه", Toast.LENGTH_SHORT).show()
        }
    }
}
