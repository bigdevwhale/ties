package app.ties.domain

import app.ties.data.Circle
import app.ties.data.Person
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Status { OVERDUE, DUE, SOON, OK }

/** Pure rules for "how long since we talked and is it time again". */
object Cadence {
    val Frequencies = listOf(7, 14, 30, 60, 90, 180)

    /** While someone stays overdue, nudge again every this many days instead of daily. */
    const val NUDGE_EVERY = 3

    fun defaultEvery(circle: Circle): Int = when (circle) {
        Circle.FAMILY -> 7
        Circle.FRIENDS -> 14
        Circle.CHILDHOOD -> 30
        Circle.WORK -> 60
        Circle.OTHER -> 30
    }

    fun toDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun anchor(person: Person): Long = person.lastTouchAt ?: person.sinceAt

    /** Days until the next contact is due; negative when overdue. */
    fun daysLeft(anchor: LocalDate, everyDays: Int, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, anchor.plusDays(everyDays.toLong()))

    fun daysLeft(person: Person, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        daysLeft(toDate(anchor(person), zone), person.everyDays, today)

    fun daysSince(millis: Long, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        ChronoUnit.DAYS.between(toDate(millis, zone), today)

    fun soonWindow(everyDays: Int): Int = (everyDays / 5).coerceIn(1, 14)

    fun status(daysLeft: Long, everyDays: Int): Status = when {
        daysLeft < 0 -> Status.OVERDUE
        daysLeft == 0L -> Status.DUE
        daysLeft <= soonWindow(everyDays) -> Status.SOON
        else -> Status.OK
    }

    /** 1 right after a contact, fading to 0 when it is due. */
    fun warmth(daysLeft: Long, everyDays: Int): Float = (daysLeft.toFloat() / everyDays).coerceIn(0f, 1f)

    /** Remind on the due day, then every [NUDGE_EVERY] days so the reminder never turns into daily noise. */
    fun shouldRemind(daysLeft: Long): Boolean =
        daysLeft == 0L || (daysLeft < 0 && -daysLeft % NUDGE_EVERY == 0L)

    fun nextBirthday(month: Int, day: Int, today: LocalDate): LocalDate {
        fun inYear(year: Int) =
            if (month == 2 && day == 29 && !Year.isLeap(year.toLong())) LocalDate.of(year, 2, 28)
            else LocalDate.of(year, month, day)
        val thisYear = inYear(today.year)
        return if (thisYear.isBefore(today)) inYear(today.year + 1) else thisYear
    }

    fun daysToBirthday(person: Person, today: LocalDate): Long? {
        val month = person.birthdayMonth ?: return null
        val day = person.birthdayDay ?: return null
        return ChronoUnit.DAYS.between(today, nextBirthday(month, day, today))
    }

    fun nextReminder(now: LocalDateTime, minutes: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}
