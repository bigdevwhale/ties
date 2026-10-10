package app.ties

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.ties.reminders.Reminders
import app.ties.ui.TiesRoot
import app.ties.ui.theme.TiesTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {
    /** Person requested from outside (tapping a reminder about a single person). */
    private val openPerson = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent {
            TiesTheme {
                TiesRoot(openPerson = openPerson)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val id = intent?.getLongExtra(Reminders.EXTRA_PERSON_ID, 0L) ?: 0L
        if (id > 0) openPerson.value = id
        Reminders.dismiss(this)
    }
}
