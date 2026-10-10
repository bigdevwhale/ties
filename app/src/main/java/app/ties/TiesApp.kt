package app.ties

import android.app.Application
import android.content.Context
import app.ties.data.SettingsStore
import app.ties.data.TiesDatabase
import app.ties.data.Touch
import app.ties.data.TouchKind
import app.ties.reminders.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val db = TiesDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a log survives the screen being closed right after tapping save. */
    fun logTouch(personId: Long, kind: TouchKind, at: Long, note: String) {
        scope.launch { db.people().logTouch(Touch(personId = personId, at = at, kind = kind, note = note.trim())) }
    }
}

class TiesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.ensureChannel(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch { Reminders.reschedule(this@TiesApp) }
    }
}

val Context.container: AppContainer get() = (applicationContext as TiesApp).container
