package app.ties.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ties.R
import app.ties.container
import app.ties.data.Person
import app.ties.data.Touch
import app.ties.domain.Cadence
import app.ties.ui.agoLabel
import app.ties.ui.birthdayInLabel
import app.ties.ui.birthdayLabel
import app.ties.ui.circleLabel
import app.ties.ui.components.Avatar
import app.ties.ui.components.ContactActions
import app.ties.ui.components.LogTouchSheet
import app.ties.ui.components.SectionTitle
import app.ties.ui.components.TiesCard
import app.ties.ui.components.rememberToday
import app.ties.ui.frequencyLabel
import app.ties.ui.kindLabel
import app.ties.ui.lastTouchLabel
import app.ties.ui.locale
import app.ties.ui.statusLabel
import app.ties.ui.theme.Ties
import app.ties.ui.theme.TiesType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun PersonScreen(id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, onCall: (Person) -> Unit) {
    val container = LocalContext.current.container
    val dao = container.db.people()
    val person by remember(id) { dao.observe(id) }.collectAsStateWithLifecycle(initialValue = null)
    val touches by remember(id) { dao.observeTouches(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = rememberToday()
    var logOpen by rememberSaveable { mutableStateOf(false) }
    val p = person

    Column(Modifier.fillMaxSize().background(Ties.Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Ties.Ink) }
            Spacer(Modifier.weight(1f))
            if (p != null) {
                IconButton(onClick = { onEdit(p.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit), tint = Ties.Muted) }
            }
        }
        if (p == null) return@Column

        val daysLeft = Cadence.daysLeft(p, today)
        val status = Cadence.status(daysLeft, p.everyDays)

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(p.name, p.circle, Cadence.warmth(daysLeft, p.everyDays), status, size = 96.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(p.name, style = TiesType.Title, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("${circleLabel(p.circle)} · ${frequencyLabel(p.everyDays)}", style = TiesType.Sub)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${lastTouchLabel(p, today)} · ${statusLabel(daysLeft, status)}",
                        style = TiesType.Muted.copy(color = Ties.status(status)),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                Actions(p, onCall = { onCall(p) }, onLog = { logOpen = true })
            }
            if (p.birthdayMonth != null && p.birthdayDay != null) {
                item {
                    Spacer(Modifier.height(12.dp))
                    TiesCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎂  " + birthdayLabel(p.birthdayMonth, p.birthdayDay), style = TiesType.Body, modifier = Modifier.weight(1f))
                            Text(birthdayInLabel(Cadence.daysToBirthday(p, today) ?: 0), style = TiesType.Muted)
                        }
                    }
                }
            }
            item { Starter() }
            if (p.notes.isNotBlank()) {
                item {
                    SectionTitle(stringResource(R.string.notes_title))
                    TiesCard { Text(p.notes, style = TiesType.Body) }
                }
            }
            item { SectionTitle(stringResource(R.string.history_title)) }
            if (touches.isEmpty()) {
                item { Text(stringResource(R.string.history_empty), style = TiesType.Muted, modifier = Modifier.padding(start = 4.dp)) }
            }
            items(touches, key = { it.id }) { t ->
                TouchRow(t, today, onDelete = { container.scope.launch { dao.removeTouch(t) } })
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (logOpen && p != null) {
        LogTouchSheet(
            name = p.name,
            onDismiss = { logOpen = false },
            onSave = { kind, at, note ->
                container.logTouch(p.id, kind, at, note)
                logOpen = false
            },
        )
    }
}

@Composable
private fun Actions(p: Person, onCall: () -> Unit, onLog: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionTile(Icons.Filled.Call, stringResource(R.string.action_call), enabled = p.phone != null, modifier = Modifier.weight(1f), onClick = onCall)
        Box(Modifier.weight(1f)) {
            ActionTile(Icons.AutoMirrored.Filled.Send, stringResource(R.string.action_write), enabled = p.phone != null, onClick = { menu = true })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ties.Surface) {
                val phone = p.phone.orEmpty()
                DropdownMenuItem(text = { Text("SMS") }, onClick = { menu = false; ContactActions.sms(context, phone) })
                DropdownMenuItem(text = { Text("Telegram") }, onClick = { menu = false; ContactActions.telegram(context, phone) })
                DropdownMenuItem(text = { Text("WhatsApp") }, onClick = { menu = false; ContactActions.whatsapp(context, phone) })
            }
        }
        ActionTile(Icons.Filled.Check, stringResource(R.string.action_log), primary = true, modifier = Modifier.weight(1f), onClick = onLog)
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = false, onClick: () -> Unit) {
    val bg = if (primary) Ties.Coral else Ties.Surface
    val fg: Color = when {
        primary -> Ties.OnCoral
        enabled -> Ties.Ink
        else -> Ties.Faint
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(Ties.RMd)
            .background(bg)
            .then(if (primary) Modifier else Modifier.border(1.dp, Ties.Line, Ties.RMd))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = TiesType.Small.copy(color = fg, fontSize = 13.sp))
    }
}

/** A gentle conversation opener; the hardest part of reconnecting is the first message. */
@Composable
private fun Starter() {
    val starters = stringArrayResource(R.array.starters)
    var index by rememberSaveable { mutableIntStateOf(Random.nextInt(starters.size)) }
    SectionTitle(stringResource(R.string.starter_title))
    TiesCard(padding = PaddingValues(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(starters[index % starters.size], style = TiesType.Body, modifier = Modifier.weight(1f))
            IconButton(onClick = { index = (index + 1) % starters.size }) {
                Icon(Icons.Filled.Refresh, stringResource(R.string.starter_next), tint = Ties.Faint)
            }
        }
    }
}

@Composable
private fun TouchRow(t: Touch, today: LocalDate, onDelete: () -> Unit) {
    val date = Cadence.toDate(t.at)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale())
    TiesCard(padding = PaddingValues(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${kindLabel(t.kind)} · ${agoLabel(Cadence.daysSince(t.at, today))}", style = TiesType.Strong.copy(fontSize = 14.5.sp))
                Text(date.format(formatter), style = TiesType.Small)
                if (t.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(t.note, style = TiesType.Muted)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, stringResource(R.string.delete), tint = Ties.Faint, modifier = Modifier.width(18.dp))
            }
        }
    }
}
