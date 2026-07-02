package com.lifepilot.app.security

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepilot.data.security.BiometricAuthManager
import com.lifepilot.data.security.BiometricAuthState

@Composable
fun BiometricLockScreen(
    biometricAuthManager: BiometricAuthManager,
    onAuthenticated: () -> Unit,
) {
    val authState by biometricAuthManager.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Track whether the initial auto-prompt has fired so we know when to show the retry button.
    var initialPromptFired by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is BiometricAuthState.Authenticated) {
            onAuthenticated()
        }
    }

    LaunchedEffect(Unit) {
        promptBiometric(context, biometricAuthManager)
        initialPromptFired = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "LifePilot",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authenticate to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (authState is BiometricAuthState.Failed) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (authState as BiometricAuthState.Failed).error ?: "Authentication failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            // Always show retry button after the initial auto-prompt has fired.
            // This covers: (a) failed attempts, (b) cancelled dialogs (state stays Idle),
            // (c) hardware errors. Without this the user has no way to try again.
            if (initialPromptFired && authState !is BiometricAuthState.Authenticated) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { promptBiometric(context, biometricAuthManager) }) {
                    Text("Unlock")
                }
            }
        }
    }
}

private fun promptBiometric(
    context: android.content.Context,
    biometricAuthManager: BiometricAuthManager,
) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(context)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            biometricAuthManager.onAuthenticationSucceeded()
        }

        override fun onAuthenticationFailed() {
            biometricAuthManager.onAuthenticationFailed("Authentication not recognized")
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
            ) {
                biometricAuthManager.onAuthenticationFailed(errString.toString())
            }
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock LifePilot")
        .setSubtitle("Use fingerprint or PIN/pattern to access your life data")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
}
