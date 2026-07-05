package com.lifepilot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifepilot.domain.model.UpdateInfo
import com.lifepilot.domain.model.UpdateStatus
import com.lifepilot.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private val Context.updateDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "lifepilot_update_prefs")

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @Named("currentVersionName") private val currentVersion: String,
    @Named("githubRepo") private val githubRepo: String,
    @Named("currentFlavor") private val currentFlavor: String,
) : UpdateRepository {

    companion object {
        private val KEY_LATEST_VERSION = stringPreferencesKey("latest_version")
        private val KEY_RELEASE_URL = stringPreferencesKey("release_url")
        private val KEY_APK_DOWNLOAD_URL = stringPreferencesKey("apk_download_url")
        private val KEY_PUBLISHED_AT = stringPreferencesKey("published_at")
        private val KEY_RELEASE_NOTES = stringPreferencesKey("release_notes")
        private val KEY_LAST_CHECKED_AT = longPreferencesKey("last_checked_at")

        private val CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(24)
    }

    override fun observeUpdateStatus(): Flow<UpdateStatus> =
        context.updateDataStore.data
            .catch { e ->
                Timber.w(e, "UpdateRepository: error reading update cache")
                emit(emptyPreferences())
            }
            .map { prefs ->
                val latestVersion = prefs[KEY_LATEST_VERSION] ?: return@map UpdateStatus.Unknown
                val releaseUrl = prefs[KEY_RELEASE_URL] ?: return@map UpdateStatus.Unknown
                val publishedAt = prefs[KEY_PUBLISHED_AT] ?: ""
                val releaseNotes = prefs[KEY_RELEASE_NOTES] ?: ""
                val apkDownloadUrl = prefs[KEY_APK_DOWNLOAD_URL]

                if (isNewerVersion(latestVersion, currentVersion)) {
                    UpdateStatus.UpdateAvailable(
                        UpdateInfo(
                            latestVersion = latestVersion,
                            releaseUrl = releaseUrl,
                            publishedAt = publishedAt,
                            releaseNotes = releaseNotes,
                            apkDownloadUrl = apkDownloadUrl,
                        )
                    )
                } else {
                    UpdateStatus.UpToDate
                }
            }

    override suspend fun checkForUpdate() {
        if (githubRepo.contains("CONFIGURE_ME")) {
            Timber.w("UpdateRepository: GitHub repo not configured — skipping update check")
            return
        }

        val prefs = runCatching {
            context.updateDataStore.data.catch { emit(emptyPreferences()) }.first()
        }.getOrElse { return }

        val lastCheckedAt = prefs[KEY_LAST_CHECKED_AT] ?: 0L
        val elapsedMs = Instant.now().toEpochMilli() - lastCheckedAt
        if (lastCheckedAt > 0L && elapsedMs < CHECK_INTERVAL_MS) return

        fetchAndPersist()
    }

    private suspend fun fetchAndPersist() {
        val url = "https://api.github.com/repos/$githubRepo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.d("UpdateRepository: HTTP ${response.code} from GitHub API")
                    return
                }
                val body = response.body?.string() ?: return
                persistRelease(body)
            }
        }.onFailure { e ->
            when (e) {
                is IOException -> Timber.d("UpdateRepository: offline — ${e.message}")
                else -> Timber.w(e, "UpdateRepository: unexpected error")
            }
        }
    }

    private suspend fun persistRelease(json: String) {
        runCatching {
            val obj = JSONObject(json)
            val tagName = obj.optString("tag_name", "").trimStart('v')
            val htmlUrl = obj.optString("html_url", "")
            val publishedAt = obj.optString("published_at", "")
            val releaseNotes = obj.optString("body", "")

            if (tagName.isBlank() || htmlUrl.isBlank()) return

            // Find the APK asset that matches the current flavor (e.g. "-demo-" for the
            // demo flavor). Falls back to any .apk if no flavor-specific one is found.
            val apkDownloadUrl = runCatching {
                val assets = obj.optJSONArray("assets")
                val allApks = (0 until (assets?.length() ?: 0))
                    .map { assets!!.getJSONObject(it) }
                    .filter { it.optString("name").endsWith(".apk") }
                // Prefer an asset whose name contains the current flavor, else take first.
                (allApks.firstOrNull { it.optString("name").contains("-$currentFlavor-", ignoreCase = true) }
                    ?: allApks.firstOrNull())
                    ?.optString("browser_download_url")
            }.getOrNull()

            context.updateDataStore.edit { prefs ->
                prefs[KEY_LATEST_VERSION] = tagName
                prefs[KEY_RELEASE_URL] = htmlUrl
                prefs[KEY_PUBLISHED_AT] = publishedAt
                prefs[KEY_RELEASE_NOTES] = releaseNotes
                prefs[KEY_LAST_CHECKED_AT] = Instant.now().toEpochMilli()
                if (apkDownloadUrl != null) {
                    prefs[KEY_APK_DOWNLOAD_URL] = apkDownloadUrl
                } else {
                    prefs.remove(KEY_APK_DOWNLOAD_URL)
                }
            }
        }.onFailure { e ->
            Timber.w(e, "UpdateRepository: failed to parse release JSON")
        }
    }

    /**
     * Compares semver strings (major.minor.patch) as integers.
     * Returns true if candidate is strictly newer than installed.
     * Falls back to lexicographic comparison for non-semver strings.
     */
    private fun isNewerVersion(candidate: String, installed: String): Boolean {
        return runCatching {
            val c = candidate.split(".").map { it.toInt() }
            val i = installed.split(".").map { it.toInt() }
            val size = maxOf(c.size, i.size)
            for (idx in 0 until size) {
                val cv = c.getOrElse(idx) { 0 }
                val iv = i.getOrElse(idx) { 0 }
                if (cv != iv) return cv > iv
            }
            false
        }.getOrElse {
            candidate != installed && candidate > installed
        }
    }
}
