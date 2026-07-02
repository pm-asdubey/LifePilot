package com.lifepilot.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lifepilot.app.BuildConfig
import com.lifepilot.app.navigation.LifePilotNavHost
import com.lifepilot.app.security.BiometricLockScreen
import com.lifepilot.data.security.BiometricAuthManager
import com.lifepilot.data.security.BiometricAuthState
import com.lifepilot.designsystem.theme.LifePilotTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Timber.d("POST_NOTIFICATIONS permission granted: $granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        requestNotificationPermissionIfNeeded()

        val deepLinkObjectId = intent?.getStringExtra("objectId")

        setContent {
            LifePilotApp(
                biometricAuthManager = biometricAuthManager,
                deepLinkObjectId = deepLinkObjectId,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Reset auth when the app genuinely goes to background (not when a biometric
        // dialog opens — that also triggers onPause/onResume which caused an infinite
        // loop where the BiometricPrompt result was immediately discarded).
        // We only reset if the user was already authenticated so that the first-launch
        // auth flow is not interrupted.
        if (biometricAuthManager.isBiometricLockEnabled() &&
            biometricAuthManager.authState.value is BiometricAuthState.Authenticated
        ) {
            biometricAuthManager.resetAuthState()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(permission)
            }
        }
    }
}

@Composable
private fun LifePilotApp(
    biometricAuthManager: BiometricAuthManager,
    deepLinkObjectId: String? = null,
) {
    LifePilotTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val lockEnabled = biometricAuthManager.isBiometricLockEnabled()
            var isAuthenticated by remember { mutableStateOf(!lockEnabled) }

            if (!isAuthenticated) {
                BiometricLockScreen(
                    biometricAuthManager = biometricAuthManager,
                    onAuthenticated = { isAuthenticated = true },
                )
            } else {
                LifePilotNavHost(deepLinkObjectId = deepLinkObjectId)
            }
        }
    }
}
