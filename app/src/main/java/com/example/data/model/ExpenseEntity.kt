package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a single expense record.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Long, // Amount in Tomans
    val isAnnual: Boolean = false, // E.g., car insurance, motor insurance, supplementary health
    val customFolder: String? = null, // Optional manual override, otherwise resolved automatically
    val timestamp: Long = System.currentTimeMillis(),
    val shamsiYear: Int,
    val shamsiMonth: Int, // 1..12
    val shamsiDay: Int,   // 1..31
    val shamsiDateString: String, // e.g. "1405/07/17"
    val note: String = ""
)

/**
 * An expense item paired with its calculated effective folder.
 */
data class ResolvedExpense(
    val entity: ExpenseEntity,
    val effectiveFolder: String,
    val isRecurring: Boolean
)

/**
 * Aggregate summary for a folder in a given time period.
 */
data class FolderSummary(
    val folderName: String,
    val totalAmount: Long,
    val itemCount: Int,
    val percentage: Float = 0f,
    val isAnnualAmortized: Boolean = false,
    val colorHex: Long = 0xFF10B981
)

/**
 * Aggregate summary for a single Shamsi month.
 */
data class MonthSummary(
    val monthIndex: Int, // 1..12
    val monthName: String,
    val year: Int,
    val directExpenseTotal: Long,
    val amortizedAnnualShare: Long,
    val totalExpense: Long, // direct + amortized
    val transactionCount: Int,
    val folderSummaries: List<FolderSummary> = emptyList()
)
