//---App.kt
package com.example.bprogress

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.core.text.layoutDirection
import com.example.bprogress.BuildConfig
import androidx.hilt.work.HiltWorkerFactory
// import androidx.work.WorkerResult // Keep this commented if it's causing an error right now, we'll see if the direct usage below resolves things
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Configuration as WorkConfiguration // Keep alias for WorkManager's Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy // Import the enum itself
import androidx.work.ExistingWorkPolicy         // Import the enum itself
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager                 // Make sure this is imported
// REMOVE these alias imports as per your request to use the enums directly
// import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE as PeriodicCancelAndReenqueue
// import androidx.work.ExistingWorkPolicy.CANCEL_AND_REENQUEUE as OneTimeCancelAndReenqueue
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import android.Manifest

@HiltAndroidApp
class App : Application(), WorkConfiguration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    @Named("DailyReminderChannelID")
    lateinit var dailyReminderChannelId: String

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()

    companion object {
        const val TAG = "BProgressApp"
        const val DAILY_REMINDER_CHANNEL_ID = "daily_reminder_channel"
        const val ACTION_LOCALE_CHANGED = "com.example.bprogress.ACTION_LOCALE_CHANGED"
        const val PREFS_NAME = "BProgressPrefs"
        const val PREF_KEY_FIRST_LAUNCH = "isFirstLaunch"
        const val PREF_KEY_DAILY_NOTIFICATION_SCHEDULED_TIME = "dailyNotificationScheduledTime"

        fun persistAppLocalePreference(context: Context, newLocaleCode: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString("appLocale", newLocaleCode)
            }
            Log.d(TAG, "Persisted app locale preference: '$newLocaleCode'.")
        }

        fun getSavedLocaleCode(context: Context): String {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString("appLocale", null) ?: getDefaultDeviceLocaleCode()
        }

        private fun getDefaultDeviceLocaleCode(): String {
            val deviceLocaleTag = Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag()
            val language = deviceLocaleTag.split("-").firstOrNull()?.lowercase(Locale.ROOT) ?: "en"
            return when (language) {
                "pt" -> "pt"
                "es" -> "es"
                "ja" -> "ja"
                else -> "en"
            }
        }

        fun updateAppLocale(context: Context, newLocaleCode: String) {
            val localeToSet = if (newLocaleCode.equals("pt", ignoreCase = true)) {
                Locale("pt", "BR")
            } else {
                Locale(newLocaleCode)
            }
            Locale.setDefault(localeToSet)
            val localeList = LocaleListCompat.create(localeToSet)
            AppCompatDelegate.setApplicationLocales(localeList)

            val currentConfig = context.resources.configuration
            val newConfig = Configuration(currentConfig)
            newConfig.setLocale(localeToSet)
            newConfig.setLayoutDirection(localeToSet)

            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
            persistAppLocalePreference(context, newLocaleCode)
            Log.d(TAG, "App locale set to '$newLocaleCode'. AppCompatDelegate called.")

            val intent = Intent(ACTION_LOCALE_CHANGED)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        @Suppress("unused")
        private fun getLayoutDirectionIntFromLocale(locale: Locale): Int {
            return locale.layoutDirection
        }

        fun wrapContextWithLocale(baseContext: Context): Context {
            val codeToUse = getSavedLocaleCode(baseContext)
            val localeToSet = if (codeToUse.equals("pt", ignoreCase = true)) {
                Locale("pt", "BR")
            } else {
                Locale(codeToUse)
            }
            Locale.setDefault(localeToSet)

            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(localeToSet)
            config.setLayoutDirection(localeToSet)
            return baseContext.createConfigurationContext(config)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate starting...")

        applySavedTheme()
        setInitialLocale()
        createDailyReminderNotificationChannel()
        scheduleDailyReminder()
        handleFirstLaunchDebugNotification()

        Log.d(TAG, "Application onCreate finished.")
    }

    private fun applySavedTheme() {
        val currentPrefs = sharedPreferences
        val isDarkMode = currentPrefs.getBoolean("isDarkMode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        Log.d(TAG, "Theme applied: Dark mode = $isDarkMode")
    }

    private fun setInitialLocale() {
        val localeToApply = getSavedLocaleCode(this)
        Log.d(TAG, "Setting initial application locale to: $localeToApply")
        updateAppLocale(this, localeToApply)
    }

    private fun createDailyReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(dailyReminderChannelId)
            if (existingChannel == null) {
                val name = getString(R.string.notification_channel_daily_reminder_name)
                val descriptionText = getString(R.string.notification_channel_daily_reminder_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(dailyReminderChannelId, name, importance).apply {
                    description = descriptionText
                }
                notificationManager.createNotificationChannel(channel)
                Log.i(TAG, "Notification channel '$dailyReminderChannelId' created.")
            } else {
                Log.d(TAG, "Notification channel '$dailyReminderChannelId' already exists.")
            }
        }
    }

    private fun scheduleDailyReminder() {
        val workManager = WorkManager.getInstance(applicationContext)

        val initialDelay = TimeUnit.MINUTES.toMillis(1)
        val repeatIntervalMillis = TimeUnit.MINUTES.toMillis(15)
        Log.d(TAG, "TESTING: Periodic worker initial delay: $initialDelay ms, repeat: $repeatIntervalMillis ms")

        // Original logic commented out
        /*
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 20)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (currentTime.after(dueTime)) {
            dueTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        val productionInitialDelay = dueTime.timeInMillis - currentTime.timeInMillis
        val productionRepeatIntervalMillis = TimeUnit.DAYS.toMillis(1)

        val productionConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val productionDailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            productionRepeatIntervalMillis,
            TimeUnit.MILLISECONDS
        )
            .setInitialDelay(productionInitialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(productionConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP, // For production
            productionDailyWorkRequest
        )
        Log.i(TAG, "Enqueued unique periodic work '${DailyReminderWorker.WORK_NAME_PERIODIC}' with KEEP policy.")
        sharedPreferences.edit {
            putLong(PREF_KEY_DAILY_NOTIFICATION_SCHEDULED_TIME, dueTime.timeInMillis)
        }
        */

        // Test Block
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            repeatIntervalMillis,
            TimeUnit.MILLISECONDS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, // Direct usage
            dailyWorkRequest
        )
        Log.i(TAG, "TESTING: Enqueued unique periodic work '${DailyReminderWorker.WORK_NAME_PERIODIC}' with CANCEL_AND_REENQUEUE policy.")
    }

    private fun handleFirstLaunchDebugNotification() {
        val isFirstLaunch = sharedPreferences.getBoolean(PREF_KEY_FIRST_LAUNCH, true)

        if (isFirstLaunch) {
            Log.d(TAG, "First launch detected.")
            var notificationsAllowed = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationsAllowed = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (notificationsAllowed) {
                Log.d(TAG, "Notifications are allowed. Scheduling debug notification.")
                val debugWorkRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                    .setInitialDelay(6, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    DailyReminderWorker.WORK_NAME_ONETIME_DEBUG,
                    ExistingWorkPolicy.REPLACE, // Direct usage
                    debugWorkRequest
                )
                Log.i(TAG, "Enqueued one-time debug notification '${DailyReminderWorker.WORK_NAME_ONETIME_DEBUG}'.")
            } else {
                Log.w(TAG, "First launch, but POST_NOTIFICATIONS permission not granted. Debug notification NOT scheduled.")
            }
            sharedPreferences.edit {
                putBoolean(PREF_KEY_FIRST_LAUNCH, false)
            }
        } else {
            Log.d(TAG, "Not the first launch. No debug notification scheduled.")
        }
    }
}
