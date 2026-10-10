package app.ties

import app.ties.domain.Cadence
import app.ties.domain.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CadenceTest {
    private val today = LocalDate.of(2026, 10, 10)

    @Test
    fun daysLeftCountsFromLastContact() {
        assertEquals(4, Cadence.daysLeft(today.minusDays(10), 14, today))
        assertEquals(0, Cadence.daysLeft(today.minusDays(14), 14, today))
        assertEquals(-6, Cadence.daysLeft(today.minusDays(20), 14, today))
    }

    @Test
    fun statusBands() {
        assertEquals(Status.OVERDUE, Cadence.status(-1, 30))
        assertEquals(Status.DUE, Cadence.status(0, 30))
        assertEquals(Status.SOON, Cadence.status(6, 30))
        assertEquals(Status.OK, Cadence.status(7, 30))
        // A weekly rhythm still gets one "soon" day.
        assertEquals(Status.SOON, Cadence.status(1, 7))
        assertEquals(Status.OK, Cadence.status(2, 7))
    }

    @Test
    fun remindsOnDueDayThenEveryThirdDay() {
        assertFalse(Cadence.shouldRemind(2))
        assertTrue(Cadence.shouldRemind(0))
        assertFalse(Cadence.shouldRemind(-1))
        assertFalse(Cadence.shouldRemind(-2))
        assertTrue(Cadence.shouldRemind(-3))
        assertTrue(Cadence.shouldRemind(-6))
    }

    @Test
    fun warmthFadesAndClamps() {
        assertEquals(1f, Cadence.warmth(14, 14))
        assertEquals(0.5f, Cadence.warmth(7, 14))
        assertEquals(0f, Cadence.warmth(-3, 14))
    }

    @Test
    fun nextBirthdayRollsOverAndHandlesLeapDay() {
        assertEquals(LocalDate.of(2026, 10, 10), Cadence.nextBirthday(10, 10, today))
        assertEquals(LocalDate.of(2027, 3, 1), Cadence.nextBirthday(3, 1, today))
        assertEquals(LocalDate.of(2027, 2, 28), Cadence.nextBirthday(2, 29, today))
        assertEquals(LocalDate.of(2028, 2, 29), Cadence.nextBirthday(2, 29, LocalDate.of(2027, 3, 1)))
    }

    @Test
    fun nextReminderIsTodayOrTomorrow() {
        val morning = LocalDateTime.of(2026, 10, 10, 9, 0)
        assertEquals(LocalDateTime.of(2026, 10, 10, 19, 0), Cadence.nextReminder(morning, 19 * 60))
        val evening = LocalDateTime.of(2026, 10, 10, 19, 0)
        assertEquals(LocalDateTime.of(2026, 10, 11, 19, 0), Cadence.nextReminder(evening, 19 * 60))
    }
}
