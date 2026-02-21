package com.quoteapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quoteapp.data.FirebaseQuoteRepository
import com.quoteapp.notifications.NotificationHelper
import com.quoteapp.ui.screen.DailyQuoteRoute
import com.quoteapp.ui.screen.OnboardingScreen
import com.quoteapp.ui.screen.SettingsScreen
import com.quoteapp.ui.theme.QuoteAppTheme
import com.quoteapp.ui.viewmodel.QuoteViewModel
import com.quoteapp.ui.viewmodel.QuoteViewModelFactory

class MainActivity : ComponentActivity() {
    private var onboardingComplete by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.scheduleFromPreferences(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(applicationContext)
        onboardingComplete = readOnboardingComplete()
        if (onboardingComplete) {
            ensureNotificationSetup()
        }

        setContent {
            QuoteAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        !onboardingComplete -> {
                            OnboardingScreen { hour, minute, enabled ->
                                NotificationHelper.saveReminderTime(applicationContext, hour, minute)
                                NotificationHelper.setRemindersEnabled(applicationContext, enabled)
                                writeOnboardingComplete(true)
                                onboardingComplete = true
                                ensureNotificationSetup()
                            }
                        }

                        showSettings -> {
                            val (hour, minute) = NotificationHelper.loadReminderTime(applicationContext)
                            SettingsScreen(
                                initialHour = hour,
                                initialMinute = minute,
                                remindersEnabled = NotificationHelper.remindersEnabled(applicationContext),
                                onBack = { showSettings = false },
                                onSave = { enabled, h, m ->
                                    NotificationHelper.saveReminderTime(applicationContext, h, m)
                                    NotificationHelper.setRemindersEnabled(applicationContext, enabled)
                                    ensureNotificationSetup()
                                }
                            )
                        }

                        else -> {
                            val vm: QuoteViewModel = viewModel(
                                factory = QuoteViewModelFactory(FirebaseQuoteRepository(), applicationContext)
                            )
                            DailyQuoteRoute(viewModel = vm, onOpenSettings = { showSettings = true })
                        }
                    }
                }
            }
        }
    }

    private fun ensureNotificationSetup() {
        if (!NotificationHelper.remindersEnabled(applicationContext)) {
            NotificationHelper.cancelScheduledReminder(applicationContext)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                NotificationHelper.scheduleFromPreferences(applicationContext)
            } else {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationHelper.scheduleFromPreferences(applicationContext)
        }
    }

    private fun readOnboardingComplete(): Boolean {
        return getSharedPreferences("quote_app", Context.MODE_PRIVATE)
            .getBoolean("onboarding_complete", false)
    }

    private fun writeOnboardingComplete(value: Boolean) {
        getSharedPreferences("quote_app", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_complete", value)
            .apply()
    }
}
