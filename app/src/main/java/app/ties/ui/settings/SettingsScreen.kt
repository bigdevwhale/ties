package app.ties.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ties.R
import app.ties.container
import app.ties.data.ReminderConfig
import app.ties.reminders.Reminders
import app.ties.ui.Language
import app.ties.ui.components.Chip
import app.ties.ui.components.CommunityLinks
import app.ties.ui.components.SectionTitle
import app.ties.ui.components.TiesCard
import app.ties.ui.theme.Ties
import app.ties.ui.theme.TiesType
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val config by remember { container.settings.reminder }.collectAsStateWithLifecycle(initialValue = ReminderConfig())
    var pickTime by remember { mutableStateOf(false) }
    var canNotify by remember { mutableStateOf(Reminders.canNotify(context)) }
    // Re-read on resume so the permission card disappears after granting it in system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canNotify = Reminders.canNotify(context) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }

    fun update(block: suspend () -> Unit) {
        container.scope.launch {
            block()
            Reminders.reschedule(context)
        }
    }

    Column(Modifier.fillMaxSize().background(Ties.Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Ties.Ink) }
            Text(stringResource(R.string.settings), style = TiesType.Strong.copy(fontSize = TiesType.Title.fontSize * 0.7f))
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (!canNotify) {
                Spacer(Modifier.height(8.dp))
                TiesCard(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text(stringResource(R.string.perm_title), style = TiesType.Strong.copy(color = Ties.Coral))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.perm_text), style = TiesType.Muted)
                }
            }

            SectionTitle(stringResource(R.string.reminders))
            TiesCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.reminder_daily), style = TiesType.Strong)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(R.string.reminder_daily_text), style = TiesType.Muted)
                    }
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { v -> update { container.settings.setReminderEnabled(v) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = Ties.Coral),
                    )
                }
                if (config.enabled) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { pickTime = true }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.reminder_time), style = TiesType.Body, modifier = Modifier.weight(1f))
                        Text("%02d:%02d".format(config.minutes / 60, config.minutes % 60), style = TiesType.Strong.copy(color = Ties.Coral))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TiesCard(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }) {
                Text(stringResource(R.string.autostart_title), style = TiesType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.autostart_text), style = TiesType.Muted)
            }

            SectionTitle(stringResource(R.string.language))
            val current = Language.current()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.lang_system), current == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", current == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", current == Language.RU, { Language.set(Language.RU) })
            }

            CommunityLinks()

            SectionTitle(stringResource(R.string.about))
            TiesCard { Text(stringResource(R.string.about_text), style = TiesType.Sub) }
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = config.minutes / 60, initialMinute = config.minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = Ties.Bg,
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    pickTime = false
                    update { container.settings.setReminderMinutes(state.hour * 60 + state.minute) }
                }) { Text(stringResource(R.string.ok), color = Ties.Coral) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel), color = Ties.Muted) } },
        )
    }
}
