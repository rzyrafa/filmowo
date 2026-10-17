package pl.filmowo.feature.about

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.BuildConfig
import pl.filmowo.R
import pl.filmowo.core.ui.components.filmowoTopBarColors
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.work.AndroidPremiereNotifier
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = message?.let { backupMessageText(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = filmowoTopBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NotificationsSection(enabled = notificationsEnabled, onChange = viewModel::onNotificationsChange)
            BackupSection(isWorking = isWorking, onExport = viewModel::onExport, onImport = viewModel::onImport)

            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_privacy),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.about_data_source_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.about_tmdb_attribution),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Przełącznik powiadomień o premierach. Na Androidzie 13+ włączenie prosi o uprawnienie;
 * przełącznik pokazuje stan faktyczny (ustawienie i zgoda systemu).
 */
@Composable
private fun NotificationsSection(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var permitted by remember { mutableStateOf(AndroidPremiereNotifier.canNotify(context)) }
    // Użytkownik mógł zmienić zgodę w ustawieniach systemu – sprawdzamy po powrocie na ekran.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permitted = AndroidPremiereNotifier.canNotify(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = AndroidPremiereNotifier.canNotify(context)
        onChange(granted)
    }
    val checked = enabled && permitted

    SettingsCard(title = stringResource(R.string.settings_notifications_title), iconRes = R.drawable.ic_notifications) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = { turnOn ->
                        if (turnOn && !permitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onChange(turnOn)
                        }
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        if (enabled && !permitted) R.string.settings_notifications_denied else R.string.settings_notifications_summary,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = FilmowoTheme.colors.inkMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun BackupSection(isWorking: Boolean, onExport: (Uri) -> Unit, onImport: (Uri) -> Unit) {
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) onExport(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri.toString()
    }

    SettingsCard(title = stringResource(R.string.backup_title), iconRes = R.drawable.ic_download) {
        Text(
            stringResource(R.string.backup_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = FilmowoTheme.colors.inkMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { exportLauncher.launch("filmowo-${LocalDate.now()}.json") },
                enabled = !isWorking,
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.backup_export))
            }
            OutlinedButton(
                // Część menedżerów plików nie rozpoznaje typu application/json – dopuszczamy każdy plik.
                onClick = { importLauncher.launch(arrayOf(BACKUP_MIME, "*/*")) },
                enabled = !isWorking,
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_upload), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.backup_import))
            }
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    onImport(uri.toUri())
                }) { Text(stringResource(R.string.backup_import_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsCard(title: String, iconRes: Int, content: @Composable () -> Unit) {
    val colors = FilmowoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised, FilmowoRadius.Lg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(iconRes), contentDescription = null, tint = colors.brand, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        content()
    }
}

@Composable
private fun backupMessageText(message: BackupMessage): String = when (message) {
    is BackupMessage.Exported -> stringResource(R.string.backup_export_done, message.items)
    BackupMessage.ExportFailed -> stringResource(R.string.backup_export_failed)
    is BackupMessage.Imported -> stringResource(R.string.backup_import_done, message.items)
    BackupMessage.InvalidFile -> stringResource(R.string.backup_import_invalid)
    BackupMessage.ImportFailed -> stringResource(R.string.backup_import_failed)
}

private const val BACKUP_MIME = "application/json"
