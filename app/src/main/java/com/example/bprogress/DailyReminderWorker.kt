// DailyReminderWorker.kt
package com.example.bprogress

import android.Manifest
import android.app.Notification // Keep this
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat // For permission check
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bprogress.data.ChatMessage
import com.example.bprogress.data.OpenAiChatRequest
import com.example.bprogress.repository.OpenAiRepository // Using the repository
import com.example.bprogress.repository.OpenAiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Named

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val activityDao: ActivityDao,
    private val openAiRepository: OpenAiRepository, // Inject repository
    private val notificationManager: NotificationManager,
    @Named("DailyReminderChannelID") private val notificationChannelId: String
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "DailyReminderPeriodicWorker"
        const val WORK_NAME_ONETIME_DEBUG = "DailyReminderOneTimeDebugWorker"
        const val NOTIFICATION_ID = 101 // You can make this dynamic if needed
        const val TAG = "DailyReminderWorker"

        // Input data keys (optional, but good practice if you ever want to pass data)
        // const val KEY_IS_DEBUG_NOTIFICATION = "is_debug_notification"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "doWork started. Channel ID: $notificationChannelId")

        // Check for notification permission (especially important for scheduled workers)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot send reminder.")
                // Depending on policy, you might return success to stop retries if permission is the issue
                return@withContext Result.success() // Or Result.failure() if you want WorkManager to retry based on policy
            }
        }

        try {
            val uncheckedActivities = activityDao.getUncheckedActivitiesList()
            if (uncheckedActivities.isEmpty()) {
                Log.i(TAG, "No unchecked activities to remind about. Notification not sent.")
                return@withContext Result.success() // Task done, no need to notify
            }

            val activityNames = uncheckedActivities.take(3) // Take a few for brevity
                .joinToString(", ") { it.column1Data ?: "a task" }
            val userPrompt = "Give a very short, encouraging tip (1-2 sentences) about the benefits of completing tasks like: $activityNames. Focus on progress and well-being."

            var tipContent = appContext.getString(R.string.default_daily_tip) // Default fallback from strings.xml

            Log.d(TAG, "Attempting to fetch tip from OpenAI with prompt: \"$userPrompt\"")
            when (val result = openAiRepository.generateMotivationalMessage(userPrompt = userPrompt)) {
                is OpenAiResult.Success -> {
                    tipContent = result.message
                    Log.i(TAG, "Successfully fetched tip from OpenAI: $tipContent")
                }
                is OpenAiResult.Error -> {
                    Log.e(TAG, "OpenAI API error: ${result.errorMessage} (Code: ${result.errorCode}). Using fallback message.")
                    // Fallback message is already set, so just log
                }
            }

            val notificationTitle = appContext.getString(R.string.notification_channel_daily_reminder_name)

            sendNotification(
                contextForNotification = appContext,
                title = notificationTitle,
                contentText = tipContent,
                notificationId = NOTIFICATION_ID // Use the class-level constant
            )
            Log.i(TAG, "Notification attempt complete with content: $tipContent")
            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in DailyReminderWorker's doWork: ${e.message}", e)
            return@withContext Result.retry() // Retry if a transient error occurs
        }
    }

    private fun sendNotification(
        contextForNotification: Context,
        title: String,
        contentText: String,
        notificationId: Int
    ) {
        // Permission check is now at the beginning of doWork,
        // but double-checking here or relying on the earlier check is fine.
        // For brevity, assuming the check in doWork is sufficient.

        val intent = Intent(contextForNotification, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            contextForNotification,
            notificationId, // Use notificationId as request code to allow updating
            intent,
            pendingIntentFlags
        )

        val notificationBuilder = Notification.Builder(contextForNotification, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(contentText.take(120) + if (contentText.length > 120) "..." else "")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
        // NO .setPriority() here for API 26+ as channel importance is used.

        // For versions BEFORE Android O, set priority on the builder.
        // For Android O and later, priority is handled by the channel's importance.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION") // Suppress for this specific pre-Oreo usage
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT)
        }

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        Log.i(TAG, "Notification $notificationId sent: $title")
    }
}
