package com.lifepilot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifepilot.data.security.EncryptedKeyStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifepilot_prefs")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedKeyStorage: EncryptedKeyStorage,
) {
    private val activeProfileIdKey = stringPreferencesKey("active_profile_id")
    private val aiProviderKey = stringPreferencesKey("ai_provider")
    private val aiModelKey = stringPreferencesKey("ai_model")
    private val biometricLockKey = booleanPreferencesKey("biometric_lock_enabled")

    private val _aiApiKeyFlow = MutableStateFlow(encryptedKeyStorage.retrieve(ENCRYPTED_AI_API_KEY))

    val activeProfileId: Flow<String?> = context.dataStore.data.map { it[activeProfileIdKey] }
    val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[biometricLockKey] ?: false }
    val aiProvider: Flow<String?> = context.dataStore.data.map { it[aiProviderKey] }
    val aiApiKey: Flow<String?> = _aiApiKeyFlow
    val aiModel: Flow<String?> = context.dataStore.data.map { it[aiModelKey] }

    suspend fun getActiveProfileId(): String? = activeProfileId.firstOrNull()

    suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { it[activeProfileIdKey] = profileId }
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { it[aiProviderKey] = provider }
    }

    fun setAiApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            encryptedKeyStorage.delete(ENCRYPTED_AI_API_KEY)
        } else {
            encryptedKeyStorage.store(ENCRYPTED_AI_API_KEY, apiKey)
        }
        _aiApiKeyFlow.value = apiKey.takeIf { it.isNotBlank() }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[aiModelKey] = model }
    }

    fun isBiometricLockEnabled(): Boolean = kotlinx.coroutines.runBlocking {
        biometricLockEnabled.firstOrNull() ?: false
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[biometricLockKey] = enabled }
    }

    suspend fun clearAiConfig() {
        context.dataStore.edit {
            it.remove(aiProviderKey)
            it.remove(aiModelKey)
        }
        encryptedKeyStorage.delete(ENCRYPTED_AI_API_KEY)
        _aiApiKeyFlow.value = null
    }

    fun observePendingVerification(objectId: String): Flow<String?> {
        val key = stringPreferencesKey("${PENDING_VERIFICATION_PREFIX}$objectId")
        return context.dataStore.data.map { it[key] }
    }

    suspend fun setPendingVerification(objectId: String, versionId: String) {
        val key = stringPreferencesKey("${PENDING_VERIFICATION_PREFIX}$objectId")
        context.dataStore.edit { it[key] = versionId }
    }

    suspend fun clearPendingVerification(objectId: String) {
        val key = stringPreferencesKey("${PENDING_VERIFICATION_PREFIX}$objectId")
        context.dataStore.edit { it.remove(key) }
    }

    companion object {
        private const val ENCRYPTED_AI_API_KEY = "encrypted_ai_api_key"
        private const val PENDING_VERIFICATION_PREFIX = "pending_verification_"
    }
}
