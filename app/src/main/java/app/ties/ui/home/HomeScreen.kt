package app.ties.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ties.R
import app.ties.container
import app.ties.data.Person
import app.ties.domain.Cadence
import app.ties.domain.Status
import app.ties.ui.birthdayInLabel
import app.ties.ui.components.Avatar
import app.ties.ui.components.LogTouchSheet
import app.ties.ui.components.PrimaryButton
import app.ties.ui.components.SectionTitle
import app.ties.ui.components.TiesCard
import app.ties.ui.components.rememberToday
import app.ties.ui.frequencyLabel
import app.ties.ui.lastTouchLabel
import app.ties.ui.statusLabel
import app.ties.ui.theme.Ties
import app.ties.ui.theme.TiesType
import java.time.LocalDate

private data class Entry(val person: Person, val daysLeft: Long, val status: Status)

@Composable
fun HomeScreen(onOpen: (Long) -> Unit, onAdd: () -> Unit, onSettings: () -> Unit, onCall: (Person) -> Unit) {
    val container = LocalContext.current.container
    val people by remember { container.db.people().observeAll() }.collectAsStateWithLifecycle(initialValue = null)
    val today = rememberToday()
    var logFor by remember { mutableStateOf<Person?>(null) }

    Scaffold(
        containerColor = Ties.Bg,
        floatingActionButton = {
            if (!people.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    containerColor = Ties.Ink,
                    contentColor = Ties.Bg,
                    shape = Ties.RMd,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.add_person), style = TiesType.Button) },
                )
            }
        },
    ) { pad ->
        val list = people ?: return@Scaffold
        val rows = list
            .map { p -> Cadence.daysLeft(p, today).let { Entry(p, it, Cadence.status(it, p.everyDays)) } }
            .sortedBy { it.daysLeft }
        val waiting = rows.filter { it.status == Status.OVERDUE || it.status == Status.DUE }
        val soon = rows.filter { it.status == Status.SOON }
        val fine = rows.filter { it.status == Status.OK }
        val birthdays = list
            .mapNotNull { p -> Cadence.daysToBirthday(p, today)?.takeIf { it <= 14 }?.let { p to it } }
            .sortedBy { it.second }

        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        ) {
            item {
                Header(
                    subtitle = when {
                        list.isEmpty() -> stringResource(R.string.home_sub_empty)
                        waiting.isNotEmpty() -> pluralStringResource(R.plurals.home_waiting, waiting.size, waiting.size)
                        else -> stringResource(R.string.home_sub_fine)
                    },
                    onSettings = onSettings,
                )
            }
            if (list.isEmpty()) {
                item { EmptyState(onAdd) }
            }
            if (birthdays.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_birthdays)) }
                items(birthdays, key = { "b" + it.first.id }) { (p, days) ->
                    BirthdayRow(p, days, onClick = { onOpen(p.id) })
                }
            }
            section(R.string.section_waiting, waiting, today, onOpen, onCall) { logFor = it }
            section(R.string.section_soon, soon, today, onOpen, onCall) { logFor = it }
            section(R.string.section_fine, fine, today, onOpen, onCall) { logFor = it }
        }
    }

    logFor?.let { p ->
        LogTouchSheet(
            name = p.name,
            onDismiss = { logFor = null },
            onSave = { kind, at, note ->
                container.logTouch(p.id, kind, at, note)
                logFor = null
            },
        )
    }
}

private fun LazyListScope.section(
    title: Int,
    rows: List<Entry>,
    today: LocalDate,
    onOpen: (Long) -> Unit,
    onCall: (Person) -> Unit,
    onLog: (Person) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "t$title") { SectionTitle(stringResource(title)) }
    items(rows, key = { it.person.id }) { row ->
        PersonRow(row, today, onOpen, onCall, onLog)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Header(subtitle: String, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = TiesType.Title)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = TiesType.Sub)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = Ties.Muted)
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    TiesCard(padding = PaddingValues(20.dp)) {
        Text(stringResource(R.string.empty_title), style = TiesType.Strong.copy(fontSize = 18.sp))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.empty_text), style = TiesType.Sub)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.add_first), onAdd, Modifier.fillMaxWidth())
    }
}

@Composable
private fun BirthdayRow(p: Person, days: Long, onClick: () -> Unit) {
    TiesCard(Modifier.padding(bottom = 10.dp), onClick = onClick, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎂", style = TiesType.Body)
            Spacer(Modifier.width(12.dp))
            Text(p.name, style = TiesType.Strong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(birthdayInLabel(days), style = TiesType.Muted.copy(color = if (days == 0L) Ties.Coral else Ties.Muted))
        }
    }
}

@Composable
private fun PersonRow(row: Entry, today: LocalDate, onOpen: (Long) -> Unit, onCall: (Person) -> Unit, onLog: (Person) -> Unit) {
    val p = row.person
    TiesCard(onClick = { onOpen(p.id) }, padding = PaddingValues(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.name, p.circle, Cadence.warmth(row.daysLeft, p.everyDays), row.status)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = TiesType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(lastTouchLabel(p, today), style = TiesType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                if (row.status == Status.OK) {
                    Text(frequencyLabel(p.everyDays), style = TiesType.Small)
                } else {
                    Text(statusLabel(row.daysLeft, row.status), style = TiesType.Small.copy(color = Ties.status(row.status)))
                }
            }
            if (p.phone != null) {
                IconButton(onClick = { onCall(p) }) {
                    Icon(Icons.Filled.Call, stringResource(R.string.action_call), tint = Ties.Muted)
                }
            }
            IconButton(
                onClick = { onLog(p) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Ties.Moss),
            ) {
                Icon(
                    Icons.Filled.Check,
                    stringResource(R.string.action_log),
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(Ties.MossSoft).padding(5.dp),
                )
            }
        }
    }
}
