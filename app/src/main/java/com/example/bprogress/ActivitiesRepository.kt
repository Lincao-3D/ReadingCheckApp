package com.example.bprogress

import android.content.Context
import android.util.Log
// REMOVE: import androidx.compose.animation.core.copy // <<< FIX 1: Unnecessary import
import androidx.work.Data // For passing data to Worker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bprogress.workers.StreakAiNotificationWorker
// import com.example.bprogress.App // Not strictly needed if appContext is always used for locale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.lowercase // Keep if getLocalizedCsvFileName uses it, which it does

@Singleton
class ActivitiesRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val userProgressDao: UserProgressDao,
    @ApplicationContext private val appContext: Context
) {
    companion object {
        const val MILESTONE_INTERVAL_FOR_AI_NOTIFICATION = 3
        private const val TAG = "ActivitiesRepository"
    }

    val allActivities: Flow<List<ActivityItem>> = activityDao.getAllActivitiesFlow()
    val userProgress: Flow<UserProgress?> = userProgressDao.getUserProgressFlow()

    suspend fun ensureInitialDataLoaded() = withContext(Dispatchers.IO) {
        if (userProgressDao.getSingleUserProgressInstance() == null) {
            Log.d(TAG, "No user progress found, creating initial progress.")
            val initialProgress = UserProgress(
                id = "USER_PROGRESS_ID",
                firstCheckTimestamp = null,
                totalChecksCount = 0,
                lastTenCheckNotificationCount = 0,
                lastMilestoneNotificationCount = 0,
                sevenDayNotificationSent = false,
                fiftyStreakAchieved = false,
                fiftyStreakFeeling = null,
                lastStreakDialogShownAtCount = 0
            )
            userProgressDao.insertUserProgress(initialProgress)
            Log.i(TAG, "Initial user progress created.")
        } else {
            Log.d(TAG, "User progress already exists.")
        }

        if (activityDao.getItemCount() == 0) {
            val csvFileName = getLocalizedCsvFileName(appContext)
            Log.d(TAG, "No activities found, attempting to load from localized CSV: $csvFileName")
            try {
                val activitiesToInsert = parseCsvData(appContext, csvFileName)
                if (activitiesToInsert.isNotEmpty()) {
                    activityDao.insertAllActivities(activitiesToInsert)
                    Log.i(TAG, "${activitiesToInsert.size} activities inserted from $csvFileName.")
                } else {
                    Log.w(TAG, "$csvFileName data was empty or failed to parse. No activities inserted.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading activities from $csvFileName", e)
            }
        } else {
            Log.d(TAG, "Activities already exist in the database.")
        }
    }

    private fun getLocalizedCsvFileName(contextForLocale: Context): String {
        val currentLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            contextForLocale.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            contextForLocale.resources.configuration.locale
        }
        val languageCode = currentLocale.language.lowercase()
        Log.d(TAG, "Determining CSV for language code: $languageCode (from locale: $currentLocale)")
        return when (languageCode) {
            "pt" -> "activities-ptbr.csv"
            "es" -> "activities-sp.csv"
            "ja" -> "activities-jp.csv"
            "en" -> "activities-eng.csv"
            else -> {
                Log.w(TAG, "Unsupported language code '$languageCode', defaulting to activities-eng.csv")
                "activities-eng.csv"
            }
        }
    }

    suspend fun toggleCheckedStatus(item: ActivityItem) = withContext(Dispatchers.IO) {
        val newCheckedStatus = !item.isChecked
        val timestamp = if (newCheckedStatus) System.currentTimeMillis() else null
        activityDao.updateCheckedStatus(item.id, newCheckedStatus, timestamp)
        // Log.d(TAG, "Toggled checked status for item ID ${item.id} to $newCheckedStatus with timestamp $timestamp") // Original log

        val currentProgress = userProgressDao.getSingleUserProgressInstance()
        currentProgress?.let { progress -> // 'progress' is defined here and is not null
            val newTotalChecks = if (newCheckedStatus) { // 'newTotalChecks' defined here
                progress.totalChecksCount + 1
            } else {
                (progress.totalChecksCount - 1).coerceAtLeast(0)
            }

            val newFirstCheckTimestamp = if (newCheckedStatus && progress.firstCheckTimestamp == null && newTotalChecks > 0) { // 'newFirstCheckTimestamp' defined here
                System.currentTimeMillis()
            } else {
                progress.firstCheckTimestamp
            }

            // <<< FIX 2: Moved log and update to correct scope >>>
            Log.d(TAG, "Updating UserProgress in toggleCheckedStatus: progress.id=${progress.id}, newTotalChecks = $newTotalChecks, newFirstCheckTimestamp = $newFirstCheckTimestamp")
            userProgressDao.updateUserProgress(
                progress.copy( // Uses 'progress' from the let block
                    totalChecksCount = newTotalChecks,
                    firstCheckTimestamp = newFirstCheckTimestamp
                )
            )
            // The Log.d(TAG, "Updated totalChecksCount to $newTotalChecks...") is now covered by the log above.
        }
    }

    suspend fun toggleImportantStatus(item: ActivityItem) = withContext(Dispatchers.IO) {
        val updatedItem = item.copy(isImportant = !item.isImportant) // Standard data class copy
        activityDao.updateActivity(updatedItem)
    }

    suspend fun saveUserFeelingAndDialogState(
        feeling: String,
        totalChecksAtMilestone: Int,
        milestoneDialogShownAtCount: Int
    ) = withContext(Dispatchers.IO) {
        val progress = userProgressDao.getSingleUserProgressInstance()
        if (progress != null) {
            Log.d(TAG, "saveUserFeelingAndDialogState: Initial progress.totalChecksCount = ${progress.totalChecksCount}, progress.lastMilestoneNotificationCount = ${progress.lastMilestoneNotificationCount}")
            Log.d(TAG, "saveUserFeelingAndDialogState: Received totalChecksAtMilestone = $totalChecksAtMilestone, MILESTONE_INTERVAL = $MILESTONE_INTERVAL_FOR_AI_NOTIFICATION")

            var updatedProgress = progress.copy( // Ensure this is var if modified later for lastMilestoneNotificationCount
                fiftyStreakFeeling = feeling,
                lastStreakDialogShownAtCount = milestoneDialogShownAtCount,
                fiftyStreakAchieved = progress.fiftyStreakAchieved || (totalChecksAtMilestone >= MILESTONE_INTERVAL_FOR_AI_NOTIFICATION)
            )
            userProgressDao.updateUserProgress(updatedProgress) // Save feeling, dialog state, and potentially fiftyStreakAchieved
            Log.i(TAG, "User feeling '$feeling' and dialog shown at count $milestoneDialogShownAtCount saved. fiftyStreakAchieved is now ${updatedProgress.fiftyStreakAchieved}")


            if (totalChecksAtMilestone > 0 &&
                totalChecksAtMilestone % MILESTONE_INTERVAL_FOR_AI_NOTIFICATION == 0 &&
                progress.lastMilestoneNotificationCount < totalChecksAtMilestone // Check original progress's last count
            ) {
                Log.i(TAG, "Milestone condition MET. Current progress.lastMilestoneNotificationCount = ${progress.lastMilestoneNotificationCount}. Will update to $totalChecksAtMilestone.")
                Log.i(TAG, "Milestone $totalChecksAtMilestone reached for AI notification. Enqueuing StreakAIWorker.")

                // Update 'lastMilestoneNotificationCount' in the progress object before saving this specific update
                updatedProgress = updatedProgress.copy(lastMilestoneNotificationCount = totalChecksAtMilestone)
                userProgressDao.updateUserProgress(updatedProgress) // This save includes the updated lastMilestoneNotificationCount
                Log.d(TAG, "Updated lastMilestoneNotificationCount to $totalChecksAtMilestone in DB.")


                val recentActivity = activityDao.getMostRecentlyCheckedActivity()
                val activityNameForPrompt = recentActivity?.column1Data ?: "your current focus"
                Log.d(TAG, "Context for AI prompt: $activityNameForPrompt")

                val inputData = Data.Builder()
                    .putString(StreakAiNotificationWorker.KEY_FEELING_INPUT, feeling)
                    .putString(StreakAiNotificationWorker.KEY_ACTIVITY_CONTEXT, activityNameForPrompt)
                    .putInt(StreakAiNotificationWorker.KEY_MILESTONE_COUNT, totalChecksAtMilestone)
                    .build()

                val streakAiWorkRequest = OneTimeWorkRequestBuilder<StreakAiNotificationWorker>()
                    .setInputData(inputData)
                    .addTag("StreakAiNotification")
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    "StreakAiNotification_ForCheck_$totalChecksAtMilestone",
                    ExistingWorkPolicy.KEEP,
                    streakAiWorkRequest
                )
                Log.d(TAG, "StreakAiNotificationWorker enqueued for milestone $totalChecksAtMilestone with context '$activityNameForPrompt'.")
            } else {
                // Enhanced log for why condition was not met
                val reasonTotalChecks = "totalChecksAtMilestone ($totalChecksAtMilestone) > 0: ${totalChecksAtMilestone > 0}"
                val reasonMultipleOfInterval = "isMultipleOfInterval (${totalChecksAtMilestone % MILESTONE_INTERVAL_FOR_AI_NOTIFICATION == 0}): ${totalChecksAtMilestone % MILESTONE_INTERVAL_FOR_AI_NOTIFICATION == 0}"
                val reasonLastMilestoneCount = "lastMilestoneNotificationCount (${progress.lastMilestoneNotificationCount}) < totalChecksAtMilestone ($totalChecksAtMilestone): ${progress.lastMilestoneNotificationCount < totalChecksAtMilestone}"
                Log.d(TAG, "Milestone condition NOT MET or already sent for milestone $totalChecksAtMilestone. " +
                        "Current Total: $totalChecksAtMilestone, Interval: $MILESTONE_INTERVAL_FOR_AI_NOTIFICATION, LastSentDB: ${progress.lastMilestoneNotificationCount}. " +
                        "Checks: [$reasonTotalChecks, $reasonMultipleOfInterval, $reasonLastMilestoneCount]")
            }
        } else {
            Log.e(TAG, "Failed to save user feeling: UserProgress was null.")
        }
    }

    private fun parseCsvData(context: Context, fileName: String): List<ActivityItem> {
        val activities = mutableListOf<ActivityItem>()
        var orderCounter = 0
        Log.d(TAG, "Attempting to parse CSV: $fileName")
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).useLines { lines ->
                    lines.drop(1)
                        .forEach { line ->
                            val tokens = line.split(";").map { it.trim() }
                            if (tokens.size >= 2) {
                                val idValue = tokens.getOrNull(0)
                                val activity = ActivityItem(
                                    id = if (idValue != null && idValue.isNotBlank()) idValue else UUID.randomUUID().toString(),
                                    column1Data = tokens.getOrElse(1) { "" },
                                    column2Data = tokens.getOrElse(2) { "" },
                                    isChecked = false,
                                    isImportant = false,
                                    orderIndex = tokens.getOrNull(3)?.toIntOrNull() ?: orderCounter++,
                                    lastCheckedDate = null
                                )
                                activities.add(activity)
                            } else {
                                Log.w(TAG, "Skipping malformed CSV line in $fileName: $line")
                            }
                        }
                }
            }
            Log.i(TAG, "Successfully parsed ${activities.size} items from $fileName")
        } catch (e: java.io.FileNotFoundException) {
            Log.e(TAG, "CSV file not found: $fileName", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV file '$fileName'", e)
        }
        return activities
    }
}
