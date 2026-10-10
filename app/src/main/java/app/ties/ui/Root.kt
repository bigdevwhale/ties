package app.ties.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.ties.R
import app.ties.container
import app.ties.data.Person
import app.ties.data.TouchKind
import app.ties.reminders.Reminders
import app.ties.ui.components.ContactActions
import app.ties.ui.edit.EditPersonScreen
import app.ties.ui.home.HomeScreen
import app.ties.ui.person.PersonScreen
import app.ties.ui.settings.SettingsScreen
import app.ties.ui.theme.Ties
import app.ties.ui.theme.TiesType
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun TiesRoot(openPerson: MutableStateFlow<Long?>) {
    val context = LocalContext.current
    val nav = rememberNavController()

    val requested by openPerson.collectAsStateWithLifecycle()
    LaunchedEffect(requested) {
        val id = requested ?: return@LaunchedEffect
        nav.navigate("person/$id") { launchSingleTop = true }
        openPerson.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // We can't see the call log, so after a call started from the app we simply ask on return.
    var calledId by rememberSaveable { mutableStateOf<Long?>(null) }
    var calledName by rememberSaveable { mutableStateOf("") }
    var pendingAsk by rememberSaveable { mutableStateOf(false) }
    var askVisible by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { if (calledId != null) pendingAsk = true }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (pendingAsk) {
            pendingAsk = false
            askVisible = true
        }
    }
    val call: (Person) -> Unit = { p ->
        if (p.phone != null && ContactActions.dial(context, p.phone)) {
            calledId = p.id
            calledName = p.name
        }
    }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpen = { nav.navigate("person/$it") },
                onAdd = { nav.navigate("edit/0") },
                onSettings = { nav.navigate("settings") },
                onCall = call,
            )
        }
        composable("person/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            PersonScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("edit/$it") },
                onCall = call,
            )
        }
        composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            EditPersonScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                onDone = { nav.popBackStack() },
                onDeleted = { nav.popBackStack("home", inclusive = false) },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }

    if (askVisible) {
        val close = {
            askVisible = false
            calledId = null
        }
        AlertDialog(
            onDismissRequest = close,
            containerColor = Ties.Bg,
            title = { Text(stringResource(R.string.ask_title, calledName), style = TiesType.Strong.copy(fontSize = 19.sp)) },
            text = { Text(stringResource(R.string.ask_text), style = TiesType.Sub) },
            confirmButton = {
                TextButton(onClick = {
                    calledId?.let { context.container.logTouch(it, TouchKind.CALL, System.currentTimeMillis(), "") }
                    close()
                }) { Text(stringResource(R.string.ask_yes), color = Ties.Coral, style = TiesType.Button) }
            },
            dismissButton = {
                TextButton(onClick = close) { Text(stringResource(R.string.ask_no), color = Ties.Muted, style = TiesType.Button) }
            },
        )
    }
}
