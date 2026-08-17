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
import androidx.compose.runtime.collectAsState
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
        val deepLinkConversationId = intent?.getStringExtra("conversationId")

        setContent {
            LifePilotApp(
                biometricAuthManager = biometricAuthManager,
                deepLinkObjectId = deepLinkObjectId,
                deepLinkConversationId = deepLinkConversationId,
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
    deepLinkConversationId: String? = null,
) {
    val themeViewModel: ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    LifePilotTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val lockEnabled = biometricAuthManager.isBiometricLockEnabled()
            var isAuthenticated by remember { mutableStateOf(!lockEnabled) }

            if (!isAuthenticated) {
                BiometricLockScreen(
                    biometricAuthManager = biometricAuthManager,
                    onAuthenticated = { isAuthenticated = true },
                )
            } else if (!BuildConfig.ONBOARDING_ENABLED) {
                // "stable" flavor: no onboarding, straight to the app.
                LifePilotNavHost(
                    deepLinkObjectId = deepLinkObjectId,
                    deepLinkConversationId = deepLinkConversationId,
                )
            } else {
                val onboardingViewModel: com.lifepilot.app.onboarding.OnboardingViewModel =
                    androidx.hilt.navigation.compose.hiltViewModel()
                val onboardingDone by onboardingViewModel.completed.collectAsState()
                when (onboardingDone) {
                    // null = flag still loading; render nothing briefly to avoid a flash.
                    null -> Unit
                    false -> com.lifepilot.app.onboarding.OnboardingScreen(viewModel = onboardingViewModel)
                    else -> LifePilotNavHost(deepLinkObjectId = deepLinkObjectId)
                }
            }
        }
    }
}
