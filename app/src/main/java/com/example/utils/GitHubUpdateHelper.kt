package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Checks the user's GitHub repository for newer app releases/tags and opens the release/download
 * URL via Android's standard ACTION_VIEW flow (compliant with Google Play policy, requiring no
 * REQUEST_INSTALL_PACKAGES permission).
 */
object GitHubUpdateHelper {

    private const val PREFS_NAME = "github_update_prefs"
    private const val KEY_REPO_INPUT = "github_repo_input"
    private const val KEY_AUTO_CHECK_DOWNLOAD = "github_auto_check_download"
    const val DEFAULT_GITHUB_REPO_URL = "https://github.com/Sepehr1971/Financial-Management"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class UpdateCheckResult(
        val isSuccess: Boolean,
        val isUpdateAvailable: Boolean,
        val currentVersion: String,
        val latestVersion: String = "",
        val releaseNotes: String = "",
        val downloadUrl: String = "",
        val releasePageUrl: String = "",
        val message: String = ""
    )

    fun getSavedRepoInput(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_REPO_INPUT, null)?.trim()
        return if (saved.isNullOrBlank()) DEFAULT_GITHUB_REPO_URL else saved
    }

    fun saveRepoInput(context: Context, repoInput: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REPO_INPUT, repoInput.trim()).apply()
    }

    fun isAutoCheckAndDownloadEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_CHECK_DOWNLOAD, true)
    }

    fun setAutoCheckAndDownloadEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_CHECK_DOWNLOAD, enabled).apply()
    }

    /**
     * Parses either "owner/repo" or "https://github.com/owner/repo" (with optional trailing paths/.git).
     */
    fun parseOwnerAndRepo(rawInput: String): Pair<String, String>? {
        val cleaned = rawInput.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")
            .removeSuffix(".git")
            .trim('/')

        val segments = cleaned.split("/").filter { it.isNotBlank() }
        if (segments.size < 2) return null
        val owner = segments[0].trim()
        val repo = segments[1].trim()
        if (owner.isBlank() || repo.isBlank()) return null
        return owner to repo
    }

    /**
     * Compares two version strings like "1.0", "v1.2.0", "2", "V5" -> returns >0 if remote > local.
     */
    fun compareVersions(remoteRaw: String, localRaw: String): Int {
        val remoteParts = extractNumericParts(remoteRaw)
        val localParts = extractNumericParts(localRaw)
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r != l) return r.compareTo(l)
        }
        return 0
    }

    private fun extractNumericParts(version: String): List<Int> {
        val cleaned = PersianDateHelper.toEnglishDigits(version)
            .trim()
            .removePrefix("v")
            .removePrefix("V")
        return Regex("\\d+")
            .findAll(cleaned)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
    }

    suspend fun checkForUpdate(rawRepoInput: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVer = BuildConfig.VERSION_NAME.ifBlank { BuildConfig.VERSION_CODE.toString() }
        val parsed = parseOwnerAndRepo(rawRepoInput)
            ?: return@withContext UpdateCheckResult(
                isSuccess = false,
                isUpdateAvailable = false,
                currentVersion = currentVer,
                message = "لطفاً آدرس مخزن گیتهاب خود را به شکل «username/repo» یا لینک کامل وارد کنید."
            )

        val (owner, repo) = parsed
        val latestReleaseApi = "https://api.github.com/repos/$owner/$repo/releases/latest"

        try {
            val request = Request.Builder()
                .url(latestReleaseApi)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "HazinehApp-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "").ifBlank { json.optString("name", "") }
                    val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo/releases")
                    val notes = json.optString("body", "").trim()

                    var apkUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val assetName = asset.optString("name", "").lowercase()
                            val browserUrl = asset.optString("browser_download_url", "")
                            if (assetName.endsWith(".apk") && browserUrl.isNotBlank()) {
                                apkUrl = browserUrl
                                break
                            }
                        }
                        if (apkUrl.isBlank() && assets.length() > 0) {
                            apkUrl = assets.optJSONObject(0)?.optString("browser_download_url", "") ?: ""
                        }
                    }

                    val finalDownloadUrl = apkUrl.ifBlank { htmlUrl }
                    val isNewer = tagName.isNotBlank() && compareVersions(tagName, currentVer) > 0

                    return@withContext UpdateCheckResult(
                        isSuccess = true,
                        isUpdateAvailable = isNewer,
                        currentVersion = currentVer,
                        latestVersion = tagName,
                        releaseNotes = notes,
                        downloadUrl = finalDownloadUrl,
                        releasePageUrl = htmlUrl,
                        message = if (isNewer) {
                            "نسخه جدید ($tagName) در گیتهاب منتشر شده است!"
                        } else {
                            "برنامه شما به آخرین نسخه ($currentVer) به‌روز است."
                        }
                    )
                } else if (response.code == 404) {
                    // Fallback to checking /tags or repo existence
                    return@withContext checkTagsFallback(owner, repo, currentVer)
                } else {
                    return@withContext UpdateCheckResult(
                        isSuccess = false,
                        isUpdateAvailable = false,
                        currentVersion = currentVer,
                        message = "خطا در ارتباط با گیتهاب (کد ${response.code})"
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext UpdateCheckResult(
                isSuccess = false,
                isUpdateAvailable = false,
                currentVersion = currentVer,
                message = "خطا در بررسی گیتهاب: اتصال اینترنت را بررسی کنید"
            )
        }
    }

    private fun checkTagsFallback(owner: String, repo: String, currentVer: String): UpdateCheckResult {
        return try {
            val tagsUrl = "https://api.github.com/repos/$owner/$repo/tags"
            val req = Request.Builder()
                .url(tagsUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "HazinehApp-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    val releasesPage = "https://github.com/$owner/$repo/releases"
                    if (arr.length() == 0) {
                        return UpdateCheckResult(
                            isSuccess = true,
                            isUpdateAvailable = false,
                            currentVersion = currentVer,
                            latestVersion = currentVer,
                            releasePageUrl = releasesPage,
                            downloadUrl = releasesPage,
                            message = "هنوز نسخه (Release) جدیدی در این مخزن گیتهاب منتشر نشده است."
                        )
                    }
                    val latestTag = arr.optJSONObject(0)?.optString("name", "").orEmpty()
                    val isNewer = latestTag.isNotBlank() && compareVersions(latestTag, currentVer) > 0
                    UpdateCheckResult(
                        isSuccess = true,
                        isUpdateAvailable = isNewer,
                        currentVersion = currentVer,
                        latestVersion = latestTag,
                        downloadUrl = releasesPage,
                        releasePageUrl = releasesPage,
                        message = if (isNewer) {
                            "نسخه جدید ($latestTag) در گیتهاب موجود است!"
                        } else {
                            "برنامه شما به آخرین نسخه ($currentVer) به‌روز است."
                        }
                    )
                } else {
                    UpdateCheckResult(
                        isSuccess = false,
                        isUpdateAvailable = false,
                        currentVersion = currentVer,
                        message = "مخزن «$owner/$repo» یا نسخه جدید یافت نشد."
                    )
                }
            }
        } catch (_: Exception) {
            UpdateCheckResult(
                isSuccess = false,
                isUpdateAvailable = false,
                currentVersion = currentVer,
                message = "خطا در دریافت تگ‌های مخزن گیتهاب"
            )
        }
    }

    fun openDownloadUrl(context: Context, url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
