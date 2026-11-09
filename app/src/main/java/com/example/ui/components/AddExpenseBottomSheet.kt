package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.repository.FolderMatcher
import com.example.utils.PersianDateHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddExpenseBottomSheet(
    onDismissRequest: () -> Unit,
    onAddExpense: (
        title: String,
        amount: Long,
        isAnnual: Boolean,
        customFolder: String?,
        shamsiDateString: String,
        note: String
    ) -> Unit,
    initialShamsiDate: String = PersianDateHelper.getShamsiDate().format(),
    expenseToEdit: ExpenseEntity? = null,
    onUpdateExpense: ((ExpenseEntity) -> Unit)? = null,
    availableFolders: List<String> = emptyList(),
    initialIsAnnual: Boolean = false,
    initialAmount: Long? = null
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("expense_suggestions_prefs", android.content.Context.MODE_PRIVATE)
    }

    val defaultItems = remember {
        listOf(
            "خرید سوپرمارکت", "بنزین", "نانوایی", "رستوران",
            "اجاره مسکن", "قبوض", "بیمه خودرو", "بیمه موتورسیکلت", "بیمه تکمیلی",
            "اینترنت", "خرید میوه", "درمان",
            "موتور", "فاطمه", "علی", "خیریه"
        )
    }

    var suggestionItems by remember {
        val saved = prefs.getString("items_list", null)
        val initialList = if (!saved.isNullOrBlank()) {
            val list = saved.split(";;;").filter { it.isNotBlank() }.toMutableList()
            list.removeAll {
                it == "هزینه های نگهداری خودرو" || it == "هزینه های نگهداری ماشین" ||
                it == "هزینه‌های نگهداری ماشین" || it == "هزینه‌های نگهداری خودرو"
            }
            if (list.contains("اجاره و قبوض")) {
                val idx = list.indexOf("اجاره و قبوض")
                list.removeAt(idx)
                if (!list.contains("اجاره مسکن")) list.add(idx, "اجاره مسکن")
                if (!list.contains("قبوض")) list.add(idx + 1, "قبوض")
            }
            if (!list.contains("اجاره مسکن")) {
                list.add("اجاره مسکن")
            }
            if (!list.contains("قبوض")) {
                list.add("قبوض")
            }
            // Replace legacy "پول فاطمه" -> "فاطمه" and "پول علی" -> "علی"
            if (list.contains("پول فاطمه")) {
                val idx = list.indexOf("پول فاطمه")
                list[idx] = "فاطمه"
            } else if (!list.contains("فاطمه")) {
                list.add("فاطمه")
            }
            if (list.contains("پول علی")) {
                val idx = list.indexOf("پول علی")
                list[idx] = "علی"
            } else if (!list.contains("علی")) {
                list.add("علی")
            }
            list.distinct()
        } else {
            defaultItems
        }
        mutableStateOf(initialList)
    }

    val isEditing = expenseToEdit != null
    var title by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountString by remember(expenseToEdit, initialAmount) {
        mutableStateOf(
            expenseToEdit?.amount?.toString()
                ?: (if (initialAmount != null && initialAmount > 0) initialAmount.toString() else "")
        )
    }
    var isAnnual by remember(expenseToEdit, initialIsAnnual) {
        mutableStateOf(expenseToEdit?.isAnnual ?: initialIsAnnual)
    }
    var customFolder by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.customFolder ?: "") }
    var selectedDateString by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.shamsiDateString ?: initialShamsiDate)
    }
    var note by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Auto-detect insurance and annual commitments to ensure 12-month amortization in all reports
    androidx.compose.runtime.LaunchedEffect(title, customFolder) {
        val t = title.trim()
        val c = customFolder.trim()
        if (t.contains("بیمه") || t.contains("عوارض") || t.contains("سالانه") || c.contains("بیمه") || c.contains("سالانه")) {
            isAnnual = true
        }
    }

    val amountLong = amountString.toLongOrNull() ?: 0L

    fun saveSuggestions(list: List<String>) {
        suggestionItems = list
        prefs.edit().putString("items_list", list.joinToString(";;;")).apply()
    }

    fun moveSuggestionItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in suggestionItems.indices && toIndex in suggestionItems.indices && fromIndex != toIndex) {
            val list = suggestionItems.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            saveSuggestions(list)
        }
    }

    fun moveItemHigherRow(index: Int) {
        val target = maxOf(0, index - 3)
        if (target != index) {
            moveSuggestionItem(index, target)
        } else if (index > 0) {
            moveSuggestionItem(index, 0)
        }
    }

    fun moveItemLowerRow(index: Int) {
        val target = minOf(suggestionItems.size - 1, index + 3)
        if (target != index) {
            moveSuggestionItem(index, target)
        } else if (index < suggestionItems.size - 1) {
            moveSuggestionItem(index, suggestionItems.size - 1)
        }
    }

    fun moveItemToFirst(index: Int) {
        if (index > 0 && index in suggestionItems.indices) {
            val list = suggestionItems.toMutableList()
            val item = list.removeAt(index)
            list.add(0, item)
            saveSuggestions(list)
        }
    }

    fun moveItemToLast(index: Int) {
        if (index < suggestionItems.size - 1 && index in suggestionItems.indices) {
            val list = suggestionItems.toMutableList()
            val item = list.removeAt(index)
            list.add(item)
            saveSuggestions(list)
        }
    }

    fun deleteSuggestionItem(index: Int) {
        if (index in suggestionItems.indices) {
            val list = suggestionItems.toMutableList()
            val removed = list.removeAt(index)
            if (title == removed) {
                title = ""
            }
            saveSuggestions(list)
        }
    }

    var isReorderingOnScreen by remember { mutableStateOf(false) }
    var isManagingItems by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var itemActionDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showManageOrderDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    var editItemText by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentView = LocalView.current
    DisposableEffect(currentView) {
        currentView.isVerticalScrollBarEnabled = false
        currentView.isHorizontalScrollBarEnabled = false
        currentView.rootView?.isVerticalScrollBarEnabled = false
        currentView.rootView?.isHorizontalScrollBarEnabled = false
        onDispose {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_expense_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEditing) "ویرایش هزینه" else "ثبت هزینه جدید",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.contains("بیمه") || it.contains("عوارض سالانه")) {
                        isAnnual = true
                    }
                },
                label = { Text("عنوان هزینه (مثال: بنزین، سوپرمارکت، بیمه ماشین)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Compact spacing between title input and suggestions card
            Spacer(modifier = Modifier.height(8.dp))

            // Beautiful styled Card for "آیتم‌های پیشنهادی و پیش‌فرض"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("suggestions_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header: Horizontal Green Title and + Add Item button (no management tab, no gray text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green text written horizontally
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFECFDF5),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "آیتم‌های پیشنهادی",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Quick + Add Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    newItemText = ""
                                    showAddItemDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "افزودن آیتم",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick suggestion white cards FlowRow with on-screen delete, edit, higher/lower row reordering
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestionItems.forEachIndexed { index, suggestion ->
                            val isSelected = title == suggestion

                            if (isReorderingOnScreen) {
                                // Reordering & Management Mode: Direct Edit, Delete, Higher row, Lower row controls
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Line 1: Title and Quick Edit / Delete
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = suggestion,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Edit button (✏️)
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFEFF6FF),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            itemToEdit = Pair(index, suggestion)
                                                            editItemText = suggestion
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "ویرایش",
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }

                                                // Delete button (🗑️)
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFFEE2E2),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            deleteSuggestionItem(index)
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "حذف",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(5.dp))

                                        // Line 2: [▲ ردیف بالا] [▼ ردیف پایین] [◀] [▶]
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Move to Higher Row (▲)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (index > 0) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable(enabled = index > 0) {
                                                        moveItemHigherRow(index)
                                                    }
                                            ) {
                                                Text(
                                                    text = "▲ ردیف بالا",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (index > 0) Color(0xFF047857) else Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Move to Lower Row (▼)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (index < suggestionItems.size - 1) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable(enabled = index < suggestionItems.size - 1) {
                                                        moveItemLowerRow(index)
                                                    }
                                            ) {
                                                Text(
                                                    text = "▼ ردیف پایین",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (index < suggestionItems.size - 1) Color(0xFF047857) else Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Step prev (◀)
                                            Surface(
                                                shape = CircleShape,
                                                color = if (index > 0) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clickable(enabled = index > 0) {
                                                        moveSuggestionItem(index, index - 1)
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "◀",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (index > 0) Color(0xFF2563EB) else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }

                                            // Step next (▶)
                                            Surface(
                                                shape = CircleShape,
                                                color = if (index < suggestionItems.size - 1) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clickable(enabled = index < suggestionItems.size - 1) {
                                                        moveSuggestionItem(index, index + 1)
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "▶",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (index < suggestionItems.size - 1) Color(0xFF2563EB) else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Normal mode: White card with selection + options button for edit/delete/reorder
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFECFDF5) else Color.White,
                                    shadowElevation = if (isSelected) 3.dp else 1.5.dp,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF059669) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            title = suggestion
                                            if (suggestion.contains("بیمه") || suggestion.contains("عوارض سالانه")) {
                                                isAnnual = true
                                            }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF047857) else Color(0xFF1E293B)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Options menu button for editing, deleting, or moving
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFFD1FAE5) else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .size(19.dp)
                                                .clickable {
                                                    itemActionDialog = Pair(index, suggestion)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "عملیات آیتم",
                                                    tint = if (isSelected) Color(0xFF047857) else Color(0xFF64748B),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount Field
            OutlinedTextField(
                value = amountString,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    amountString = filtered
                },
                label = { Text("مبلغ به تومان") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                supportingText = {
                    if (amountLong > 0) {
                        Text(
                            text = "مبلغ: ${PersianDateHelper.formatCurrency(amountLong)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date Picker Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showDatePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تاریخ ثبت هزینه:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = PersianDateHelper.toPersianDigits(selectedDateString),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "تغییر تاریخ",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Annual Amortized Expense Toggle (Requirement 7)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAnnual) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (isAnnual) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isAnnual) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "هزینه سالانه (بیمه ماشین، موتور، درمان)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAnnual) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "سرشکن شدن مبلغ به ۱۲ ماه سال",
                                    fontSize = 11.sp,
                                    color = if (isAnnual) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAnnual,
                            onCheckedChange = { isAnnual = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD97706),
                                checkedTrackColor = Color(0xFFFDE68A)
                            ),
                            modifier = Modifier.testTag("is_annual_switch")
                        )
                    }

                    AnimatedVisibility(visible = isAnnual) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .padding(10.dp)
                            ) {
                                val monthlyShare = if (amountLong > 0) amountLong / 12 else 0L
                                Text(
                                    text = "این هزینه در پوشه «هزینه‌های سالانه» قرار می‌گیرد و سهم ماهانه آن (${PersianDateHelper.formatCurrency(monthlyShare)}) به صورت خودکار به عنوان هزینه ثابت ماهانه در گزارش‌های هر ماه محاسبه خواهد شد.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Note Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("یادداشت یا توضیحات (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "لطفاً عنوان هزینه را وارد کنید"
                        return@Button
                    }
                    if (amountLong <= 0) {
                        errorMessage = "لطفاً مبلغ معتبری وارد کنید"
                        return@Button
                    }

                    val cleanDate = PersianDateHelper.toEnglishDigits(selectedDateString)
                    val parts = cleanDate.split("/")
                    val cleanedFolder = customFolder.takeIf { it.isNotBlank() }?.let {
                        val c = FolderMatcher.cleanFolderName(it)
                        if (c.isNotBlank() && FolderMatcher.getCanonicalGroupId(it) == FolderMatcher.getCanonicalGroupId(c)) c else it
                    }

                    if (isEditing && expenseToEdit != null) {
                        val y = parts.getOrNull(0)?.toIntOrNull() ?: expenseToEdit.shamsiYear
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: expenseToEdit.shamsiMonth
                        val d = parts.getOrNull(2)?.toIntOrNull() ?: expenseToEdit.shamsiDay
                        val mPadded = if (m < 10) "0$m" else "$m"
                        val dPadded = if (d < 10) "0$d" else "$d"

                        val updated = expenseToEdit.copy(
                            title = title.trim(),
                            amount = amountLong,
                            isAnnual = isAnnual,
                            customFolder = cleanedFolder,
                            shamsiYear = y,
                            shamsiMonth = m,
                            shamsiDay = d,
                            shamsiDateString = "$y/$mPadded/$dPadded",
                            note = note.trim()
                        )
                        onUpdateExpense?.invoke(updated)
                    } else {
                        onAddExpense(
                            title,
                            amountLong,
                            isAnnual,
                            cleanedFolder,
                            cleanDate,
                            note
                        )
                    }
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_expense_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "ذخیره تغییرات" else "ثبت هزینه",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val sClean = PersianDateHelper.toEnglishDigits(selectedDateString)
        val parts = sClean.split("/")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().year
        val m = parts.getOrNull(1)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().month
        val d = parts.getOrNull(2)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().day

        PersianDatePickerDialog(
            initialYear = y,
            initialMonth = m,
            initialDay = d,
            onDateSelected = { year, month, day, formatted ->
                selectedDateString = formatted
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    // Add New Suggestion Item Dialog
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = {
                Text(
                    text = "افزودن آیتم جدید به لیست",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "عنوان آیتم جدید را وارد کنید تا به دکمه‌های سریع اضافه شود:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        placeholder = { Text("مثال: کرایه تاکسی، میوه") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newItemText.trim()
                        if (trimmed.isNotBlank() && !suggestionItems.contains(trimmed)) {
                            val updated = suggestionItems.toMutableList().apply { add(trimmed) }
                            saveSuggestions(updated)
                            title = trimmed
                            if (trimmed.contains("بیمه") || trimmed.contains("عوارض سالانه")) {
                                isAnnual = true
                            }
                        }
                        showAddItemDialog = false
                    },
                    enabled = newItemText.trim().isNotBlank()
                ) {
                    Text("افزودن و انتخاب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Edit Existing Suggestion Item Dialog
    itemToEdit?.let { (editIndex, currentItemTitle) ->
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = {
                Text(
                    text = "ویرایش یا حذف آیتم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "می‌توانید عنوان این آیتم را تغییر داده یا آن را به طور کامل حذف کنید:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editItemText,
                        onValueChange = { editItemText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            if (editIndex in suggestionItems.indices) {
                                val updated = suggestionItems.toMutableList().apply { removeAt(editIndex) }
                                saveSuggestions(updated)
                            }
                            itemToEdit = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف آیتم")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val trimmed = editItemText.trim()
                            if (trimmed.isNotBlank() && editIndex in suggestionItems.indices) {
                                val updated = suggestionItems.toMutableList().apply {
                                    set(editIndex, trimmed)
                                }
                                saveSuggestions(updated)
                            }
                            itemToEdit = null
                        },
                        enabled = editItemText.trim().isNotBlank()
                    ) {
                        Text("ذخیره تغییرات")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Single Item Quick Actions Dialog (ویرایش، حذف، جابجایی ردیف بالا، ردیف پایین، اول، آخر)
    itemActionDialog?.let { (actionIndex, actionTitle) ->
        AlertDialog(
            onDismissRequest = { itemActionDialog = null },
            title = {
                Text(
                    text = "تنظیمات آیتم «$actionTitle»",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "عملیات مورد نظر را انتخاب کنید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. ویرایش عنوان
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val item = actionTitle
                                itemActionDialog = null
                                itemToEdit = Pair(actionIndex, item)
                                editItemText = item
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ویرایش نام آیتم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        }
                    }

                    // 2. انتقال به یک ردیف بالاتر (▲)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = actionIndex > 0) {
                                moveItemHigherRow(actionIndex)
                                itemActionDialog = null
                            },
                        colors = CardDefaults.cardColors(containerColor = if (actionIndex > 0) Color(0xFFECFDF5) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (actionIndex > 0) Color(0xFF047857) else Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("انتقال به ردیف بالاتر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (actionIndex > 0) Color(0xFF065F46) else Color.Gray)
                        }
                    }

                    // 3. انتقال به یک ردیف پایین‌تر (▼)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = actionIndex < suggestionItems.size - 1) {
                                moveItemLowerRow(actionIndex)
                                itemActionDialog = null
                            },
                        colors = CardDefaults.cardColors(containerColor = if (actionIndex < suggestionItems.size - 1) Color(0xFFECFDF5) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("▼", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (actionIndex < suggestionItems.size - 1) Color(0xFF047857) else Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("انتقال به ردیف پایین‌تر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (actionIndex < suggestionItems.size - 1) Color(0xFF065F46) else Color.Gray)
                        }
                    }

                    // 4. انتقال به ابتدای لیست (ردیف اول)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = actionIndex > 0) {
                                moveItemToFirst(actionIndex)
                                itemActionDialog = null
                            },
                        colors = CardDefaults.cardColors(containerColor = if (actionIndex > 0) Color(0xFFF0FDF4) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏮", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (actionIndex > 0) Color(0xFF15803D) else Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("انتقال به ابتدای لیست (ردیف اول)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (actionIndex > 0) Color(0xFF15803D) else Color.Gray)
                        }
                    }

                    // 5. انتقال به انتهای لیست (ردیف آخر)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = actionIndex < suggestionItems.size - 1) {
                                moveItemToLast(actionIndex)
                                itemActionDialog = null
                            },
                        colors = CardDefaults.cardColors(containerColor = if (actionIndex < suggestionItems.size - 1) Color(0xFFFFFBEB) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏭", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (actionIndex < suggestionItems.size - 1) Color(0xFFB45309) else Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("انتقال به انتهای لیست (ردیف آخر)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (actionIndex < suggestionItems.size - 1) Color(0xFFB45309) else Color.Gray)
                        }
                    }

                    // 6. حذف آیتم
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                deleteSuggestionItem(actionIndex)
                                itemActionDialog = null
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("حذف این آیتم از لیست", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { itemActionDialog = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Manage and Reorder Suggestion Items Dialog
    if (showManageOrderDialog) {
        var addInputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManageOrderDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدیریت و اولویت‌بندی آیتم‌ها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                saveSuggestions(defaultItems)
                            }
                    ) {
                        Text(
                            text = "بازنشانی",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "می‌توانید آیتم دلخواه اضافه یا ویرایش کنید و هر کدام را به ردیف بالا یا پایین جابجا نمایید:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Input to add new item directly
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = addInputText,
                            onValueChange = { addInputText = it },
                            placeholder = { Text("عنوان جدید (مثال: پول علی)", fontSize = 11.5.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val trimmed = addInputText.trim()
                                if (trimmed.isNotBlank() && !suggestionItems.contains(trimmed)) {
                                    val list = suggestionItems.toMutableList()
                                    list.add(0, trimmed) // Add to top
                                    saveSuggestions(list)
                                    addInputText = ""
                                }
                            },
                            enabled = addInputText.trim().isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("افزودن", fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable list of all items with edit, reordering, and delete buttons
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(suggestionItems) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = PersianDateHelper.toPersianDigits(index + 1),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Action buttons: [✏️] [اول] [▲] [▼] [آخر] [🗑]
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Edit Button (✏️)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEFF6FF),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    itemToEdit = Pair(index, item)
                                                    editItemText = item
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "ویرایش",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(3.dp)
                                            )
                                        }

                                        // Move to First position (ردیف اول)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (index > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = index > 0) {
                                                    moveItemToFirst(index)
                                                }
                                        ) {
                                            Text(
                                                text = "اول",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index > 0) Color(0xFF15803D) else Color.Gray,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                            )
                                        }

                                        // Move to Higher Row (▲)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (index > 0) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = index > 0) {
                                                    moveItemHigherRow(index)
                                                }
                                        ) {
                                            Text(
                                                text = "▲",
                                                fontSize = 11.sp,
                                                color = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Move to Lower Row (▼)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (index < suggestionItems.size - 1) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = index < suggestionItems.size - 1) {
                                                    moveItemLowerRow(index)
                                                }
                                        ) {
                                            Text(
                                                text = "▼",
                                                fontSize = 11.sp,
                                                color = if (index < suggestionItems.size - 1) MaterialTheme.colorScheme.primary else Color.Gray,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Move to Last position (ردیف آخر)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (index < suggestionItems.size - 1) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = index < suggestionItems.size - 1) {
                                                    moveItemToLast(index)
                                                }
                                        ) {
                                            Text(
                                                text = "آخر",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index < suggestionItems.size - 1) Color(0xFFB45309) else Color.Gray,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                            )
                                        }

                                        // Delete item (🗑)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEE2E2),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    deleteSuggestionItem(index)
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showManageOrderDialog = false }) {
                    Text("بستن و تایید")
                }
            }
        )
    }
}
