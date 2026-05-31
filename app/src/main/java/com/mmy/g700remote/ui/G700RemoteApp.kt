package com.mmy.g700remote.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmy.g700remote.R
import com.mmy.g700remote.BuildConfig
import com.mmy.g700remote.ConnectedCarNotificationService
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.G700RemoteViewModel
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.data.AppColorMode
import com.mmy.g700remote.data.AppLanguage
import com.mmy.g700remote.data.AppTheme
import com.mmy.g700remote.data.AppUpdateInfo
import com.mmy.g700remote.data.AppUpdateState
import com.mmy.g700remote.data.LockStateMapping
import com.mmy.g700remote.data.LockCommandProgress
import com.mmy.g700remote.data.NavigationHistoryEntry
import com.mmy.g700remote.data.NavigationShareResult
import com.mmy.g700remote.data.PairedDevice
import com.mmy.g700remote.data.RemoteUiState
import com.mmy.g700remote.data.UpdateGateReason
import com.mmy.g700remote.data.VehicleTelemetry
import com.mmy.g700remote.protocol.ClimateAction
import com.mmy.g700remote.protocol.MirrorAction
import com.mmy.g700remote.protocol.NavigationShareParser
import com.mmy.g700remote.protocol.OnOffAction
import com.mmy.g700remote.protocol.OpenCloseAction
import com.mmy.g700remote.protocol.ParkingChargeAction
import com.mmy.g700remote.protocol.RaceChargeAction
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.SeatPosition
import com.mmy.g700remote.protocol.WindowAction
import com.mmy.g700remote.security.LocalAuthGate
import com.mmy.g700remote.ui.theme.G700RemoteTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }
private const val DISPLAY_MIRROR_PROJECT_URL = "https://github.com/Baghdady92/DisplayMirror"
private const val AIRCON_START_FAN_SPEED = 3

@Composable
private fun tr(text: String): String = translate(LocalAppLanguage.current, text)

private fun translate(language: AppLanguage, text: String): String =
    if (language == AppLanguage.Arabic) ArabicTranslations[text] ?: text else text

@Composable
fun G700RemoteApp(
    activity: Activity,
    permissionsGranted: Boolean,
    sharedNavigationText: String?,
    showUpdates: Boolean,
    onSharedNavigationConsumed: () -> Unit,
    onUpdatesShown: () -> Unit,
    onRequestPermissions: () -> Unit,
    onShareLog: (String) -> Unit,
    viewModel: G700RemoteViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val language = state.appLanguage
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val authGate = remember { LocalAuthGate() }
    var pendingConfirmation by remember { mutableStateOf<RemoteCommand?>(null) }
    var demoMode by rememberSaveable { mutableStateOf(false) }
    var demoTelemetry by remember { mutableStateOf(demoTelemetry()) }
    var requestedTab by remember { mutableStateOf<AppTab?>(null) }
    var navigationShareResult by remember { mutableStateOf<NavigationShareResult?>(null) }
    var navigationShareInProgress by remember { mutableStateOf(false) }
    var showStandaloneHistory by rememberSaveable { mutableStateOf(false) }
    var wasRecentlyConnected by remember { mutableStateOf(false) }
    var showReleaseNotes by rememberSaveable { mutableStateOf(false) }

    fun showUserNotice(message: String) {
        scope.launch { snackbarHostState.showSnackbar(translate(language, message)) }
    }

    fun submit(command: RemoteCommand) {
        if (demoMode) {
            demoTelemetry = applyDemoCommand(demoTelemetry, command)
            scope.launch { snackbarHostState.showSnackbar(translate(language, "Demo mode only. No command was sent to the car.")) }
            return
        }
        if (!command.sensitive) {
            viewModel.send(command)
            return
        }
        if (state.localAuthEnabled) {
            authGate.authenticate(
                activity = activity,
                title = "${translate(language, "Confirm")} ${command.displayName()}",
                subtitle = translate(language, "Use biometrics or device PIN to continue."),
                onSuccess = { viewModel.send(command) },
                onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )
        } else {
            pendingConfirmation = command
        }
    }

    fun sendNavigationText(text: String) {
        navigationShareInProgress = true
        viewModel.sendSharedNavigation(text) { result ->
            scope.launch {
                navigationShareInProgress = false
                navigationShareResult = result
            }
        }
    }

    fun resendNavigationHistory(entry: NavigationHistoryEntry) {
        viewModel.resendNavigationHistory(entry.id) { result ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    translate(
                        language,
                        if (result.sent) "Destination sent to car" else "Destination could not be sent",
                    ) + if (!result.sent && !result.errorMessage.isNullOrBlank()) {
                        ": ${result.errorMessage}"
                    } else {
                        ""
                    },
                )
            }
        }
    }

    val displayedState = if (demoMode) {
        state.copy(
            connectionState = RemoteConnectionState.Ready(TransportKind.Ble),
            pairedDevice = PairedDevice("Demo mode", "DEMO", TransportKind.Ble),
            telemetry = demoTelemetry,
            demoMode = true,
        )
    } else {
        state
    }

    G700RemoteTheme(appTheme = state.appTheme, colorMode = state.appColorMode) {
        CompositionLocalProvider(
            LocalAppLanguage provides language,
            LocalLayoutDirection provides if (language == AppLanguage.Arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            pendingConfirmation?.let { command ->
                AlertDialog(
                    onDismissRequest = { pendingConfirmation = null },
                    title = { Text(tr("Confirm command")) },
                    text = { Text(command.displayName()) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingConfirmation = null
                                viewModel.send(command)
                            },
                        ) { Text(tr("Send")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingConfirmation = null }) { Text(tr("Cancel")) }
                    },
                )
            }
            navigationShareResult?.let { result ->
                NavigationShareResultDialog(
                    result = result,
                    onDismiss = { navigationShareResult = null },
                    onOpenHistory = {
                        navigationShareResult = null
                        if (displayedState.pairedDevice == null) {
                            showStandaloneHistory = true
                        } else {
                            requestedTab = AppTab.NavigationHistory
                        }
                    },
                )
            }
            if (navigationShareInProgress) {
                NavigationShareProgressDialog()
            }
            if (showReleaseNotes) {
                ReleaseNotesDialog(
                    initialLanguage = language,
                    onDismiss = {
                        showReleaseNotes = false
                        viewModel.markReleaseNotesSeen(BuildConfig.VERSION_NAME)
                    },
                )
            }

    LaunchedEffect(state.connectionState) {
        when (state.connectionState) {
            is RemoteConnectionState.Ready -> wasRecentlyConnected = true
            RemoteConnectionState.Disconnected,
            is RemoteConnectionState.Error,
            -> {
                if (wasRecentlyConnected) {
                    wasRecentlyConnected = false
                    snackbarHostState.showSnackbar(translate(language, "DisplayMirror disconnected"))
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(state.lastError) {
        state.lastError?.let { message ->
            if (!message.isRoutineConnectionNotice()) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(state.lastNavigationStatus) {
        state.lastNavigationStatus?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(updateState.message) {
        updateState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUpdateMessage()
        }
    }

    LaunchedEffect(sharedNavigationText) {
        val text = sharedNavigationText
        if (!text.isNullOrBlank()) {
            sendNavigationText(text)
            onSharedNavigationConsumed()
        }
    }

    LaunchedEffect(state.lastSeenReleaseNotesVersion) {
        if (state.lastSeenReleaseNotesVersion != BuildConfig.VERSION_NAME) {
            showReleaseNotes = true
        }
    }

    LaunchedEffect(state.connectionState, state.connectedNotificationEnabled, demoMode) {
        val shouldRun = !demoMode &&
            state.connectedNotificationEnabled &&
            state.connectionState is RemoteConnectionState.Ready
        runCatching { ConnectedCarNotificationService.setRunning(activity, shouldRun) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onForeground()
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (displayedState.pairedDevice != null) 108.dp else 0.dp),
            )
        },
    ) { padding ->
        if (updateState.isUseBlocked) {
            UpdateGateScreen(
                updateState = updateState,
                onCheckForUpdates = viewModel::checkForUpdates,
                onDownloadUpdate = { viewModel.downloadAndInstallUpdate(activity, it) },
                modifier = Modifier.padding(padding),
            )
        } else if (showStandaloneHistory && !demoMode && state.pairedDevice == null) {
            NavigationHistoryScreen(
                state = state,
                onResendNavigationHistory = ::resendNavigationHistory,
                onDelete = viewModel::deleteNavigationHistory,
                onClearAll = viewModel::clearNavigationHistory,
                onBack = { showStandaloneHistory = false },
                modifier = Modifier.padding(padding),
            )
        } else if (demoMode) {
            MainRemoteScaffold(
                state = displayedState,
                onCommand = ::submit,
                onReconnect = {},
                onDisconnect = { demoMode = false },
                onStartScan = viewModel::startScan,
                onPair = viewModel::pairAndConnect,
                onClearPairing = viewModel::clearPairing,
                onPairingCodeChanged = viewModel::setPairingCode,
                onBleEnabledChanged = viewModel::setBleEnabled,
                onLanEnabledChanged = viewModel::setLanEnabled,
                onConnectionPreferenceChanged = viewModel::setConnectionPreference,
                onAppLanguageChanged = viewModel::setAppLanguage,
                onAppThemeChanged = viewModel::setAppTheme,
                onAppColorModeChanged = viewModel::setAppColorMode,
                onRegionalFeaturesChanged = viewModel::setRegionalFeaturesEnabled,
                onLocalAuthChanged = viewModel::setLocalAuthEnabled,
                onLockMappingChanged = viewModel::setLockStateMapping,
                onLoggingChanged = viewModel::setLoggingEnabled,
                onConnectedNotificationChanged = viewModel::setConnectedNotificationEnabled,
                updateState = updateState,
                onCheckForUpdates = viewModel::checkForUpdates,
                onDownloadUpdate = { viewModel.downloadAndInstallUpdate(activity, it) },
                onShowReleaseNotes = { showReleaseNotes = true },
                onRefresh = { scope.launch { snackbarHostState.showSnackbar(translate(language, "Demo mode only. No command was sent to the car.")) } },
                onShareLog = { onShareLog(viewModel.exportLogText()) },
                onResendNavigationHistory = {
                    scope.launch {
                        snackbarHostState.showSnackbar(translate(language, "Demo mode only. No command was sent to the car."))
                    }
                },
                onDeleteNavigationHistory = viewModel::deleteNavigationHistory,
                onClearNavigationHistory = viewModel::clearNavigationHistory,
                onDemoModeChanged = { enabled -> demoMode = enabled },
                onUserNotice = ::showUserNotice,
                showUpdates = showUpdates,
                onUpdatesShown = onUpdatesShown,
                requestedTab = requestedTab,
                onRequestedTabConsumed = { requestedTab = null },
                contentPadding = padding,
            )
        } else if (state.pairedDevice == null) {
            PairingScreen(
                state = state,
                permissionsGranted = permissionsGranted,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onPair = viewModel::pairAndConnect,
                onPairingCodeChanged = viewModel::setPairingCode,
                onRequestPermissions = onRequestPermissions,
                onStartDemo = { demoMode = true },
                modifier = Modifier.padding(padding),
            )
        } else if (!permissionsGranted) {
            PermissionScreen(
                onRequestPermissions = onRequestPermissions,
                modifier = Modifier.padding(padding),
            )
        } else {
            MainRemoteScaffold(
                state = state,
                onCommand = ::submit,
                onReconnect = viewModel::connectSaved,
                onDisconnect = viewModel::disconnect,
                onStartScan = viewModel::startScan,
                onPair = viewModel::pairAndConnect,
                onClearPairing = viewModel::clearPairing,
                onPairingCodeChanged = viewModel::setPairingCode,
                onBleEnabledChanged = viewModel::setBleEnabled,
                onLanEnabledChanged = viewModel::setLanEnabled,
                onConnectionPreferenceChanged = viewModel::setConnectionPreference,
                onAppLanguageChanged = viewModel::setAppLanguage,
                onAppThemeChanged = viewModel::setAppTheme,
                onAppColorModeChanged = viewModel::setAppColorMode,
                onRegionalFeaturesChanged = viewModel::setRegionalFeaturesEnabled,
                onLocalAuthChanged = viewModel::setLocalAuthEnabled,
                onLockMappingChanged = viewModel::setLockStateMapping,
                onLoggingChanged = viewModel::setLoggingEnabled,
                onConnectedNotificationChanged = viewModel::setConnectedNotificationEnabled,
                updateState = updateState,
                onCheckForUpdates = viewModel::checkForUpdates,
                onDownloadUpdate = { viewModel.downloadAndInstallUpdate(activity, it) },
                onShowReleaseNotes = { showReleaseNotes = true },
                onRefresh = viewModel::refreshNow,
                onShareLog = { onShareLog(viewModel.exportLogText()) },
                onResendNavigationHistory = ::resendNavigationHistory,
                onDeleteNavigationHistory = viewModel::deleteNavigationHistory,
                onClearNavigationHistory = viewModel::clearNavigationHistory,
                onDemoModeChanged = { enabled ->
                    if (enabled) demoTelemetry = demoTelemetry()
                    demoMode = enabled
                },
                onUserNotice = ::showUserNotice,
                showUpdates = showUpdates,
                onUpdatesShown = onUpdatesShown,
                requestedTab = requestedTab,
                onRequestedTabConsumed = { requestedTab = null },
                contentPadding = padding,
            )
        }
    }
    }
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Bluetooth, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(tr("Bluetooth permission required"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            tr("G700 Remote scans for the DisplayMirror BLE service and can also discover DisplayMirror over LAN."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermissions) {
            Text(tr("Grant permissions"))
        }
    }
}

@Composable
private fun NavigationShareResultDialog(
    result: NavigationShareResult,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    result.sent -> tr("Sent to car")
                    result.saved -> tr("Saved for later")
                    else -> tr("Location not sent")
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(result.title, fontWeight = FontWeight.SemiBold)
                Text(
                    if (result.sent) {
                        tr("The destination was sent to DisplayMirror and saved in Shared links.")
                    } else if (result.saved) {
                        "${tr("The destination is saved in Shared links so you can resend it later.")} ${result.errorMessage ?: ""}".trim()
                    } else {
                        result.errorMessage ?: tr("G700 Remote could not read a destination from this share.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (result.saved) {
                TextButton(onClick = onOpenHistory) { Text(tr("Shared links")) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Done")) }
        },
    )
}

@Composable
private fun NavigationShareProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(tr("Preparing location")) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        tr("Reading link details"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        tr("Short Google Maps links can take a few seconds."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ReleaseNotesDialog(
    initialLanguage: AppLanguage,
    onDismiss: () -> Unit,
) {
    var notesLanguage by rememberSaveable { mutableStateOf(initialLanguage) }
    val notes = releaseNotes(notesLanguage)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(notes.title, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SegmentedChoice(
                        label = "EN",
                        selected = notesLanguage == AppLanguage.English,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { notesLanguage = AppLanguage.English },
                    )
                    SegmentedChoice(
                        label = "AR",
                        selected = notesLanguage == AppLanguage.Arabic,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { notesLanguage = AppLanguage.Arabic },
                    )
                }
            }
        },
        text = {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (notesLanguage == AppLanguage.Arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        notes.intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    notes.items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary)
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (notesLanguage == AppLanguage.Arabic) "تم" else "Done")
            }
        },
    )
}

@Composable
private fun UpdateGateScreen(
    updateState: AppUpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (AppUpdateInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            shadowElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    when (updateState.gateReason) {
                        UpdateGateReason.UpdateAvailable -> tr("Update required")
                        UpdateGateReason.StaleCheck -> tr("Version check required")
                        UpdateGateReason.None -> tr("App updates")
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (updateState.gateReason) {
                        UpdateGateReason.UpdateAvailable -> tr("To make the best use of G700 Remote, update now. This version is out of support while a newer release is available.")
                        UpdateGateReason.StaleCheck -> tr("G700 Remote needs to check GitHub releases at least every 7 days before vehicle controls can be used.")
                        UpdateGateReason.None -> tr("G700 Remote is up to date.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                updateState.availableUpdate?.let { info ->
                    Spacer(Modifier.height(12.dp))
                    MetricRow(tr("Available version"), info.versionName)
                }
                Spacer(Modifier.height(18.dp))
                updateState.availableUpdate?.let { info ->
                    Button(
                        onClick = { onDownloadUpdate(info) },
                        enabled = !updateState.isDownloading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Text(
                            if (updateState.isDownloading) {
                                updateState.downloadProgress?.let { "${tr("Downloading")} $it%" } ?: tr("Downloading")
                            } else {
                                tr("Download and install")
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = !updateState.isChecking && !updateState.isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (updateState.isChecking) tr("Checking") else tr("Check for updates"))
                }
            }
        }
    }
}

@Composable
private fun PairingScreen(
    state: RemoteUiState,
    permissionsGranted: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPair: (com.mmy.g700remote.ble.ScannedDevice) -> Unit,
    onPairingCodeChanged: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onStartDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(tr("Set up G700 Remote"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Use your phone as a clean Jetour G700 remote for lock and unlock, climate, windows, lights, charging, and vehicle status when DisplayMirror exposes them."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Section(tr("Setup requirements")) {
                SetupGuideLine(
                    number = "1",
                    title = tr("Install DisplayMirror"),
                    body = tr("The head unit should have the DisplayMirror app prepared by Baghdady92 installed and configured."),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { uriHandler.openUri(DISPLAY_MIRROR_PROJECT_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Open DisplayMirror project"))
                }
                Spacer(Modifier.height(12.dp))
                SetupGuideLine(
                    number = "2",
                    title = tr("Enable Remote Access"),
                    body = tr("In DisplayMirror on the car screen, turn on Remote Access and keep the generated pairing code visible."),
                )
                Spacer(Modifier.height(10.dp))
                SetupGuideLine(
                    number = "3",
                    title = tr("Enter the pairing code here"),
                    body = tr("Type the code below, then scan and select the car connection."),
                )
            }
        }
        item {
            Section(tr("Pairing code")) {
                PairingCodeField(
                    value = state.pairingCode,
                    onValueChange = onPairingCodeChanged,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { if (permissionsGranted) onStartScan() else onRequestPermissions() },
                        enabled = !state.isScanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.BluetoothSearching, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (permissionsGranted) tr("Scan") else tr("Grant permissions"))
                    }
                    OutlinedButton(
                        onClick = onStopScan,
                        enabled = permissionsGranted && state.isScanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(tr("Stop"))
                    }
                }
                if (state.isScanning) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (state.scanResults.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("Scan after entering the pairing code."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.scanResults.isNotEmpty()) {
            item {
                Text(tr("Available connections"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(state.scanResults, key = { it.address }) { device ->
                ElevatedCard(
                    onClick = { onPair(device) },
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Bluetooth, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name ?: tr("Unnamed DisplayMirror device"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${device.transport.label()}  ${device.address}${if (device.transport == TransportKind.Ble) "  RSSI ${device.rssi}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onStartDemo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Try demo mode"))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tr("DisplayMirror app by Baghdady92"),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                tr("Developed by Mahmood Majeed with ❤️ in Bahrain 🇧🇭"),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetupGuideLine(
    number: String,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MainRemoteScaffold(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartScan: () -> Unit,
    onPair: (com.mmy.g700remote.ble.ScannedDevice) -> Unit,
    onClearPairing: () -> Unit,
    onPairingCodeChanged: (String) -> Unit,
    onBleEnabledChanged: (Boolean) -> Unit,
    onLanEnabledChanged: (Boolean) -> Unit,
    onConnectionPreferenceChanged: (ConnectionPreference) -> Unit,
    onAppLanguageChanged: (AppLanguage) -> Unit,
    onAppThemeChanged: (AppTheme) -> Unit,
    onAppColorModeChanged: (AppColorMode) -> Unit,
    onRegionalFeaturesChanged: (Boolean) -> Unit,
    onLocalAuthChanged: (Boolean) -> Unit,
    onLockMappingChanged: (LockStateMapping) -> Unit,
    onLoggingChanged: (Boolean) -> Unit,
    onConnectedNotificationChanged: (Boolean) -> Unit,
    updateState: AppUpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (AppUpdateInfo) -> Unit,
    onShowReleaseNotes: () -> Unit,
    onRefresh: () -> Unit,
    onShareLog: () -> Unit,
    onResendNavigationHistory: (NavigationHistoryEntry) -> Unit,
    onDeleteNavigationHistory: (Long) -> Unit,
    onClearNavigationHistory: () -> Unit,
    onDemoModeChanged: (Boolean) -> Unit,
    onUserNotice: (String) -> Unit,
    showUpdates: Boolean,
    onUpdatesShown: () -> Unit,
    requestedTab: AppTab?,
    onRequestedTabConsumed: () -> Unit,
    contentPadding: PaddingValues,
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    BackHandler(enabled = tab != AppTab.Home) {
        tab = AppTab.Home
    }
    LaunchedEffect(showUpdates) {
        if (showUpdates) {
            tab = AppTab.Settings
            onUpdatesShown()
        }
    }
    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            tab = it
            onRequestedTabConsumed()
        }
    }
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConnectionHeader(
                state = state,
                onHome = { tab = AppTab.Home },
                onReconnect = onReconnect,
                onRefresh = onRefresh,
                onOpenHistory = { tab = AppTab.NavigationHistory },
                onOpenSettings = { tab = AppTab.Settings },
            )
        },
        bottomBar = {
            FloatingNavBar(
                selected = tab,
                onSelected = { tab = it },
            )
        },
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
        when (tab) {
            AppTab.Home -> HomeScreen(state, onCommand, onUserNotice, modifier)
            AppTab.Climate -> ClimateScreen(state, onCommand, onUserNotice, modifier)
            AppTab.Openings -> OpeningsScreen(state, onCommand, modifier)
            AppTab.Charging -> ChargingScreen(state, onCommand, modifier)
            AppTab.Lighting -> LightingScreen(state, onCommand, modifier)
            AppTab.NavigationHistory -> NavigationHistoryScreen(
                state = state,
                onResendNavigationHistory = onResendNavigationHistory,
                onDelete = onDeleteNavigationHistory,
                onClearAll = onClearNavigationHistory,
                modifier = modifier,
            )
            AppTab.Settings -> SettingsScreen(
                state = state,
                onCommand = onCommand,
                onStartScan = onStartScan,
                onPair = onPair,
                onClearPairing = onClearPairing,
                onPairingCodeChanged = onPairingCodeChanged,
                onBleEnabledChanged = onBleEnabledChanged,
                onLanEnabledChanged = onLanEnabledChanged,
                onConnectionPreferenceChanged = onConnectionPreferenceChanged,
                onAppLanguageChanged = onAppLanguageChanged,
                onAppThemeChanged = onAppThemeChanged,
                onAppColorModeChanged = onAppColorModeChanged,
                onRegionalFeaturesChanged = onRegionalFeaturesChanged,
                onLocalAuthChanged = onLocalAuthChanged,
                onLockMappingChanged = onLockMappingChanged,
                onLoggingChanged = onLoggingChanged,
                onConnectedNotificationChanged = onConnectedNotificationChanged,
                updateState = updateState,
                onCheckForUpdates = onCheckForUpdates,
                onDownloadUpdate = onDownloadUpdate,
                onShowReleaseNotes = onShowReleaseNotes,
                onShareLog = onShareLog,
                onDisconnect = onDisconnect,
                onDemoModeChanged = onDemoModeChanged,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ConnectionHeader(
    state: RemoteUiState,
    onHome: () -> Unit,
    onReconnect: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isReady = state.connectionState is RemoteConnectionState.Ready
    val isConnecting = state.connectionState is RemoteConnectionState.Connecting ||
        state.connectionState == RemoteConnectionState.DiscoveringServices ||
        state.connectionState == RemoteConnectionState.EnablingNotifications ||
        state.connectionState == RemoteConnectionState.Handshaking ||
        state.connectionState == RemoteConnectionState.Scanning
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JetourHeaderMark(onClick = onHome)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(state.connectionState.color(), CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        connectionStatusLine(state),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    state.pairedDevice?.name ?: state.pairedDevice?.address ?: tr("No paired device"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExpressiveIconButton(onClick = onOpenHistory) {
                    Icon(Icons.Outlined.Link, contentDescription = tr("Shared links"))
                }
                ExpressiveIconButton(
                    onClick = if (isReady) onRefresh else onReconnect,
                    enabled = !isConnecting && state.pairedDevice != null,
                ) {
                    Icon(
                        if (isReady) Icons.Outlined.Refresh else Icons.Outlined.BluetoothSearching,
                        contentDescription = if (isReady) tr("Refresh status") else tr("Connect"),
                    )
                }
                ExpressiveIconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = tr("Settings"))
                }
            }
        }
    }
}

@Composable
private fun JetourHeaderMark(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_jetour_logomark),
                contentDescription = "JETOUR",
                modifier = Modifier.size(width = 30.dp, height = 26.dp),
            )
        }
    }
}

@Composable
private fun ExpressiveIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.9f else 1f,
        animationSpec = expressiveSpring(),
        label = "icon-press-scale",
    )
    Surface(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ExpressiveActionSurface(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    selected: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = expressiveSpring(),
        label = "surface-press-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            danger && selected -> MaterialTheme.colorScheme.errorContainer
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = expressiveSpring(),
        label = "surface-container-color",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            danger && selected -> MaterialTheme.colorScheme.onErrorContainer
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = expressiveSpring(),
        label = "surface-content-color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        },
        animationSpec = expressiveSpring(),
        label = "surface-border-color",
    )
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (enabled) containerColor else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f))
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    content = { content() },
                )
            }
        }
    }
}

private fun <T> expressiveSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

@Composable
private fun FloatingNavBar(
    selected: AppTab,
    onSelected: (AppTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTab.entries
                    .filter { it.showInBottomBar }
                    .forEach { item ->
                        NavPillItem(
                            item = item,
                            selected = selected == item,
                            onClick = { onSelected(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    item: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = expressiveSpring(),
        label = "nav-pill-scale",
    )
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = expressiveSpring(),
        label = "nav-pill-color",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = expressiveSpring(),
        label = "nav-pill-content",
    )
    Surface(
        modifier = modifier
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(26.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(item.icon, contentDescription = tr(item.label), modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                tr(item.label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    onUserNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    val locked = isLocked(state)
    val lockActionIsUnlock = locked == true
    val airOn = state.telemetry.fanSpeed?.let { it > 0 }
    val lockPending = state.pendingLockCommand != null
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
                ) {
                    Text(
                        lockLabel(state),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (!ready) {
                        Text(
                            tr("Connect to DisplayMirror"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    HeroCommandButton(
                        text = if (lockActionIsUnlock) tr("Unlock") else tr("Lock"),
                        icon = if (lockActionIsUnlock) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        enabled = ready && !lockPending,
                        loading = lockPending,
                        danger = lockActionIsUnlock,
                        onClick = { onCommand(if (lockActionIsUnlock) RemoteCommand.Unlock else RemoteCommand.Lock) },
                    )
                }
            }
        }
        item {
            TelemetryGrid(state)
        }
        item {
            Section(tr("Quick Actions")) {
                ModeToggleGrid(
                    toggles = listOf(
                        ToggleSpec(
                            label = tr("Air conditioner"),
                            icon = Icons.Outlined.AcUnit,
                            checked = airOn,
                            onOn = { requestCabinAirToggle(state, onCommand, onUserNotice) },
                            onOff = { requestCabinAirToggle(state, onCommand, onUserNotice) },
                        ),
                        ToggleSpec(
                            label = tr("Hazards"),
                            icon = Icons.Outlined.Warning,
                            checked = state.telemetry.hazardsOn,
                            onOn = { onCommand(RemoteCommand.Hazards(OnOffAction.On)) },
                            onOff = { onCommand(RemoteCommand.Hazards(OnOffAction.Off)) },
                        ),
                    ),
                    enabled = ready,
                )
            }
        }
    }
}

@Composable
private fun ClimateScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    onUserNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    val telemetry = state.telemetry
    val airOn = telemetry.fanSpeed?.let { it > 0 }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Section(tr("Cabin")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeToggleBox(
                        spec = ToggleSpec(
                            label = tr("Air conditioner"),
                            icon = Icons.Outlined.AcUnit,
                            checked = airOn,
                            onOn = { requestCabinAirToggle(state, onCommand, onUserNotice) },
                            onOff = { requestCabinAirToggle(state, onCommand, onUserNotice) },
                        ),
                        enabled = ready,
                        modifier = Modifier.weight(2f),
                    )
                    ModeToggleBox(
                        spec = ToggleSpec(
                            label = tr("A/C"),
                            icon = Icons.Outlined.AcUnit,
                            checked = telemetry.acOn,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.AcOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.AcOff)) },
                        ),
                        enabled = ready && airOn == true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TemperatureControlCard(
                        label = tr("Left"),
                        value = telemetry.tempLeft ?: 22.0,
                        enabled = ready,
                        modifier = Modifier.weight(1f),
                    ) {
                        onCommand(RemoteCommand.Climate(ClimateAction.SetTempLeft, numericValue = it))
                    }
                    TemperatureControlCard(
                        label = tr("Right"),
                        value = telemetry.tempRight ?: 22.0,
                        enabled = ready,
                        modifier = Modifier.weight(1f),
                    ) {
                        onCommand(RemoteCommand.Climate(ClimateAction.SetTempRight, numericValue = it))
                    }
                }
                Spacer(Modifier.height(14.dp))
                FanBarControl(
                    value = telemetry.fanSpeed ?: 3,
                    maxLevel = 10,
                    enabled = ready,
                ) {
                    onCommand(RemoteCommand.Climate(ClimateAction.SetFanSpeed, numericValue = it))
                }
            }
        }
        item {
            Section(tr("Modes")) {
                ModeToggleGrid(
                    toggles = listOf(
                        ToggleSpec(tr("Fast cool"), Icons.Outlined.AcUnit, telemetry.fastCool,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.FastCoolOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.FastCoolOff)) }),
                        ToggleSpec(tr("Fast heat"), Icons.Outlined.Thermostat, telemetry.fastHeat,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.FastHeatOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.FastHeatOff)) }),
                        ToggleSpec(tr("Rear defrost"), Icons.Outlined.Window, telemetry.rearDefrost,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.RearDefrostOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.RearDefrostOff)) }),
                        ToggleSpec(tr("Front glass heat"), Icons.Outlined.Window, null,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.FrontHeatOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.FrontHeatOff)) }),
                        ToggleSpec(tr("Auto defrost"), Icons.Outlined.AcUnit, telemetry.autoDefrost,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.AutoDefrostOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.AutoDefrostOff)) }),
                    ) + if (state.regionalFeaturesEnabled) {
                        listOf(
                            ToggleSpec(tr("PM2.5 filter"), Icons.Outlined.Air, null,
                                onOn = { onCommand(RemoteCommand.Climate(ClimateAction.Pm25On)) },
                                onOff = { onCommand(RemoteCommand.Climate(ClimateAction.Pm25Off)) }),
                            ToggleSpec(tr("Steering heat"), Icons.Outlined.Thermostat, null,
                                onOn = { onCommand(RemoteCommand.Climate(ClimateAction.SteeringHeatOn)) },
                                onOff = { onCommand(RemoteCommand.Climate(ClimateAction.SteeringHeatOff)) }),
                        )
                    } else {
                        emptyList()
                    },
                    enabled = ready,
                )
            }
        }
        item {
            Section(tr("Parking AC")) {
                ActionBoxGrid(
                    actions = listOf(
                        ActionSpec(tr("Smart"), Icons.Outlined.AcUnit) {
                            onCommand(RemoteCommand.Climate(ClimateAction.ParkingAcSmart))
                        },
                        ActionSpec(tr("Vent"), Icons.Outlined.Air) {
                            onCommand(RemoteCommand.Climate(ClimateAction.ParkingAcVent))
                        },
                        ActionSpec(tr("Off"), Icons.Outlined.PowerSettingsNew) {
                            onCommand(RemoteCommand.Climate(ClimateAction.ParkingAcOff))
                        },
                    ),
                    enabled = ready,
                    columns = 3,
                )
            }
        }
        item {
            Section(tr("Seats")) {
                SeatControlGrid(
                    telemetry = telemetry,
                    enabled = ready,
                    showHeating = state.regionalFeaturesEnabled,
                    onCommand = onCommand,
                )
            }
        }
    }
}

@Composable
private fun OpeningsScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        CompactSection(tr("Windows")) {
            ActionBoxGrid(
                actions = listOf(
                    ActionSpec(tr("Front open"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Window(WindowAction.FrontOpen))
                    },
                    ActionSpec(tr("Front close"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Window(WindowAction.FrontClose))
                    },
                    ActionSpec(tr("Open all"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Window(WindowAction.OpenAll))
                    },
                    ActionSpec(tr("Close all"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Window(WindowAction.CloseAll))
                    },
                    ActionSpec(tr("Vent"), Icons.Outlined.Air) {
                        onCommand(RemoteCommand.Window(WindowAction.Vent))
                    },
                ),
                enabled = ready,
                fullWidthLastSingle = true,
                itemHeight = 48.dp,
            )
        }
        CompactSection(tr("Sunshade")) {
            ActionBoxGrid(
                actions = listOf(
                    ActionSpec(tr("Open"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Sunshade(OpenCloseAction.Open))
                    },
                    ActionSpec(tr("Close"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Sunshade(OpenCloseAction.Close))
                    },
                ),
                enabled = ready,
                itemHeight = 48.dp,
            )
        }
        CompactSection(tr("Sunroof")) {
            ActionBoxGrid(
                actions = listOf(
                    ActionSpec(tr("Open"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Sunroof(OpenCloseAction.Open))
                    },
                    ActionSpec(tr("Close"), Icons.Outlined.Window) {
                        onCommand(RemoteCommand.Sunroof(OpenCloseAction.Close))
                    },
                ),
                enabled = ready,
                itemHeight = 48.dp,
            )
        }
        CompactSection(tr("Mirrors")) {
            ActionBoxGrid(
                actions = listOf(
                    ActionSpec(tr("Fold"), Icons.Outlined.DirectionsCar) {
                        onCommand(RemoteCommand.Mirror(MirrorAction.Fold))
                    },
                    ActionSpec(tr("Unfold"), Icons.Outlined.DirectionsCar) {
                        onCommand(RemoteCommand.Mirror(MirrorAction.Unfold))
                    },
                ),
                enabled = ready,
                itemHeight = 48.dp,
            )
        }
    }
}

@Composable
private fun ChargingScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    var target by remember(state.telemetry.parkingChargeTargetSoc) {
        mutableFloatStateOf((state.telemetry.parkingChargeTargetSoc ?: 50).toFloat())
    }
    var raceTarget by remember(state.telemetry.raceChargeTarget) {
        mutableFloatStateOf((state.telemetry.raceChargeTarget ?: 80).toFloat())
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Section(tr("Target SOC")) {
            Text("${target.toInt()}%", style = MaterialTheme.typography.headlineSmall)
            Slider(
                value = target,
                onValueChange = { target = it },
                valueRange = 25f..70f,
                steps = 44,
                enabled = ready,
                onValueChangeFinished = { onCommand(RemoteCommand.Soc(target.toInt())) },
            )
        }
        Section(tr("Parking Charge")) {
            ActionBoxGrid(
                actions = listOf(
                    ActionSpec(tr("Off"), Icons.Outlined.PowerSettingsNew) {
                        onCommand(RemoteCommand.ParkingCharge(ParkingChargeAction.Off))
                    },
                    ActionSpec(tr("Quiet"), Icons.Outlined.Bolt) {
                        onCommand(RemoteCommand.ParkingCharge(ParkingChargeAction.Quiet))
                    },
                    ActionSpec(tr("Fast"), Icons.Outlined.Bolt) {
                        onCommand(RemoteCommand.ParkingCharge(ParkingChargeAction.Fast))
                    },
                ),
                enabled = ready,
                columns = 3,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onCommand(RemoteCommand.ParkingCharge(ParkingChargeAction.Status)) },
                enabled = ready,
            ) {
                Text(tr("Refresh charge mode"))
            }
        }
        Section(tr("Race Charge")) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("State"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when (state.telemetry.raceChargeActive) {
                        true -> tr("Active")
                        false -> tr("Off")
                        null -> tr("Unknown")
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${raceTarget.toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Slider(
                value = raceTarget,
                onValueChange = { raceTarget = it },
                valueRange = 25f..100f,
                steps = 74,
                enabled = ready,
            )
            ActionBoxGrid(
                actions = listOf(
                    ActionSpec(tr("Start"), Icons.Outlined.Bolt) {
                        onCommand(RemoteCommand.RaceCharge(RaceChargeAction.Start, raceTarget.toInt()))
                    },
                    ActionSpec(tr("Stop"), Icons.Outlined.PowerSettingsNew) {
                        onCommand(RemoteCommand.RaceCharge(RaceChargeAction.Stop))
                    },
                    ActionSpec(tr("Status"), Icons.Outlined.Refresh) {
                        onCommand(RemoteCommand.RaceCharge(RaceChargeAction.Status))
                    },
                ),
                enabled = ready,
                columns = 3,
            )
            Spacer(Modifier.height(8.dp))
            MetricRow(tr("Target"), state.telemetry.raceChargeTarget?.let { "$it%" } ?: tr("Unknown"))
            MetricRow(tr("ETA"), state.telemetry.raceChargeEtaMin?.let { "$it ${tr("min")}" } ?: tr("Unknown"))
        }
        Section(tr("Charge Telemetry")) {
            MetricRow(tr("Mode"), formatChargeMode(state.telemetry.chargeMode) ?: state.telemetry.parkingChargeMode?.toString() ?: tr("Unknown"))
            MetricRow(tr("ETA"), state.telemetry.parkingChargeEtaMin?.let { "$it ${tr("min")}" } ?: tr("Unknown"))
            MetricRow(tr("Pack voltage"), state.telemetry.packVoltage?.let { "%.1f V".format(it) } ?: tr("Unknown"))
            MetricRow(tr("Pack current"), state.telemetry.packCurrent?.let { "%.1f A".format(it) } ?: tr("Unknown"))
            MetricRow(tr("Pack power"), state.telemetry.packPower?.let { "%.1f kW".format(it) } ?: tr("Unknown"))
            MetricRow(tr("Safety SOC floor"), state.telemetry.safetySocFloor?.let { "$it%" } ?: tr("Unknown"))
        }
    }
}

@Composable
private fun LightingScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Section(tr("Lighting")) {
            ModeToggleGrid(
                toggles = listOf(
                    ToggleSpec(
                        label = tr("Hazards"),
                        icon = Icons.Outlined.Warning,
                        checked = state.telemetry.hazardsOn,
                        onOn = { onCommand(RemoteCommand.Hazards(OnOffAction.On)) },
                        onOff = { onCommand(RemoteCommand.Hazards(OnOffAction.Off)) },
                    ),
                    ToggleSpec(
                        label = tr("Daytime Running Lights"),
                        icon = Icons.Outlined.Lightbulb,
                        checked = state.telemetry.drlOn,
                        onOn = { onCommand(RemoteCommand.Drl(OnOffAction.On)) },
                        onOff = { onCommand(RemoteCommand.Drl(OnOffAction.Off)) },
                    ),
                ),
                enabled = ready,
                columns = 1,
            )
        }
    }
}

@Composable
private fun NavigationHistoryScreen(
    state: RemoteUiState,
    onResendNavigationHistory: (NavigationHistoryEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<NavigationHistoryEntry?>(null) }
    val context = LocalContext.current
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(tr("Clear shared links?")) },
            text = { Text(tr("This removes all saved shared-location history from this phone.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearAll()
                    },
                ) { Text(tr("Clear all")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(tr("Cancel")) }
            },
        )
    }
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr("Delete shared link?")) },
            text = { Text("${tr("Remove this destination from history?")}\n${entry.title}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(entry.id)
                    },
                ) { Text(tr("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(tr("Cancel")) }
            },
        )
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (onBack != null) {
                    ExpressiveIconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = tr("Back"))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(tr("Shared links"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        tr("Locations shared to the car can be resent from here."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.navigationHistory.isNotEmpty()) {
                    ExpressiveIconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = tr("Clear all"))
                    }
                }
            }
        }
        if (state.navigationHistory.isEmpty()) {
            item {
                Section(tr("History")) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(tr("No shared links yet"), fontWeight = FontWeight.SemiBold)
                        Text(
                            tr("Share a Google Maps place or geo link to G700 Remote."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(state.navigationHistory, key = { it.id }) { entry ->
                NavigationHistoryRow(
                    entry = entry,
                    onOpenOriginal = { openOriginalShare(context, entry) },
                    onResend = { onResendNavigationHistory(entry) },
                    onDelete = { pendingDelete = entry },
                )
            }
        }
    }
}

@Composable
private fun NavigationHistoryRow(
    entry: NavigationHistoryEntry,
    onOpenOriginal: () -> Unit,
    onResend: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(
            enabled = entry.openableUri() != null,
            onClick = onOpenOriginal,
        ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.detail,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.previewText?.let { preview ->
                    Text(
                        preview,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    formatTimeAgo(entry.sentAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExpressiveIconButton(onClick = onResend) {
                Icon(Icons.Outlined.Send, contentDescription = tr("Resend"))
            }
            ExpressiveIconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Close, contentDescription = tr("Delete"))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    onStartScan: () -> Unit,
    onPair: (com.mmy.g700remote.ble.ScannedDevice) -> Unit,
    onClearPairing: () -> Unit,
    onPairingCodeChanged: (String) -> Unit,
    onBleEnabledChanged: (Boolean) -> Unit,
    onLanEnabledChanged: (Boolean) -> Unit,
    onConnectionPreferenceChanged: (ConnectionPreference) -> Unit,
    onAppLanguageChanged: (AppLanguage) -> Unit,
    onAppThemeChanged: (AppTheme) -> Unit,
    onAppColorModeChanged: (AppColorMode) -> Unit,
    onRegionalFeaturesChanged: (Boolean) -> Unit,
    onLocalAuthChanged: (Boolean) -> Unit,
    onLockMappingChanged: (LockStateMapping) -> Unit,
    onLoggingChanged: (Boolean) -> Unit,
    onConnectedNotificationChanged: (Boolean) -> Unit,
    updateState: AppUpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (AppUpdateInfo) -> Unit,
    onShowReleaseNotes: () -> Unit,
    onShareLog: () -> Unit,
    onDisconnect: () -> Unit,
    onDemoModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Section(tr("Connectivity & pairing")) {
                Button(
                    onClick = onDisconnect,
                    enabled = state.connectionState is RemoteConnectionState.Ready,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Disconnect"))
                }
                Spacer(Modifier.height(10.dp))
                MetricRow(tr("State"), state.connectionState.label())
                MetricRow(tr("Active"), state.connectionState.activeTransportLabel())
                state.lastStatusRefreshMillis?.let {
                    MetricRow(tr("Last refresh"), formatFriendlyRefreshTime(it))
                }
                MetricRow(tr("Device"), state.pairedDevice?.name ?: tr("Unnamed"))
                MetricRow(tr("Address"), state.pairedDevice?.address ?: tr("not paired"))
                MetricRow(tr("Transport"), state.pairedDevice?.transport?.label() ?: tr("Unknown"))
                Spacer(Modifier.height(10.dp))
                PairingCodeField(
                    value = state.pairingCode,
                    onValueChange = onPairingCodeChanged,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onStartScan, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                        Icon(Icons.Outlined.BluetoothSearching, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Scan"))
                    }
                    OutlinedButton(onClick = onClearPairing, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Clear"))
                    }
                }
                if (state.isScanning) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(14.dp))
                ProtocolSwitchRow(
                    title = tr("Bluetooth LE"),
                    subtitle = tr("Best for close range and remote-key use."),
                    checked = state.bleEnabled,
                    icon = Icons.Outlined.Bluetooth,
                    onCheckedChange = onBleEnabledChanged,
                )
                ProtocolSwitchRow(
                    title = tr("LAN / mDNS"),
                    subtitle = tr("Uses CarKey on _carkey._tcp. port 9274 when the phone and head unit share a network."),
                    checked = state.lanEnabled,
                    icon = Icons.Outlined.Wifi,
                    onCheckedChange = onLanEnabledChanged,
                )
                ProtocolSwitchRow(
                    title = tr("Connected notification"),
                    subtitle = tr("Keep a persistent notification with light status and quick actions while connected."),
                    checked = state.connectedNotificationEnabled,
                    icon = Icons.Outlined.DirectionsCar,
                    onCheckedChange = onConnectedNotificationChanged,
                )
                Spacer(Modifier.height(8.dp))
                Text(tr("Priority"), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                PreferenceSelector(
                    preference = state.connectionPreference,
                    onPreference = onConnectionPreferenceChanged,
                )
            }
        }
        items(state.scanResults, key = { it.address }) { device ->
            ElevatedCard(onClick = { onPair(device) }, shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (device.transport == TransportKind.Lan) Icons.Outlined.Wifi else Icons.Outlined.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name ?: tr("Unnamed"))
                        Text(
                            "${device.transport.label()}  ${device.address}${if (device.transport == TransportKind.Ble) "  RSSI ${device.rssi}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Section(tr("Language")) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text(tr("App language"), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    SegmentedChoice("English", selected = state.appLanguage == AppLanguage.English, enabled = true) {
                        onAppLanguageChanged(AppLanguage.English)
                    }
                    Spacer(Modifier.width(6.dp))
                    SegmentedChoice("العربية", selected = state.appLanguage == AppLanguage.Arabic, enabled = true) {
                        onAppLanguageChanged(AppLanguage.Arabic)
                    }
                }
            }
        }
        item {
            Section(tr("Themes & color")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SegmentedChoice(
                        label = tr("Dark"),
                        selected = state.appColorMode == AppColorMode.Dark,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onAppColorModeChanged(AppColorMode.Dark) },
                    )
                    SegmentedChoice(
                        label = tr("Light"),
                        selected = state.appColorMode == AppColorMode.Light,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onAppColorModeChanged(AppColorMode.Light) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                ThemeSelector(
                    selected = state.appTheme,
                    onTheme = onAppThemeChanged,
                )
            }
        }
        item {
            Section(tr("Security")) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Biometric or PIN gate"))
                        Text(
                            tr("Unlock, opening controls, and charge mode changes are gated."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.localAuthEnabled, onCheckedChange = onLocalAuthChanged)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    tr("This app controls the DisplayMirror head-unit protocol. It is not an OEM-certified digital key."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Section(tr("Additional regional features")) {
                ProtocolSwitchRow(
                    title = tr("Show unavailable regional features"),
                    subtitle = tr("Adds steering heat, seat heating, and PM2.5 controls for cars that support them."),
                    checked = state.regionalFeaturesEnabled,
                    icon = Icons.Outlined.Settings,
                    onCheckedChange = onRegionalFeaturesChanged,
                )
            }
        }
        item {
            Section(tr("App updates")) {
                MetricRow(tr("Current version"), BuildConfig.VERSION_NAME)
                updateState.lastCheckedMillis?.let {
                    MetricRow(tr("Last checked"), formatTimeAgo(it))
                }
                updateState.availableUpdate?.let { info ->
                    MetricRow(tr("Available version"), info.versionName)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onDownloadUpdate(info) },
                        enabled = !updateState.isDownloading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (updateState.isDownloading) {
                                updateState.downloadProgress?.let { "${tr("Downloading")} $it%" } ?: tr("Downloading")
                            } else {
                                tr("Download and install")
                            },
                        )
                    }
                } ?: Text(
                    tr("Checks GitHub releases for signed APK updates twice daily."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = !updateState.isChecking && !updateState.isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (updateState.isChecking) tr("Checking") else tr("Check for updates"))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onShowReleaseNotes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(tr("What's new in this version"))
                }
            }
        }
        item {
            Section(tr("Advanced")) {
                OutlinedButton(
                    onClick = { advancedExpanded = !advancedExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (advancedExpanded) tr("Hide diagnostics") else tr("Show diagnostics"))
                }
                if (advancedExpanded) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Protocol logging"))
                            Text(
                                tr("Keep off unless troubleshooting."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.loggingEnabled, onCheckedChange = onLoggingChanged)
                    }
                    Spacer(Modifier.height(10.dp))
                    MetricRow(tr("Connection"), state.connectionState.label())
                    MetricRow(tr("Log entries"), state.protocolLog.size.toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onCommand(RemoteCommand.Status) },
                            enabled = state.connectionState is RemoteConnectionState.Ready,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(tr("Status"))
                        }
                        OutlinedButton(
                            onClick = { onCommand(RemoteCommand.Ping) },
                            enabled = state.connectionState is RemoteConnectionState.Ready,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(tr("Ping"))
                        }
                    }
                    state.carLocation?.let { location ->
                        Spacer(Modifier.height(8.dp))
                        MetricRow(tr("Car location"), "%.6f, %.6f".format(location.lat, location.lon))
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.GetLocation) },
                        enabled = state.connectionState is RemoteConnectionState.Ready,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(tr("Get car location"))
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onShareLog,
                        enabled = state.loggingEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Export redacted protocol log"))
                    }
                }
            }
        }
        item {
            Section(tr("Demo mode")) {
                Text(
                    tr("Use this to test app functions when no car is connected. No commands are sent to the car."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                if (state.demoMode) {
                    Button(
                        onClick = { onDemoModeChanged(false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Disable demo mode"))
                    }
                } else {
                    OutlinedButton(
                        onClick = { onDemoModeChanged(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Enable demo mode"))
                    }
                }
            }
        }
        item {
            Text(
                tr("Developed by Mahmood Majeed with ❤️ in Bahrain 🇧🇭"),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class ToggleSpec(
    val label: String,
    val icon: ImageVector,
    val checked: Boolean?,
    val onOn: () -> Unit,
    val onOff: () -> Unit,
)

private data class ActionSpec(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun ThemeSelector(
    selected: AppTheme,
    onTheme: (AppTheme) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTheme.entries.chunked(2).forEach { rowThemes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowThemes.forEach { theme ->
                    SegmentedChoice(
                        label = tr(theme.label()),
                        selected = theme == selected,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onTheme(theme) },
                    )
                }
                if (rowThemes.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TemperatureControlCard(
    label: String,
    value: Double,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValue: (Double) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    formatTemp(value),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onValue(value - 0.5) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("-")
                }
                Button(
                    onClick = { onValue(value + 0.5) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("+")
                }
            }
        }
    }
}

@Composable
private fun FanBarControl(
    value: Int,
    maxLevel: Int,
    enabled: Boolean,
    onValue: (Int) -> Unit,
) {
    val displayLevel = value.coerceIn(0, maxLevel)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Fan"), fontWeight = FontWeight.Medium)
            Text(displayLevel.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            (1..maxLevel).forEach { level ->
                val active = displayLevel >= level
                val barColor by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
                    },
                    animationSpec = expressiveSpring(),
                    label = "fan-bar-color",
                )
                val barHeight by animateDpAsState(
                    targetValue = (18 + level * 3 + if (active) 4 else 0).dp,
                    animationSpec = expressiveSpring(),
                    label = "fan-bar-height",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(enabled = enabled) { onValue(level) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .background(
                                color = barColor,
                                shape = RoundedCornerShape(8.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggleGrid(
    toggles: List<ToggleSpec>,
    enabled: Boolean,
    columns: Int = 2,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        toggles.chunked(columns).forEach { rowToggles ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowToggles.forEach { spec ->
                    ModeToggleBox(
                        spec = spec,
                        enabled = enabled,
                        modifier = if (rowToggles.size == 1) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    )
                }
                repeat(if (rowToggles.size == 1) 0 else columns - rowToggles.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModeToggleBox(
    spec: ToggleSpec,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val active = spec.checked == true
    ExpressiveActionSurface(
        modifier = modifier.height(72.dp),
        enabled = enabled,
        selected = active,
        onClick = { if (active) spec.onOff() else spec.onOn() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    spec.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when (spec.checked) {
                        true -> tr("On")
                        false -> tr("Off")
                        null -> tr("Unknown")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ActionBoxGrid(
    actions: List<ActionSpec>,
    enabled: Boolean,
    columns: Int = 2,
    fullWidthLastSingle: Boolean = false,
    itemHeight: Dp = 68.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(columns).forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowActions.forEach { action ->
                    ExpressiveActionSurface(
                        enabled = enabled,
                        selected = false,
                        onClick = action.onClick,
                        modifier = if (fullWidthLastSingle && rowActions.size == 1) {
                            Modifier.fillMaxWidth().height(itemHeight)
                        } else {
                            Modifier.weight(1f).height(itemHeight)
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(21.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                action.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                repeat(if (fullWidthLastSingle && rowActions.size == 1) 0 else columns - rowActions.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TelemetryGrid(state: RemoteUiState) {
    Section(tr("Vehicle")) {
        val tiles = listOf(
            TileData(tr("Battery"), state.telemetry.batterySoc?.let { "$it%" } ?: tr("Unknown"), Icons.Outlined.ElectricBolt),
            TileData(tr("Fuel"), state.telemetry.fuelPercent?.let { "$it%" } ?: tr("Unknown"), Icons.Outlined.LocalGasStation),
            TileData(tr("Air"), when (state.telemetry.fanSpeed?.let { it > 0 }) {
                true -> tr("On")
                false -> tr("Off")
                null -> tr("Unknown")
            }, Icons.Outlined.AcUnit),
            TileData(tr("Cabin"), state.telemetry.cabinTemp?.let { formatTemp(it) } ?: tr("Unknown"), Icons.Outlined.Thermostat),
            TileData(tr("Coolant"), state.telemetry.coolantTemp?.let { formatTemp(it) } ?: tr("Unknown"), Icons.Outlined.WaterDrop),
            TileData(tr("Outside"), state.telemetry.outdoorTemp?.let { formatTemp(it) } ?: tr("Unknown"), Icons.Outlined.Air),
        )
        tiles.chunked(3).forEachIndexed { index, rowTiles ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowTiles.forEach { tile ->
                    MetricTile(tile.label, tile.value, Modifier.weight(1f), tile.icon)
                }
            }
        }
    }
}

private data class TileData(
    val label: String,
    val value: String,
    val icon: ImageVector,
)

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun CompactSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(9.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun PairingCodeField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() }.take(8))
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(tr("Pairing code")) },
        placeholder = { Text(tr("4 to 8 digits")) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        supportingText = {
            val valid = value.length in 4..8
            Text(
                if (valid) tr("Saved on this phone") else tr("Enter the code shown by DisplayMirror"),
                color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        isError = value.isNotEmpty() && value.length !in 4..8,
    )
}

@Composable
private fun ProtocolSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PreferenceSelector(
    preference: ConnectionPreference,
    onPreference: (ConnectionPreference) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PreferenceButton(ConnectionPreference.BleFirst, preference, tr("BLE first"), Modifier.weight(1f), onPreference)
            PreferenceButton(ConnectionPreference.LanFirst, preference, tr("LAN first"), Modifier.weight(1f), onPreference)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PreferenceButton(ConnectionPreference.BleOnly, preference, tr("BLE only"), Modifier.weight(1f), onPreference)
            PreferenceButton(ConnectionPreference.LanOnly, preference, tr("LAN only"), Modifier.weight(1f), onPreference)
        }
    }
}

@Composable
private fun PreferenceButton(
    value: ConnectionPreference,
    selected: ConnectionPreference,
    label: String,
    modifier: Modifier,
    onPreference: (ConnectionPreference) -> Unit,
) {
    SegmentedChoice(
        label = label,
        selected = value == selected,
        enabled = true,
        modifier = modifier,
        onClick = { onPreference(value) },
    )
}

@Composable
private fun HeroCommandButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = expressiveSpring(),
        label = "hero-command-scale",
    )
    val size by animateDpAsState(
        targetValue = if (pressed && enabled) 130.dp else 142.dp,
        animationSpec = expressiveSpring(),
        label = "hero-command-size",
    )
    Surface(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
        contentColor = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 6.dp,
        shadowElevation = 7.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(42.dp),
                        color = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CommandButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ControlToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean?,
    enabled: Boolean,
    onOn: () -> Unit,
    onOff: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                when (checked) {
                    true -> tr("On")
                    false -> tr("Off")
                    null -> tr("Unknown")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SegmentedChoice(tr("On"), selected = checked == true, enabled = enabled, onClick = onOn)
        Spacer(Modifier.width(6.dp))
        SegmentedChoice(tr("Off"), selected = checked == false, enabled = enabled, onClick = onOff)
    }
}

@Composable
private fun SegmentedChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = expressiveSpring(),
        label = "segmented-choice-scale",
    )
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentPadding = PaddingValues(horizontal = 12.dp),
        interactionSource = interactionSource,
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        },
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ToggleCommands(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onOn: () -> Unit,
    onOff: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        SegmentedChoice(tr("On"), selected = checked, enabled = enabled, onClick = onOn)
        Spacer(Modifier.width(6.dp))
        SegmentedChoice(tr("Off"), selected = !checked, enabled = enabled, onClick = onOff)
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Double,
    unit: String,
    enabled: Boolean,
    onValue: (Double) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text("%.1f %s".format(value, unit), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = { onValue(value - 0.5) }, enabled = enabled) { Text("-") }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = { onValue(value + 0.5) }, enabled = enabled) { Text("+") }
    }
}

@Composable
private fun IntSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onValue: (Int) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.toInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            enabled = enabled,
        )
    }
}

@Composable
private fun CirculationRow(
    value: Int?,
    enabled: Boolean,
    onValue: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tr("Circulation"))
            Text(
                when (value) {
                    1 -> tr("Recirculate")
                    2 -> tr("Fresh air")
                    else -> tr("Unknown")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SegmentedChoice(tr("Recirc"), selected = value == 1, enabled = enabled) { onValue(1) }
        Spacer(Modifier.width(6.dp))
        SegmentedChoice(tr("Fresh"), selected = value == 2, enabled = enabled) { onValue(2) }
    }
}

@Composable
private fun ThreeCommandRow(
    labels: List<String>,
    enabled: Boolean,
    commands: List<() -> Unit>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            OutlinedButton(
                onClick = commands[index],
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SeatControlGrid(
    telemetry: VehicleTelemetry,
    enabled: Boolean,
    showHeating: Boolean,
    onCommand: (RemoteCommand) -> Unit,
) {
    SeatPosition.entries.chunked(2).forEachIndexed { rowIndex, rowSeats ->
        if (rowIndex > 0) Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rowSeats.forEach { seat ->
                SeatControlBox(
                    seat = seat,
                    heatLevel = telemetry.seatHeatLevels[seat.wireValue] ?: 0,
                    ventLevel = telemetry.seatVentLevels[seat.wireValue] ?: 0,
                    enabled = enabled,
                    showHeating = showHeating,
                    modifier = Modifier.weight(1f),
                    onCommand = onCommand,
                )
            }
        }
    }
}

@Composable
private fun SeatControlBox(
    seat: SeatPosition,
    heatLevel: Int,
    ventLevel: Int,
    enabled: Boolean,
    showHeating: Boolean,
    modifier: Modifier = Modifier,
    onCommand: (RemoteCommand) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(tr(seat.label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            SeatFunctionBars(
                label = tr("Vent"),
                icon = Icons.Outlined.Air,
                level = ventLevel,
                warm = false,
                enabled = enabled,
                onClick = {
                    onCommand(
                        RemoteCommand.Climate(
                            ClimateAction.SetSeatVent,
                            position = seat,
                            level = nextSeatLevel(ventLevel),
                        ),
                    )
                },
            )
            if (showHeating) {
                SeatFunctionBars(
                    label = tr("Heat"),
                    icon = Icons.Outlined.Thermostat,
                    level = heatLevel,
                    warm = true,
                    enabled = enabled,
                    onClick = {
                        onCommand(
                            RemoteCommand.Climate(
                                ClimateAction.SetSeatHeat,
                                position = seat,
                                level = nextSeatLevel(heatLevel),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SeatFunctionBars(
    label: String,
    icon: ImageVector,
    level: Int,
    warm: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(5.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        SeatLedIndicator(level = level.coerceIn(0, 3), warm = warm)
    }
}

private fun nextSeatLevel(level: Int): Int = (level.coerceIn(0, 3) + 1) % 4

@Composable
private fun SeatControlRow(
    seat: SeatPosition,
    heatLevel: Int,
    ventLevel: Int,
    enabled: Boolean,
    showHeating: Boolean,
    onCommand: (RemoteCommand) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(tr(seat.label), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (showHeating) {
            SeatLevelRow(
                label = tr("Heating"),
                icon = Icons.Outlined.Thermostat,
                selectedLevel = heatLevel,
                warm = true,
                enabled = enabled,
            ) { level ->
                onCommand(
                    RemoteCommand.Climate(
                        ClimateAction.SetSeatHeat,
                        position = seat,
                        level = level,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        SeatLevelRow(
            label = tr("Ventilation"),
            icon = Icons.Outlined.Air,
            selectedLevel = ventLevel,
            warm = false,
            enabled = enabled,
        ) { level ->
            onCommand(
                RemoteCommand.Climate(
                    ClimateAction.SetSeatVent,
                    position = seat,
                    level = level,
                ),
            )
        }
    }
}

@Composable
private fun SeatLevelRow(
    label: String,
    icon: ImageVector,
    selectedLevel: Int,
    warm: Boolean,
    enabled: Boolean,
    onLevel: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (selectedLevel <= 0) tr("Off") else selectedLevel.coerceIn(0, 3).toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (0..3).forEach { level ->
            OutlinedButton(
                onClick = { onLevel(level) },
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(54.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = if (level == selectedLevel.coerceIn(0, 3)) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = if (warm) Color(0xFFFFDAD1) else Color(0xFFD7E3FF),
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
            ) {
                    SeatLedIndicator(level = level, warm = warm)
                }
            }
        }
    }
}

@Composable
private fun SeatLedIndicator(
    level: Int,
    warm: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..3).forEach { led ->
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height(22.dp)
                    .background(
                        color = if (level >= led) {
                            if (warm) Color(0xFFE86F45) else Color(0xFF4D7FE8)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f)
                        },
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            if (value.contains("°C")) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappingChip(
    label: String,
    mapping: LockStateMapping,
    selected: LockStateMapping,
    onSelected: (LockStateMapping) -> Unit,
) {
    FilterChip(
        selected = mapping == selected,
        onClick = { onSelected(mapping) },
        label = { Text(label) },
    )
}

private enum class AppTab(val label: String, val icon: ImageVector, val showInBottomBar: Boolean = true) {
    Home("Home", Icons.Outlined.DirectionsCar),
    Climate("Climate", Icons.Outlined.Thermostat),
    Openings("Windows", Icons.Outlined.Window),
    Charging("Charge", Icons.Outlined.Bolt),
    Lighting("Lights", Icons.Outlined.Lightbulb),
    NavigationHistory("Links", Icons.Outlined.Link, showInBottomBar = false),
    Settings("Settings", Icons.Outlined.Settings, showInBottomBar = false),
}

@Composable
private fun RemoteConnectionState.color(): Color = when (this) {
    is RemoteConnectionState.Ready -> MaterialTheme.colorScheme.primary
    is RemoteConnectionState.Error -> MaterialTheme.colorScheme.error
    RemoteConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun RemoteConnectionState.label(): String = when (this) {
    RemoteConnectionState.Disconnected -> tr("Disconnected")
    RemoteConnectionState.Scanning -> tr("Scanning")
    is RemoteConnectionState.Connecting -> tr("Connecting")
    RemoteConnectionState.DiscoveringServices -> tr("Discovering")
    RemoteConnectionState.EnablingNotifications -> tr("Subscribing")
    RemoteConnectionState.Handshaking -> tr("Handshaking")
    is RemoteConnectionState.Ready -> "${tr("Connected")} ${transport.label()}"
    is RemoteConnectionState.Error -> tr("Error")
}

@Composable
private fun RemoteConnectionState.activeTransportLabel(): String =
    if (this is RemoteConnectionState.Ready) transport.label() else tr("None")

@Composable
private fun connectionStatusLine(state: RemoteUiState): String =
    state.lastStatusRefreshMillis?.let { "${state.connectionState.label()} • ${formatFriendlyRefreshTime(it)}" }
        ?: state.connectionState.label()

private fun RemoteConnectionState.readyTransportIcon(): ImageVector =
    if (this is RemoteConnectionState.Ready && transport == TransportKind.Lan) Icons.Outlined.Wifi else Icons.Outlined.Bluetooth

@Composable
private fun TransportKind.label(): String = when (this) {
    TransportKind.Ble -> tr("BLE")
    TransportKind.Lan -> tr("LAN")
}

private fun AppTheme.label(): String = when (this) {
    AppTheme.G700Horizon -> "G700 Horizon"
    AppTheme.HimalayaSlate -> "Himalaya Slate"
    AppTheme.NomadStone -> "Nomad Stone"
    AppTheme.ModernPastel -> "Modern Pastel"
    AppTheme.Minimal -> "Minimal"
}

@Composable
private fun lockLabel(state: RemoteUiState): String {
    when (state.pendingLockCommand) {
        LockCommandProgress.Locking -> return tr("Locking...")
        LockCommandProgress.Unlocking -> return tr("Unlocking...")
        null -> Unit
    }
    val raw = state.telemetry.lockState ?: return tr("Lock state Unknown")
    return if (raw == 1) tr("Locked") else tr("Unlocked")
}

private fun isLocked(state: RemoteUiState): Boolean? =
    state.telemetry.lockState?.let { it == 1 }

private fun requestCabinAirToggle(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    onUserNotice: (String) -> Unit,
) {
    val fanSpeed = state.telemetry.fanSpeed
    if (fanSpeed == null || fanSpeed <= 0) {
        onCommand(RemoteCommand.Climate(ClimateAction.SetFanSpeed, numericValue = AIRCON_START_FAN_SPEED))
    } else {
        onUserNotice("Turning off air conditioning is not supported remotely. Please turn it off from the car.")
    }
}

private fun formatTemp(value: Double): String = "%.1f °C".format(value)

private fun formatTimeAgo(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))

@Composable
private fun formatFriendlyRefreshTime(value: Long): String {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = value }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(value))
    return if (
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    ) {
        "${tr("Today")}, $time"
    } else {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(value))
    }
}

private fun formatChargeMode(value: String?): String? =
    value?.uppercase()?.replace('_', ' ')

private fun String.isRoutineConnectionNotice(): Boolean {
    val normalized = lowercase(Locale.US)
    return listOf(
        "no displaymirror device is paired",
        "not connected to displaymirror",
        "connection failed",
        "disconnected",
        "ble disconnected",
        "bluetooth is off",
        "bluetooth permissions",
        "lan",
        "timeout",
        "timed out",
        "unable to connect",
        "no destination found in shared text",
    ).any { normalized.contains(it) }
}

private fun NavigationHistoryEntry.openableUri(): String? =
    originalLink ?: NavigationShareParser.firstShareUri(originalText)

private fun openOriginalShare(context: Context, entry: NavigationHistoryEntry): Boolean {
    val target = entry.openableUri() ?: return false
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun demoTelemetry(): VehicleTelemetry =
    VehicleTelemetry(
        lockState = 1,
        cabinTemp = 24.0,
        outdoorTemp = 32.0,
        coolantTemp = 76.0,
        batterySoc = 68,
        fuelPercent = 57,
        acOn = true,
        tempLeft = 22.0,
        tempRight = 22.0,
        fanSpeed = 3,
        fastCool = false,
        fastHeat = false,
        rearDefrost = false,
        hazardsOn = false,
        drlOn = true,
        parkingChargeTargetSoc = 50,
        chargeMode = "idle",
        raceChargeActive = false,
        raceChargeTarget = 80,
        seatVentLevels = mapOf("fl" to 1, "fr" to 0, "rl" to 0, "rr" to 0),
        seatHeatLevels = mapOf("fl" to 0, "fr" to 0, "rl" to 0, "rr" to 0),
    )

private fun applyDemoCommand(telemetry: VehicleTelemetry, command: RemoteCommand): VehicleTelemetry =
    when (command) {
        RemoteCommand.Lock -> telemetry.copy(lockState = 1)
        RemoteCommand.Unlock -> telemetry.copy(lockState = 2)
        is RemoteCommand.Hazards -> telemetry.copy(hazardsOn = command.action == OnOffAction.On)
        is RemoteCommand.Drl -> telemetry.copy(drlOn = command.action == OnOffAction.On)
        is RemoteCommand.ParkingCharge -> telemetry.copy(
            parkingChargeMode = when (command.action) {
                ParkingChargeAction.Off -> 0
                ParkingChargeAction.Quiet -> 1
                ParkingChargeAction.Fast -> 2
                ParkingChargeAction.Status -> telemetry.parkingChargeMode
            },
        )
        is RemoteCommand.RaceCharge -> telemetry.copy(
            raceChargeActive = command.action == RaceChargeAction.Start ||
                (command.action == RaceChargeAction.Status && telemetry.raceChargeActive == true),
            raceChargeTarget = command.target ?: telemetry.raceChargeTarget,
        )
        is RemoteCommand.Climate -> when (command.action) {
            ClimateAction.AcOn -> telemetry.copy(acOn = true)
            ClimateAction.AcOff -> telemetry.copy(acOn = false)
            ClimateAction.SetTempLeft -> telemetry.copy(tempLeft = command.numericValue?.toDouble() ?: telemetry.tempLeft)
            ClimateAction.SetTempRight -> telemetry.copy(tempRight = command.numericValue?.toDouble() ?: telemetry.tempRight)
            ClimateAction.SetFanSpeed -> telemetry.copy(fanSpeed = command.numericValue?.toInt()?.coerceIn(0, 10) ?: telemetry.fanSpeed)
            ClimateAction.FastCoolOn -> telemetry.copy(fastCool = true)
            ClimateAction.FastCoolOff -> telemetry.copy(fastCool = false)
            ClimateAction.FastHeatOn -> telemetry.copy(fastHeat = true)
            ClimateAction.FastHeatOff -> telemetry.copy(fastHeat = false)
            ClimateAction.RearDefrostOn -> telemetry.copy(rearDefrost = true)
            ClimateAction.RearDefrostOff -> telemetry.copy(rearDefrost = false)
            ClimateAction.SetSeatVent -> {
                val position = command.position?.wireValue ?: return telemetry
                telemetry.copy(seatVentLevels = telemetry.seatVentLevels + (position to (command.level ?: 0).coerceIn(0, 3)))
            }
            ClimateAction.SetSeatHeat -> {
                val position = command.position?.wireValue ?: return telemetry
                telemetry.copy(seatHeatLevels = telemetry.seatHeatLevels + (position to (command.level ?: 0).coerceIn(0, 3)))
            }
            ClimateAction.ParkingAcSmart -> telemetry.copy(fanSpeed = 3, acOn = true)
            ClimateAction.ParkingAcVent -> telemetry.copy(fanSpeed = 3, acOn = false)
            ClimateAction.ParkingAcOff -> telemetry.copy(fanSpeed = 0, acOn = false)
            else -> telemetry
        }
        else -> telemetry
    }

private data class ReleaseNotesCopy(
    val title: String,
    val intro: String,
    val items: List<String>,
)

private fun releaseNotes(language: AppLanguage): ReleaseNotesCopy =
    if (language == AppLanguage.Arabic) {
        ReleaseNotesCopy(
            title = "ما الجديد في الإصدار ${BuildConfig.VERSION_NAME}",
            intro = "تحسينات هذا الإصدار تركز على جعل التطبيق أوضح عند الاتصال، وأسهل للاستخدام اليومي.",
            items = listOf(
                "عند القفل أو الفتح يظهر انتظار واضح لمدة قصيرة قبل تحديث حالة السيارة، حتى لا تتغير الحالة بشكل مفاجئ.",
                "يتم حفظ آخر حالة معروفة للسيارة محلياً، ويمكنك رؤيتها لاحقاً حتى إذا لم تكن السيارة متصلة.",
                "إضافة آخر وقت تحديث بجانب حالة الاتصال في أعلى التطبيق.",
                "إشعار مستمر عند الاتصال يعرض البطارية والوقود مع أزرار سريعة للقفل أو الفتح والتحذير.",
                "تحسين سجل الروابط ليعرض اسم الموقع بشكل أنظف بدون تكرار كود الموقع.",
                "يمكن قراءة هذه الرسالة مرة أخرى من الإعدادات.",
            ),
        )
    } else {
        ReleaseNotesCopy(
            title = "What's new in ${BuildConfig.VERSION_NAME}",
            intro = "This update focuses on clearer connection state and smoother daily use.",
            items = listOf(
                "Lock and unlock now show a short in-progress state before the refreshed car status appears.",
                "The app saves the last known vehicle status locally so you can still view it while offline.",
                "The header now shows the last refresh time beside the connection state.",
                "A connected notification shows battery, fuel, and quick Lock/Unlock and Hazards actions.",
                "Shared-link history is cleaner and avoids repeating location plus codes in the title.",
                "You can reopen these release notes from Settings.",
            ),
        )
    }

private val ArabicTranslations = mapOf(
    "Confirm" to "تأكيد",
    "Use biometrics or device PIN to continue." to "استخدم البصمة أو رمز الهاتف للمتابعة.",
    "Confirm command" to "تأكيد الأمر",
    "Send" to "إرسال",
    "Cancel" to "إلغاء",
    "Back" to "رجوع",
    "Connect" to "اتصال",
    "DisplayMirror disconnected" to "انقطع الاتصال مع DisplayMirror",
    "Bluetooth permission required" to "يلزم إذن البلوتوث",
    "G700 Remote scans for the DisplayMirror BLE service and can also discover DisplayMirror over LAN." to "يبحث G700 Remote عن خدمة DisplayMirror عبر البلوتوث ويمكنه اكتشافها عبر الشبكة المحلية.",
    "Grant permissions" to "منح الأذونات",
    "Set up G700 Remote" to "إعداد G700 Remote",
    "Use your phone as a clean Jetour G700 remote for lock and unlock, climate, windows, lights, charging, and vehicle status when DisplayMirror exposes them." to "استخدم هاتفك كجهاز تحكم بسيط لسيارة Jetour G700 للقفل والفتح والمكيّف والنوافذ والأنوار والشحن وحالة السيارة عندما يتيحها DisplayMirror.",
    "Setup requirements" to "متطلبات الإعداد",
    "Install DisplayMirror" to "تثبيت DisplayMirror",
    "The head unit should have the DisplayMirror app prepared by Baghdady92 installed and configured." to "يجب تثبيت تطبيق DisplayMirror المجهز بواسطة Baghdady92 على شاشة السيارة وإعداده.",
    "Open DisplayMirror project" to "فتح مشروع DisplayMirror",
    "Enable Remote Access" to "تفعيل الوصول عن بعد",
    "In DisplayMirror on the car screen, turn on Remote Access and keep the generated pairing code visible." to "من تطبيق DisplayMirror على شاشة السيارة، فعّل Remote Access واترك رمز الاقتران الظاهر أمامك.",
    "Enter the pairing code here" to "أدخل رمز الاقتران هنا",
    "Type the code below, then scan and select the car connection." to "اكتب الرمز في الأسفل، ثم ابدأ البحث واختر اتصال السيارة.",
    "Pairing code" to "رمز الاقتران",
    "4 to 8 digits" to "من 4 إلى 8 أرقام",
    "Saved on this phone" to "محفوظ على هذا الهاتف",
    "Enter the code shown by DisplayMirror" to "أدخل الرمز الظاهر في DisplayMirror",
    "Scan after entering the pairing code." to "ابدأ البحث بعد إدخال رمز الاقتران.",
    "Available connections" to "الاتصالات المتاحة",
    "DisplayMirror app by Baghdady92" to "تطبيق DisplayMirror بواسطة Baghdady92",
    "Scan" to "بحث",
    "Stop" to "إيقاف",
    "Clear" to "مسح",
    "Try demo mode" to "تجربة الوضع التجريبي",
    "Demo mode" to "وضع تجريبي",
    "Demo mode only. No command was sent to the car." to "الوضع التجريبي فقط. لم يتم إرسال أي أمر للسيارة.",
    "Demo mode is active" to "الوضع التجريبي مفعل",
    "Try the app without the car" to "جرّب التطبيق بدون السيارة",
    "Use simulated vehicle data for app review or testing. No commands are sent to the car." to "استخدم بيانات سيارة افتراضية للمراجعة أو الاختبار. لن يتم إرسال أي أوامر إلى السيارة.",
    "Use this to test app functions when no car is connected. No commands are sent to the car." to "استخدم هذا الوضع لاختبار وظائف التطبيق عند عدم اتصال السيارة. لن يتم إرسال أي أوامر إلى السيارة.",
    "Enable demo mode" to "تفعيل الوضع التجريبي",
    "Disable demo mode" to "إيقاف الوضع التجريبي",
    "Unnamed DisplayMirror device" to "جهاز DisplayMirror بدون اسم",
    "Unnamed" to "بدون اسم",
    "No paired device" to "لا يوجد جهاز مقترن",
    "Refresh status" to "تحديث الحالة",
    "Shared links" to "الروابط المرسلة",
    "Disconnect" to "قطع الاتصال",
    "More" to "المزيد",
    "Settings" to "الإعدادات",
    "Home" to "الرئيسية",
    "Climate" to "المكيّف",
    "Windows" to "النوافذ",
    "Charge" to "الشحن",
    "Lights" to "الإضاءة",
    "Lock" to "قفل",
    "Unlock" to "فتح القفل",
    "Locking..." to "جار القفل...",
    "Unlocking..." to "جار الفتح...",
    "Locked" to "مقفلة",
    "Unlocked" to "مفتوحة",
    "Lock state Unknown" to "حالة القفل غير معروفة",
    "Ready for remote commands" to "جاهز لأوامر التحكم عن بعد",
    "Connect to DisplayMirror" to "اتصل بتطبيق DisplayMirror",
    "Raw state" to "القيمة الخام",
    "Unknown" to "غير معروف",
    "Quick Actions" to "إجراءات سريعة",
    "Vehicle" to "المركبة",
    "Battery" to "البطارية",
    "Battery SOC" to "نسبة البطارية",
    "Fuel" to "الوقود",
    "Coolant" to "حرارة المحرك",
    "AC" to "المكيّف",
    "Air" to "الهواء",
    "Air conditioner" to "تشغيل المكيّف",
    "Turning off air conditioning is not supported remotely. Please turn it off from the car." to "إيقاف المكيّف غير مدعوم عن بعد حالياً. أوقفه من شاشة السيارة.",
    "A/C" to "A/C",
    "Aux AC controls" to "تحكم المكيّف الإضافي",
    "A/C compressor" to "كمبروسر المكيّف",
    "Left" to "اليسار",
    "Right" to "اليمين",
    "Left temp" to "حرارة اليسار",
    "Right temp" to "حرارة اليمين",
    "Cabin" to "المقصورة",
    "Outside" to "الخارج",
    "On" to "تشغيل",
    "Off" to "إيقاف",
    "Fan" to "المروحة",
    "Tap to toggle" to "اضغط للتبديل",
    "Circulation" to "دورة الهواء",
    "Recirculate" to "تدوير الهواء الداخلي",
    "Recirc" to "تدوير",
    "Fresh" to "خارجي",
    "Fresh air" to "هواء خارجي",
    "Modes" to "الأوضاع",
    "Fast cool" to "تبريد سريع",
    "Fast heat" to "تدفئة سريعة",
    "Auto defrost" to "إزالة الضباب تلقائياً",
    "Rear defrost" to "تسخين الزجاج الخلفي",
    "Front glass heat" to "تسخين الزجاج الأمامي",
    "PM2.5 filter" to "فلتر PM2.5",
    "Steering heat" to "تدفئة المقود",
    "Parking AC" to "مكيّف التوقف",
    "Smart" to "ذكي",
    "Vent" to "تهوية",
    "Heat" to "تدفئة",
    "Seats" to "المقاعد",
    "Driver" to "السائق",
    "Passenger" to "الراكب",
    "Rear Left" to "الخلفي الأيسر",
    "Rear Right" to "الخلفي الأيمن",
    "Heating" to "تدفئة",
    "Ventilation" to "تهوية",
    "Door glass controls" to "تحكم زجاج الأبواب",
    "Open all" to "فتح الكل",
    "Close all" to "إغلاق الكل",
    "Front open" to "فتح الأمام",
    "Front close" to "إغلاق الأمام",
    "Open" to "فتح",
    "Close" to "إغلاق",
    "Sunshade" to "ستارة السقف",
    "Sunroof" to "فتحة السقف",
    "Mirrors" to "المرايا",
    "Fold" to "طي",
    "Unfold" to "فتح الطي",
    "Target SOC" to "نسبة الشحن المستهدفة",
    "Parking Charge" to "شحن التوقف",
    "Quiet" to "هادئ",
    "Fast" to "سريع",
    "Race Charge" to "الشحن السريع",
    "Start" to "بدء",
    "Target" to "الهدف",
    "Refresh charge mode" to "تحديث وضع الشحن",
    "Charge Telemetry" to "بيانات الشحن",
    "Mode" to "الوضع",
    "ETA" to "الوقت المتبقي",
    "min" to "دقيقة",
    "Pack voltage" to "جهد البطارية",
    "Pack current" to "تيار البطارية",
    "Pack power" to "قدرة البطارية",
    "Safety SOC floor" to "حد الأمان للشحن",
    "Lighting" to "الإضاءة",
    "Hazards" to "إشارات التحذير",
    "DRL" to "الإضاءة النهارية",
    "Daytime Running Lights" to "أنوار القيادة النهارية",
    "Language" to "اللغة",
    "App language" to "لغة التطبيق",
    "Theme" to "المظهر",
    "Themes & color" to "المظهر والألوان",
    "Dark" to "داكن",
    "Light" to "فاتح",
    "Minimal" to "بسيط",
    "G700 Horizon" to "أفق G700",
    "Himalaya Slate" to "هيمالايا سليت",
    "Nomad Stone" to "حجر الصحراء",
    "Modern Pastel" to "عصري هادئ",
    "Pairing" to "الاقتران",
    "Device" to "الجهاز",
    "Address" to "العنوان",
    "not paired" to "غير مقترن",
    "Transport" to "طريقة الاتصال",
    "Connectivity" to "الاتصال",
    "Connectivity & pairing" to "الاتصال والاقتران",
    "Bluetooth LE" to "بلوتوث LE",
    "Best for close range and remote-key use." to "الأفضل للاستخدام القريب كمفتاح عن بعد.",
    "LAN / mDNS" to "الشبكة المحلية / mDNS",
    "Uses CarKey on _carkey._tcp. port 9274 when the phone and head unit share a network." to "يستخدم CarKey على _carkey._tcp. والمنفذ 9274 عند اتصال الهاتف والشاشة بنفس الشبكة.",
    "Priority" to "الأولوية",
    "BLE first" to "البلوتوث أولاً",
    "LAN first" to "الشبكة أولاً",
    "BLE only" to "البلوتوث فقط",
    "LAN only" to "الشبكة فقط",
    "Active" to "النشط",
    "State" to "الحالة",
    "Last refresh" to "آخر تحديث",
    "Today" to "اليوم",
    "Connected notification" to "إشعار الاتصال",
    "Keep a persistent notification with light status and quick actions while connected." to "يعرض إشعاراً مستمراً عند الاتصال مع حالة مختصرة وأزرار سريعة.",
    "Security" to "الأمان",
    "Additional regional features" to "ميزات إقليمية إضافية",
    "Show unavailable regional features" to "إظهار الميزات غير المتوفرة محلياً",
    "Adds steering heat, seat heating, and PM2.5 controls for cars that support them." to "يعرض تدفئة المقود وتدفئة المقاعد وفلتر PM2.5 للسيارات التي تدعمها.",
    "Biometric or PIN gate" to "حماية بالبصمة أو رمز الهاتف",
    "Unlock, opening controls, and charge mode changes are gated." to "فتح القفل والتحكم بالفتحات وتغيير وضع الشحن تتطلب تأكيداً.",
    "This app controls the DisplayMirror head-unit protocol. It is not an OEM-certified digital key." to "هذا التطبيق يتحكم ببروتوكول DisplayMirror في الشاشة، وليس مفتاحاً رقمياً معتمداً من المصنع.",
    "Advanced" to "متقدم",
    "Hide diagnostics" to "إخفاء التشخيص",
    "Show diagnostics" to "عرض التشخيص",
    "Protocol logging" to "تسجيل البروتوكول",
    "Keep off unless troubleshooting." to "اتركه متوقفاً إلا عند التشخيص.",
    "Connection" to "الاتصال",
    "Log entries" to "سجلات",
    "Status" to "الحالة",
    "Ping" to "اختبار الاتصال",
    "Car location" to "موقع السيارة",
    "Get car location" to "جلب موقع السيارة",
    "Export redacted protocol log" to "تصدير سجل البروتوكول بعد حجب البيانات",
    "App updates" to "تحديثات التطبيق",
    "Update required" to "التحديث مطلوب",
    "Version check required" to "يلزم فحص الإصدار",
    "To make the best use of G700 Remote, update now. This version is out of support while a newer release is available." to "لأفضل استخدام لتطبيق G700 Remote، قم بالتحديث الآن. هذا الإصدار أصبح خارج الدعم مع توفر إصدار أحدث.",
    "G700 Remote needs to check GitHub releases at least every 7 days before vehicle controls can be used." to "يحتاج G700 Remote إلى فحص إصدارات GitHub مرة كل 7 أيام على الأقل قبل استخدام أوامر السيارة.",
    "G700 Remote is up to date." to "G700 Remote محدث.",
    "Current version" to "الإصدار الحالي",
    "Last checked" to "آخر فحص",
    "Available version" to "الإصدار المتاح",
    "Download and install" to "تنزيل وتثبيت",
    "Downloading" to "جار التنزيل",
    "Check for updates" to "التحقق من التحديثات",
    "Checking" to "جار التحقق",
    "What's new in this version" to "ما الجديد في هذا الإصدار",
    "Checks GitHub releases for signed APK updates twice daily." to "يفحص إصدارات GitHub مرتين يومياً لتحديثات APK الموقعة.",
    "Links" to "الروابط",
    "Locations shared to the car can be resent from here." to "يمكن إعادة إرسال المواقع التي تمت مشاركتها مع السيارة من هنا.",
    "Sent to car" to "تم الإرسال للسيارة",
    "Destination sent to car" to "تم إرسال الوجهة إلى السيارة",
    "Destination could not be sent" to "تعذر إرسال الوجهة",
    "Saved for later" to "تم الحفظ لاحقاً",
    "Location not sent" to "لم يتم إرسال الموقع",
    "Preparing location" to "جاري تجهيز الموقع",
    "Reading link details" to "جاري قراءة تفاصيل الرابط",
    "Short Google Maps links can take a few seconds." to "روابط خرائط Google المختصرة قد تستغرق بضع ثوانٍ.",
    "The destination was sent to DisplayMirror and saved in Shared links." to "تم إرسال الوجهة إلى DisplayMirror وحفظها في الروابط المرسلة.",
    "The destination is saved in Shared links so you can resend it later." to "تم حفظ الوجهة في الروابط المرسلة لتتمكن من إعادة إرسالها لاحقاً.",
    "G700 Remote could not read a destination from this share." to "لم يتمكن G700 Remote من قراءة وجهة من هذه المشاركة.",
    "Done" to "تم",
    "Clear shared links?" to "مسح الروابط المرسلة؟",
    "This removes all saved shared-location history from this phone." to "سيتم حذف سجل المواقع المرسلة المحفوظ على هذا الهاتف.",
    "Clear all" to "مسح الكل",
    "Delete shared link?" to "حذف الرابط المرسل؟",
    "Remove this destination from history?" to "هل تريد حذف هذه الوجهة من السجل؟",
    "History" to "السجل",
    "No shared links yet" to "لا توجد روابط مرسلة بعد",
    "Share a Google Maps place or geo link to G700 Remote." to "شارك موقعاً من خرائط Google أو رابط geo إلى G700 Remote.",
    "Resend" to "إعادة إرسال",
    "Delete" to "حذف",
    "Developed by Mahmood Majeed with ❤️ in Bahrain 🇧🇭" to "تم التطوير بواسطة Mahmood Majeed ❤️ في البحرين 🇧🇭",
    "Disconnected" to "غير متصل",
    "Scanning" to "جار البحث",
    "Connecting" to "جار الاتصال",
    "Discovering" to "جار الاكتشاف",
    "Subscribing" to "جار الاشتراك",
    "Handshaking" to "جار المصادقة",
    "Connected" to "متصل",
    "Error" to "خطأ",
    "None" to "لا يوجد",
    "BLE" to "بلوتوث",
    "LAN" to "شبكة",
)
