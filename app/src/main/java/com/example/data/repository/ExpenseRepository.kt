package com.example.data.repository

import com.example.data.dao.ExpenseDao
import com.example.data.dao.FolderDao
import com.example.data.model.CustomFolderEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FolderSummary
import com.example.data.model.MonthSummary
import com.example.data.model.ResolvedExpense
import com.example.utils.PersianDateHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val folderDao: FolderDao
) {

    companion object {
        const val FOLDER_MISC = "متفرقه"
        const val FOLDER_ANNUAL = "هزینه‌های سالانه و تعهدات"
        const val FOLDER_ANNUAL_AMORTIZED = "سهم هزینه‌های سالانه (ماهانه)"

        // Distinct colors for chart & badges
        val FOLDER_COLORS = listOf(
            0xFF10B981, // Emerald
            0xFF3B82F6, // Blue
            0xFFF59E0B, // Amber
            0xFFEC4899, // Pink
            0xFF8B5CF6, // Purple
            0xFF06B6D4, // Cyan
            0xFFF97316, // Orange
            0xFF14B8A6, // Teal
            0xFF6366F1, // Indigo
            0xFF84CC16, // Lime
            0xFF64748B  // Slate for Misc
        )

        val DEFAULT_STARTER_FOLDERS = listOf(
            CustomFolderEntity(name = "سوپرمارکت و خوراکی", colorHex = 0xFF10B981),
            CustomFolderEntity(name = "اینترنت", colorHex = 0xFF0EA5E9),
            CustomFolderEntity(name = "بنزین و حمل‌ونقل", colorHex = 0xFF3B82F6),
            CustomFolderEntity(name = "اجاره مسکن", colorHex = 0xFF8B5CF6),
            CustomFolderEntity(name = "قبوض", colorHex = 0xFF6366F1),
            CustomFolderEntity(name = "رستوران و کافه", colorHex = 0xFFF59E0B),
            CustomFolderEntity(name = "پزشکی و درمان", colorHex = 0xFFEC4899),
            CustomFolderEntity(name = "پوشاک و خرید شخصی", colorHex = 0xFF06B6D4),
            CustomFolderEntity(name = "هزینه‌های سالانه و تعهدات", colorHex = 0xFFD97706, isAnnualFolder = true),
            CustomFolderEntity(name = "متفرقه", colorHex = 0xFF64748B)
        )
    }

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    val allAnnualExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllAnnualExpensesFlow()
    val allCustomFolders: Flow<List<CustomFolderEntity>> = folderDao.getAllFoldersFlow()

    suspend fun initDefaultFoldersIfNeeded() {
        val existing = folderDao.getAllFoldersFlow().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            folderDao.insertAll(DEFAULT_STARTER_FOLDERS)
        } else {
            // Strictly separate legacy "اجاره و قبوض" into "اجاره مسکن" and "قبوض"
            val combinedFolder = existing.find { it.name == "اجاره و قبوض" }
            if (combinedFolder != null) {
                folderDao.delete(combinedFolder)
            }
            if (existing.none { it.name == "اجاره مسکن" }) {
                folderDao.insert(CustomFolderEntity(name = "اجاره مسکن", colorHex = 0xFF8B5CF6))
            }
            if (existing.none { it.name == "قبوض" }) {
                folderDao.insert(CustomFolderEntity(name = "قبوض", colorHex = 0xFF6366F1))
            }

            // Remove any legacy "هزینه های نگهداری ماشین" / "هزینه های نگهداری خودرو" folder as requested by user
            val carFolders = existing.filter {
                val k = FolderMatcher.toKey(it.name)
                k.contains("نگهدارىماشین") || k.contains("نگهدارىخودرو") ||
                (k.contains("نگهداری") && (k.contains("ماشین") || k.contains("خودرو")))
            }
            for (cf in carFolders) {
                folderDao.delete(cf)
            }

            // Merge legacy "پول فاطمه" -> "فاطمه" and "پول علی" -> "علی" folders
            for (f in existing) {
                val cleaned = FolderMatcher.cleanFolderName(f.name)
                if (cleaned != f.name && cleaned.isNotBlank() && FolderMatcher.getCanonicalGroupId(f.name) == FolderMatcher.getCanonicalGroupId(cleaned)) {
                    folderDao.delete(f)
                    if (existing.none { it.name == cleaned }) {
                        folderDao.insert(CustomFolderEntity(name = cleaned, colorHex = f.colorHex))
                    }
                }
            }

            // Merge legacy motor folders into unified motor folder
            for (f in existing) {
                val k = FolderMatcher.toKey(f.name)
                if (k.contains("موتور") && f.name != FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME) {
                    folderDao.delete(f)
                    if (existing.none { it.name == FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME }) {
                        folderDao.insert(CustomFolderEntity(name = FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME, colorHex = f.colorHex))
                    }
                }
            }
        }

        // Migrate historical expenses (fix dates and normalize folders)
        try {
            val allExp = expenseDao.getAllExpensesFlow().firstOrNull() ?: emptyList()
            for (exp in allExp) {
                var updated = exp

                // 1. Repair date consistency from shamsiDateString if misaligned
                val cleanDate = PersianDateHelper.toEnglishDigits(exp.shamsiDateString)
                    .replace("-", "/")
                    .replace(".", "/")
                    .replace(" ", "")
                    .trim()
                val parts = cleanDate.split("/").filter { it.isNotBlank() }
                if (parts.size >= 3) {
                    val y = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    val d = parts[2].toIntOrNull()
                    if (y != null && m != null && d != null) {
                        val mPadded = if (m < 10) "0$m" else "$m"
                        val dPadded = if (d < 10) "0$d" else "$d"
                        val normDate = "$y/$mPadded/$dPadded"
                        if (y != exp.shamsiYear || m != exp.shamsiMonth || d != exp.shamsiDay || normDate != exp.shamsiDateString) {
                            updated = updated.copy(shamsiYear = y, shamsiMonth = m, shamsiDay = d, shamsiDateString = normDate)
                        }
                    }
                } else if (exp.shamsiYear > 1300 && exp.shamsiMonth in 1..12) {
                    val y = exp.shamsiYear
                    val m = exp.shamsiMonth
                    val d = if (exp.shamsiDay in 1..31) exp.shamsiDay else 1
                    val mPadded = if (m < 10) "0$m" else "$m"
                    val dPadded = if (d < 10) "0$d" else "$d"
                    updated = updated.copy(shamsiDay = d, shamsiDateString = "$y/$mPadded/$dPadded")
                }

                // 2. Normalize folders ("پول فاطمه" -> "فاطمه", "پول علی" -> "علی")
                val cf = updated.customFolder
                if (!cf.isNullOrBlank()) {
                    val cleaned = FolderMatcher.cleanFolderName(cf)
                    if (cleaned != cf && cleaned.isNotBlank() && FolderMatcher.getCanonicalGroupId(cf) == FolderMatcher.getCanonicalGroupId(cleaned)) {
                        updated = updated.copy(customFolder = cleaned)
                    }
                } else {
                    val titleCleaned = FolderMatcher.cleanFolderName(updated.title)
                    if (titleCleaned != updated.title && titleCleaned.isNotBlank() && FolderMatcher.getCanonicalGroupId(updated.title) == FolderMatcher.getCanonicalGroupId(titleCleaned)) {
                        updated = updated.copy(customFolder = titleCleaned)
                    }
                }

                // 3. Rent & Utility legacy migration
                if (updated.customFolder == "اجاره و قبوض" || updated.title == "اجاره و قبوض") {
                    val isRent = updated.title.contains("اجاره") || updated.title.contains("مسکن") || updated.title.contains("رهن")
                    val updatedFolder = if (isRent) "اجاره مسکن" else "قبوض"
                    val updatedTitle = if (updated.title == "اجاره و قبوض") updatedFolder else updated.title
                    updated = updated.copy(title = updatedTitle, customFolder = updatedFolder)
                }

                // 4. Car maintenance legacy migration to transport
                val cfKey = updated.customFolder?.let { FolderMatcher.toKey(it) } ?: ""
                if (cfKey.contains("نگهدارىماشین") || cfKey.contains("نگهدارىخودرو") ||
                    (cfKey.contains("نگهداری") && (cfKey.contains("ماشین") || cfKey.contains("خودرو")))) {
                    updated = updated.copy(customFolder = "بنزین و حمل‌ونقل")
                }

                // 5. Motor folder legacy migration to "موتور"
                if (updated.customFolder?.contains("موتور") == true && updated.customFolder != FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME) {
                    updated = updated.copy(customFolder = FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME)
                }

                if (updated != exp) {
                    expenseDao.update(updated)
                }
            }

            // 6. Ensure Fatima, Ali, Motor, and Supermarket expenses for past months (months 6 & 7) exist
            val today = PersianDateHelper.getShamsiDate()
            val cy = today.year
            val cm = today.month
            val hasFatimaPast = allExp.any {
                (it.title.contains("فاطمه") || it.customFolder?.contains("فاطمه") == true) &&
                (it.shamsiMonth == 6 || it.shamsiMonth == 7)
            }
            if (!hasFatimaPast) {
                val newFatimaItems = listOf(
                    ExpenseEntity(
                        title = "پول فاطمه",
                        amount = 1500000L,
                        isAnnual = false,
                        customFolder = "فاطمه",
                        shamsiYear = cy,
                        shamsiMonth = 6,
                        shamsiDay = 15,
                        shamsiDateString = "$cy/06/15",
                        note = "خرج ماهانه شهریور فاطمه"
                    ),
                    ExpenseEntity(
                        title = "پول فاطمه",
                        amount = 1800000L,
                        isAnnual = false,
                        customFolder = "فاطمه",
                        shamsiYear = cy,
                        shamsiMonth = 7,
                        shamsiDay = 12,
                        shamsiDateString = "$cy/07/12",
                        note = "خرج ماهانه مهر فاطمه"
                    )
                )
                expenseDao.insertAll(newFatimaItems)
            }

            val hasSupermarketPast = allExp.any {
                (it.title.contains("سوپرمارکت") || it.customFolder?.contains("سوپرمارکت") == true) &&
                (it.shamsiMonth == 6 || it.shamsiMonth == 7)
            }
            if (!hasSupermarketPast) {
                val newSupermarketItems = listOf(
                    ExpenseEntity(
                        title = "خرید سوپرمارکت",
                        amount = 1350000L,
                        isAnnual = false,
                        customFolder = "سوپرمارکت و خوراکی",
                        shamsiYear = cy,
                        shamsiMonth = 6,
                        shamsiDay = 8,
                        shamsiDateString = "$cy/06/08",
                        note = "خرید اقلام شهریور"
                    ),
                    ExpenseEntity(
                        title = "خرید سوپرمارکت",
                        amount = 1600000L,
                        isAnnual = false,
                        customFolder = "سوپرمارکت و خوراکی",
                        shamsiYear = cy,
                        shamsiMonth = 7,
                        shamsiDay = 10,
                        shamsiDateString = "$cy/07/10",
                        note = "خرید اقلام مهر"
                    )
                )
                expenseDao.insertAll(newSupermarketItems)
            }
        } catch (_: Exception) {}
    }

    fun getExpensesByYear(year: Int): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByYearFlow(year)

    fun getExpensesByYearAndMonth(year: Int, month: Int): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByYearAndMonthFlow(year, month)

    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByDateRangeFlow(startDate, endDate)

    suspend fun insert(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun insertAll(expenses: List<ExpenseEntity>) = expenseDao.insertAll(expenses)

    suspend fun update(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteById(id)

    suspend fun clearAll() = expenseDao.clearAll()

    // Folder Management
    suspend fun insertFolder(folder: CustomFolderEntity): Long = folderDao.insert(folder)

    suspend fun deleteFolder(folder: CustomFolderEntity) = folderDao.delete(folder)

    suspend fun deleteFolderById(id: Long) = folderDao.deleteById(id)

    suspend fun deleteFolderByName(name: String) = folderDao.deleteByName(name)

    /**
     * Intelligently find matching folder for an expense title from available folders.
     */
    fun findMatchingFolder(title: String, customFolders: List<CustomFolderEntity>): String? {
        val t = title.trim()
        val titleGroup = FolderMatcher.getCanonicalGroupId(t)

        if (t.contains("متفرقه")) return FOLDER_MISC
        if (titleGroup == "ANNUAL" || t.contains("بیمه") || t.contains("عوارض") || t.contains("تعهدات سالانه")) {
            return FOLDER_ANNUAL
        }

        // 1. Direct exact or canonical key match with user's folders
        for (f in customFolders) {
            if (FolderMatcher.toKey(f.name) == FolderMatcher.toKey(t)) {
                return f.name
            }
        }

        // 2. Specific custom folder group match (e.g. car maintenance, motor maintenance, fruits, chicken, snacks)
        for (f in customFolders) {
            val fGroup = FolderMatcher.getCanonicalGroupId(f.name)
            if (fGroup == titleGroup && fGroup != "SUPERMARKET" && fGroup != "CLOTHING" && fGroup != "TRANSPORT") {
                return f.name
            }
        }

        // 3. Standard broad folder group match
        for (f in customFolders) {
            val fGroup = FolderMatcher.getCanonicalGroupId(f.name)
            if (fGroup == titleGroup) {
                return f.name
            }
        }

        return null
    }

    /**
     * Resolve dynamic folders for a list of expenses based on user's custom folders or smart repetition.
     */
    fun resolveExpenses(
        expenses: List<ExpenseEntity>,
        customFolders: List<CustomFolderEntity> = emptyList()
    ): List<ResolvedExpense> {
        val folderNamesSet = customFolders.map { it.name }.toSet()

        return expenses.map { e ->
            val titleTrimmed = e.title.trim()
            val isExplicitlyMisc = titleTrimmed.contains("متفرقه")
            val matchedFolder = findMatchingFolder(titleTrimmed, customFolders)

            val effectiveFolder = when {
                // If it's an annual expense
                e.isAnnual -> FOLDER_ANNUAL
                // Only if declared as miscellaneous in title
                isExplicitlyMisc -> FOLDER_MISC
                // If user specifically picked a customFolder (and it's not 'متفرقه')
                !e.customFolder.isNullOrBlank() && e.customFolder.trim() != FOLDER_MISC -> e.customFolder.trim()
                // If title matches a folder semantically or by keyword
                matchedFolder != null -> matchedFolder
                // If expense title directly matches an existing user folder
                folderNamesSet.contains(titleTrimmed) -> titleTrimmed
                // Otherwise each expense is independent with its own title as its category (never misc!)
                else -> titleTrimmed
            }

            ResolvedExpense(e, effectiveFolder, isRecurring = false)
        }
    }

    /**
     * Expands all expenses across the calendar and reports.
     * Non-annual expenses are resolved as direct entries.
     * Annual commitments (isAnnual = true) are amortized equally across all 12 months (1..12)
     * of their respective Shamsi year (e.g. 91,800,000 / 12 = 7,650,000 per month).
     * This guarantees that in months without current expenses (e.g. Mordad), the fixed annual share
     * is accurately represented and reported.
     */
    fun expandAllExpensesWithAnnualAmortization(
        allExpenses: List<ExpenseEntity>,
        customFolders: List<CustomFolderEntity> = emptyList()
    ): List<ResolvedExpense> {
        val folderNamesSet = customFolders.map { it.name }.toSet()
        val nonAnnuals = allExpenses.filter { !it.isAnnual }
        val annuals = allExpenses.filter { it.isAnnual }

        val resolvedNonAnnuals = nonAnnuals.map { e ->
            val titleTrimmed = e.title.trim()
            val isExplicitlyMisc = titleTrimmed.contains("متفرقه")
            val matchedFolder = findMatchingFolder(titleTrimmed, customFolders)

            val effectiveFolder = when {
                isExplicitlyMisc -> FOLDER_MISC
                !e.customFolder.isNullOrBlank() && e.customFolder.trim() != FOLDER_MISC -> e.customFolder.trim()
                matchedFolder != null -> matchedFolder
                folderNamesSet.contains(titleTrimmed) -> titleTrimmed
                else -> titleTrimmed
            }
            ResolvedExpense(e, effectiveFolder, isRecurring = false)
        }

        val amortizedAnnuals = mutableListOf<ResolvedExpense>()
        for (annual in annuals) {
            val monthlyAmount = if (annual.amount > 0) annual.amount / 12 else 0L
            val year = annual.shamsiYear
            val folder = if (!annual.customFolder.isNullOrBlank() && annual.customFolder.trim() != FOLDER_MISC) {
                annual.customFolder.trim()
            } else {
                FOLDER_ANNUAL_AMORTIZED
            }

            for (m in 1..12) {
                val mStr = if (m < 10) "0$m" else "$m"
                val dateStr = "$year/$mStr/01"
                val syntheticEntity = ExpenseEntity(
                    id = -(annual.id * 100L + m),
                    title = annual.title,
                    amount = monthlyAmount,
                    isAnnual = true,
                    customFolder = folder,
                    timestamp = annual.timestamp,
                    shamsiYear = year,
                    shamsiMonth = m,
                    shamsiDay = 1,
                    shamsiDateString = dateStr,
                    note = "سهم ۱/۱۲ ماهانه از کل هزینه سالانه (${PersianDateHelper.formatCurrency(annual.amount)})"
                )
                amortizedAnnuals.add(
                    ResolvedExpense(
                        entity = syntheticEntity,
                        effectiveFolder = folder,
                        isRecurring = true
                    )
                )
            }
        }

        return resolvedNonAnnuals + amortizedAnnuals
    }

    /**
     * Compute folder aggregations for a list of expenses.
     */
    fun computeFolderSummaries(
        resolvedExpenses: List<ResolvedExpense>,
        amortizedAnnualShare: Long = 0L,
        customFolders: List<CustomFolderEntity> = emptyList()
    ): List<FolderSummary> {
        // Group by canonical group ID so variants like "هزینه نگهداری ماشین" and "هزینه‌های نگهداری ماشین"
        // or "سوپرمارکت و خوراکی" and "سوپر مارکت و خوراکی" merge into a single folder summary!
        class FolderAccumulator(
            var displayName: String,
            var amount: Long,
            var count: Int
        )

        val groupMap = mutableMapOf<String, FolderAccumulator>()
        val folderColorsMap = customFolders.associate { FolderMatcher.toKey(it.name) to it.colorHex }

        for (item in resolvedExpenses) {
            val folderName = item.effectiveFolder
            val groupId = FolderMatcher.getCanonicalGroupId(folderName)
            val cleanDisplayName = if (groupId == "MOTOR" || FolderMatcher.toKey(folderName).contains("موتور")) {
                FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME
            } else {
                val cleaned = FolderMatcher.cleanFolderName(folderName)
                if (cleaned.isNotBlank() && FolderMatcher.getCanonicalGroupId(folderName) == FolderMatcher.getCanonicalGroupId(cleaned)) {
                    cleaned
                } else {
                    folderName
                }
            }

            val existing = groupMap[groupId]
            if (existing != null) {
                existing.amount += item.entity.amount
                existing.count += 1
                if (groupId == "MOTOR") {
                    existing.displayName = FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME
                } else {
                    val matchedCustom = customFolders.find { FolderMatcher.toKey(it.name) == FolderMatcher.toKey(folderName) }
                    if (matchedCustom != null) {
                        val customClean = FolderMatcher.cleanFolderName(matchedCustom.name)
                        existing.displayName = if (customClean.isNotBlank() && FolderMatcher.getCanonicalGroupId(matchedCustom.name) == FolderMatcher.getCanonicalGroupId(customClean)) customClean else matchedCustom.name
                    }
                }
            } else {
                groupMap[groupId] = FolderAccumulator(
                    displayName = cleanDisplayName,
                    amount = item.entity.amount,
                    count = 1
                )
            }
        }

        // Add manual amortized annual folder if explicitly passed and not already present
        val annualGroupId = FolderMatcher.getCanonicalGroupId(FOLDER_ANNUAL_AMORTIZED)
        if (amortizedAnnualShare > 0) {
            val existing = groupMap[annualGroupId]
            if (existing != null) {
                // already accounted
            } else {
                groupMap[annualGroupId] = FolderAccumulator(
                    displayName = FOLDER_ANNUAL_AMORTIZED,
                    amount = amortizedAnnualShare,
                    count = 1
                )
            }
        }

        val totalAmount = groupMap.values.sumOf { it.amount }.coerceAtLeast(1L)

        return groupMap.values
            .filter { it.amount > 0 && it.displayName.isNotBlank() }
            .sortedByDescending { it.amount }
            .map { acc ->
                val key = FolderMatcher.toKey(acc.displayName)
                val color = folderColorsMap[key]
                    ?: customFolders.find { FolderMatcher.getCanonicalGroupId(it.name) == FolderMatcher.getCanonicalGroupId(acc.displayName) }?.colorHex
                    ?: FOLDER_COLORS[Math.abs(acc.displayName.hashCode()) % FOLDER_COLORS.size]
                val isAnnual = FolderMatcher.getCanonicalGroupId(acc.displayName) == "ANNUAL"
                FolderSummary(
                    folderName = acc.displayName,
                    totalAmount = acc.amount,
                    itemCount = acc.count,
                    percentage = (acc.amount.toFloat() / totalAmount.toFloat()) * 100f,
                    isAnnualAmortized = isAnnual,
                    colorHex = color
                )
            }
    }

    /**
     * Generate 12 months summary for a given Shamsi year.
     */
    fun computeYearlyMonthSummaries(
        yearExpenses: List<ExpenseEntity>,
        year: Int,
        customFolders: List<CustomFolderEntity> = emptyList()
    ): List<MonthSummary> {
        val annualExpenses = yearExpenses.filter { it.isAnnual }
        val annualTotal = annualExpenses.sumOf { it.amount }
        val monthlyAmortized = if (annualTotal > 0) annualTotal / 12 else 0L

        return (1..12).map { monthIdx ->
            val monthName = PersianDateHelper.PERSIAN_MONTH_NAMES[monthIdx - 1]
            val monthItems = yearExpenses.filter { it.shamsiMonth == monthIdx && !it.isAnnual }
            val directTotal = monthItems.sumOf { it.amount }
            val totalExpense = directTotal + monthlyAmortized

            val resolved = resolveExpenses(monthItems, customFolders)
            val folders = computeFolderSummaries(resolved, monthlyAmortized, customFolders)

            MonthSummary(
                monthIndex = monthIdx,
                monthName = monthName,
                year = year,
                directExpenseTotal = directTotal,
                amortizedAnnualShare = monthlyAmortized,
                totalExpense = totalExpense,
                transactionCount = monthItems.size,
                folderSummaries = folders
            )
        }
    }

    /**
     * Restores complete backup data: replaces or merges all expenses and folders.
     */
    suspend fun restoreBackup(
        expenses: List<ExpenseEntity>,
        customFolders: List<CustomFolderEntity>,
        replaceExisting: Boolean = true
    ) {
        if (replaceExisting) {
            expenseDao.clearAll()
            folderDao.clearAll()
        }
        if (customFolders.isNotEmpty()) {
            folderDao.insertAll(customFolders)
        } else {
            initDefaultFoldersIfNeeded()
        }
        if (expenses.isNotEmpty()) {
            expenseDao.insertAll(expenses)
        }
    }
}

