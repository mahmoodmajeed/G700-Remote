package com.mmy.g700remote.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmy.g700remote.BuildConfig
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.G700RemoteViewModel
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.data.AppLanguage
import com.mmy.g700remote.data.AppTheme
import com.mmy.g700remote.data.AppUpdateInfo
import com.mmy.g700remote.data.AppUpdateState
import com.mmy.g700remote.data.LockStateMapping
import com.mmy.g700remote.data.RemoteUiState
import com.mmy.g700remote.data.VehicleTelemetry
import com.mmy.g700remote.protocol.ClimateAction
import com.mmy.g700remote.protocol.MirrorAction
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
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }
private const val DISPLAY_MIRROR_PROJECT_URL = "https://github.com/Baghdady92/DisplayMirror"

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

    fun submit(command: RemoteCommand) {
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

    G700RemoteTheme(appTheme = state.appTheme) {
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

    LaunchedEffect(state.lastError) {
        state.lastError?.let { snackbarHostState.showSnackbar(it) }
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
            viewModel.sendSharedNavigation(text)
            onSharedNavigationConsumed()
        }
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (state.pairedDevice != null) 92.dp else 0.dp),
            )
        },
    ) { padding ->
        if (!permissionsGranted) {
            PermissionScreen(
                onRequestPermissions = onRequestPermissions,
                modifier = Modifier.padding(padding),
            )
        } else if (state.pairedDevice == null) {
            PairingScreen(
                state = state,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onPair = viewModel::pairAndConnect,
                onPairingCodeChanged = viewModel::setPairingCode,
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
                onRegionalFeaturesChanged = viewModel::setRegionalFeaturesEnabled,
                onLocalAuthChanged = viewModel::setLocalAuthEnabled,
                onLockMappingChanged = viewModel::setLockStateMapping,
                onLoggingChanged = viewModel::setLoggingEnabled,
                updateState = updateState,
                onCheckForUpdates = viewModel::checkForUpdates,
                onDownloadUpdate = { viewModel.downloadAndInstallUpdate(activity, it) },
                onRefresh = viewModel::refreshNow,
                onShareLog = { onShareLog(viewModel.exportLogText()) },
                showUpdates = showUpdates,
                onUpdatesShown = onUpdatesShown,
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
private fun PairingScreen(
    state: RemoteUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPair: (com.mmy.g700remote.ble.ScannedDevice) -> Unit,
    onPairingCodeChanged: (String) -> Unit,
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
                        onClick = onStartScan,
                        enabled = !state.isScanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.BluetoothSearching, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Scan"))
                    }
                    OutlinedButton(
                        onClick = onStopScan,
                        enabled = state.isScanning,
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
    onRegionalFeaturesChanged: (Boolean) -> Unit,
    onLocalAuthChanged: (Boolean) -> Unit,
    onLockMappingChanged: (LockStateMapping) -> Unit,
    onLoggingChanged: (Boolean) -> Unit,
    updateState: AppUpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (AppUpdateInfo) -> Unit,
    onRefresh: () -> Unit,
    onShareLog: () -> Unit,
    showUpdates: Boolean,
    onUpdatesShown: () -> Unit,
    contentPadding: PaddingValues,
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    LaunchedEffect(showUpdates) {
        if (showUpdates) {
            tab = AppTab.Settings
            onUpdatesShown()
        }
    }
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            ConnectionHeader(
                state = state,
                onRefresh = onRefresh,
                onDisconnect = onDisconnect,
                onOpenSettings = { tab = AppTab.Settings },
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.filter { it != AppTab.Settings }.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = tr(item.label)) },
                        label = { Text(tr(item.label)) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (tab) {
            AppTab.Home -> HomeScreen(state, onCommand, modifier)
            AppTab.Climate -> ClimateScreen(state, onCommand, modifier)
            AppTab.Openings -> OpeningsScreen(state, onCommand, modifier)
            AppTab.Charging -> ChargingScreen(state, onCommand, modifier)
            AppTab.Lighting -> LightingScreen(state, onCommand, modifier)
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
                onRegionalFeaturesChanged = onRegionalFeaturesChanged,
                onLocalAuthChanged = onLocalAuthChanged,
                onLockMappingChanged = onLockMappingChanged,
                onLoggingChanged = onLoggingChanged,
                updateState = updateState,
                onCheckForUpdates = onCheckForUpdates,
                onDownloadUpdate = onDownloadUpdate,
                onShareLog = onShareLog,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ConnectionHeader(
    state: RemoteUiState,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (state.connectionState is RemoteConnectionState.Ready) {
                    state.connectionState.readyTransportIcon()
                } else {
                    Icons.Outlined.BluetoothSearching
                },
                contentDescription = null,
                tint = state.connectionState.color(),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(state.connectionState.label(), style = MaterialTheme.typography.titleSmall)
                Text(
                    state.pairedDevice?.name ?: state.pairedDevice?.address ?: tr("No paired device"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onRefresh,
                enabled = state.connectionState is RemoteConnectionState.Ready,
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = tr("Refresh status"))
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = tr("More"))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(tr("Settings")) },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(tr("Disconnect")) },
                        leadingIcon = { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDisconnect()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: RemoteUiState,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    val locked = isLocked(state)
    val lockActionIsUnlock = locked == true
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    lockLabel(state),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(22.dp))
                HeroCommandButton(
                    text = if (lockActionIsUnlock) tr("Unlock") else tr("Lock"),
                    icon = if (lockActionIsUnlock) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    enabled = ready,
                    danger = lockActionIsUnlock,
                    onClick = { onCommand(if (lockActionIsUnlock) RemoteCommand.Unlock else RemoteCommand.Lock) },
                )
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
                            label = tr("AC"),
                            icon = Icons.Outlined.AcUnit,
                            checked = state.telemetry.acOn,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.AcOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.AcOff)) },
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
    modifier: Modifier = Modifier,
) {
    val ready = state.connectionState is RemoteConnectionState.Ready
    val telemetry = state.telemetry
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Section(tr("Cabin")) {
                ModeToggleBox(
                    spec = ToggleSpec(
                        label = tr("AC"),
                        icon = Icons.Outlined.AcUnit,
                        checked = telemetry.acOn,
                        onOn = { onCommand(RemoteCommand.Climate(ClimateAction.AcOn)) },
                        onOff = { onCommand(RemoteCommand.Climate(ClimateAction.AcOff)) },
                    ),
                    enabled = ready,
                )
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
                        ToggleSpec(tr("Auto defrost"), Icons.Outlined.Air, telemetry.autoDefrost,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.AutoDefrostOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.AutoDefrostOff)) }),
                        ToggleSpec(tr("Rear defrost"), Icons.Outlined.Window, telemetry.rearDefrost,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.RearDefrostOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.RearDefrostOff)) }),
                        ToggleSpec(tr("Front glass heat"), Icons.Outlined.Window, null,
                            onOn = { onCommand(RemoteCommand.Climate(ClimateAction.FrontHeatOn)) },
                            onOff = { onCommand(RemoteCommand.Climate(ClimateAction.FrontHeatOff)) }),
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Section(tr("Windows")) {
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
            )
        }
        Section(tr("Sunshade")) {
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
            )
        }
        Section(tr("Sunroof")) {
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
            )
        }
        Section(tr("Mirrors")) {
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
            )
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
    onRegionalFeaturesChanged: (Boolean) -> Unit,
    onLocalAuthChanged: (Boolean) -> Unit,
    onLockMappingChanged: (LockStateMapping) -> Unit,
    onLoggingChanged: (Boolean) -> Unit,
    updateState: AppUpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (AppUpdateInfo) -> Unit,
    onShareLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
            Section(tr("Theme")) {
                ThemeSelector(
                    selected = state.appTheme,
                    onTheme = onAppThemeChanged,
                )
            }
        }
        item {
            Section(tr("Pairing")) {
                PairingCodeField(
                    value = state.pairingCode,
                    onValueChange = onPairingCodeChanged,
                )
                Spacer(Modifier.height(8.dp))
                MetricRow(tr("Device"), state.pairedDevice?.name ?: tr("Unnamed"))
                MetricRow(tr("Address"), state.pairedDevice?.address ?: tr("not paired"))
                MetricRow(tr("Transport"), state.pairedDevice?.transport?.label() ?: tr("Unknown"))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onStartScan, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.BluetoothSearching, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Scan"))
                    }
                    OutlinedButton(onClick = onClearPairing, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Clear"))
                    }
                }
                if (state.isScanning) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
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
            Section(tr("Connectivity")) {
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
                Spacer(Modifier.height(8.dp))
                Text(tr("Priority"), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                PreferenceSelector(
                    preference = state.connectionPreference,
                    onPreference = onConnectionPreferenceChanged,
                )
                Spacer(Modifier.height(8.dp))
                MetricRow(tr("Active"), state.connectionState.activeTransportLabel())
                MetricRow(tr("State"), state.connectionState.label())
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(formatTemp(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onValue(value - 0.5) }, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("-")
                }
                OutlinedButton(onClick = { onValue(value + 0.5) }, enabled = enabled, modifier = Modifier.weight(1f)) {
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = enabled) { onValue(level) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((18 + level * 3).dp)
                            .background(
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                                },
                                shape = RoundedCornerShape(4.dp),
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
    OutlinedButton(
        onClick = { if (active) spec.onOff() else spec.onOn() },
        enabled = enabled,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(8.dp),
        colors = if (active) {
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(
                    when (spec.checked) {
                        true -> tr("On")
                        false -> tr("Off")
                        null -> tr("Unknown")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(columns).forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowActions.forEach { action ->
                    OutlinedButton(
                        onClick = action.onClick,
                        enabled = enabled,
                        modifier = if (fullWidthLastSingle && rowActions.size == 1) {
                            Modifier.fillMaxWidth().height(58.dp)
                        } else {
                            Modifier.weight(1f).height(58.dp)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(6.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
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
            TileData(tr("Battery"), state.telemetry.batterySoc?.let { "$it%" } ?: tr("Unknown"), Icons.Outlined.Bolt),
            TileData(tr("Fuel"), state.telemetry.fuelPercent?.let { "$it%" } ?: tr("Unknown"), Icons.Outlined.DirectionsCar),
            TileData(tr("AC"), when (state.telemetry.acOn) {
                true -> tr("On")
                false -> tr("Off")
                null -> tr("Unknown")
            }, Icons.Outlined.AcUnit),
            TileData(tr("Cabin"), state.telemetry.cabinTemp?.let { formatTemp(it) } ?: tr("Unknown"), Icons.Outlined.Thermostat),
            TileData(tr("Coolant"), state.telemetry.coolantTemp?.let { formatTemp(it) } ?: tr("Unknown"), Icons.Outlined.Thermostat),
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
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = if (danger) {
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    } else {
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = colors,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(132.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            ButtonDefaults.outlinedButtonColors()
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.DirectionsCar),
    Climate("Climate", Icons.Outlined.Thermostat),
    Openings("Windows", Icons.Outlined.Window),
    Charging("Charge", Icons.Outlined.Bolt),
    Lighting("Lights", Icons.Outlined.Lightbulb),
    Settings("Settings", Icons.Outlined.Settings),
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

private fun RemoteConnectionState.readyTransportIcon(): ImageVector =
    if (this is RemoteConnectionState.Ready && transport == TransportKind.Lan) Icons.Outlined.Wifi else Icons.Outlined.Bluetooth

@Composable
private fun TransportKind.label(): String = when (this) {
    TransportKind.Ble -> tr("BLE")
    TransportKind.Lan -> tr("LAN")
}

private fun AppTheme.label(): String = when (this) {
    AppTheme.Minimal -> "Minimal"
    AppTheme.G700Horizon -> "G700 Horizon"
    AppTheme.ModernPastel -> "Modern Pastel"
}

@Composable
private fun lockLabel(state: RemoteUiState): String {
    val raw = state.telemetry.lockState ?: return tr("Lock state Unknown")
    return if (raw == 1) tr("Locked") else tr("Unlocked")
}

private fun isLocked(state: RemoteUiState): Boolean? =
    state.telemetry.lockState?.let { it == 1 }

private fun formatTemp(value: Double): String = "%.1f °C".format(value)

private fun formatTimeAgo(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))

private fun formatChargeMode(value: String?): String? =
    value?.uppercase()?.replace('_', ' ')

private val ArabicTranslations = mapOf(
    "Confirm" to "تأكيد",
    "Use biometrics or device PIN to continue." to "استخدم البصمة أو رمز الهاتف للمتابعة.",
    "Confirm command" to "تأكيد الأمر",
    "Send" to "إرسال",
    "Cancel" to "إلغاء",
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
    "Unnamed DisplayMirror device" to "جهاز DisplayMirror بدون اسم",
    "Unnamed" to "بدون اسم",
    "No paired device" to "لا يوجد جهاز مقترن",
    "Refresh status" to "تحديث الحالة",
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
    "Locked" to "مقفلة",
    "Unlocked" to "مفتوحة",
    "Lock state Unknown" to "حالة القفل غير معروفة",
    "Raw state" to "القيمة الخام",
    "Unknown" to "غير معروف",
    "Quick Actions" to "إجراءات سريعة",
    "Vehicle" to "المركبة",
    "Battery" to "البطارية",
    "Battery SOC" to "نسبة البطارية",
    "Fuel" to "الوقود",
    "Coolant" to "حرارة المحرك",
    "AC" to "المكيّف",
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
    "Minimal" to "بسيط",
    "G700 Horizon" to "أفق G700",
    "Modern Pastel" to "عصري هادئ",
    "Pairing" to "الاقتران",
    "Device" to "الجهاز",
    "Address" to "العنوان",
    "not paired" to "غير مقترن",
    "Transport" to "طريقة الاتصال",
    "Connectivity" to "الاتصال",
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
    "Current version" to "الإصدار الحالي",
    "Last checked" to "آخر فحص",
    "Available version" to "الإصدار المتاح",
    "Download and install" to "تنزيل وتثبيت",
    "Downloading" to "جار التنزيل",
    "Check for updates" to "التحقق من التحديثات",
    "Checking" to "جار التحقق",
    "Checks GitHub releases for signed APK updates twice daily." to "يفحص إصدارات GitHub مرتين يومياً لتحديثات APK الموقعة.",
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
