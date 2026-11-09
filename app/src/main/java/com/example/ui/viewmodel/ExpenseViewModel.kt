package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CustomFolderEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FolderSummary
import com.example.data.model.MarketRatesState
import com.example.data.model.MonthSummary
import com.example.data.model.NewsArticle
import com.example.data.model.ResolvedExpense
import com.example.data.network.MarketRatesService
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FolderMatcher
import com.example.data.repository.NewsRepository
import com.example.utils.PersianDateHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveClockState(
    val shamsiDate: PersianDateHelper.ShamsiDate = PersianDateHelper.getShamsiDate(),
    val gregorianDateString: String = PersianDateHelper.getGregorianFormatted(),
    val timeString: String = PersianDateHelper.getTimeFormatted(),
    val gregorianNumeric: String = PersianDateHelper.getGregorianNumericDate(),
    val gregorianPersianMonthDay: String = PersianDateHelper.getGregorianPersianMonthDay()
)

data class ReportsFilterState(
    val startDate: String, // "1405/01/01"
    val endDate: String,   // "1405/07/17"
    val presetName: String = "این ماه"
)

data class MonthlyBreakdownItem(
    val year: Int,
    val month: Int,
    val monthName: String,
    val yearMonthLabel: String,
    val totalAmount: Long,
    val transactionCount: Int,
    val percentageOfTotal: Float,
    val transactions: List<ResolvedExpense>
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    // Live Clock State (updates every 1000ms)
    private val _clockState = MutableStateFlow(LiveClockState())
    val clockState: StateFlow<LiveClockState> = _clockState.asStateFlow()

    // Current selected Shamsi Year (defaults to current year e.g. 1405)
    private val _currentYear = MutableStateFlow(PersianDateHelper.getShamsiDate().year)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    // Selected Month index for detailed view in Calendar (1..12, defaults to current month)
    private val _selectedMonthIndex = MutableStateFlow(PersianDateHelper.getShamsiDate().month)
    val selectedMonthIndex: StateFlow<Int> = _selectedMonthIndex.asStateFlow()

    // Reports filter state (Initialized to cover entire current month)
    private val currentShamsi = PersianDateHelper.getShamsiDate()
    private val initY = currentShamsi.year
    private val initM = currentShamsi.month
    private val initMStr = if (initM < 10) "0$initM" else "$initM"
    private val initDays = PersianDateHelper.getDaysInMonth(initY, initM)
    private val initDStr = if (initDays < 10) "0$initDays" else "$initDays"
    private val defaultStart = "$initY/$initMStr/01"
    private val defaultEnd = "$initY/$initMStr/$initDStr"
    private val _reportsFilter = MutableStateFlow(ReportsFilterState(defaultStart, defaultEnd, "این ماه"))
    val reportsFilter: StateFlow<ReportsFilterState> = _reportsFilter.asStateFlow()

    // Selected folder filter for independent folder reporting (null = all folders)
    private val _selectedReportFolder = MutableStateFlow<String?>(null)
    val selectedReportFolder: StateFlow<String?> = _selectedReportFolder.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Market Rates Service (TGJU Dollar, Euro, Sekeh Emami)
    private val marketRatesService = MarketRatesService(application)
    private val _marketRates = MutableStateFlow(marketRatesService.getCachedOrDefault())
    val marketRates: StateFlow<MarketRatesState> = _marketRates.asStateFlow()

    // News Repository (40 Curated Articles from Tabnak, Zoomit, IranWire)
    private val newsRepository = NewsRepository()
    val newsList: StateFlow<List<NewsArticle>> = newsRepository.newsList
    val isNewsLoading: StateFlow<Boolean> = newsRepository.isLoading
    val newsLastUpdated: StateFlow<String> = newsRepository.lastUpdated

    fun refreshNews() {
        viewModelScope.launch {
            newsRepository.refreshNews()
        }
    }

    fun refreshMarketRates(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _marketRates.value = _marketRates.value.copy(isLoading = true)
            val updated = marketRatesService.fetchRates()
            _marketRates.value = updated
            onComplete?.invoke()
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao(), database.folderDao())

        // Start live clock ticker
        startClockTicker()

        // Fetch live market rates from TGJU
        refreshMarketRates()

        // Initialize default folders if needed
        viewModelScope.launch {
            repository.initDefaultFoldersIfNeeded()
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val shamsi = PersianDateHelper.getShamsiDate(now)
                val greg = PersianDateHelper.getGregorianFormatted(now)
                val time = PersianDateHelper.getTimeFormatted(now)
                val gregNum = PersianDateHelper.getGregorianNumericDate(now)
                val gregPersian = PersianDateHelper.getGregorianPersianMonthDay(now)
                _clockState.value = LiveClockState(shamsi, greg, time, gregNum, gregPersian)
                delay(1000L)
            }
        }
    }

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User's custom folders
    val customFolders: StateFlow<List<CustomFolderEntity>> = repository.allCustomFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Annual Expenses (insurance, commitments)
    val annualExpenses: StateFlow<List<ExpenseEntity>> = repository.allAnnualExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All expenses expanded across the 12 months with amortized annual commitments
    val allExpandedExpenses: StateFlow<List<ResolvedExpense>> = combine(allExpenses, customFolders) { expenses, folders ->
        repository.expandAllExpensesWithAnnualAmortization(expenses, folders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 12 Months Summary for interactive Annual Calendar
    val yearMonthSummaries: StateFlow<List<MonthSummary>> = combine(allExpenses, _currentYear, customFolders) { expenses, year, folders ->
        repository.computeYearlyMonthSummaries(expenses.filter { it.shamsiYear == year }, year, folders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current month summary and resolved items
    val currentMonthSummary: StateFlow<MonthSummary?> = combine(yearMonthSummaries, _clockState) { summaries, clock ->
        val month = clock.shamsiDate.month
        summaries.find { it.monthIndex == month }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current month resolved expenses (includes direct expenses + 1/12 monthly amortized share of annual commitments)
    // Sorted from latest expense to earliest expense based on expenditure date
    val currentMonthResolvedExpenses: StateFlow<List<ResolvedExpense>> = combine(allExpandedExpenses, _clockState) { expanded, clock ->
        val y = clock.shamsiDate.year
        val m = clock.shamsiDate.month
        expanded.filter { it.entity.shamsiYear == y && it.entity.shamsiMonth == m }
            .sortedWith(
                compareByDescending<ResolvedExpense> { extractExpenseDateNum(it.entity) }
                    .thenByDescending { it.entity.timestamp }
                    .thenByDescending { it.entity.id }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Detailed month summary (for selected month in calendar)
    val selectedMonthDetail: StateFlow<MonthSummary?> = combine(yearMonthSummaries, _selectedMonthIndex) { summaries, idx ->
        summaries.find { it.monthIndex == idx }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Resolved expenses for selected month in calendar (includes amortized annual commitments)
    // Sorted from latest expense to earliest expense based on expenditure date
    val selectedMonthResolvedExpenses: StateFlow<List<ResolvedExpense>> = combine(allExpandedExpenses, _currentYear, _selectedMonthIndex) { expanded, y, m ->
        expanded.filter { it.entity.shamsiYear == y && it.entity.shamsiMonth == m }
            .sortedWith(
                compareByDescending<ResolvedExpense> { extractExpenseDateNum(it.entity) }
                    .thenByDescending { it.entity.timestamp }
                    .thenByDescending { it.entity.id }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun parseDateToNumber(dateStr: String): Int {
        if (dateStr.isBlank()) return 0
        val clean = PersianDateHelper.toEnglishDigits(dateStr)
            .replace("-", "/")
            .replace(".", "/")
            .replace(" ", "")
            .replace("‌", "")
            .trim()
        val parts = clean.split("/").filter { it.isNotBlank() }
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return y * 10000 + m * 100 + d
    }

    fun extractExpenseDateNum(entity: ExpenseEntity): Int {
        // Priority 1: Entity's own year & month if valid
        if (entity.shamsiYear in 1300..1500 && entity.shamsiMonth in 1..12) {
            val d = if (entity.shamsiDay in 1..31) entity.shamsiDay else 1
            return entity.shamsiYear * 10000 + entity.shamsiMonth * 100 + d
        }
        // Priority 2: Parse from shamsiDateString
        val parsed = parseDateToNumber(entity.shamsiDateString)
        if (parsed > 13000000) return parsed
        // Priority 3: Fallback from timestamp
        if (entity.timestamp > 0) {
            val s = PersianDateHelper.getShamsiDate(entity.timestamp)
            return s.year * 10000 + s.month * 100 + s.day
        }
        return 0
    }

    private fun isFolderMatch(expense: ResolvedExpense, folderFilter: String): Boolean {
        return FolderMatcher.matches(
            expenseEffectiveFolder = expense.effectiveFolder,
            expenseCustomFolder = expense.entity.customFolder,
            expenseTitle = expense.entity.title,
            isAnnualExpense = expense.entity.isAnnual,
            folderFilter = folderFilter
        )
    }

    // Filtered expenses for Reports view (Includes direct expenses + 1/12 monthly share of annual commitments)
    val reportResolvedExpenses: StateFlow<List<ResolvedExpense>> = combine(allExpandedExpenses, _reportsFilter, _searchQuery) { expanded, filter, query ->
        val startNum = parseDateToNumber(filter.startDate)
        val endNum = parseDateToNumber(filter.endDate)

        val inRange = expanded.filter {
            val dateNum = extractExpenseDateNum(it.entity)
            if (startNum > 0 && endNum > 0) {
                dateNum in startNum..endNum
            } else if (startNum > 0) {
                dateNum >= startNum
            } else if (endNum > 0) {
                dateNum <= endNum
            } else {
                true
            }
        }
        if (query.isBlank()) {
            inRange
        } else {
            val q = query.trim().lowercase()
            inRange.filter {
                it.entity.title.lowercase().contains(q) ||
                it.effectiveFolder.lowercase().contains(q) ||
                it.entity.note.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Folder summaries for Reports view (feeds the dynamic Bar Chart!)
    val reportFolderSummaries: StateFlow<List<FolderSummary>> = combine(reportResolvedExpenses, customFolders) { resolved, folders ->
        repository.computeFolderSummaries(resolved, amortizedAnnualShare = 0L, customFolders = folders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total expenses in the report date range
    val reportTotalAmount: StateFlow<Long> = reportResolvedExpenses.map { list ->
        list.sumOf { it.entity.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Expenses in report range filtered by selected folder (if any)
    val folderReportExpenses: StateFlow<List<ResolvedExpense>> = combine(
        reportResolvedExpenses,
        _selectedReportFolder
    ) { expenses, folderFilter ->
        if (folderFilter == null) {
            expenses
        } else {
            expenses.filter { isFolderMatch(it, folderFilter) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total amount for selected folder (or all folders) in report date range
    val folderReportTotalAmount: StateFlow<Long> = folderReportExpenses.map { list ->
        list.sumOf { it.entity.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Month-by-month breakdown for the selected folder & date range
    val folderMonthlyBreakdown: StateFlow<List<MonthlyBreakdownItem>> = combine(
        folderReportExpenses,
        _reportsFilter,
        folderReportTotalAmount
    ) { expenses, filter, grandTotal ->
        val months = getMonthsBetween(filter.startDate, filter.endDate)
        if (months.isEmpty()) {
            emptyList()
        } else {
            val totalAmount = if (grandTotal > 0) grandTotal else 1L
            months.map { (y, m) ->
                val monthItems = expenses.filter {
                    val dateNum = extractExpenseDateNum(it.entity)
                    val ey = dateNum / 10000
                    val em = (dateNum % 10000) / 100
                    ey == y && em == m
                }
                val mSum = monthItems.sumOf { it.entity.amount }
                val mCount = monthItems.size
                val mName = PersianDateHelper.getMonthName(m)
                val yDigits = PersianDateHelper.toPersianDigits(y)
                MonthlyBreakdownItem(
                    year = y,
                    month = m,
                    monthName = mName,
                    yearMonthLabel = "$mName $yDigits",
                    totalAmount = mSum,
                    transactionCount = mCount,
                    percentageOfTotal = (mSum.toFloat() / totalAmount.toFloat() * 100f).coerceIn(0f, 100f),
                    transactions = monthItems
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Average monthly expense in the selected date range
    val reportAverageMonthlyAmount: StateFlow<Long> = combine(
        folderReportTotalAmount,
        folderMonthlyBreakdown
    ) { total, months ->
        val monthCount = months.size.coerceAtLeast(1)
        total / monthCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // All distinct folders and their all-time totals
    val allTimeFolderSummaries: StateFlow<List<FolderSummary>> = combine(allExpenses, customFolders) { list, folders ->
        val resolved = repository.resolveExpenses(list, folders)
        repository.computeFolderSummaries(resolved, customFolders = folders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Annual Amortized statistics
    val totalAnnualCommitments: StateFlow<Long> = combine(annualExpenses) { list ->
        list[0].sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyAmortizedShare: StateFlow<Long> = combine(totalAnnualCommitments) { total ->
        if (total[0] > 0) total[0] / 12 else 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Folder Operations
    fun createFolder(name: String, colorHex: Long = 0xFF10B981, isAnnual: Boolean = false) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val cleaned = FolderMatcher.cleanFolderName(name)
                val finalName = if (cleaned.isNotBlank() && FolderMatcher.getCanonicalGroupId(name) == FolderMatcher.getCanonicalGroupId(cleaned)) {
                    cleaned
                } else {
                    name.trim()
                }
                repository.insertFolder(
                    CustomFolderEntity(
                        name = finalName,
                        colorHex = colorHex,
                        isAnnualFolder = isAnnual
                    )
                )
            }
        }
    }

    fun deleteFolder(folder: CustomFolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    // User Operations
    fun addExpense(
        title: String,
        amount: Long,
        isAnnual: Boolean,
        customFolder: String? = null,
        shamsiDateString: String? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            val dateStr = shamsiDateString ?: PersianDateHelper.getShamsiDate().format()
            val cleanDate = PersianDateHelper.toEnglishDigits(dateStr)
                .replace("-", "/")
                .replace(".", "/")
                .replace(" ", "")
                .trim()
            val parts = cleanDate.split("/").filter { it.isNotBlank() }
            val y = parts.getOrNull(0)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().year
            val m = parts.getOrNull(1)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().month
            val d = parts.getOrNull(2)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().day
            val mPadded = if (m < 10) "0$m" else "$m"
            val dPadded = if (d < 10) "0$d" else "$d"
            val normalizedDateStr = "$y/$mPadded/$dPadded"

            val cleanedFolder = customFolder?.takeIf { it.isNotBlank() }?.let {
                val c = FolderMatcher.cleanFolderName(it)
                if (c.isNotEmpty() && FolderMatcher.getCanonicalGroupId(c) == FolderMatcher.getCanonicalGroupId(it)) c else it
            }

            val entity = ExpenseEntity(
                title = title.trim(),
                amount = amount,
                isAnnual = isAnnual,
                customFolder = cleanedFolder,
                timestamp = System.currentTimeMillis(),
                shamsiYear = y,
                shamsiMonth = m,
                shamsiDay = d,
                shamsiDateString = normalizedDateStr,
                note = note.trim()
            )
            repository.insert(entity)
        }
    }

    fun getOriginalExpenseForEdit(entity: ExpenseEntity): ExpenseEntity {
        if (entity.id < 0) {
            val originalId = Math.abs(entity.id) / 100L
            return allExpenses.value.find { it.id == originalId } ?: entity
        }
        return entity
    }

    fun updateExpense(entity: ExpenseEntity) {
        viewModelScope.launch {
            if (entity.id < 0) {
                val originalId = Math.abs(entity.id) / 100L
                val original = allExpenses.value.find { it.id == originalId }
                if (original != null) {
                    repository.update(original.copy(title = entity.title, amount = entity.amount * 12, note = entity.note))
                }
            } else {
                repository.update(entity)
            }
        }
    }

    fun deleteExpense(entity: ExpenseEntity) {
        viewModelScope.launch {
            if (entity.id < 0) {
                val originalId = Math.abs(entity.id) / 100L
                repository.deleteById(originalId)
            } else {
                repository.delete(entity)
            }
        }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            val originalId = if (id < 0) Math.abs(id) / 100L else id
            repository.deleteById(originalId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun createFullBackupJson(): String {
        return com.example.utils.BackupRestoreHelper.createBackupJson(
            expenses = allExpenses.value,
            customFolders = customFolders.value
        )
    }

    fun restoreBackup(
        expenses: List<com.example.data.model.ExpenseEntity>,
        customFolders: List<com.example.data.model.CustomFolderEntity>,
        replaceExisting: Boolean = true,
        onComplete: (Boolean, Int, Int) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.restoreBackup(expenses, customFolders, replaceExisting)
                onComplete(true, expenses.size, customFolders.size)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, 0, 0)
            }
        }
    }

    fun setSelectedMonth(monthIndex: Int) {
        if (monthIndex in 1..12) {
            _selectedMonthIndex.value = monthIndex
        }
    }

    fun setYear(year: Int) {
        _currentYear.value = year
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectReportFolder(folderName: String?) {
        _selectedReportFolder.value = folderName
    }

    private fun getMonthsBetween(startDate: String, endDate: String): List<Pair<Int, Int>> {
        val sClean = PersianDateHelper.toEnglishDigits(startDate)
        val eClean = PersianDateHelper.toEnglishDigits(endDate)
        val startParts = sClean.split("/")
        val endParts = eClean.split("/")
        val startY = startParts.getOrNull(0)?.toIntOrNull() ?: return emptyList()
        val startM = startParts.getOrNull(1)?.toIntOrNull() ?: return emptyList()
        val endY = endParts.getOrNull(0)?.toIntOrNull() ?: return emptyList()
        val endM = endParts.getOrNull(1)?.toIntOrNull() ?: return emptyList()

        val result = mutableListOf<Pair<Int, Int>>()
        var currY = startY
        var currM = startM
        var safetyCounter = 0
        while ((currY < endY || (currY == endY && currM <= endM)) && safetyCounter < 120) {
            result.add(Pair(currY, currM))
            currM++
            if (currM > 12) {
                currM = 1
                currY++
            }
            safetyCounter++
        }
        return result
    }

    private fun calculatePastMonthsStartDate(monthsBack: Int): String {
        val today = PersianDateHelper.getShamsiDate()
        var targetM = today.month - (monthsBack - 1)
        var targetY = today.year
        while (targetM <= 0) {
            targetM += 12
            targetY -= 1
        }
        val mStr = if (targetM < 10) "0$targetM" else "$targetM"
        return "$targetY/$mStr/01"
    }

    fun setDateRangePreset(preset: String) {
        val today = PersianDateHelper.getShamsiDate()
        val y = today.year
        val m = today.month
        val daysInMonth = PersianDateHelper.getDaysInMonth(y, m)
        val endOfMonthStr = "$y/${if (m < 10) "0$m" else "$m"}/${if (daysInMonth < 10) "0$daysInMonth" else "$daysInMonth"}"

        when (preset) {
            "این ماه" -> {
                val start = "$y/${if (m < 10) "0$m" else "$m"}/01"
                _reportsFilter.value = ReportsFilterState(start, endOfMonthStr, preset)
            }
            "ماه گذشته" -> {
                val lastM = if (m == 1) 12 else m - 1
                val lastY = if (m == 1) y - 1 else y
                val daysInLastM = PersianDateHelper.getDaysInMonth(lastY, lastM)
                val mStr = if (lastM < 10) "0$lastM" else "$lastM"
                val start = "$lastY/$mStr/01"
                val end = "$lastY/$mStr/${if (daysInLastM < 10) "0$daysInLastM" else "$daysInLastM"}"
                _reportsFilter.value = ReportsFilterState(start, end, preset)
            }
            "۳ ماه اخیر" -> {
                val start = calculatePastMonthsStartDate(3)
                _reportsFilter.value = ReportsFilterState(start, endOfMonthStr, preset)
            }
            "۴ ماه اخیر" -> {
                val start = calculatePastMonthsStartDate(4)
                _reportsFilter.value = ReportsFilterState(start, endOfMonthStr, preset)
            }
            "۵ ماه اخیر" -> {
                val start = calculatePastMonthsStartDate(5)
                _reportsFilter.value = ReportsFilterState(start, endOfMonthStr, preset)
            }
            "۶ ماه اخیر" -> {
                val start = calculatePastMonthsStartDate(6)
                _reportsFilter.value = ReportsFilterState(start, endOfMonthStr, preset)
            }
            "کل سال جاری" -> {
                val daysInLastMonthOfYear = PersianDateHelper.getDaysInMonth(y, 12)
                val start = "$y/01/01"
                val end = "$y/12/${if (daysInLastMonthOfYear < 10) "0$daysInLastMonthOfYear" else "$daysInLastMonthOfYear"}"
                _reportsFilter.value = ReportsFilterState(start, end, preset)
            }
        }
    }

    fun setCustomDateRange(start: String, end: String) {
        _reportsFilter.value = ReportsFilterState(start, end, "بازه دلخواه")
    }

    fun exportToCsv(folderFilter: String? = null): String {
        val filter = _reportsFilter.value
        val expanded = allExpandedExpenses.value
        val startNum = parseDateToNumber(filter.startDate)
        val endNum = parseDateToNumber(filter.endDate)
        val inRange = expanded.filter {
            val dateNum = if (it.entity.shamsiYear > 0 && it.entity.shamsiMonth > 0) {
                it.entity.shamsiYear * 10000 + it.entity.shamsiMonth * 100 + it.entity.shamsiDay.coerceAtLeast(1)
            } else {
                parseDateToNumber(it.entity.shamsiDateString)
            }
            dateNum in startNum..endNum
        }
        val resolved = if (folderFilter != null) inRange.filter { isFolderMatch(it, folderFilter) } else inRange
        val sb = StringBuilder()
        sb.append("شناسه,عنوان,مبلغ (تومان),تاریخ شمسی,نوع هزینه,پوشه,یادداشت\n")
        for (r in resolved) {
            val type = if (r.entity.isAnnual) "سهم ماهانه هزینه سالانه" else "عادی"
            sb.append("${r.entity.id},\"${r.entity.title}\",${r.entity.amount},\"${r.entity.shamsiDateString}\",\"$type\",\"${r.effectiveFolder}\",\"${r.entity.note}\"\n")
        }
        return sb.toString()
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.initDefaultFoldersIfNeeded()

            val today = PersianDateHelper.getShamsiDate()
            val y = today.year
            val m = today.month

            val samples = listOf(
                // Annual commitments (amortized over 12 months)
                ExpenseEntity(
                    title = "بیمه شخص ثالث خودرو",
                    amount = 12000000L,
                    isAnnual = true,
                    customFolder = "هزینه‌های سالانه و تعهدات",
                    shamsiYear = y,
                    shamsiMonth = 1,
                    shamsiDay = 15,
                    shamsiDateString = "$y/01/15",
                    note = "بیمه ایران - پرداخت سالانه سرشکن شده به ۱۲ ماه"
                ),
                ExpenseEntity(
                    title = "بیمه تکمیلی درمان",
                    amount = 6000000L,
                    isAnnual = true,
                    customFolder = "هزینه‌های سالانه و تعهدات",
                    shamsiYear = y,
                    shamsiMonth = 2,
                    shamsiDay = 10,
                    shamsiDateString = "$y/02/10",
                    note = "بیمه دانا - سرشکن ماهانه ۵۰۰ هزار تومان"
                ),
                ExpenseEntity(
                    title = "بیمه موتورسیکلت",
                    amount = 2400000L,
                    isAnnual = true,
                    customFolder = "هزینه‌های سالانه و تعهدات",
                    shamsiYear = y,
                    shamsiMonth = 3,
                    shamsiDay = 20,
                    shamsiDateString = "$y/03/20",
                    note = "بیمه آسیا - سرشکن ماهانه ۲۰۰ هزار تومان"
                ),

                // Monthly expenses
                ExpenseEntity(
                    title = "خرید سوپرمارکت",
                    amount = 1450000L,
                    isAnnual = false,
                    customFolder = "سوپرمارکت و خوراکی",
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 2,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/02",
                    note = "خرید اقلام روزمره"
                ),
                ExpenseEntity(
                    title = "خرید میوه و پروتئین",
                    amount = 980000L,
                    isAnnual = false,
                    customFolder = "سوپرمارکت و خوراکی",
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 12,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/12",
                    note = "لبنیات و پروتئین"
                ),
                ExpenseEntity(
                    title = "بنزین",
                    amount = 150000L,
                    isAnnual = false,
                    customFolder = "بنزین و حمل‌ونقل",
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 4,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/04",
                    note = "باک بنزین"
                ),
                ExpenseEntity(
                    title = "تاکسی اینترنتی",
                    amount = 120000L,
                    isAnnual = false,
                    customFolder = "بنزین و حمل‌ونقل",
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 14,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/14",
                    note = ""
                ),
                ExpenseEntity(
                    title = "تعمیر کفش",
                    amount = 180000L,
                    isAnnual = false,
                    customFolder = null,
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 8,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/08",
                    note = "تعمیر زیپ و واکس"
                ),
                ExpenseEntity(
                    title = "خرید کتاب",
                    amount = 320000L,
                    isAnnual = false,
                    customFolder = null,
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 10,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/10",
                    note = "کتاب‌فروشی مرکزی"
                ),
                ExpenseEntity(
                    title = "هزینه متفرقه و پیش‌بینی‌نشده",
                    amount = 95000L,
                    isAnnual = false,
                    customFolder = "متفرقه",
                    shamsiYear = y,
                    shamsiMonth = m,
                    shamsiDay = 15,
                    shamsiDateString = "$y/${if (m < 10) "0$m" else "$m"}/15",
                    note = "خرید خرده‌ریز متفرقه"
                )
            ).toMutableList()

            // 1. Fatima expenses across past 3 months (addresses user's specific test example)
            val fatimaAmounts = listOf(1500000L, 1800000L, 2000000L)
            val fatimaNotes = listOf("خرج ماهانه شهریور فاطمه", "خرج ماهانه مهر فاطمه", "خرج ماهانه آبان فاطمه")
            for (idx in 0..2) {
                var targetM = m - (2 - idx)
                var targetY = y
                while (targetM <= 0) {
                    targetM += 12
                    targetY -= 1
                }
                val mStr = if (targetM < 10) "0$targetM" else "$targetM"
                samples.add(
                    ExpenseEntity(
                        title = "پول فاطمه",
                        amount = fatimaAmounts[idx],
                        isAnnual = false,
                        customFolder = "فاطمه",
                        shamsiYear = targetY,
                        shamsiMonth = targetM,
                        shamsiDay = 12,
                        shamsiDateString = "$targetY/$mStr/12",
                        note = fatimaNotes[idx]
                    )
                )
            }

            // 2. Ali expenses across past 3 months
            val aliAmounts = listOf(1200000L, 1400000L, 1500000L)
            for (idx in 0..2) {
                var targetM = m - (2 - idx)
                var targetY = y
                while (targetM <= 0) {
                    targetM += 12
                    targetY -= 1
                }
                val mStr = if (targetM < 10) "0$targetM" else "$targetM"
                samples.add(
                    ExpenseEntity(
                        title = "پول علی",
                        amount = aliAmounts[idx],
                        isAnnual = false,
                        customFolder = "علی",
                        shamsiYear = targetY,
                        shamsiMonth = targetM,
                        shamsiDay = 10,
                        shamsiDateString = "$targetY/$mStr/10",
                        note = "خرج ماهانه علی"
                    )
                )
            }

            // 3. Motorcycle upkeep across past 3 months
            val motorTitles = listOf("سرویس و تعویض روغن موتور", "تنظیم و آچارکشی موتور", "تعمیر لنت ترمز موتور")
            val motorAmounts = listOf(350000L, 280000L, 420000L)
            for (idx in 0..2) {
                var targetM = m - (2 - idx)
                var targetY = y
                while (targetM <= 0) {
                    targetM += 12
                    targetY -= 1
                }
                val mStr = if (targetM < 10) "0$targetM" else "$targetM"
                samples.add(
                    ExpenseEntity(
                        title = motorTitles[idx],
                        amount = motorAmounts[idx],
                        isAnnual = false,
                        customFolder = "موتور",
                        shamsiYear = targetY,
                        shamsiMonth = targetM,
                        shamsiDay = 18,
                        shamsiDateString = "$targetY/$mStr/18",
                        note = "هزینه نگهداری موتور"
                    )
                )
            }

            // 4. Supermarket items across past months
            val superTitles = listOf("خرید سوپرمارکت", "خرید های سوپرمارکت", "خرید سوپرمارکت", "خرید های سوپرمارکت")
            val superAmounts = listOf(1350000L, 950000L, 1600000L, 1100000L)
            val superDays = listOf(5, 20, 8, 22)
            val superMonthsOffset = listOf(2, 2, 1, 1)
            for (idx in superTitles.indices) {
                var targetM = m - superMonthsOffset[idx]
                var targetY = y
                while (targetM <= 0) {
                    targetM += 12
                    targetY -= 1
                }
                val mStr = if (targetM < 10) "0$targetM" else "$targetM"
                val d = superDays[idx]
                val dStr = if (d < 10) "0$d" else "$d"
                samples.add(
                    ExpenseEntity(
                        title = superTitles[idx],
                        amount = superAmounts[idx],
                        isAnnual = false,
                        customFolder = "سوپرمارکت و خوراکی",
                        shamsiYear = targetY,
                        shamsiMonth = targetM,
                        shamsiDay = d,
                        shamsiDateString = "$targetY/$mStr/$dStr",
                        note = "خرید خواربار و اقلام روزمره"
                    )
                )
            }

            // Internet expenses across past 5 months (addresses user's specific test example)
            val internetTitles = listOf("بسته اینترنت همراه اول", "شارژ اینترنت شاتل", "بسته اینترنت ایرانسل", "شارژ اینترنت شاتل", "بسته مودم ثابت")
            val internetAmounts = listOf(240000L, 350000L, 190000L, 350000L, 280000L)
            for (i in 0..4) {
                var targetM = m - i
                var targetY = y
                while (targetM <= 0) {
                    targetM += 12
                    targetY -= 1
                }
                val mStr = if (targetM < 10) "0$targetM" else "$targetM"
                samples.add(
                    ExpenseEntity(
                        title = internetTitles[i],
                        amount = internetAmounts[i],
                        isAnnual = false,
                        customFolder = "اینترنت",
                        shamsiYear = targetY,
                        shamsiMonth = targetM,
                        shamsiDay = 15,
                        shamsiDateString = "$targetY/$mStr/15",
                        note = "هزینه اینترنت ماه ${PersianDateHelper.getMonthName(targetM)}"
                    )
                )
            }

            // Expenses in previous year (so user can view past years' calendar data)
            val prevYear = y - 1
            samples.add(
                ExpenseEntity(
                    title = "بیمه خودرو سال قبل",
                    amount = 9000000L,
                    isAnnual = true,
                    customFolder = "هزینه‌های سالانه و تعهدات",
                    shamsiYear = prevYear,
                    shamsiMonth = 1,
                    shamsiDay = 10,
                    shamsiDateString = "$prevYear/01/10",
                    note = "پرداخت سال گذشته"
                )
            )
            samples.add(
                ExpenseEntity(
                    title = "سرویس دوره‌ای خودرو",
                    amount = 2100000L,
                    isAnnual = false,
                    customFolder = "بنزین و حمل‌ونقل",
                    shamsiYear = prevYear,
                    shamsiMonth = 7,
                    shamsiDay = 18,
                    shamsiDateString = "$prevYear/07/18",
                    note = "تعویض تسمه و روغن"
                )
            )
            samples.add(
                ExpenseEntity(
                    title = "خرید پوشاک زمستانه",
                    amount = 3500000L,
                    isAnnual = false,
                    customFolder = "پوشاک و خرید شخصی",
                    shamsiYear = prevYear,
                    shamsiMonth = 9,
                    shamsiDay = 25,
                    shamsiDateString = "$prevYear/09/25",
                    note = "پالتو و کفش"
                )
            )

            repository.insertAll(samples)
        }
    }
}
