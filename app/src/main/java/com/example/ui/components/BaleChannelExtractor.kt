package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.network.BaleChannelPriceParser
import com.example.data.network.BaleExtractedRates
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import kotlinx.coroutines.delay
import org.json.JSONTokener

private fun buildBaleExtractJs(expectedUid: String, shouldScrollToBottom: Boolean): String {
    val scrollFlag = if (shouldScrollToBottom) "true" else "false"
    return """
(function() {
    try {
        var expectedUid = "$expectedUid";
        var href = window.location ? (window.location.href || "") : "";
        if (expectedUid && href.indexOf("chat") !== -1 && href.indexOf(expectedUid) === -1 && href.indexOf("login") === -1) {
            return "__WRONG_UID__:" + href;
        }

        function isIgnoredZone(el) {
            var cur = el;
            while (cur && cur !== document.body) {
                var tag = (cur.tagName || "").toLowerCase();
                var cls = (typeof cur.className === "string") ? cur.className.toLowerCase() : "";
                var id = (cur.id || "").toLowerCase();
                var role = (cur.getAttribute && cur.getAttribute("role")) ? cur.getAttribute("role").toLowerCase() : "";
                if (tag === "aside" || tag === "nav" || role === "navigation" ||
                    cls.indexOf("sidebar") !== -1 || cls.indexOf("chatlist") !== -1 ||
                    cls.indexOf("chat-list") !== -1 || cls.indexOf("dialoglist") !== -1 ||
                    cls.indexOf("dialog-list") !== -1 || cls.indexOf("conversationlist") !== -1 ||
                    cls.indexOf("pinned") !== -1 || cls.indexOf("composer") !== -1 ||
                    id.indexOf("sidebar") !== -1 || id.indexOf("chat-list") !== -1) {
                    return true;
                }
                cur = cur.parentElement;
            }
            return false;
        }

        if ($scrollFlag) {
            var scrollables = document.querySelectorAll('div, main, section, ul');
            for (var s = 0; s < scrollables.length; s++) {
                var sc = scrollables[s];
                if (isIgnoredZone(sc)) continue;
                if (sc.scrollHeight > sc.clientHeight + 60 && sc.clientHeight > 120) {
                    var rect = sc.getBoundingClientRect();
                    if (rect.width >= window.innerWidth * 0.45) {
                        sc.scrollTop = sc.scrollHeight;
                    }
                }
            }
        }

        var rawNodes = document.querySelectorAll('[data-message-id], [id^="message-"], [class*="messageItem"], [class*="message-item"], [class*="MessageItem"]');
        if (!rawNodes || rawNodes.length === 0) {
            rawNodes = document.querySelectorAll('[class*="message"], [class*="Message"]');
        }

        var items = [];
        for (var i = 0; i < rawNodes.length; i++) {
            var el = rawNodes[i];
            if (isIgnoredZone(el)) continue;
            var text = (el.innerText || "").trim();
            if (!text) continue;
            var r = el.getBoundingClientRect();
            if (r.width < 80 || r.height < 12) continue;

            // Skip wrapper elements if we already found more specific child message nodes
            var nested = el.querySelectorAll('[data-message-id]');
            if (!el.hasAttribute('data-message-id') && nested && nested.length > 0) continue;

            items.push({ el: el, top: r.top, text: text });
        }

        // Deduplicate ancestors/descendants keeping the cleanest individual message blocks
        var leafItems = [];
        for (var a = 0; a < items.length; a++) {
            var hasChildSelected = false;
            for (var b = 0; b < items.length; b++) {
                if (a !== b && items[a].el.contains(items[b].el)) {
                    hasChildSelected = true;
                    break;
                }
            }
            if (!hasChildSelected) {
                leafItems.push(items[a]);
            }
        }

        if (leafItems.length > 0) {
            leafItems.sort(function(x, y) { return x.top - y.top; });
            var seen = {};
            var out = [];
            for (var k = 0; k < leafItems.length; k++) {
                var t = leafItems[k].text;
                if (!seen[t]) {
                    seen[t] = true;
                    out.push(t);
                }
            }
            return out.join("\n");
        }

        var mainEl = document.querySelector('main') || document.body;
        return (mainEl && mainEl.innerText) ? mainEl.innerText : '';
    } catch (e) {
        return (document.body && document.body.innerText) ? document.body.innerText : '';
    }
})();
""".trimIndent()
}

private fun extractUidFromUrl(url: String): String {
    return Regex("""uid=([^&]+)""").find(url)?.groupValues?.get(1) ?: ""
}

private fun decodeJsStringResult(rawResult: String?): String {
    if (rawResult.isNullOrBlank() || rawResult == "null") return ""
    return try {
        (JSONTokener(rawResult).nextValue() as? String) ?: ""
    } catch (_: Exception) {
        rawResult.trim('"')
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureBaleWebView(webView: WebView) {
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        useWideViewPort = true
        loadWithOverviewMode = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

/**
 * Silent 1dp WebView runner used when the user already has an authenticated Bale Web session.
 * Iterates sequentially through the 3 Bale channel URLs and stops with onNeedsLogin() if Bale prompts for login.
 */
@Composable
fun BaleBackgroundSyncRunner(
    active: Boolean,
    onCompleted: (BaleExtractedRates) -> Unit,
    onNeedsLogin: () -> Unit,
    onFailed: () -> Unit
) {
    if (!active) return

    val context = LocalContext.current
    var stepIndex by remember { mutableIntStateOf(0) }
    var currentUrlLoaded by remember { mutableStateOf("") }
    var extractedState by remember { mutableStateOf(BaleExtractedRates()) }

    val steps = remember {
        listOf(
            BaleChannelPriceParser.URL_FX_CHANNEL,
            BaleChannelPriceParser.URL_GOLD_CHANNEL,
            BaleChannelPriceParser.URL_CRYPTO_CHANNEL
        )
    }

    val webView = remember(context) {
        WebView(context).apply {
            configureBaleWebView(this)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (url != null) currentUrlLoaded = url
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null) currentUrlLoaded = url
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(stepIndex) {
        if (stepIndex >= steps.size) {
            CookieManager.getInstance().flush()
            if (extractedState.hasAnyRate()) {
                onCompleted(extractedState)
            } else {
                onFailed()
            }
            return@LaunchedEffect
        }

        val targetUrl = steps[stepIndex]
        val expectedUid = extractUidFromUrl(targetUrl)
        currentUrlLoaded = ""
        webView.loadUrl(targetUrl)

        // Allow SPA transition time so we never accidentally parse the previous channel's DOM
        delay(1100L)

        var stepResolved = false
        var lastSignature = ""
        var stableHits = 0
        var latestStepExtracted = extractedState

        for (attempt in 0 until 18) {
            delay(750L)
            var pageText = ""
            var evalDone = false
            val js = buildBaleExtractJs(expectedUid = expectedUid, shouldScrollToBottom = attempt < 4)
            webView.evaluateJavascript(js) { raw ->
                pageText = decodeJsStringResult(raw)
                evalDone = true
            }
            var waitCount = 0
            while (!evalDone && waitCount < 15) {
                delay(50L)
                waitCount++
            }

            if (pageText.startsWith("__WRONG_UID__:")) {
                if (attempt % 4 == 3) {
                    webView.loadUrl(targetUrl)
                }
                continue
            }

            if (BaleChannelPriceParser.detectBaleLoginRequired(pageText, webView.url ?: currentUrlLoaded)) {
                onNeedsLogin()
                return@LaunchedEffect
            }

            if (pageText.isBlank()) {
                continue
            }

            when (stepIndex) {
                0 -> {
                    val fx = BaleChannelPriceParser.parseChannel4827170321(pageText)
                    val sig = "${fx.dollar?.amount}|${fx.euro?.amount}"
                    if (fx.dollar != null && fx.euro != null) {
                        latestStepExtracted = extractedState.copy(dollar = fx.dollar, euro = fx.euro)
                        if (sig == lastSignature) stableHits++ else {
                            lastSignature = sig
                            stableHits = 1
                        }
                        if (stableHits >= 2 && attempt >= 2) {
                            extractedState = latestStepExtracted
                            stepResolved = true
                            break
                        }
                    } else if (attempt >= 14 && (fx.dollar != null || fx.euro != null)) {
                        extractedState = extractedState.copy(
                            dollar = fx.dollar ?: extractedState.dollar,
                            euro = fx.euro ?: extractedState.euro
                        )
                        stepResolved = true
                        break
                    }
                }
                1 -> {
                    val gold = BaleChannelPriceParser.parseChannel5160361766(pageText)
                    val foundCount = listOf(gold.goldOunceUsd, gold.goldKaratToman, gold.sekehEmami, gold.robSekeh).count { it != null }
                    val sig = "${gold.goldOunceUsd?.amount}|${gold.goldKaratToman?.amount}|${gold.sekehEmami?.amount}|${gold.robSekeh?.amount}"
                    if (foundCount >= 4) {
                        latestStepExtracted = extractedState.copy(
                            goldOunceUsd = gold.goldOunceUsd ?: extractedState.goldOunceUsd,
                            goldKaratToman = gold.goldKaratToman ?: extractedState.goldKaratToman,
                            goldKaratTitle = gold.goldKaratTitle,
                            sekehEmami = gold.sekehEmami ?: extractedState.sekehEmami,
                            robSekeh = gold.robSekeh ?: extractedState.robSekeh
                        )
                        if (sig == lastSignature) stableHits++ else {
                            lastSignature = sig
                            stableHits = 1
                        }
                        if (stableHits >= 2 && attempt >= 2) {
                            extractedState = latestStepExtracted
                            stepResolved = true
                            break
                        }
                    } else if (attempt >= 12 && foundCount >= 1) {
                        extractedState = extractedState.copy(
                            goldOunceUsd = gold.goldOunceUsd ?: extractedState.goldOunceUsd,
                            goldKaratToman = gold.goldKaratToman ?: extractedState.goldKaratToman,
                            goldKaratTitle = gold.goldKaratTitle,
                            sekehEmami = gold.sekehEmami ?: extractedState.sekehEmami,
                            robSekeh = gold.robSekeh ?: extractedState.robSekeh
                        )
                        stepResolved = true
                        break
                    }
                }
                2 -> {
                    val crypto = BaleChannelPriceParser.parseChannel5138319664(pageText)
                    val sig = "${crypto.tether?.amount}|${crypto.bitcoinUsd?.amount}"
                    if (crypto.tether != null && crypto.bitcoinUsd != null) {
                        latestStepExtracted = extractedState.copy(tether = crypto.tether, bitcoinUsd = crypto.bitcoinUsd)
                        if (sig == lastSignature) stableHits++ else {
                            lastSignature = sig
                            stableHits = 1
                        }
                        if (stableHits >= 2 && attempt >= 2) {
                            extractedState = latestStepExtracted
                            stepResolved = true
                            break
                        }
                    } else if (attempt >= 14 && (crypto.tether != null || crypto.bitcoinUsd != null)) {
                        extractedState = extractedState.copy(
                            tether = crypto.tether ?: extractedState.tether,
                            bitcoinUsd = crypto.bitcoinUsd ?: extractedState.bitcoinUsd
                        )
                        stepResolved = true
                        break
                    }
                }
            }
        }

        if (!stepResolved && stableHits >= 1) {
            extractedState = latestStepExtracted
            stepResolved = true
        }

        if (!stepResolved && stepIndex == 0 && !extractedState.hasAnyRate()) {
            onNeedsLogin()
        } else {
            stepIndex += 1
        }
    }

    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
    ) {
        AndroidView(factory = { webView })
    }
}

/**
 * Interactive Bale WebView Dialog that lets the user log into web.bale.ai once (or watch live extraction),
 * then automatically steps through all 3 Bale channels and extracts exact market prices.
 */
@Composable
fun BaleInteractiveSyncDialog(
    onDismiss: () -> Unit,
    onRatesExtracted: (BaleExtractedRates) -> Unit
) {
    val context = LocalContext.current
    val steps = remember {
        listOf(
            BaleChannelPriceParser.URL_FX_CHANNEL to "دلار و یورو (1 از 3)",
            BaleChannelPriceParser.URL_GOLD_CHANNEL to "انس، طلا و سکه (2 از 3)",
            BaleChannelPriceParser.URL_CRYPTO_CHANNEL to "تتر و بیت‌کوین (3 از 3)"
        )
    }

    var stepIndex by remember { mutableIntStateOf(0) }
    var isLoginScreen by remember { mutableStateOf(false) }
    var statusLabel by remember { mutableStateOf("در حال اتصال به کانال دلار و یورو در بله...") }
    var extractedState by remember { mutableStateOf(BaleExtractedRates()) }
    var manualTriggerTick by remember { mutableIntStateOf(0) }

    val webView = remember(context) {
        WebView(context).apply {
            configureBaleWebView(this)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(stepIndex, manualTriggerTick) {
        if (stepIndex >= steps.size) {
            CookieManager.getInstance().flush()
            onRatesExtracted(extractedState)
            return@LaunchedEffect
        }

        val (targetUrl, stepTitle) = steps[stepIndex]
        val expectedUid = extractUidFromUrl(targetUrl)
        statusLabel = "در حال بارگذاری کانال $stepTitle..."
        webView.loadUrl(targetUrl)

        delay(1000L)

        var idleAttemptsAfterChat = 0
        var lastSignature = ""
        var stableHits = 0

        while (true) {
            delay(800L)
            var pageText = ""
            var evalDone = false
            val js = buildBaleExtractJs(expectedUid = expectedUid, shouldScrollToBottom = idleAttemptsAfterChat < 4)
            webView.evaluateJavascript(js) { raw ->
                pageText = decodeJsStringResult(raw)
                evalDone = true
            }
            var waitCount = 0
            while (!evalDone && waitCount < 15) {
                delay(50L)
                waitCount++
            }

            if (pageText.startsWith("__WRONG_UID__:")) {
                idleAttemptsAfterChat++
                if (idleAttemptsAfterChat % 4 == 0) {
                    webView.loadUrl(targetUrl)
                }
                continue
            }

            val currentUrl = webView.url ?: targetUrl
            val needsLogin = BaleChannelPriceParser.detectBaleLoginRequired(pageText, currentUrl)
            isLoginScreen = needsLogin

            if (needsLogin) {
                statusLabel = "لطفاً با شماره موبایل خود وارد حساب بله شوید؛ پس از ورود، استخراج خودکار انجام می‌شود."
                idleAttemptsAfterChat = 0
                lastSignature = ""
                stableHits = 0
                continue
            }

            if (pageText.isBlank()) {
                continue
            }

            idleAttemptsAfterChat++
            statusLabel = "در حال خواندن نرخ‌ها از $stepTitle..."

            var stepDone = false
            when (stepIndex) {
                0 -> {
                    val fx = BaleChannelPriceParser.parseChannel4827170321(pageText)
                    if (fx.dollar != null || fx.euro != null) {
                        extractedState = extractedState.copy(
                            dollar = fx.dollar ?: extractedState.dollar,
                            euro = fx.euro ?: extractedState.euro
                        )
                    }
                    val sig = "${fx.dollar?.amount}|${fx.euro?.amount}"
                    if (fx.dollar != null && fx.euro != null) {
                        if (sig == lastSignature) stableHits++ else {
                            lastSignature = sig
                            stableHits = 1
                        }
                        if (stableHits >= 2 && idleAttemptsAfterChat >= 2) {
                            stepDone = true
                        }
                    } else if (idleAttemptsAfterChat >= 14 && (fx.dollar != null || fx.euro != null)) {
                        stepDone = true
                    }
                }
                1 -> {
                    val gold = BaleChannelPriceParser.parseChannel5160361766(pageText)
                    if (gold.goldOunceUsd != null || gold.goldKaratToman != null || gold.sekehEmami != null || gold.robSekeh != null) {
                        extractedState = extractedState.copy(
                            goldOunceUsd = gold.goldOunceUsd ?: extractedState.goldOunceUsd,
                            goldKaratToman = gold.goldKaratToman ?: extractedState.goldKaratToman,
                            goldKaratTitle = gold.goldKaratTitle,
                            sekehEmami = gold.sekehEmami ?: extractedState.sekehEmami,
                            robSekeh = gold.robSekeh ?: extractedState.robSekeh
                        )
                    }
                    val count = listOf(gold.goldOunceUsd, gold.goldKaratToman, gold.sekehEmami, gold.robSekeh).count { it != null }
                    val sig = "${gold.goldOunceUsd?.amount}|${gold.goldKaratToman?.amount}|${gold.sekehEmami?.amount}|${gold.robSekeh?.amount}"
                    if (count >= 4) {
                        if (sig == lastSignature) stableHits++ else {
                            lastSignature = sig
                            stableHits = 1
                        }
                        if (stableHits >= 2 && idleAttemptsAfterChat >= 2) {
                            stepDone = true
                        }
                    } else if (idleAttemptsAfterChat >= 12 && count >= 1) {
                        stepDone = true
                    }
                }
                2 -> {
                    val crypto = BaleChannelPriceParser.parseChannel5138319664(pageText)
                    if (crypto.tether != null || crypto.bitcoinUsd != null) {
                        extractedState = extractedState.copy(
                            tether = crypto.tether ?: extractedState.tether,
                            bitcoinUsd = crypto.bitcoinUsd ?: extractedState.bitcoinUsd
                        )
                    }
                    val sig = "${crypto.tether?.amount}|${crypto.bitcoinUsd?.amount}"
                    if (crypto.tether != null && crypto.bitcoinUsd != null) {
                        if (sig == lastSignature) stableHits++ else {
                            lastSignature = sig
                            stableHits = 1
                        }
                        if (stableHits >= 2 && idleAttemptsAfterChat >= 2) {
                            stepDone = true
                        }
                    } else if (idleAttemptsAfterChat >= 14 && (crypto.tether != null || crypto.bitcoinUsd != null)) {
                        stepDone = true
                    }
                }
            }

            if (stepDone) {
                CookieManager.getInstance().flush()
                stepIndex += 1
                break
            } else if (idleAttemptsAfterChat >= 22) {
                stepIndex += 1
                break
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ScaledDialogContent {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(scaledDp(14f))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isLoginScreen) Icons.Default.LockOpen else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isLoginScreen) Color(0xFFD97706) else Color(0xFF059669),
                            modifier = Modifier.size(scaledDp(20f))
                        )
                        Spacer(modifier = Modifier.width(scaledDp(8f)))
                        Text(
                            text = "اتصال و استخراج قیمت از بله",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                            softWrap = true
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLoginScreen) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = scaledDp(10f), vertical = scaledDp(8f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isLoginScreen && stepIndex < steps.size) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(scaledDp(15f)),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = enDigits(statusLabel),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLoginScreen) Color(0xFF92400E) else MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 16.sp,
                            softWrap = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 380.dp, max = 460.dp)
                ) {
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            stepIndex = 0
                            manualTriggerTick++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = scaledDp(44f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(scaledDp(16f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بارگذاری مجدد", fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = true)
                    }

                    Button(
                        onClick = {
                            if (extractedState.hasAnyRate()) {
                                CookieManager.getInstance().flush()
                                onRatesExtracted(extractedState)
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = scaledDp(44f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (extractedState.hasAnyRate()) "ثبت نرخ‌های دریافت‌شده" else "بستن",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    }
                }
            }
        }
        }
    }
}
