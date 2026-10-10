package app.ties.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class ReminderConfig(
    val enabled: Boolean = true,
    /** Minutes after midnight of the daily nudge. */
    val minutes: Int = 19 * 60,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val reminder: Flow<ReminderConfig> = store.data.map {
        ReminderConfig(enabled = it[ENABLED] ?: true, minutes = it[MINUTES] ?: (19 * 60))
    }

    suspend fun reminderNow(): ReminderConfig = reminder.first()

    suspend fun setReminderEnabled(enabled: Boolean) {
        store.edit { it[ENABLED] = enabled }
    }

    suspend fun setReminderMinutes(minutes: Int) {
        store.edit { it[MINUTES] = minutes }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("reminder_enabled")
        val MINUTES = intPreferencesKey("reminder_minutes")
    }
}
