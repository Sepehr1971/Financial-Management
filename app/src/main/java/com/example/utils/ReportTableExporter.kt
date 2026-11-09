package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.ResolvedExpense
import com.example.data.repository.FolderMatcher
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Handles generating and exporting high-fidelity Excel (.xlsx) and PDF reports
 * matching the exact Iranian accounting table layout provided in the reference template.
 * Uses font "B Mitra" and size 14 with clearly visible column and row borders.
 */
object ReportTableExporter {

    private val numberFormat = DecimalFormat("#,###")

    data class TableRowData(
        val rowNumber: Int,
        val title: String,
        val date: String,
        val amount: Long?,
        val subtotal: Long?,
        val note: String,
        val isFirstInGroup: Boolean,
        val groupSize: Int
    )

    data class ProcessedReportData(
        val reportTitle: String,
        val startDate: String,
        val endDate: String,
        val rows: List<TableRowData>,
        val grandTotal: Long,
        val dataRowCount: Int
    )

    /**
     * Resolves an expense into a standardized canonical group title and preserves
     * any specific description/title in effectiveNote.
     * Guarantees that all supermarket items across all months group under "خرید سوپرمارکت",
     * all Fatima items group under "فاطمه", all Ali items under "علی", etc.
     */
    fun resolveCanonicalGroupTitle(expense: ResolvedExpense): Pair<String, String> {
        val rawTitle = expense.entity.title.trim()
        val rawFolder = expense.effectiveFolder.trim()
        val customFolder = expense.entity.customFolder?.trim().orEmpty()
        val rawNote = expense.entity.note.trim()

        val titleKey = FolderMatcher.toKey(rawTitle)
        val folderKey = FolderMatcher.toKey(rawFolder)
        val customKey = FolderMatcher.toKey(customFolder)

        val titleGroup = FolderMatcher.getCanonicalGroupId(rawTitle)
        val folderGroup = FolderMatcher.getCanonicalGroupId(rawFolder)
        val customGroup = if (customFolder.isNotBlank()) FolderMatcher.getCanonicalGroupId(customFolder) else ""

        val canonicalTitle: String
        var effectiveNote = rawNote

        when {
            // 1. Supermarket / Groceries (Unifies all grocery items across all months)
            titleGroup == "SUPERMARKET" || folderGroup == "SUPERMARKET" || customGroup == "SUPERMARKET" ||
            titleKey.contains("سوپرمارکت") || titleKey.contains("سوپر") || folderKey.contains("سوپرمارکت") || folderKey.contains("سوپر") -> {
                canonicalTitle = "خرید سوپرمارکت"
                if (effectiveNote.isBlank() && rawTitle != "خرید سوپرمارکت" && rawTitle != "سوپرمارکت" && rawTitle != "خرید های سوپرمارکت" && rawTitle != "خرید های سوپرمارکتی") {
                    effectiveNote = rawTitle
                }
            }

            // 2. Family: Fatima (Unifies "فاطمه", "پول فاطمه", "شهریه فاطمه", etc.)
            titleGroup == "فاطمه" || folderGroup == "فاطمه" || customGroup == "فاطمه" ||
            titleKey.contains("فاطمه") || folderKey.contains("فاطمه") || customKey.contains("فاطمه") -> {
                canonicalTitle = "فاطمه"
                if (effectiveNote.isBlank() && rawTitle != "فاطمه" && rawTitle != "پول فاطمه") {
                    effectiveNote = rawTitle
                }
            }

            // 3. Family: Ali (Unifies "علی", "پول علی", etc.)
            titleGroup == "علی" || folderGroup == "علی" || customGroup == "علی" ||
            titleKey.contains("علی") || folderKey.contains("علی") || customKey.contains("علی") -> {
                canonicalTitle = "علی"
                if (effectiveNote.isBlank() && rawTitle != "علی" && rawTitle != "پول علی") {
                    effectiveNote = rawTitle
                }
            }

            // 4. Motorcycle (Unifies all motor maintenance, fuel, and amortized insurance)
            titleGroup == "MOTOR" || folderGroup == "MOTOR" || customGroup == "MOTOR" ||
            titleKey.contains("موتور") || folderKey.contains("موتور") || customKey.contains("موتور") -> {
                canonicalTitle = FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME
                if (effectiveNote.isBlank() && rawTitle != "موتور") {
                    effectiveNote = rawTitle
                }
            }

            // 5. Fruits & Produce
            titleGroup == "FRUIT" || folderGroup == "FRUIT" || customGroup == "FRUIT" ||
            titleKey.contains("میوه") || titleKey.contains("ترهبار") || folderKey.contains("میوه") -> {
                canonicalTitle = "خرید میوه و تره‌بار"
                if (effectiveNote.isBlank() && !rawTitle.contains("میوه")) {
                    effectiveNote = rawTitle
                }
            }

            // 6. Chicken & Meat
            titleGroup == "CHICKEN_MEAT" || folderGroup == "CHICKEN_MEAT" || customGroup == "CHICKEN_MEAT" ||
            titleKey.contains("مرغ") || titleKey.contains("گوشت") -> {
                canonicalTitle = "خرید پروتئین و گوشت"
                if (effectiveNote.isBlank()) {
                    effectiveNote = rawTitle
                }
            }

            // 7. Bakery / Bread
            titleGroup == "BAKERY" || folderGroup == "BAKERY" || customGroup == "BAKERY" ||
            titleKey.contains("نان") || folderKey.contains("نان") -> {
                canonicalTitle = "نانوایی"
                if (effectiveNote.isBlank() && rawTitle != "نانوایی" && rawTitle != "نان") {
                    effectiveNote = rawTitle
                }
            }

            // 8. Snacks & Confectionery
            titleGroup == "SNACKS" || folderGroup == "SNACKS" || customGroup == "SNACKS" ||
            titleKey.contains("تنقلات") || titleKey.contains("شیرینی") || titleKey.contains("آجیل") -> {
                canonicalTitle = "تنقلات و شیرینی"
                if (effectiveNote.isBlank() && rawTitle != "تنقلات و شیرینی" && rawTitle != "تنقلات") {
                    effectiveNote = rawTitle
                }
            }

            // 9. Fuel & Transportation
            titleGroup == "TRANSPORT" || folderGroup == "TRANSPORT" || customGroup == "TRANSPORT" ||
            titleKey.contains("بنزین") || titleKey.contains("حمل") || titleKey.contains("اسنپ") || titleKey.contains("تپسی") || titleKey.contains("تاکسی") -> {
                canonicalTitle = "بنزین و حمل‌ونقل"
                if (effectiveNote.isBlank() && rawTitle != "بنزین و حمل‌ونقل" && rawTitle != "بنزین") {
                    effectiveNote = rawTitle
                }
            }

            // 10. Clothing & Personal Apparel
            titleGroup == "CLOTHING" || folderGroup == "CLOTHING" || customGroup == "CLOTHING" ||
            titleKey.contains("پوشاک") || titleKey.contains("لباس") || titleKey.contains("کفش") -> {
                canonicalTitle = "پوشاک و خرید شخصی"
                if (effectiveNote.isBlank() && rawTitle != "پوشاک و خرید شخصی") {
                    effectiveNote = rawTitle
                }
            }

            // 11. Housing Rent
            titleGroup == "RENT" || folderGroup == "RENT" || customGroup == "RENT" ||
            titleKey.contains("اجاره") || titleKey.contains("مسکن") || titleKey.contains("رهن") -> {
                canonicalTitle = "اجاره مسکن"
                if (effectiveNote.isBlank() && rawTitle != "اجاره مسکن") {
                    effectiveNote = rawTitle
                }
            }

            // 12. Utilities / Bills
            titleGroup == "BILLS" || folderGroup == "BILLS" || customGroup == "BILLS" ||
            titleKey.contains("قبض") || titleKey.contains("قبوض") || titleKey.contains("برق") || titleKey.contains("گاز") || titleKey.contains("آب") -> {
                canonicalTitle = "قبوض مصرفی"
                if (effectiveNote.isBlank() && rawTitle != "قبوض مصرفی") {
                    effectiveNote = rawTitle
                }
            }

            // 13. Internet
            titleGroup == "INTERNET" || folderGroup == "INTERNET" || customGroup == "INTERNET" ||
            titleKey.contains("اینترنت") || titleKey.contains("مودم") || titleKey.contains("وایفای") || folderKey.contains("اینترنت") -> {
                canonicalTitle = "اینترنت"
                if (effectiveNote.isBlank() && rawTitle != "اینترنت") {
                    effectiveNote = rawTitle
                }
            }

            // 14. Restaurant & Cafe
            titleGroup == "RESTAURANT" || folderGroup == "RESTAURANT" || customGroup == "RESTAURANT" ||
            titleKey.contains("رستوران") || titleKey.contains("کافه") || titleKey.contains("فستفود") -> {
                canonicalTitle = "رستوران و کافه"
                if (effectiveNote.isBlank() && rawTitle != "رستوران و کافه") {
                    effectiveNote = rawTitle
                }
            }

            // 15. Medical & Health
            titleGroup == "MEDICAL" || folderGroup == "MEDICAL" || customGroup == "MEDICAL" ||
            titleKey.contains("پزشک") || titleKey.contains("دارو") || titleKey.contains("درمان") || titleKey.contains("دندان") -> {
                canonicalTitle = "درمان و دارو"
                if (effectiveNote.isBlank() && rawTitle != "درمان و دارو") {
                    effectiveNote = rawTitle
                }
            }

            // 16. Annual Commitments & Insurance
            expense.entity.isAnnual || titleGroup == "ANNUAL" || folderGroup == "ANNUAL" || customGroup == "ANNUAL" ||
            titleKey.contains("بیمه") || folderKey.contains("سالانه") || folderKey.contains("تعهدات") -> {
                canonicalTitle = "تعهدات و بیمه سالانه"
                if (effectiveNote.isBlank()) {
                    effectiveNote = rawTitle
                }
            }

            // 17. Miscellaneous
            titleKey.contains("متفرقه") || folderKey.contains("متفرقه") -> {
                canonicalTitle = "هزینه‌های متفرقه"
                if (effectiveNote.isBlank() && rawTitle != "متفرقه" && rawTitle != "هزینه های متفرقه" && rawTitle != "هزینه‌های متفرقه") {
                    effectiveNote = rawTitle
                }
            }

            else -> {
                val clean = FolderMatcher.cleanFolderName(rawTitle)
                canonicalTitle = if (clean.isNotBlank() && clean.length <= 25) clean else {
                    val cf = FolderMatcher.cleanFolderName(rawFolder)
                    if (cf.isNotBlank()) cf else "هزینه‌های متفرقه"
                }
                if (effectiveNote.isBlank() && rawTitle != canonicalTitle) {
                    effectiveNote = rawTitle
                }
            }
        }

        return Pair(canonicalTitle, effectiveNote)
    }

    private fun getGroupSortPriority(title: String): Int {
        val norm = FolderMatcher.toKey(title)
        return when {
            norm.contains("سوپرمارکت") || norm.contains("سوپر") -> 10
            norm.contains("میوه") || norm.contains("تره‌بار") || norm.contains("ترهبار") -> 20
            norm.contains("نان") || norm.contains("نانوایی") -> 30
            norm.contains("گوشت") || norm.contains("مرغ") || norm.contains("پروتئین") -> 40
            norm.contains("تنقلات") || norm.contains("شیرینی") -> 50
            norm.contains("بنزین") || norm.contains("حمل") || norm.contains("تاکسی") || norm.contains("اسنپ") -> 60
            norm.contains("موتور") -> 70
            norm.contains("فاطمه") -> 80
            norm.contains("علی") -> 90
            norm.contains("پوشاک") || norm.contains("لباس") -> 100
            norm.contains("اینترنت") || norm.contains("مودم") -> 110
            norm.contains("رستوران") || norm.contains("کافه") -> 120
            norm.contains("اجاره") || norm.contains("مسکن") -> 130
            norm.contains("قبض") || norm.contains("قبوض") || norm.contains("برق") || norm.contains("گاز") || norm.contains("آب") -> 140
            norm.contains("درمان") || norm.contains("دارو") || norm.contains("پزشک") -> 150
            norm.contains("بیمه") || norm.contains("تعهدات") || norm.contains("سالانه") -> 900
            norm.contains("متفرقه") -> 999
            else -> 500
        }
    }

    private fun parseExpenseDateToNum(dateStr: String): Int {
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

    private data class MappedExpenseItem(
        val expense: ResolvedExpense,
        val canonicalTitle: String,
        val effectiveNote: String,
        val dateNum: Int
    )

    /**
     * Groups and prepares expense items into rows with subtotals (جمع جزء),
     * consecutive grouped rows sorted chronologically by date,
     * dynamic 20+ row expansion, and grand total.
     */
    fun processExpenses(
        expenses: List<ResolvedExpense>,
        startDate: String,
        endDate: String,
        selectedFolder: String?
    ): ProcessedReportData {
        val cleanStart = PersianDateHelper.toPersianDigits(startDate)
        val cleanEnd = PersianDateHelper.toPersianDigits(endDate)

        val reportTitle = if (!selectedFolder.isNullOrBlank()) {
            "گزارش هزینه‌های پوشه «$selectedFolder» از تاریخ $cleanStart تا $cleanEnd"
        } else {
            "گزارش هزینه ها از تاریخ $cleanStart تا $cleanEnd"
        }

        // 1. Map all items to canonical group titles and effective notes
        val mappedList = expenses.map { item ->
            val (canonTitle, effNote) = resolveCanonicalGroupTitle(item)
            val dateNum = parseExpenseDateToNum(item.entity.shamsiDateString)
            MappedExpenseItem(item, canonTitle, effNote, dateNum)
        }

        // 2. Group items by their canonical title
        val groupsMap = LinkedHashMap<String, MutableList<MappedExpenseItem>>()
        for (m in mappedList) {
            groupsMap.getOrPut(m.canonicalTitle) { mutableListOf() }.add(m)
        }

        // 3. Sort groups deterministically: priority order, then alphabetically
        val sortedGroups = groupsMap.entries.sortedWith(
            compareBy<Map.Entry<String, MutableList<MappedExpenseItem>>> { getGroupSortPriority(it.key) }
                .thenBy { it.key }
        )

        val rows = mutableListOf<TableRowData>()
        var currentRowIndex = 1

        // 4. Populate rows: all items for each category are strictly consecutive and sorted chronologically
        for ((title, items) in sortedGroups) {
            val sortedItems = items.sortedWith(
                compareBy<MappedExpenseItem> { it.dateNum }
                    .thenBy { it.expense.entity.shamsiDateString }
                    .thenBy { it.expense.entity.id }
            )
            val groupTotal = sortedItems.sumOf { it.expense.entity.amount }
            val groupSize = sortedItems.size

            sortedItems.forEachIndexed { idx, item ->
                val isFirst = idx == 0
                val subtotal = if (isFirst && groupSize > 1) groupTotal else null

                rows.add(
                    TableRowData(
                        rowNumber = currentRowIndex++,
                        title = title,
                        date = item.expense.entity.shamsiDateString,
                        amount = item.expense.entity.amount,
                        subtotal = subtotal,
                        note = item.effectiveNote,
                        isFirstInGroup = isFirst,
                        groupSize = groupSize
                    )
                )
            }
        }

        val dataRowCount = rows.size
        val totalPaddingRows = if (dataRowCount < 20) 20 - dataRowCount else 0

        // If less than 20 rows, fill with blank formatted rows up to row 20
        for (i in 1..totalPaddingRows) {
            rows.add(
                TableRowData(
                    rowNumber = currentRowIndex++,
                    title = "",
                    date = "",
                    amount = null,
                    subtotal = null,
                    note = "",
                    isFirstInGroup = true,
                    groupSize = 1
                )
            )
        }

        val grandTotal = expenses.sumOf { it.entity.amount }

        return ProcessedReportData(
            reportTitle = reportTitle,
            startDate = cleanStart,
            endDate = cleanEnd,
            rows = rows,
            grandTotal = grandTotal,
            dataRowCount = dataRowCount
        )
    }

    /**
     * Generates a PDF report matching the reference accounting table with:
     * - Font: B Mitra, size 14
     * - Crisp, clear black grid lines for all columns and rows
     * - Multi-item subtotal cells vertically merged in "جمع جزء"
     * - Automatic pagination for large data
     */
    fun createPdfDocument(context: Context, reportData: ProcessedReportData): ByteArray {
        val pdfDocument = PdfDocument()

        // Load B Mitra font (falls back to system font if unavailable)
        val bMitraTypeface = try {
            ResourcesCompat.getFont(context, R.font.b_mitra)
        } catch (_: Exception) {
            Typeface.create("B Mitra", Typeface.NORMAL)
        } ?: Typeface.DEFAULT

        val bMitraBold = Typeface.create(bMitraTypeface, Typeface.BOLD)

        // Paints with requested font "B Mitra" and size 14
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bMitraBold
            textSize = 14.5f
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bMitraBold
            textSize = 13.5f
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }

        val cellTextCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bMitraTypeface
            textSize = 13.0f
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }

        val cellTextRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bMitraTypeface
            textSize = 12.5f
            textAlign = Paint.Align.RIGHT
            color = Color.BLACK
        }

        val cellBoldCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bMitraBold
            textSize = 13.0f
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }

        // Crisp, distinct black grid lines
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.95f
        }

        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }

        val headerFillPaint = Paint().apply {
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }

        val totalFillPaint = Paint().apply {
            color = Color.rgb(254, 243, 199)
            style = Paint.Style.FILL
        }

        // A4 page setup (595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 30f
        val rightMargin = 565f
        val topMargin = 36f
        val bottomMargin = 806f

        // Column X coordinates from Left to Right (RTL data layout):
        // Col 6 (توضیحات): [x0, x1] = [30, 190] (width 160)
        // Col 5 (جمع جزء): [x1, x2] = [190, 276] (width 86)
        // Col 4 (مبلغ تومان): [x2, x3] = [276, 362] (width 86)
        // Col 3 (تاریخ): [x3, x4] = [362, 430] (width 68)
        // Col 2 (عنوان): [x4, x5] = [430, 533] (width 103)
        // Col 1 (ردیف): [x5, x6] = [533, 565] (width 32)
        val colX = floatArrayOf(30f, 190f, 276f, 362f, 430f, 533f, 565f)

        fun drawHeaderBlock(canvas: android.graphics.Canvas, startY: Float): Float {
            var y = startY
            val titleHeight = 30f
            val headerHeight = 26f

            // 1. Top Title Row: گزارش هزینه ها از تاریخ ... تا ...
            canvas.drawRect(colX[0], y, colX[6], y + titleHeight, headerFillPaint)
            canvas.drawRect(colX[0], y, colX[6], y + titleHeight, borderPaint)
            val titleCenterY = y + (titleHeight / 2f) - ((titlePaint.descent() + titlePaint.ascent()) / 2f)
            canvas.drawText(reportData.reportTitle, (colX[0] + colX[6]) / 2f, titleCenterY, titlePaint)
            y += titleHeight

            // 2. Column Headers Row: ردیف | عنوان | تاریخ | مبلغ (تومان) | جمع جزء | توضیحات
            canvas.drawRect(colX[0], y, colX[6], y + headerHeight, headerFillPaint)
            canvas.drawRect(colX[0], y, colX[6], y + headerHeight, borderPaint)

            // Draw vertical column dividers for header
            for (i in 1..5) {
                canvas.drawLine(colX[i], y, colX[i], y + headerHeight, borderPaint)
            }

            val headers = listOf("توضیحات", "جمع جزء", "مبلغ (تومان)", "تاریخ", "عنوان", "ردیف")
            val headerCenterY = y + (headerHeight / 2f) - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
            for (i in 0..5) {
                val cx = (colX[i] + colX[i + 1]) / 2f
                canvas.drawText(headers[i], cx, headerCenterY, headerTextPaint)
            }
            y += headerHeight
            return y
        }

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = drawHeaderBlock(canvas, topMargin)

        // Process rows and multi-item subtotal cell grouping
        var rowIdx = 0
        while (rowIdx < reportData.rows.size) {
            val row = reportData.rows[rowIdx]

            // If start of a multi-item group, calculate full group height
            val isMultiGroup = row.isFirstInGroup && row.groupSize > 1
            val groupSpan = if (isMultiGroup) row.groupSize else 1

            // Pre-calculate heights of rows in this group
            val rowHeights = mutableListOf<Float>()
            var groupTotalHeight = 0f
            for (g in 0 until groupSpan) {
                val item = reportData.rows[rowIdx + g]
                val noteLines = wrapText(item.note, cellTextRightPaint, 150f)
                val h = when {
                    noteLines.size >= 3 -> 42f
                    noteLines.size == 2 -> 32f
                    else -> 22.5f
                }
                rowHeights.add(h)
                groupTotalHeight += h
            }

            // Check page overflow
            if (currentY + groupTotalHeight > bottomMargin - 30f && rowIdx > 0) {
                // Outer table border for current page
                canvas.drawRect(colX[0], topMargin, colX[6], currentY, outerBorderPaint)
                pdfDocument.finishPage(page)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = drawHeaderBlock(canvas, topMargin)
            }

            val groupStartY = currentY

            // Draw individual item rows in the group
            for (g in 0 until groupSpan) {
                val item = reportData.rows[rowIdx + g]
                val itemHeight = rowHeights[g]
                val itemBottomY = currentY + itemHeight

                // Draw horizontal row lines for columns 0 (توضیحات), 2 (مبلغ), 3 (تاریخ), 4 (عنوان), 5 (ردیف)
                canvas.drawLine(colX[0], itemBottomY, colX[1], itemBottomY, borderPaint)
                canvas.drawLine(colX[2], itemBottomY, colX[6], itemBottomY, borderPaint)

                // If not multi-group or single item, also draw line for column 1 (جمع جزء)
                if (!isMultiGroup) {
                    canvas.drawLine(colX[1], itemBottomY, colX[2], itemBottomY, borderPaint)
                }

                // Vertical grid lines
                for (i in 0..6) {
                    if (isMultiGroup && (i == 1 || i == 2)) {
                        // Skip internal line drawing for merged column; will draw group bounds
                        continue
                    }
                    canvas.drawLine(colX[i], currentY, colX[i], itemBottomY, borderPaint)
                }

                val rowCenterY = currentY + (itemHeight / 2f) - ((cellTextCenterPaint.descent() + cellTextCenterPaint.ascent()) / 2f)

                // 1. ردیف (Col 5 to 6)
                val rText = PersianDateHelper.toPersianDigits(item.rowNumber)
                canvas.drawText(rText, (colX[5] + colX[6]) / 2f, rowCenterY, cellTextCenterPaint)

                // 2. عنوان (Col 4 to 5)
                canvas.drawText(item.title, (colX[4] + colX[5]) / 2f, rowCenterY, cellTextCenterPaint)

                // 3. تاریخ (Col 3 to 4)
                val dText = PersianDateHelper.toPersianDigits(item.date)
                canvas.drawText(dText, (colX[3] + colX[4]) / 2f, rowCenterY, cellTextCenterPaint)

                // 4. مبلغ تومان (Col 2 to 3)
                if (item.amount != null) {
                    val amtText = PersianDateHelper.toPersianDigits(numberFormat.format(item.amount))
                    canvas.drawText(amtText, (colX[2] + colX[3]) / 2f, rowCenterY, cellTextCenterPaint)
                }

                // 5. توضیحات (Col 0 to 1) - Multi-line wrapping
                if (item.note.isNotBlank()) {
                    val noteLines = wrapText(item.note, cellTextRightPaint, 150f)
                    val noteLineHeight = 12f
                    val totalNoteH = noteLines.size * noteLineHeight
                    val noteStartY = currentY + ((itemHeight - totalNoteH) / 2f) + 9.5f
                    for ((lineIdx, line) in noteLines.withIndex()) {
                        canvas.drawText(line, colX[1] - 5f, noteStartY + (lineIdx * noteLineHeight), cellTextRightPaint)
                    }
                }

                currentY = itemBottomY
            }

            // Draw merged "جمع جزء" cell and subtotal for multi-item group
            if (isMultiGroup) {
                val groupEndY = currentY
                // Vertical borders for merged column 1 (جمع جزء)
                canvas.drawLine(colX[1], groupStartY, colX[1], groupEndY, borderPaint)
                canvas.drawLine(colX[2], groupStartY, colX[2], groupEndY, borderPaint)
                // Bottom border for merged column 1
                canvas.drawLine(colX[1], groupEndY, colX[2], groupEndY, borderPaint)

                // Draw subtotal text centered vertically within the merged group cell
                if (row.subtotal != null) {
                    val subtotalText = PersianDateHelper.toPersianDigits(numberFormat.format(row.subtotal))
                    val groupCenterY = groupStartY + (groupTotalHeight / 2f) - ((cellBoldCenterPaint.descent() + cellBoldCenterPaint.ascent()) / 2f)
                    canvas.drawText(subtotalText, (colX[1] + colX[2]) / 2f, groupCenterY, cellBoldCenterPaint)
                }
            }

            rowIdx += groupSpan
        }

        // Draw Summary Row: جمع کل :
        val totalRowHeight = 28f
        if (currentY + totalRowHeight > bottomMargin) {
            canvas.drawRect(colX[0], topMargin, colX[6], currentY, outerBorderPaint)
            pdfDocument.finishPage(page)

            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = drawHeaderBlock(canvas, topMargin)
        }

        val totalBottomY = currentY + totalRowHeight
        // Fill total row
        canvas.drawRect(colX[0], currentY, colX[6], totalBottomY, totalFillPaint)

        // Draw borders for total row
        canvas.drawRect(colX[0], currentY, colX[6], totalBottomY, borderPaint)
        canvas.drawLine(colX[2], currentY, colX[2], totalBottomY, borderPaint)
        canvas.drawLine(colX[3], currentY, colX[3], totalBottomY, borderPaint)

        val totalCenterY = currentY + (totalRowHeight / 2f) - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)

        // "جمع کل :" label (from Col 3 to Col 6)
        canvas.drawText("جمع کل :", (colX[3] + colX[6]) / 2f, totalCenterY, headerTextPaint)

        // Grand Total Amount (in Col 2 to Col 3)
        val grandTotalText = PersianDateHelper.toPersianDigits(numberFormat.format(reportData.grandTotal))
        canvas.drawText(grandTotalText, (colX[2] + colX[3]) / 2f, totalCenterY, headerTextPaint)

        currentY = totalBottomY

        // Draw outer bold border around the whole table on final page
        canvas.drawRect(colX[0], topMargin, colX[6], currentY, outerBorderPaint)

        pdfDocument.finishPage(page)

        val bos = ByteArrayOutputStream()
        pdfDocument.writeTo(bos)
        pdfDocument.close()
        return bos.toByteArray()
    }

    /**
     * Generates a fully compliant Microsoft Excel (.xlsx) OpenXML workbook
     * with font "B Mitra", size 14, and sharp black column/row grid borders.
     */
    fun createExcelWorkbook(reportData: ProcessedReportData): ByteArray {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        fun addEntry(path: String, content: String) {
            val entry = ZipEntry(path)
            zos.putNextEntry(entry)
            zos.write(content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        // 1. [Content_Types].xml
        addEntry(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
        )

        // 2. _rels/.rels
        addEntry(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        )

        // 3. xl/_rels/workbook.xml.rels
        addEntry(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
        )

        // 4. xl/workbook.xml
        addEntry(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="گزارش هزینه‌ها" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
        )

        // 5. xl/styles.xml with font "B Mitra", size 14, and sharp black borders
        addEntry(
            "xl/styles.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0"/>
  </numFmts>
  <fonts count="3">
    <!-- Font 0: B Mitra 14pt Normal -->
    <font><sz val="14"/><name val="B Mitra"/><family val="2"/></font>
    <!-- Font 1: B Mitra 15pt Bold for Header -->
    <font><b/><sz val="15"/><name val="B Mitra"/><family val="2"/><color rgb="FF000000"/></font>
    <!-- Font 2: B Mitra 14pt Bold -->
    <font><b/><sz val="14"/><name val="B Mitra"/><family val="2"/><color rgb="FF000000"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF5F7FA"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <!-- Border 1: Distinct black grid lines on all 4 sides -->
    <border>
      <left style="thin"><color rgb="FF000000"/></left>
      <right style="thin"><color rgb="FF000000"/></right>
      <top style="thin"><color rgb="FF000000"/></top>
      <bottom style="thin"><color rgb="FF000000"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="9">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    <!-- 1: Main Title Header (Centered, Bold 15pt, Shaded, Black Border) -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 2: Column Header (Centered, Bold 14pt, Shaded, Black Border) -->
    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 3: Data Text Center (Row number, Date, Title) with Black Border -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 4: Data Text Right (Note) with Black Border -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center" wrapText="1"/>
    </xf>
    <!-- 5: Data Amount Number (Formatted with commas) with Black Border -->
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 6: Subtotal Amount Number (Bold 14pt, Formatted with commas) with Black Border -->
    <xf numFmtId="164" fontId="2" fillId="0" borderId="1" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 7: Grand Total Label with Black Border -->
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 8: Grand Total Number with Black Border -->
    <xf numFmtId="164" fontId="2" fillId="3" borderId="1" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""
        )

        // 6. xl/worksheets/sheet1.xml
        val sheetBuilder = StringBuilder()
        sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1"/>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="10" customWidth="1"/>
    <col min="2" max="2" width="26" customWidth="1"/>
    <col min="3" max="3" width="16" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
    <col min="6" max="6" width="36" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Row 1: Header title spanning A1:F1
        sheetBuilder.append("""    <row r="1" ht="36" customHeight="1">
      <c r="A1" s="1" t="inlineStr"><is><t>${escapeXml(reportData.reportTitle)}</t></is></c>
      <c r="B1" s="1"/>
      <c r="C1" s="1"/>
      <c r="D1" s="1"/>
      <c r="E1" s="1"/>
      <c r="F1" s="1"/>
    </row>
""")

        // Row 2: Table Columns: ردیف | عنوان | تاریخ | مبلغ (تومان) | جمع جزء | توضیحات
        sheetBuilder.append("""    <row r="2" ht="30" customHeight="1">
      <c r="A2" s="2" t="inlineStr"><is><t>ردیف</t></is></c>
      <c r="B2" s="2" t="inlineStr"><is><t>عنوان</t></is></c>
      <c r="C2" s="2" t="inlineStr"><is><t>تاریخ</t></is></c>
      <c r="D2" s="2" t="inlineStr"><is><t>مبلغ (تومان)</t></is></c>
      <c r="E2" s="2" t="inlineStr"><is><t>جمع جزء</t></is></c>
      <c r="F2" s="2" t="inlineStr"><is><t>توضیحات</t></is></c>
    </row>
""")

        val mergeRefs = mutableListOf<String>()
        mergeRefs.add("A1:F1")

        var excelRowIdx = 3
        var currentGroupStartRow = -1
        var currentGroupSize = 0

        for (row in reportData.rows) {
            sheetBuilder.append("""    <row r="$excelRowIdx" ht="26" customHeight="1">
""")

            // Col A: ردیف
            sheetBuilder.append("""      <c r="A$excelRowIdx" s="3"><v>${row.rowNumber}</v></c>
""")

            // Col B: عنوان
            sheetBuilder.append("""      <c r="B$excelRowIdx" s="3" t="inlineStr"><is><t>${escapeXml(row.title)}</t></is></c>
""")

            // Col C: تاریخ
            sheetBuilder.append("""      <c r="C$excelRowIdx" s="3" t="inlineStr"><is><t>${escapeXml(row.date)}</t></is></c>
""")

            // Col D: مبلغ (تومان)
            if (row.amount != null) {
                sheetBuilder.append("""      <c r="D$excelRowIdx" s="5"><v>${row.amount}</v></c>
""")
            } else {
                sheetBuilder.append("""      <c r="D$excelRowIdx" s="5"/>
""")
            }

            // Col E: جمع جزء
            if (row.subtotal != null) {
                sheetBuilder.append("""      <c r="E$excelRowIdx" s="6"><v>${row.subtotal}</v></c>
""")
            } else {
                sheetBuilder.append("""      <c r="E$excelRowIdx" s="6"/>
""")
            }

            // Subtotal row merging tracking
            if (row.isFirstInGroup && row.groupSize > 1) {
                currentGroupStartRow = excelRowIdx
                currentGroupSize = row.groupSize
            }
            if (currentGroupStartRow > 0 && excelRowIdx == currentGroupStartRow + currentGroupSize - 1) {
                mergeRefs.add("E$currentGroupStartRow:E$excelRowIdx")
                currentGroupStartRow = -1
            }

            // Col F: توضیحات
            sheetBuilder.append("""      <c r="F$excelRowIdx" s="4" t="inlineStr"><is><t>${escapeXml(row.note)}</t></is></c>
""")

            sheetBuilder.append("""    </row>
""")
            excelRowIdx++
        }

        // Final row: جمع کل :
        val totalRowIdx = excelRowIdx
        mergeRefs.add("A$totalRowIdx:C$totalRowIdx")

        sheetBuilder.append("""    <row r="$totalRowIdx" ht="32" customHeight="1">
      <c r="A$totalRowIdx" s="7" t="inlineStr"><is><t>جمع کل :</t></is></c>
      <c r="B$totalRowIdx" s="7"/>
      <c r="C$totalRowIdx" s="7"/>
      <c r="D$totalRowIdx" s="8"><v>${reportData.grandTotal}</v></c>
      <c r="E$totalRowIdx" s="7"/>
      <c r="F$totalRowIdx" s="7"/>
    </row>
""")

        sheetBuilder.append("""  </sheetData>
""")

        // Merged cells
        if (mergeRefs.isNotEmpty()) {
            sheetBuilder.append("""  <mergeCells count="${mergeRefs.size}">
""")
            for (ref in mergeRefs) {
                sheetBuilder.append("""    <mergeCell ref="$ref"/>
""")
            }
            sheetBuilder.append("""  </mergeCells>
""")
        }

        sheetBuilder.append("""</worksheet>""")

        addEntry("xl/worksheets/sheet1.xml", sheetBuilder.toString())

        zos.finish()
        zos.close()
        return bos.toByteArray()
    }

    /**
     * Helper to wrap text according to paint and maxWidth
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    /**
     * Shares a generated file (Excel or PDF) via the Android Sharesheet.
     */
    fun shareGeneratedFile(
        context: Context,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        chooserTitle: String
    ) {
        try {
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val reportFile = File(cacheDir, fileName)
            FileOutputStream(reportFile).use { it.write(fileBytes) }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName.substringBeforeLast("."))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در اشتراک‌گذاری فایل: ${e.localizedMessage ?: "ناشناخته"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
