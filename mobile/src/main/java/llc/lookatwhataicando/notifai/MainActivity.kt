package llc.lookatwhataicando.notifai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import llc.lookatwhataicando.notifai.startup.Advisory
import llc.lookatwhataicando.notifai.startup.Requirement
import llc.lookatwhataicando.notifai.startup.StartupCoordinator
import llc.lookatwhataicando.notifai.startup.StartupSnapshot
import llc.lookatwhataicando.notifai.startup.StartupState
import llc.lookatwhataicando.notifai.ui.theme.NotifAITheme
import llc.lookatwhataicando.notifai.util.MyLogUtils
import llc.lookatwhataicando.notifai.util.MyNotificationUtils
import llc.lookatwhataicando.notifai.util.MyPermissionUtils

/**
 * Responsibilities:
 *   1. Install system SplashScreen before super.onCreate()
 *   2. Hold splash until startupEvaluated = true
 *      (NOT until isReady — that would deadlock on first launch)
 *   3. Hand off entirely to Compose
 */
class MainActivity : ComponentActivity() {
    companion object {
        private val TAG = MyLogUtils.TAG(MainActivity::class)

        private const val ACTION_PIN = "llc.lookatwhataicando.notifai.MainActivity.action.PIN"

        fun intentShow(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun intentPin(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_PIN)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private val coordinator: StartupCoordinator by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition {
            !coordinator.startupEvaluated.value
        }
        setContent {
            NotifAITheme {
                AppRoot(coordinator)
            }
        }
    }
}

/**
 * Lifecycle re-evaluation:
 *   DisposableEffect + LifecycleEventObserver is the correct primitive here.
 *   recheck() is synchronous — it doesn't need a coroutine scope.
 *   LaunchedEffect/repeatOnLifecycle would work but implies async work.
 *
 *   ON_RESUME covers:
 *     - Returning from Settings after granting POST_NOTIFICATIONS
 *     - Returning from Settings after granting NOTIFICATION_LISTENER
 *       (belt-and-suspenders alongside the ContentObserver)
 *     - Returning from battery optimization settings
 *     - Task re-entry after process death/recreation
 */
@Composable
fun AppRoot(coordinator: StartupCoordinator) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coordinator.recheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by coordinator.state.collectAsStateWithLifecycle()
    when (state) {
        // recheck() hasn't returned yet. Splash is covering the window.
        // Render nothing — avoids a single-frame flash of empty UI.
        is StartupState.Checking -> Unit
        is StartupState.Result -> {
            val snapshot = (state as StartupState.Result).snapshot
            when {
                // Shouldn't occur (Result always has evaluated=true) but
                // guard defensively so splash logic stays correct.
                !snapshot.evaluated -> Unit
                // One or more hard requirements are missing.
                snapshot.missing.isNotEmpty() -> PermissionsGateScreen(snapshot, coordinator)
                // All requirements met. Advisory items may still be shown
                // inside OperationalScreen as non-blocking recommendations.
                else -> OperationalScreen(snapshot, coordinator)
            }
        }
    }
}

/**
 * Hard-requirement cards use filled Button  (primary visual weight).
 * Advisory cards use OutlinedButton         (secondary visual weight).
 * Visual weight communicates criticality without extra copy.
 *
 * POST_NOTIFICATIONS gets a secondary "Notification settings" button to
 * handle the edge case where the permission is granted but notifications
 * are channel-disabled in system settings.
 *
 * Permission callback result is intentionally ignored — recheck() is the
 * authoritative source of truth. This avoids stale state from the OS
 * callback race condition.
 */
@Composable
fun PermissionsGateScreen(
    snapshot: StartupSnapshot,
    coordinator: StartupCoordinator
) {
    val forceShowAdvisories = false

    val context = LocalContext.current

    val postNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Ignore the Boolean — recheck() is authoritative
        coordinator.recheck()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Setup Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This app runs as a Notification Listener foreground service. " +
                    "The following ${if (snapshot.missing.size > 1) "permissions are" else "permission is"} " +
                    "required before it can start.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        // ── Hard requirements ────────────────────────────────────────
        if (Requirement.POST_NOTIFICATIONS in snapshot.missing) {
            RequirementCard(
                title = "Post Notifications",
                description = "Required to post the persistent foreground-service notification.",
                primaryText = "Grant",
                onPrimary = { MyNotificationUtils.requestPostNotifications(postNotifLauncher) },
                secondaryText = "Application Notification Settings",
                onSecondary = { MyNotificationUtils.startActivityAppNotificationSettings(context) }
            )
        }
        if (Requirement.NOTIFICATION_LISTENER in snapshot.missing) {
            RequirementCard(
                title = "Notification Listener Access",
                description = "Required to read notifications via NotificationListenerService. " +
                        "Enable the `NotifAI` app in the list that opens.",
                primaryText = "Notification read, reply & control",
                onPrimary = { MyNotificationUtils.startActivityNotificationListenerSettings(context) }
            )
        }
        // ── Advisories (visible here too — user can action before entering app) ──
        if (forceShowAdvisories || snapshot.advisories.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Recommended", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
        }
        if (forceShowAdvisories || Advisory.BATTERY_OPTIMIZATION in snapshot.advisories) {
            AdvisoryCard(
                title = "Ignore Battery Optimizations",
                description = "Helps keep the service alive on OEMs with aggressive battery management " +
                        "(Samsung, Xiaomi, OnePlus, etc). Not required, but strongly recommended.",
                buttonText = "Request Exemption",
                onClick = { MyPermissionUtils.startActivityIgnoreBatteryOptimizations(context) }
            )
        }
    }
}

/**
 * By the time we reach here:
 *   ✓ POST_NOTIFICATIONS granted (or API < 33)
 *   ✓ NOTIFICATION_LISTENER enabled
 *   ✓ FGS is legal to start
 *
 * FGS is started via LaunchedEffect(Unit) — fires once when this
 * composable first enters composition. In a real app this would go
 * through a ServiceManager/Repository rather than directly here.
 *
 * Advisory items are shown as non-blocking cards. The user is already
 * in the app and can dismiss or action them at their leisure.
 */
@Composable
fun OperationalScreen(
    snapshot: StartupSnapshot,
    coordinator: StartupCoordinator
) {
    val context = LocalContext.current

    // Start FGS once, only when this screen is first composed (i.e. isReady)
    LaunchedEffect(Unit) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MyNotificationListenerService::class.java)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Service Running", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Notification listener is active.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Show pending advisories as non-blocking recommendations
        if (snapshot.advisories.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Recommended", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (Advisory.BATTERY_OPTIMIZATION in snapshot.advisories) {
                AdvisoryCard(
                    title       = "Battery Optimization Exemption",
                    description = "Recommended for reliable background operation, especially on Samsung, " +
                            "Xiaomi, and OnePlus devices.",
                    buttonText  = "Request Exemption",
                    onClick     = { MyPermissionUtils.startActivityIgnoreBatteryOptimizations(context) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        // Debug/QA convenience — harmless in production
        OutlinedButton(onClick = { coordinator.recheck() }) {
            Text("Re-check Permissions")
        }
    }
}

/**
 * Card for a hard Requirement.
 * Filled primary Button signals "you must do this".
 * Optional OutlinedButton secondary action for edge cases.
 */
@Composable
private fun RequirementCard(
    title: String,
    description: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPrimary) {
                    Text(primaryText)
                }
                if (secondaryText != null && onSecondary != null) {
                    OutlinedButton(onClick = onSecondary) {
                        Text(secondaryText)
                    }
                }
            }
        }
    }
}

/**
 * Card for an Advisory.
 * OutlinedButton signals "this is recommended, not required".
 */
@Composable
private fun AdvisoryCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}
