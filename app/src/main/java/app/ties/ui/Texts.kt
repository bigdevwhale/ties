package app.ties.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.ties.R
import app.ties.data.Circle
import app.ties.data.Person
import app.ties.data.TouchKind
import app.ties.domain.Cadence
import app.ties.domain.Status
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun circleLabel(c: Circle): String = stringResource(
    when (c) {
        Circle.FAMILY -> R.string.circle_family
        Circle.FRIENDS -> R.string.circle_friends
        Circle.CHILDHOOD -> R.string.circle_childhood
        Circle.WORK -> R.string.circle_work
        Circle.OTHER -> R.string.circle_other
    }
)

@Composable
fun kindLabel(k: TouchKind): String = stringResource(
    when (k) {
        TouchKind.CALL -> R.string.kind_call
        TouchKind.MESSAGE -> R.string.kind_message
        TouchKind.MEETING -> R.string.kind_meeting
    }
)

@Composable
fun frequencyLabel(days: Int): String = when (days) {
    7 -> stringResource(R.string.freq_week)
    14 -> stringResource(R.string.freq_2weeks)
    30 -> stringResource(R.string.freq_month)
    60 -> stringResource(R.string.freq_2months)
    90 -> stringResource(R.string.freq_3months)
    180 -> stringResource(R.string.freq_halfyear)
    else -> pluralStringResource(R.plurals.freq_days, days, days)
}

/** "today", "3 days ago", "2 weeks ago", "4 months ago", "over a year ago". */
@Composable
fun agoLabel(days: Long): String = when {
    days <= 0 -> stringResource(R.string.ago_today)
    days == 1L -> stringResource(R.string.ago_yesterday)
    days < 14 -> pluralStringResource(R.plurals.ago_days, days.toInt(), days.toInt())
    days < 60 -> (days / 7).toInt().let { pluralStringResource(R.plurals.ago_weeks, it, it) }
    days < 365 -> (days / 30).toInt().let { pluralStringResource(R.plurals.ago_months, it, it) }
    else -> stringResource(R.string.ago_year_plus)
}

@Composable
fun lastTouchLabel(person: Person, today: LocalDate): String {
    val last = person.lastTouchAt
    if (last != null) return stringResource(R.string.last_touch, agoLabel(Cadence.daysSince(last, today)))
    // Nothing logged yet: show the user's own estimate from when the person was added, if they gave one.
    val estimated = Cadence.daysSince(person.sinceAt, today)
    return if (estimated > Cadence.daysSince(person.createdAt, today)) stringResource(R.string.last_touch, agoLabel(estimated))
    else stringResource(R.string.last_never)
}

@Composable
fun statusLabel(daysLeft: Long, status: Status): String = when (status) {
    // A running count past a month reads as reproach, not a nudge.
    Status.OVERDUE -> if (-daysLeft > 30) stringResource(R.string.status_overdue_long)
    else pluralStringResource(R.plurals.status_overdue, (-daysLeft).toInt(), (-daysLeft).toInt())
    Status.DUE -> stringResource(R.string.status_due)
    Status.SOON -> pluralStringResource(R.plurals.status_soon, daysLeft.toInt(), daysLeft.toInt())
    Status.OK -> stringResource(R.string.status_ok)
}

@Composable
fun birthdayLabel(month: Int, day: Int): String =
    LocalDate.of(2000, month, day).format(DateTimeFormatter.ofPattern("d MMMM", locale()))

@Composable
fun birthdayInLabel(days: Long): String = when (days) {
    0L -> stringResource(R.string.bday_today)
    1L -> stringResource(R.string.bday_tomorrow)
    else -> pluralStringResource(R.plurals.bday_in, days.toInt(), days.toInt())
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
