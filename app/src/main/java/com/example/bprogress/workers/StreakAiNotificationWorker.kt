package com.example.bprogress.workers

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bprogress.MainActivity
import com.example.bprogress.R
import com.example.bprogress.repository.OpenAiRepository
import com.example.bprogress.repository.OpenAiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Named

@HiltWorker
class StreakAiNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val openAiRepository: OpenAiRepository,
    private val notificationManager: NotificationManager,
    @Named("DailyReminderChannelID") private val notificationChannelId: String
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "StreakAiWorker"
        const val NOTIFICATION_ID_STREAK = 102 // Unique ID for streak notifications

        // Keys for input data
        const val KEY_FEELING_INPUT = "KEY_FEELING_INPUT"
        const val KEY_ACTIVITY_CONTEXT = "KEY_ACTIVITY_CONTEXT" // If you pass activity context
        const val KEY_MILESTONE_COUNT = "KEY_MILESTONE_COUNT"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "doWork started for streak AI notification.")

        val feeling = inputData.getString(KEY_FEELING_INPUT) ?: "great"
        val activityContext = inputData.getString(KEY_ACTIVITY_CONTEXT) ?: "making progress"
        val milestoneCount = inputData.getInt(KEY_MILESTONE_COUNT, 0)

        if (milestoneCount == 0) {
            Log.e(TAG, "Milestone count is 0, cannot generate relevant AI message.")
            return@withContext Result.failure()
        }

        val systemPrompt = "You are an AI assistant specialized in behavioral psychology and motivation. " +
                "Provide a short, insightful, and encouraging message (1-2 sentences) " +
                "based on the user's current achievement, their feeling, and their recent activity focus. " +
                "Include a subtle best practice or tip for maintaining momentum or dealing with challenges related to tasks like '${activityContext}'."

        val userPrompt = "I've just hit a ${milestoneCount}-check streak on tasks, including focusing on '${activityContext}', and I'm feeling '$feeling'. " +
                "What's one key insight or best practice I can use to keep this momentum going or " +
                "improve further, considering how I feel and my work on '${activityContext}'?"

        Log.d(TAG, "User prompt for OpenAI: $userPrompt")
        var notificationContent = "You've hit $milestoneCount checks, especially with '$activityContext'! Keep up the fantastic work!"

        when (val result = openAiRepository.generateMotivationalMessage(userPrompt, systemInstructions = systemPrompt, maxTokens = 90)) {
            is OpenAiResult.Success -> {
                notificationContent = result.message
                Log.i(TAG, "Successfully fetched streak AI message: $notificationContent")
            }
            is OpenAiResult.Error -> {
                Log.e(TAG, "OpenAI error for streak message: ${result.errorMessage} (Code: ${result.errorCode}). Using fallback.")
            }
        }

        val notificationTitle = appContext.getString(R.string.streak_milestone_notification_title)

        sendNotification(
            contextForNotification = appContext,
            title = notificationTitle,
            contentText = notificationContent,
            notificationId = NOTIFICATION_ID_STREAK + milestoneCount,
            milestoneAchieved = milestoneCount
        )
        Log.i(TAG, "Streak AI Notification attempt complete.")
        return@withContext Result.success()
    }


    private fun sendNotification(
        contextForNotification: Context,
        title: String,
        contentText: String,
        notificationId: Int,
        milestoneAchieved: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    contextForNotification,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification ID $notificationId not sent.")
                return
            }
        }

        // --- SCOPE CORRECTION HERE ---
        val notificationIntent = Intent(contextForNotification, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("source", "streak_notification")
            putExtra("milestone", milestoneAchieved)
        } // The .apply block ends here

        // pendingIntentFlags and pendingIntent should be defined outside the .apply block
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            contextForNotification,
            notificationId, // Use notificationId as request code for uniqueness per notification instance
            notificationIntent,
            pendingIntentFlags
        )
        // --- END SCOPE CORRECTION ---

        val notificationBuilder = Notification.Builder(contextForNotification, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification_icon_streak)
            .setContentTitle(title)
            .setContentText(contentText.take(120) + if (contentText.length > 120) "..." else "")
            .setStyle(Notification.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent) // Now pendingIntent is in scope
            .setAutoCancel(true)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        Log.i(TAG, "Notification $notificationId sent: $title for milestone $milestoneAchieved")
    }
}
