package app.ties.ui.edit

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ties.R
import app.ties.container
import app.ties.data.Circle
import app.ties.data.Person
import app.ties.domain.Cadence
import app.ties.ui.birthdayLabel
import app.ties.ui.circleLabel
import app.ties.ui.components.Chip
import app.ties.ui.components.PrimaryButton
import app.ties.ui.components.SectionTitle
import app.ties.ui.frequencyLabel
import app.ties.ui.theme.Ties
import app.ties.ui.theme.TiesType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Rough guess of the last contact for someone new, so the countdown starts somewhere honest. */
private enum class Since(val days: Long, val label: Int) {
    RECENT(0, R.string.since_recent),
    MONTH(30, R.string.since_month),
    HALF_YEAR(180, R.string.since_half_year),
    LONG(365, R.string.since_long),
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditPersonScreen(id: Long, onDone: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val dao = container.db.people()
    val isNew = id == 0L

    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var circle by rememberSaveable { mutableStateOf(Circle.FRIENDS) }
    var everyDays by rememberSaveable { mutableStateOf(Cadence.defaultEvery(Circle.FRIENDS)) }
    var everyTouched by rememberSaveable { mutableStateOf(!isNew) }
    var bMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var bDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var since by rememberSaveable { mutableStateOf(Since.MONTH) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (loaded) return@LaunchedEffect
        dao.get(id)?.let { p ->
            name = p.name
            phone = p.phone.orEmpty()
            circle = p.circle
            everyDays = p.everyDays
            bMonth = p.birthdayMonth
            bDay = p.birthdayDay
            notes = p.notes
        }
        loaded = true
    }

    // Picking a phone entry (not a whole contact) grants one-off read access, so no READ_CONTACTS needed.
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getString(0)?.let { if (name.isBlank()) name = it }
                c.getString(1)?.let { phone = it }
            }
        }
    }

    fun save() {
        val now = System.currentTimeMillis()
        val cleanPhone = phone.trim().ifEmpty { null }
        container.scope.launch {
            if (isNew) {
                val sinceAt = LocalDate.now().minusDays(since.days).atTime(12, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                dao.insert(
                    Person(
                        name = name.trim(), circle = circle, phone = cleanPhone, everyDays = everyDays,
                        birthdayMonth = bMonth, birthdayDay = bDay, notes = notes.trim(),
                        createdAt = now, sinceAt = minOf(sinceAt, now),
                    )
                )
            } else {
                dao.get(id)?.let {
                    dao.update(
                        it.copy(
                            name = name.trim(), circle = circle, phone = cleanPhone, everyDays = everyDays,
                            birthdayMonth = bMonth, birthdayDay = bDay, notes = notes.trim(),
                        )
                    )
                }
            }
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(Ties.Bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Ties.Ink) }
            Text(
                stringResource(if (isNew) R.string.edit_new_title else R.string.edit_title),
                style = TiesType.Strong.copy(fontSize = TiesType.Title.fontSize * 0.7f),
            )
        }
        if (!loaded) return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        ) {
            OutlinedButton(
                onClick = {
                    pickContact.launch(Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = Ties.RSm,
            ) {
                Icon(Icons.Filled.Person, null, tint = Ties.Coral)
                Spacer(Modifier.padding(4.dp))
                Text(stringResource(R.string.pick_contact), color = Ties.Ink, style = TiesType.Button)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true, shape = Ties.RSm, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text(stringResource(R.string.field_phone)) },
                singleLine = true, shape = Ties.RSm, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            SectionTitle(stringResource(R.string.field_circle))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Circle.entries.forEach { c ->
                    Chip(circleLabel(c), circle == c, {
                        circle = c
                        if (!everyTouched) everyDays = Cadence.defaultEvery(c)
                    }, color = Ties.circle(c))
                }
            }

            SectionTitle(stringResource(R.string.field_every))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cadence.Frequencies.forEach { d ->
                    Chip(frequencyLabel(d), everyDays == d, {
                        everyDays = d
                        everyTouched = true
                    })
                }
            }

            if (isNew) {
                SectionTitle(stringResource(R.string.field_since))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Since.entries.forEach { s -> Chip(stringResource(s.label), since == s, { since = s }) }
                }
            }

            SectionTitle(stringResource(R.string.field_birthday))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val m = bMonth
                val d = bDay
                OutlinedButton(onClick = { pickDate = true }, shape = Ties.RSm) {
                    Text(
                        if (m != null && d != null) birthdayLabel(m, d) else stringResource(R.string.birthday_set),
                        color = Ties.Ink, style = TiesType.Button,
                    )
                }
                if (m != null) {
                    TextButton(onClick = { bMonth = null; bDay = null }) {
                        Text(stringResource(R.string.birthday_clear), color = Ties.Muted)
                    }
                }
            }

            SectionTitle(stringResource(R.string.field_notes))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                placeholder = { Text(stringResource(R.string.notes_hint), color = Ties.Faint) },
                minLines = 3, shape = Ties.RSm, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            if (!isNew) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.delete_person), color = Ties.Coral)
                }
            }
        }
        PrimaryButton(
            stringResource(R.string.save), ::save,
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
            enabled = name.isNotBlank(),
        )
    }

    if (pickDate) {
        val initial = if (bMonth != null && bDay != null) {
            LocalDate.of(2000, bMonth!!, bDay!!).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else null
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        bMonth = date.monthValue
                        bDay = date.dayOfMonth
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.ok), color = Ties.Coral) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel), color = Ties.Muted) } },
        ) {
            DatePicker(state = state, title = { Text(stringResource(R.string.birthday_year_hint), style = TiesType.Muted, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ties.Bg,
            title = { Text(stringResource(R.string.delete_confirm_title, name), style = TiesType.Strong) },
            text = { Text(stringResource(R.string.delete_confirm_text), style = TiesType.Sub) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    container.scope.launch { dao.get(id)?.let { dao.delete(it) } }
                    onDeleted()
                }) { Text(stringResource(R.string.delete), color = Ties.Coral) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel), color = Ties.Muted) } },
        )
    }
}
