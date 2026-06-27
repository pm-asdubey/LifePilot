package com.lifepilot.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.lifepilot.data.repository.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BiometricAvailability {
    data object Available : BiometricAvailability
    data object NotEnrolled : BiometricAvailability
    data object NotAvailable : BiometricAvailability
}

sealed interface BiometricAuthState {
    data object Idle : BiometricAuthState
    data object Authenticated : BiometricAuthState
    data class Failed(val error: String?) : BiometricAuthState
}

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
) {
    private val _authState = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    val authState: StateFlow<BiometricAuthState> = _authState

    fun checkAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NotEnrolled
            else -> BiometricAvailability.NotAvailable
        }
    }

    fun isBiometricLockEnabled(): Boolean = preferenceManager.isBiometricLockEnabled()

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        preferenceManager.setBiometricLockEnabled(enabled)
        if (!enabled) {
            _authState.value = BiometricAuthState.Authenticated
        }
    }

    fun onAuthenticationSucceeded() {
        _authState.value = BiometricAuthState.Authenticated
    }

    fun onAuthenticationFailed(error: String?) {
        _authState.value = BiometricAuthState.Failed(error)
    }

    fun resetAuthState() {
        _authState.value = BiometricAuthState.Idle
    }
}
