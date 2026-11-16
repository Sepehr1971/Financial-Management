package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper
import com.example.utils.PersianVoiceExpenseParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceExpenseDialog(
    onDismissRequest: () -> Unit,
    onApplyToForm: (PersianVoiceExpenseParser.ParsedVoiceExpense) -> Unit,
    onDirectSaveExpense: ((PersianVoiceExpenseParser.ParsedVoiceExpense) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val coroutineScope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    var keepListeningMode by remember { mutableStateOf(false) }
    var autoRestartCount by remember { mutableIntStateOf(0) }
    var isDisposed by remember { mutableStateOf(false) }
    var retryJob by remember { mutableStateOf<Job?>(null) }

    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var transcript by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("روی میکروفون بزنید و هزینه را به فارسی بگویید") }
    var errorMessage by remember { mutableStateOf("") }
    var autoStarted by remember { mutableStateOf(false) }

    val parsedExpense = remember(transcript) {
        PersianVoiceExpenseParser.parse(transcript)
    }

    val recognizerAvailable = remember(context) {
        SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun buildSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Give user plenty of time before starting and between words
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 7_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5_000L)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "با خیال راحت صحبت کنید (مثال: دویست و پنجاه هزار تومان خرید نان)")
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        keepListeningMode = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            if (best.isNotBlank()) {
                transcript = best
                errorMessage = ""
                statusMessage = "متن با موفقیت دریافت و تحلیل شد"
            }
        }
    }

    val speechRecognizer = remember(context, recognizerAvailable) {
        if (recognizerAvailable) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun launchSystemSpeechFallback() {
        keepListeningMode = false
        retryJob?.cancel()
        try {
            errorMessage = ""
            statusMessage = "در حال باز کردن پنجره تشخیص گفتار..."
            systemSpeechLauncher.launch(buildSpeechIntent())
        } catch (_: Exception) {
            errorMessage = "سرویس تشخیص گفتار روی این دستگاه نصب نیست؛ می‌توانید متن را تایپ یا سرویس گوگل را فعال کنید."
        }
    }

    fun startInlineListening(resetRetryCount: Boolean = true) {
        if (!hasAudioPermission || isDisposed) return
        retryJob?.cancel()
        if (speechRecognizer == null) {
            launchSystemSpeechFallback()
            return
        }
        if (resetRetryCount) {
            autoRestartCount = 0
        }
        keepListeningMode = true
        try {
            errorMessage = ""
            statusMessage = "منتظر صدای شما... با خیال راحت صحبت کنید"
            isListening = true
            speechRecognizer.startListening(buildSpeechIntent())
        } catch (_: Exception) {
            isListening = false
            keepListeningMode = false
            launchSystemSpeechFallback()
        }
    }

    fun stopInlineListening() {
        keepListeningMode = false
        retryJob?.cancel()
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        isListening = false
        rmsLevel = 0f
        if (transcript.isBlank()) {
            statusMessage = "روی میکروفون بزنید و دوباره امتحان کنید"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            errorMessage = ""
            startInlineListening(resetRetryCount = true)
        } else {
            isListening = false
            keepListeningMode = false
            errorMessage = "برای ثبت صوتی هزینه، دسترسی میکروفون لازم است."
            statusMessage = "دسترسی میکروفون داده نشد"
        }
    }

    DisposableEffect(speechRecognizer) {
        isDisposed = false
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                errorMessage = ""
                statusMessage = "در حال گوش دادن... با فرصت کافی صحبت کنید"
            }

            override fun onBeginningOfSpeech() {
                statusMessage = "در حال دریافت صدای شما..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = rmsdB.coerceIn(0f, 12f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                statusMessage = "در حال تبدیل گفتار به متن..."
            }

            override fun onError(error: Int) {
                rmsLevel = 0f
                val isQuietTimeout = (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)

                // Patiently restart listening if the user hasn't started speaking yet
                if (isQuietTimeout && keepListeningMode && !isDisposed && autoRestartCount < 10) {
                    autoRestartCount += 1
                    isListening = true
                    errorMessage = ""
                    statusMessage = "همچنان منتظر صحبت شما... (می‌توانید صحبت کنید یا دکمه توقف را بزنید)"
                    retryJob?.cancel()
                    retryJob = coroutineScope.launch {
                        delay(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 450L else 220L)
                        if (keepListeningMode && !isDisposed) {
                            try {
                                speechRecognizer.cancel()
                                speechRecognizer.startListening(buildSpeechIntent())
                            } catch (_: Exception) {
                                isListening = false
                                keepListeningMode = false
                            }
                        }
                    }
                    return
                }

                isListening = false
                keepListeningMode = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "خطا در ضبط صدا"
                    SpeechRecognizer.ERROR_CLIENT -> "سرویس گفتار آماده نبود؛ از گزینه پنجره صوتی سیستم استفاده کنید"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون لازم است"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای شبکه برای تشخیص گفتار آنلاین"
                    SpeechRecognizer.ERROR_NO_MATCH -> "صدایی تشخیص داده نشد؛ دوباره دکمه میکروفون را بزنید یا پنجره صوتی سیستم را باز کنید"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس تشخیص گفتار مشغول است؛ لحظه‌ای صبر کنید"
                    SpeechRecognizer.ERROR_SERVER -> "خطا از سمت سرویس تشخیص گفتار"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "مهلت انتظار برای صحبت به پایان رسید؛ برای تلاش مجدد میکروفون را بزنید"
                    else -> "خطا در تشخیص گفتار (کد $error)"
                }
                errorMessage = msg
                statusMessage = "آماده تلاش مجدد"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull()?.trim().orEmpty()
                if (best.isNotBlank()) {
                    isListening = false
                    keepListeningMode = false
                    rmsLevel = 0f
                    transcript = best
                    errorMessage = ""
                    statusMessage = "گفتار شما با موفقیت تحلیل شد"
                } else if (keepListeningMode && !isDisposed && autoRestartCount < 10) {
                    autoRestartCount += 1
                    retryJob?.cancel()
                    retryJob = coroutineScope.launch {
                        delay(220L)
                        if (keepListeningMode && !isDisposed) {
                            try {
                                speechRecognizer.startListening(buildSpeechIntent())
                            } catch (_: Exception) {
                                isListening = false
                                keepListeningMode = false
                            }
                        }
                    }
                } else {
                    isListening = false
                    keepListeningMode = false
                    rmsLevel = 0f
                    errorMessage = "متنی تشخیص داده نشد؛ لطفاً دوباره صحبت کنید"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val current = partials?.firstOrNull()?.trim().orEmpty()
                if (current.isNotBlank()) {
                    transcript = current
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            isDisposed = true
            keepListeningMode = false
            retryJob?.cancel()
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    // Request permission or auto-start listening once when opened (brief delay for dialog settle)
    LaunchedEffect(Unit) {
        if (!autoStarted) {
            autoStarted = true
            delay(250L)
            if (hasAudioPermission) {
                startInlineListening(resetRetryCount = true)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.16f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val gradientColors = if (isDark) {
        listOf(Color(0xFF0F766E), Color(0xFF059669))
    } else {
        listOf(Color(0xFF10B981), Color(0xFF0EA5E9))
    }

    Dialog(
        onDismissRequest = {
            stopInlineListening()
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ScaledDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scaledDp(18f), vertical = scaledDp(16f))
                    .testTag("voice_expense_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(scaledDp(18f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت هوشمند هزینه با صدا",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "تشخیص گفتار فارسی و استخراج خودکار مبلغ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            stopInlineListening()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پنجره صوتی",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pulsing Microphone Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (isListening) {
                        val dynamicScale = pulseScale + (rmsLevel / 50f)
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .scale(dynamicScale)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.22f))
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .testTag("voice_mic_button")
                            .clickable {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else if (isListening) {
                                    stopInlineListening()
                                } else {
                                    startInlineListening()
                                }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(
                                Brush.linearGradient(
                                    colors = if (isListening) {
                                        listOf(Color(0xFFEF4444), Color(0xFFF97316))
                                    } else {
                                        gradientColors
                                    }
                                )
                            )
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "توقف ضبط صدا" else "شروع تشخیص گفتار",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isListening) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = statusMessage,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (!hasAudioPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("voice_grant_permission_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعطای دسترسی میکروفون", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { launchSystemSpeechFallback() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("voice_system_dialog_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "باز کردن پنجره صوتی سیستم",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guide banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "نمونه جملات قابل تشخیص:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = enDigits("• «دویست و پنجاه هزار تومان خرید نان و پنیر»\n• «سه و نیم میلیون اجاره مسکن دیروز»\n• «500 هزار تومن بنزین»"),
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recognized / Editable transcript field
                OutlinedTextField(
                    value = transcript,
                    onValueChange = { transcript = it },
                    label = { Text("متن گفتار تشخیص‌داده‌شده") },
                    placeholder = { Text("مثلاً: دویست هزار تومان خرید میوه دیروز", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_transcript_input"),
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                // Live Parsed Expense Preview Card
                AnimatedVisibility(visible = transcript.isNotBlank()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_parsed_preview_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF132A2E) else Color(0xFFECFDF5)
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.65f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "پیش‌نمایش تحلیل خودکار:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "عنوان هزینه:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = parsedExpense.title.ifBlank { "نامشخص (لطفاً وارد کنید)" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (parsedExpense.title.isNotBlank()) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مبلغ تشخیص‌داده‌شده:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = enDigits(
                                            if (parsedExpense.amountTomans > 0L) {
                                                PersianDateHelper.formatCurrency(parsedExpense.amountTomans)
                                            } else {
                                                "مبلغی یافت نشد"
                                            }
                                        ),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (parsedExpense.amountTomans > 0L) {
                                            if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(scaledDp(15f))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "تاریخ:",
                                            fontSize = 12.sp,
                                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                    Text(
                                        text = enDigits(parsedExpense.shamsiDateString),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                                    )
                                }

                                if (parsedExpense.isAnnual) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = scaledDp(8f), vertical = scaledDp(4f)),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(scaledDp(14f))
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = enDigits("به عنوان تعهد سالانه (سرشکن 12 ماهه) شناسایی شد"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                val canDirectSave = onDirectSaveExpense != null &&
                    parsedExpense.title.isNotBlank() &&
                    parsedExpense.amountTomans > 0L

                val canApplyToForm = parsedExpense.title.isNotBlank() || parsedExpense.amountTomans > 0L

                if (onDirectSaveExpense != null) {
                    Button(
                        onClick = {
                            stopInlineListening()
                            onDirectSaveExpense(parsedExpense)
                        },
                        enabled = canDirectSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("voice_save_direct_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ثبت مستقیم هزینه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            stopInlineListening()
                            onApplyToForm(parsedExpense)
                        },
                        enabled = canApplyToForm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("voice_open_in_sheet_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ویرایش در فرم کامل",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            stopInlineListening()
                            onApplyToForm(parsedExpense)
                        },
                        enabled = canApplyToForm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("voice_apply_to_sheet_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اعمال در فرم هزینه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
        }
    }
}
