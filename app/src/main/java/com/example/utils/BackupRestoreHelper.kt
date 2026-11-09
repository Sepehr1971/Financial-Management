package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CustomFolderEntity
import com.example.data.model.ExpenseEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Handles complete, lossless JSON backup creation and restoration
 * for all expenses and custom folders.
 */
object BackupRestoreHelper {

    data class BackupData(
        val version: Int,
        val appName: String,
        val backupDateShamsi: String,
        val timestamp: Long,
        val customFolders: List<CustomFolderEntity>,
        val expenses: List<ExpenseEntity>
    )

    /**
     * Serializes all expenses and custom folders into a comprehensive JSON string.
     */
    fun createBackupJson(
        expenses: List<ExpenseEntity>,
        customFolders: List<CustomFolderEntity>
    ): String {
        val root = JSONObject()
        val now = System.currentTimeMillis()
        val shamsiDate = PersianDateHelper.getShamsiDate(now).format()

        root.put("version", 1)
        root.put("appName", "مدیریت مالی و هزینه‌ها")
        root.put("backupDateShamsi", shamsiDate)
        root.put("backupTimestamp", now)

        // Custom Folders array
        val foldersArray = JSONArray()
        for (folder in customFolders) {
            val fObj = JSONObject()
            fObj.put("id", folder.id)
            fObj.put("name", folder.name)
            fObj.put("colorHex", folder.colorHex)
            fObj.put("isAnnualFolder", folder.isAnnualFolder)
            fObj.put("createdAt", folder.createdAt)
            foldersArray.put(fObj)
        }
        root.put("customFolders", foldersArray)

        // Expenses array
        val expensesArray = JSONArray()
        for (exp in expenses) {
            val eObj = JSONObject()
            eObj.put("id", exp.id)
            eObj.put("title", exp.title)
            eObj.put("amount", exp.amount)
            eObj.put("isAnnual", exp.isAnnual)
            eObj.put("customFolder", exp.customFolder ?: "")
            eObj.put("timestamp", exp.timestamp)
            eObj.put("shamsiYear", exp.shamsiYear)
            eObj.put("shamsiMonth", exp.shamsiMonth)
            eObj.put("shamsiDay", exp.shamsiDay)
            eObj.put("shamsiDateString", exp.shamsiDateString)
            eObj.put("note", exp.note)
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    /**
     * Writes the backup JSON to a file in the app's cache directory and shares it
     * via Android system share chooser so the user can save it anywhere.
     */
    fun shareBackupFile(context: Context, jsonString: String): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val shamsiClean = PersianDateHelper.getShamsiDate(now).format().replace("/", "-")
            val timeClean = PersianDateHelper.getTimeFormatted(now).replace(":", "-")
            val fileName = "Backup_Hazineh_${shamsiClean}_${timeClean}.json"

            val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val backupFile = File(backupsDir, fileName)

            FileOutputStream(backupFile).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "پشتیبان کامل برنامه مدیریت هزینه")
                putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان کامل داده‌ها و هزینه‌ها - تاریخ $shamsiClean")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "ذخیره یا اشتراک‌گذاری فایل پشتیبان (بکاپ)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در ایجاد یا اشتراک‌گذاری فایل پشتیبان: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Parses and validates a JSON string from a backup file.
     */
    fun parseBackupJson(jsonString: String): BackupData? {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val appName = root.optString("appName", "مدیریت مالی و هزینه‌ها")
            val backupDateShamsi = root.optString("backupDateShamsi", "")
            val timestamp = root.optLong("backupTimestamp", System.currentTimeMillis())

            val customFoldersList = mutableListOf<CustomFolderEntity>()
            val foldersArray = root.optJSONArray("customFolders")
            if (foldersArray != null) {
                for (i in 0 until foldersArray.length()) {
                    val fObj = foldersArray.optJSONObject(i) ?: continue
                    val name = fObj.optString("name", "").trim()
                    if (name.isNotBlank()) {
                        customFoldersList.add(
                            CustomFolderEntity(
                                id = 0L, // Reset ID so Room auto-generates or re-indexes
                                name = name,
                                colorHex = fObj.optLong("colorHex", 0xFF10B981),
                                isAnnualFolder = fObj.optBoolean("isAnnualFolder", false),
                                createdAt = fObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            val expensesList = mutableListOf<ExpenseEntity>()
            val expensesArray = root.optJSONArray("expenses")
            if (expensesArray != null) {
                for (i in 0 until expensesArray.length()) {
                    val eObj = expensesArray.optJSONObject(i) ?: continue
                    val title = eObj.optString("title", "").trim()
                    val amount = eObj.optLong("amount", 0L)
                    val dateStr = eObj.optString("shamsiDateString", "")
                    val isAnnual = eObj.optBoolean("isAnnual", false)
                    val customFolderRaw = eObj.optString("customFolder", "").trim()
                    val customFolder = if (customFolderRaw.isNotBlank()) customFolderRaw else null
                    val note = eObj.optString("note", "")
                    val ts = eObj.optLong("timestamp", System.currentTimeMillis())

                    var y = eObj.optInt("shamsiYear", 0)
                    var m = eObj.optInt("shamsiMonth", 0)
                    var d = eObj.optInt("shamsiDay", 0)

                    if ((y == 0 || m == 0 || d == 0) && dateStr.isNotBlank()) {
                        val parts = PersianDateHelper.toEnglishDigits(dateStr).split("/")
                        y = parts.getOrNull(0)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().year
                        m = parts.getOrNull(1)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().month
                        d = parts.getOrNull(2)?.toIntOrNull() ?: 1
                    }

                    if (title.isNotBlank()) {
                        expensesList.add(
                            ExpenseEntity(
                                id = 0L, // Reset ID for clean Room autogeneration
                                title = title,
                                amount = amount,
                                isAnnual = isAnnual,
                                customFolder = customFolder,
                                timestamp = ts,
                                shamsiYear = y,
                                shamsiMonth = m,
                                shamsiDay = d,
                                shamsiDateString = dateStr,
                                note = note
                            )
                        )
                    }
                }
            }

            BackupData(
                version = version,
                appName = appName,
                backupDateShamsi = backupDateShamsi,
                timestamp = timestamp,
                customFolders = customFoldersList,
                expenses = expensesList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
