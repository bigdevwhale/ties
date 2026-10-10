package app.ties.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.ties.R
import app.ties.data.Circle
import app.ties.data.TouchKind
import app.ties.domain.Status
import app.ties.ui.initials
import app.ties.ui.kindLabel
import app.ties.ui.theme.Inter
import app.ties.ui.theme.Ties
import app.ties.ui.theme.TiesType
import java.time.LocalDate
import java.time.ZoneId

/** Initials in the person's circle colour, wrapped in a ring that empties as the next contact comes due. */
@Composable
fun Avatar(name: String, circle: Circle, warmth: Float?, status: Status?, size: Dp = 48.dp) {
    val ring = 3.dp
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (warmth != null && status != null) {
            val track = Ties.statusSoft(status)
            val arc = Ties.status(status)
            Canvas(Modifier.fillMaxSize()) {
                val stroke = ring.toPx()
                val inset = stroke / 2
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                // An overdue ring stays full coral: it reads as "calling for you", not "empty".
                val sweep = if (status == Status.OVERDUE || status == Status.DUE) 360f else 360f * warmth.coerceAtLeast(0.04f)
                drawArc(arc, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        val inner = if (warmth != null) size - ring * 2 - 4.dp else size
        Box(
            Modifier.size(inner).clip(CircleShape).background(Ties.circle(circle).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(name),
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = (inner.value * 0.36f).sp,
                color = Ties.circle(circle),
            )
        }
    }
}

@Composable
fun TiesCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Ties.RMd)
            .background(Ties.Surface)
            .border(1.dp, Ties.Line, Ties.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = TiesType.Eyebrow, modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp))
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = Ties.RSm,
        colors = ButtonDefaults.buttonColors(containerColor = Ties.Coral, contentColor = Ties.OnCoral),
    ) { Text(text, style = TiesType.Button) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = Ties.Coral) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
        shape = Ties.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Ties.Surface,
            labelColor = Ties.Muted,
            selectedContainerColor = color.copy(alpha = 0.14f),
            selectedLabelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Ties.Line,
            selectedBorderColor = color.copy(alpha = 0.5f),
        ),
    )
}

/** "We talked": how, when, and an optional note worth remembering. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogTouchSheet(name: String, onDismiss: () -> Unit, onSave: (TouchKind, Long, String) -> Unit) {
    var kind by rememberSaveable { mutableStateOf(TouchKind.CALL) }
    var yesterday by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ties.Bg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.log_title, name), style = TiesType.Title.copy(fontSize = 22.sp, lineHeight = 28.sp))
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TouchKind.entries.forEach { Chip(kindLabel(it), kind == it, { kind = it }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.ago_today), !yesterday, { yesterday = false })
                Chip(stringResource(R.string.ago_yesterday), yesterday, { yesterday = true })
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.log_note_hint), color = Ties.Faint) },
                minLines = 2,
                shape = Ties.RSm,
            )
            Spacer(Modifier.height(16.dp))
            val savedLabel = stringResource(R.string.log_saved)
            val context = LocalContext.current
            PrimaryButton(stringResource(R.string.log_save), {
                val at = if (yesterday) {
                    LocalDate.now().minusDays(1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else System.currentTimeMillis()
                onSave(kind, at, note)
                Toast.makeText(context, savedLabel, Toast.LENGTH_SHORT).show()
            }, Modifier.fillMaxWidth())
        }
    }
}

object ContactActions {
    fun dial(context: Context, phone: String) =
        launch(context, Intent(Intent.ACTION_DIAL, ("tel:" + Uri.encode(phone)).toUri()))

    fun sms(context: Context, phone: String) =
        launch(context, Intent(Intent.ACTION_SENDTO, ("smsto:" + Uri.encode(phone)).toUri()))

    fun whatsapp(context: Context, phone: String) =
        launch(context, Intent(Intent.ACTION_VIEW, ("https://wa.me/" + phone.filter { it.isDigit() }).toUri()))

    fun telegram(context: Context, phone: String) =
        launch(context, Intent(Intent.ACTION_VIEW, ("https://t.me/+" + phone.filter { it.isDigit() }).toUri()))

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
        false
    }
}

/** Today's date, refreshed whenever the app comes back to the foreground. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = LocalDate.now() }
    return today
}
