package app.ties.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.ties.MainActivity
import app.ties.R
import app.ties.container
import app.ties.data.Person
import app.ties.domain.Cadence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One daily check at the user's chosen time; posts a notification only when someone is due or has a birthday. */
object Reminders {
    private const val CHANNEL_ID = "nudges_v1"
    private const val NOTIFICATION_ID = 11
    const val ACTION_FIRE = "app.ties.reminders.FIRE"
    const val EXTRA_PERSON_ID = "person_id"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    private fun fireIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    suspend fun reschedule(context: Context) {
        val config = context.container.settings.reminderNow()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = fireIntent(context)
        if (!config.enabled) {
            am.cancel(pi)
            return
        }
        val at = Cadence.nextReminder(LocalDateTime.now(), config.minutes)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // A nudge does not need to-the-minute precision; this avoids the SCHEDULE_EXACT_ALARM permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    suspend fun fire(context: Context) {
        if (!canNotify(context)) return
        val today = LocalDate.now()
        val people = context.container.db.people().all()
        val birthdays = people.filter { Cadence.daysToBirthday(it, today) == 0L }
        val due = people
            .map { it to Cadence.daysLeft(it, today) }
            .filter { (p, left) -> Cadence.shouldRemind(left) && p !in birthdays }
            .sortedBy { it.second }
            .map { it.first }
        if (birthdays.isEmpty() && due.isEmpty()) return

        val title: String
        val text: String
        if (birthdays.isNotEmpty()) {
            title = context.getString(R.string.notif_birthday_title, names(context, birthdays))
            text = if (due.isEmpty()) context.getString(R.string.notif_birthday_text)
            else context.getString(R.string.notif_due_text, names(context, due))
        } else {
            title = context.getString(R.string.notif_due_title)
            text = context.getString(R.string.notif_due_text, names(context, due))
        }

        val single = (birthdays + due).singleOrNull()
        val openIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (single != null) openIntent.putExtra(EXTRA_PERSON_ID, single.id)
        val open = PendingIntent.getActivity(
            context, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.coral))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
        }
    }

    private fun names(context: Context, people: List<Person>): String {
        val shown = people.take(3).joinToString(", ") { it.name }
        val rest = people.size - 3
        return if (rest <= 0) shown
        else shown + context.resources.getQuantityString(R.plurals.and_more, rest, rest)
    }

    fun dismiss(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Reminders.ACTION_FIRE) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                if (context.container.settings.reminderNow().enabled) Reminders.fire(context)
                Reminders.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Reminders.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
