package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.ExpenseEntity
import com.example.data.repository.FolderMatcher
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper
import com.example.utils.ReceiptImageHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddExpenseBottomSheet(
    onDismissRequest: () -> Unit,
    onAddExpense: (
        title: String,
        amount: Long,
        isAnnual: Boolean,
        customFolder: String?,
        shamsiDateString: String,
        note: String,
        receiptImagePath: String?
    ) -> Unit,
    initialShamsiDate: String = PersianDateHelper.getShamsiDate().format(),
    expenseToEdit: ExpenseEntity? = null,
    onUpdateExpense: ((ExpenseEntity) -> Unit)? = null,
    availableFolders: List<String> = emptyList(),
    initialIsAnnual: Boolean = false,
    initialAmount: Long? = null,
    initialTitle: String? = null,
    initialReceiptPath: String? = null
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
            "موتور", "خیریه"
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
            val cleaned = list.filterNot {
                val trimmed = it.trim()
                trimmed == "فاطمه" || trimmed == "علی" || trimmed == "پول فاطمه" || trimmed == "پول علی"
            }.distinct()
            if (!prefs.getBoolean("migrated_remove_ali_fatemeh_v1", false)) {
                prefs.edit()
                    .putString("items_list", cleaned.joinToString(";;;"))
                    .putBoolean("migrated_remove_ali_fatemeh_v1", true)
                    .apply()
            }
            cleaned
        } else {
            defaultItems
        }
        mutableStateOf(initialList)
    }

    val isEditing = expenseToEdit != null
    var title by remember(expenseToEdit, initialTitle) {
        mutableStateOf(expenseToEdit?.title ?: initialTitle ?: "")
    }
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
    var selectedDateString by remember(expenseToEdit, initialShamsiDate) {
        mutableStateOf(expenseToEdit?.shamsiDateString ?: initialShamsiDate)
    }
    var note by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.note ?: "") }
    var receiptImagePath by remember(expenseToEdit, initialReceiptPath) {
        mutableStateOf(expenseToEdit?.receiptImagePath ?: initialReceiptPath)
    }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showReceiptPreview by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val sheetTakePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            receiptImagePath = file.absolutePath
            Toast.makeText(context, "تصویر رسید با دوربین ثبت شد", Toast.LENGTH_SHORT).show()
        } else {
            file?.delete()
        }
        pendingCameraFile = null
    }

    fun startSheetCameraCapture() {
        try {
            val (file, uri) = ReceiptImageHelper.createCameraCaptureTarget(context)
            pendingCameraFile = file
            sheetTakePictureLauncher.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(context, "امکان باز کردن دوربین وجود ندارد", Toast.LENGTH_SHORT).show()
        }
    }

    val sheetCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSheetCameraCapture()
        } else {
            Toast.makeText(context, "مجوز دوربین برای عکسبرداری از رسید لازم است", Toast.LENGTH_SHORT).show()
        }
    }

    val sheetPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ReceiptImageHelper.copyUriToInternalReceiptFile(context, uri)
            if (savedPath != null) {
                receiptImagePath = savedPath
                Toast.makeText(context, "تصویر رسید از گالری انتخاب شد", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        val target = maxOf(0, index - 2)
        if (target != index) {
            moveSuggestionItem(index, target)
        } else if (index > 0) {
            moveSuggestionItem(index, 0)
        }
    }

    fun moveItemLowerRow(index: Int) {
        val target = minOf(suggestionItems.size - 1, index + 2)
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
        ScaledDialogContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaledDp(16f))
                .padding(top = scaledDp(16f), bottom = scaledDp(32f))
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
                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .heightIn(min = scaledDp(48f))
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("sheet_voice_input_button")
                            .clickable { showVoiceDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = scaledDp(12f), vertical = scaledDp(8f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "ثبت هزینه با صدا",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(scaledDp(18f))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ورودی صوتی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(scaledDp(36f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(scaledDp(20f))
                            )
                        }
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

            // Vibrant, uniform 2-column Card for "آیتم‌های پیشنهادی"
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("suggestions_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.72f)
                    else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(
                    1.3.dp,
                    if (isDark) Color(0xFF38BDF8).copy(alpha = 0.45f)
                    else Color(0xFF0EA5E9).copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 11.dp)) {
                    // Header: Title and + Add Item button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "آیتم‌های پیشنهادی",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                softWrap = true
                            )
                        }

                        // Quick + Add Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("add_suggestion_item_button")
                                .clickable {
                                    newItemText = ""
                                    showAddItemDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "افزودن آیتم جدید",
                                    tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "افزودن آیتم",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Uniform 2-column grid of vibrant colorful suggestion cards
                    val indexedRows = suggestionItems.withIndex().chunked(2)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        indexedRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                rowItems.forEach { indexed ->
                                    val index = indexed.index
                                    val suggestion = indexed.value
                                    val isSelected = title == suggestion
                                    val style = remember(suggestion, index, isDark) {
                                        resolveSuggestionCardStyle(suggestion, index, isDark)
                                    }

                                    if (isReorderingOnScreen) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = style.bgStart,
                                            border = BorderStroke(1.3.dp, style.border),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .heightIn(min = scaledDp(56f))
                                                .clip(RoundedCornerShape(14.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = scaledDp(8f), vertical = scaledDp(6f)),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = suggestion,
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = style.titleColor,
                                                        textAlign = TextAlign.Start,
                                                        softWrap = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFFEFF6FF),
                                                            modifier = Modifier
                                                                .size(scaledDp(24f))
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
                                                                    modifier = Modifier.size(scaledDp(13f))
                                                                )
                                                            }
                                                        }

                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFFFEE2E2),
                                                            modifier = Modifier
                                                                .size(scaledDp(24f))
                                                                .clickable { deleteSuggestionItem(index) }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "حذف",
                                                                    tint = Color(0xFFDC2626),
                                                                    modifier = Modifier.size(scaledDp(13f))
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(5.dp))

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
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
                                                            text = "▲ بالا",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (index > 0) Color(0xFF047857) else Color(0xFF94A3B8),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                                        )
                                                    }

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
                                                            text = "▼ پایین",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (index < suggestionItems.size - 1) Color(0xFF047857) else Color(0xFF94A3B8),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                                        )
                                                    }

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
                                        // Normal mode: Uniform symmetric gradient card with category icon & vibrant styling
                                        val gradientColors = if (isSelected) {
                                            listOf(
                                                style.accent.copy(alpha = if (isDark) 0.45f else 0.24f),
                                                style.bgEnd
                                            )
                                        } else {
                                            listOf(style.bgStart, style.bgEnd)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = style.bgStart,
                                            shadowElevation = if (isSelected) 4.dp else 1.5.dp,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.2.dp else 1.2.dp,
                                                color = if (isSelected) style.accent else style.border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .heightIn(min = scaledDp(54f))
                                                .clip(RoundedCornerShape(14.dp))
                                                .testTag("suggestion_card_$index")
                                                .clickable {
                                                    title = suggestion
                                                    if (suggestion.contains("بیمه") || suggestion.contains("عوارض سالانه")) {
                                                        isAnnual = true
                                                    }
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.linearGradient(colors = gradientColors))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(start = scaledDp(6f), end = scaledDp(2f), top = scaledDp(7f), bottom = scaledDp(7f))
                                                ) {
                                                    // Category Icon Badge
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isSelected) style.accent else style.accent.copy(alpha = if (isDark) 0.26f else 0.16f),
                                                        modifier = Modifier.size(scaledDp(22f))
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = if (isSelected) Icons.Default.Check else style.icon,
                                                                contentDescription = null,
                                                                tint = if (isSelected) Color.White else style.accent,
                                                                modifier = Modifier.size(scaledDp(12.5f))
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(5.dp))

                                                    Text(
                                                        text = suggestion,
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                        color = style.titleColor,
                                                        textAlign = TextAlign.Start,
                                                        softWrap = true,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    // Tinted Options Button (⋮)
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .width(30.dp)
                                                            .height(38.dp)
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                itemActionDialog = Pair(index, suggestion)
                                                            }
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = style.accent.copy(alpha = if (isDark) 0.24f else 0.15f),
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.MoreVert,
                                                                    contentDescription = "عملیات آیتم",
                                                                    tint = style.accent,
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

                                // Keep trailing odd item aligned to exact 50% width
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
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
                    val filtered = PersianDateHelper.toEnglishDigits(input).filter { it.isDigit() }
                    amountString = filtered
                },
                label = { Text("مبلغ به تومان") },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = OutfitFontFamily),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                supportingText = {
                    if (amountLong > 0) {
                        Text(
                            text = enDigits("مبلغ: ${PersianDateHelper.formatCurrency(amountLong)}"),
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
                        .padding(scaledDp(14f)),
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
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = PersianDateHelper.toEnglishDigits(selectedDateString),
                                fontFamily = OutfitFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface)
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
                Column(modifier = Modifier.padding(scaledDp(14f))) {
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
                                modifier = Modifier.size(scaledDp(22f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "هزینه سالانه (بیمه ماشین، موتور، درمان)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAnnual) Color(0xFF78350F) else appTextColor(MaterialTheme.colorScheme.onSurface)
                                )
                                Text(
                                    text = enDigits("سرشکن شدن مبلغ به 12 ماه سال"),
                                    fontSize = 11.sp,
                                    color = if (isAnnual) Color(0xFF92400E) else appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    text = enDigits("این هزینه در پوشه «هزینه‌های سالانه» قرار می‌گیرد و سهم ماهانه آن (${PersianDateHelper.formatCurrency(monthlyShare)}) به صورت خودکار به عنوان هزینه ثابت ماهانه در گزارش‌های هر ماه محاسبه خواهد شد."),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Receipt Image Attachment Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!receiptImagePath.isNullOrBlank()) {
                        Color(0xFF10B981).copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!receiptImagePath.isNullOrBlank()) Color(0xFF10B981).copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصویر رسید بانکی / فاکتور (اختیاری)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!receiptImagePath.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showReceiptPreview = true }
                            ) {
                                AsyncImage(
                                    model = File(receiptImagePath!!),
                                    contentDescription = "تصویر رسید پیوست‌شده",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "تصویر رسید پیوست شد",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "برای بزرگ‌نمایی لمس کنید",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { receiptImagePath = null },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف تصویر رسید",
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    startSheetCameraCapture()
                                } else {
                                    sheetCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("sheet_receipt_camera_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("دوربین", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                sheetPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("sheet_receipt_gallery_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("گالری", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

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
                            note = note.trim(),
                            receiptImagePath = receiptImagePath?.takeIf { it.isNotBlank() }
                        )
                        onUpdateExpense?.invoke(updated)
                    } else {
                        onAddExpense(
                            title,
                            amountLong,
                            isAnnual,
                            cleanedFolder,
                            cleanDate,
                            note,
                            receiptImagePath?.takeIf { it.isNotBlank() }
                        )
                    }
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scaledDp(52f))
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
    }

    if (showReceiptPreview && !receiptImagePath.isNullOrBlank()) {
        ReceiptPreviewDialog(
            expense = ExpenseEntity(
                title = title.ifBlank { "رسید جدید" },
                amount = amountLong,
                shamsiYear = 1405,
                shamsiMonth = 1,
                shamsiDay = 1,
                shamsiDateString = selectedDateString,
                receiptImagePath = receiptImagePath
            ),
            onDismissRequest = { showReceiptPreview = false }
        )
    }

    if (showVoiceDialog) {
        VoiceExpenseDialog(
            onDismissRequest = { showVoiceDialog = false },
            onApplyToForm = { parsed ->
                if (parsed.title.isNotBlank()) {
                    title = parsed.title
                }
                if (parsed.amountTomans > 0L) {
                    amountString = parsed.amountTomans.toString()
                }
                selectedDateString = parsed.shamsiDateString
                if (parsed.isAnnual) {
                    isAnnual = true
                }
                errorMessage = ""
                showVoiceDialog = false
            }
        )
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
                ScaledDialogContent {
                    Text(
                        text = "افزودن آیتم جدید به لیست",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Column {
                        Text(
                            text = "عنوان آیتم جدید را وارد کنید تا به دکمه‌های سریع اضافه شود:",
                            fontSize = 13.sp,
                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
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
                }
            },
            confirmButton = {
                ScaledDialogContent {
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
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    TextButton(onClick = { showAddItemDialog = false }) {
                        Text("انصراف")
                    }
                }
            }
        )
    }

    // Edit Existing Suggestion Item Dialog
    itemToEdit?.let { (editIndex, currentItemTitle) ->
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = {
                ScaledDialogContent {
                    Text(
                        text = "ویرایش یا حذف آیتم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Column {
                        Text(
                            text = "می‌توانید عنوان این آیتم را تغییر داده یا آن را به طور کامل حذف کنید:",
                            fontSize = 13.sp,
                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
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
                }
            },
            confirmButton = {
                ScaledDialogContent {
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
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    TextButton(onClick = { itemToEdit = null }) {
                        Text("انصراف")
                    }
                }
            }
        )
    }

    // Single Item Quick Actions Dialog (ویرایش، حذف، جابجایی ردیف بالا، ردیف پایین، اول، آخر)
    itemActionDialog?.let { (actionIndex, actionTitle) ->
        AlertDialog(
            onDismissRequest = { itemActionDialog = null },
            title = {
                ScaledDialogContent {
                    Text(
                        text = "تنظیمات آیتم «$actionTitle»",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                ScaledDialogContent {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "عملیات مورد نظر را انتخاب کنید:",
                        fontSize = 12.sp,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
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
                }
            },
            confirmButton = {},
            dismissButton = {
                ScaledDialogContent {
                    TextButton(onClick = { itemActionDialog = null }) {
                        Text("بستن")
                    }
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
                ScaledDialogContent {
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
                }
            },
            text = {
                ScaledDialogContent {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = scaledDp(420f))
                ) {
                    Text(
                        text = "می‌توانید آیتم دلخواه اضافه یا ویرایش کنید و هر کدام را به ردیف بالا یا پایین جابجا نمایید:",
                        fontSize = 11.5.sp,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
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
                            placeholder = { Text("عنوان جدید (مثال: پارکینگ)", fontSize = 11.5.sp) },
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
                                            modifier = Modifier.size(scaledDp(20f))
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontFamily = OutfitFontFamily,
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
                                            color = appTextColor(MaterialTheme.colorScheme.onSurface)
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
                }
            },
            confirmButton = {
                ScaledDialogContent {
                    Button(onClick = { showManageOrderDialog = false }) {
                        Text("بستن و تایید")
                    }
                }
            }
        )
    }
}

private data class SuggestionCardStyle(
    val bgStart: Color,
    val bgEnd: Color,
    val border: Color,
    val accent: Color,
    val titleColor: Color,
    val icon: ImageVector
)

private fun resolveSuggestionCardStyle(
    title: String,
    index: Int,
    isDark: Boolean
): SuggestionCardStyle {
    val normalized = title.trim()

    fun palette(
        darkStart: Long,
        darkEnd: Long,
        darkBorder: Long,
        darkAccent: Long,
        darkTitle: Long,
        lightStart: Long,
        lightEnd: Long,
        lightBorder: Long,
        lightAccent: Long,
        lightTitle: Long,
        icon: ImageVector
    ): SuggestionCardStyle {
        return if (isDark) {
            SuggestionCardStyle(
                bgStart = Color(darkStart),
                bgEnd = Color(darkEnd),
                border = Color(darkBorder),
                accent = Color(darkAccent),
                titleColor = Color(darkTitle),
                icon = icon
            )
        } else {
            SuggestionCardStyle(
                bgStart = Color(lightStart),
                bgEnd = Color(lightEnd),
                border = Color(lightBorder),
                accent = Color(lightAccent),
                titleColor = Color(lightTitle),
                icon = icon
            )
        }
    }

    return when {
        normalized.contains("سوپرمارکت") || normalized.contains("سوپر مارکت") || normalized.contains("لبنیات") ->
            palette(
                0xFF064E3B, 0xFF065F46, 0xFF34D399, 0xFF34D399, 0xFFECFDF5,
                0xFFECFDF5, 0xFFD1FAE5, 0xFF34D399, 0xFF059669, 0xFF064E3B,
                Icons.Default.ShoppingCart
            )

        normalized.contains("نان") ->
            palette(
                0xFF78350F, 0xFF92400E, 0xFFFBBF24, 0xFFFBBF24, 0xFFFEF3C7,
                0xFFFFFBEB, 0xFFFEF3C7, 0xFFF59E0B, 0xFFD97706, 0xFF78350F,
                Icons.Default.BakeryDining
            )

        normalized.contains("تنقلات") || normalized.contains("فست") || normalized.contains("کافه") ->
            palette(
                0xFF831843, 0xFF9D174D, 0xFFF472B6, 0xFFF472B6, 0xFFFCE7F3,
                0xFFFDF2F8, 0xFFFCE7F3, 0xFFF472B6, 0xFFDB2777, 0xFF831843,
                Icons.Default.Fastfood
            )

        normalized.contains("میوه") || normalized.contains("سبزی") ->
            palette(
                0xFF14532D, 0xFF166534, 0xFF4ADE80, 0xFF4ADE80, 0xFFDCFCE7,
                0xFFF0FDF4, 0xFFDCFCE7, 0xFF4ADE80, 0xFF16A34A, 0xFF14532D,
                Icons.Default.Eco
            )

        normalized.contains("رستوران") || normalized.contains("مرغ") || normalized.contains("گوشت") || normalized.contains("غذا") ->
            palette(
                0xFF7C2D12, 0xFF9A3412, 0xFFFB923C, 0xFFFB923C, 0xFFFFEDD5,
                0xFFFFF7ED, 0xFFFFEDD5, 0xFFFB923C, 0xFFEA580C, 0xFF7C2D12,
                Icons.Default.Restaurant
            )

        normalized.contains("اجاره") || normalized.contains("مسکن") || normalized.contains("خانه") ->
            palette(
                0xFF312E81, 0xFF3730A3, 0xFF818CF8, 0xFF818CF8, 0xFFE0E7FF,
                0xFFEEF2FF, 0xFFE0E7FF, 0xFF818CF8, 0xFF4F46E5, 0xFF312E81,
                Icons.Default.Home
            )

        normalized.contains("بنزین") || normalized.contains("سوخت") ->
            palette(
                0xFF7F1D1D, 0xFF991B1B, 0xFFF87171, 0xFFF87171, 0xFFFEE2E2,
                0xFFFEF2F2, 0xFFFEE2E2, 0xFFF87171, 0xFFDC2626, 0xFF7F1D1D,
                Icons.Default.LocalGasStation
            )

        normalized.contains("درمان") || normalized.contains("دارو") || normalized.contains("پزشک") || normalized.contains("دندان") ->
            palette(
                0xFF881337, 0xFF9F1239, 0xFFFB7185, 0xFFFB7185, 0xFFFFE4E6,
                0xFFFFF1F2, 0xFFFFE4E6, 0xFFFB7185, 0xFFE11D48, 0xFF881337,
                Icons.Default.MedicalServices
            )

        normalized.contains("تعمیر") ->
            if (normalized.contains("موتور")) {
                palette(
                    0xFF164E63, 0xFF155E75, 0xFF22D3EE, 0xFF22D3EE, 0xFFCFFAFE,
                    0xFFECFEFF, 0xFFCFFAFE, 0xFF22D3EE, 0xFF0891B2, 0xFF164E63,
                    Icons.Default.Build
                )
            } else {
                palette(
                    0xFF4C1D95, 0xFF5B21B6, 0xFFA78BFA, 0xFFA78BFA, 0xFFEDE9FE,
                    0xFFF5F3FF, 0xFFEDE9FE, 0xFFA78BFA, 0xFF7C3AED, 0xFF4C1D95,
                    Icons.Default.Build
                )
            }

        normalized.contains("بیمه") ->
            if (normalized.contains("موتور")) {
                palette(
                    0xFF0F766E, 0xFF115E59, 0xFF2DD4BF, 0xFF2DD4BF, 0xFFCCFBF1,
                    0xFFF0FDFA, 0xFFCCFBF1, 0xFF2DD4BF, 0xFF0D9488, 0xFF134E4A,
                    Icons.Default.Shield
                )
            } else {
                palette(
                    0xFF1E3A8A, 0xFF1D4ED8, 0xFF60A5FA, 0xFF60A5FA, 0xFFDBEAFE,
                    0xFFEFF6FF, 0xFFDBEAFE, 0xFF60A5FA, 0xFF2563EB, 0xFF1E3A8A,
                    Icons.Default.Shield
                )
            }

        normalized.contains("موتور") ->
            palette(
                0xFF164E63, 0xFF155E75, 0xFF22D3EE, 0xFF22D3EE, 0xFFCFFAFE,
                0xFFECFEFF, 0xFFCFFAFE, 0xFF22D3EE, 0xFF0891B2, 0xFF164E63,
                Icons.Default.TwoWheeler
            )

        normalized.contains("ماشین") || normalized.contains("خودرو") ->
            palette(
                0xFF0C4A6E, 0xFF075985, 0xFF38BDF8, 0xFF38BDF8, 0xFFE0F2FE,
                0xFFF0F9FF, 0xFFE0F2FE, 0xFF38BDF8, 0xFF0284C7, 0xFF0C4A6E,
                Icons.Default.DirectionsCar
            )

        normalized.contains("اینترنت") || normalized.contains("وای") ->
            palette(
                0xFF3B0764, 0xFF581C87, 0xFFC084FC, 0xFFC084FC, 0xFFF3E8FF,
                0xFFFAF5FF, 0xFFF3E8FF, 0xFFC084FC, 0xFF9333EA, 0xFF581C87,
                Icons.Default.Wifi
            )

        normalized.contains("موبایل") || normalized.contains("تلفن") || normalized.contains("شارژ") ->
            palette(
                0xFF1E40AF, 0xFF0369A1, 0xFF38BDF8, 0xFF67E8F9, 0xFFECFEFF,
                0xFFEFF6FF, 0xFFE0F2FE, 0xFF38BDF8, 0xFF0284C7, 0xFF0C4A6E,
                Icons.Default.Smartphone
            )

        normalized.contains("پوشاک") || normalized.contains("لباس") || normalized.contains("کفش") ->
            palette(
                0xFF701A75, 0xFF86198F, 0xFFE879F9, 0xFFE879F9, 0xFFFAE8FF,
                0xFFFDF4FF, 0xFFFAE8FF, 0xFFE879F9, 0xFFC026D3, 0xFF701A75,
                Icons.Default.Checkroom
            )

        normalized.contains("خیریه") || normalized.contains("کمک") || normalized.contains("صدقه") ->
            palette(
                0xFF881337, 0xFFBE185D, 0xFFFDA4AF, 0xFFFDA4AF, 0xFFFFF1F2,
                0xFFFFF1F2, 0xFFFCE7F3, 0xFFFB7185, 0xFFE11D48, 0xFF881337,
                Icons.Default.Favorite
            )

        normalized.contains("فاطمه") ->
            palette(
                0xFF831843, 0xFF9D174D, 0xFFF472B6, 0xFFF472B6, 0xFFFCE7F3,
                0xFFFDF2F8, 0xFFFCE7F3, 0xFFF472B6, 0xFFDB2777, 0xFF831843,
                Icons.Default.Person
            )

        normalized.contains("علی") ->
            palette(
                0xFF0C4A6E, 0xFF0369A1, 0xFF38BDF8, 0xFF38BDF8, 0xFFE0F2FE,
                0xFFF0F9FF, 0xFFE0F2FE, 0xFF38BDF8, 0xFF0284C7, 0xFF0C4A6E,
                Icons.Default.Person
            )

        normalized.contains("قبض") || normalized.contains("قبوض") || normalized.contains("بدهی") ->
            palette(
                0xFF134E4A, 0xFF115E59, 0xFF2DD4BF, 0xFF2DD4BF, 0xFFCCFBF1,
                0xFFF0FDFA, 0xFFCCFBF1, 0xFF2DD4BF, 0xFF0D9488, 0xFF134E4A,
                Icons.Default.Receipt
            )

        normalized.contains("قسط") || normalized.contains("اقساط") ->
            palette(
                0xFF78350F, 0xFFB45309, 0xFFFBBF24, 0xFFFBBF24, 0xFFFEF3C7,
                0xFFFFFBEB, 0xFFFEF3C7, 0xFFF59E0B, 0xFFD97706, 0xFF78350F,
                Icons.Default.Receipt
            )

        else -> {
            val rotating = listOf(
                palette(
                    0xFF4C1D95, 0xFF5B21B6, 0xFFA78BFA, 0xFFA78BFA, 0xFFEDE9FE,
                    0xFFF5F3FF, 0xFFEDE9FE, 0xFFA78BFA, 0xFF7C3AED, 0xFF4C1D95,
                    Icons.Default.LocalOffer
                ),
                palette(
                    0xFF0F766E, 0xFF047857, 0xFF34D399, 0xFF34D399, 0xFFECFDF5,
                    0xFFECFDF5, 0xFFD1FAE5, 0xFF34D399, 0xFF059669, 0xFF064E3B,
                    Icons.Default.LocalOffer
                ),
                palette(
                    0xFF0C4A6E, 0xFF075985, 0xFF38BDF8, 0xFF38BDF8, 0xFFE0F2FE,
                    0xFFF0F9FF, 0xFFE0F2FE, 0xFF38BDF8, 0xFF0284C7, 0xFF0C4A6E,
                    Icons.Default.LocalOffer
                ),
                palette(
                    0xFF7C2D12, 0xFF9A3412, 0xFFFB923C, 0xFFFB923C, 0xFFFFEDD5,
                    0xFFFFF7ED, 0xFFFFEDD5, 0xFFFB923C, 0xFFEA580C, 0xFF7C2D12,
                    Icons.Default.LocalOffer
                ),
                palette(
                    0xFF831843, 0xFF9D174D, 0xFFF472B6, 0xFFF472B6, 0xFFFCE7F3,
                    0xFFFDF2F8, 0xFFFCE7F3, 0xFFF472B6, 0xFFDB2777, 0xFF831843,
                    Icons.Default.LocalOffer
                ),
                palette(
                    0xFF312E81, 0xFF3730A3, 0xFF818CF8, 0xFF818CF8, 0xFFE0E7FF,
                    0xFFEEF2FF, 0xFFE0E7FF, 0xFF818CF8, 0xFF4F46E5, 0xFF312E81,
                    Icons.Default.LocalOffer
                )
            )
            rotating[index % rotating.size]
        }
    }
}
