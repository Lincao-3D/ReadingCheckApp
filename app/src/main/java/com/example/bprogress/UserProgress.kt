package com.example.bprogress

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: String = "USER_PROGRESS_ID",
    val firstCheckTimestamp: Long? = null,
    val totalChecksCount: Int = 0,
    val lastTenCheckNotificationCount: Int = 0, // For a different notification logic if needed
    val lastMilestoneNotificationCount: Int = 0,
    val sevenDayNotificationSent: Boolean = false,
    val fiftyStreakAchieved: Boolean = false,
    val fiftyStreakFeeling: String? = null,
    val lastStreakDialogShownAtCount: Int = 0 // New field for the dialog logic
)