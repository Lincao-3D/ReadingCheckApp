// --- app/src/main/java/com/example/bprogress/UserProgressDao.kt ---
package com.example.bprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import kotlinx.coroutines.flow.Flow
import dagger.Provides

@Dao
interface UserProgressDao {
    // Assuming table name from UserProgress entity is 'user_progress'
    @Query("SELECT * FROM user_progress LIMIT 1")
    fun getUserProgressFlow(): Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(userProgress: UserProgress)

    @Update
    suspend fun updateUserProgress(userProgress: UserProgress)

    @Query("SELECT * FROM user_progress LIMIT 1")
    suspend fun getSingleUserProgressInstance(): UserProgress?

    // Assuming UserProgress has a primary key 'id' of type String for this example.
    // Adjust ':id' and parameter type if your PK is different (e.g., Long for auto-generated).
    // If there's truly only ONE row always, you might not even need 'WHERE id = :id'.
    @Query("UPDATE user_progress SET totalChecksCount = :newCount WHERE id = :id")
    suspend fun updateTotalChecksCount(id: String, newCount: Int)

    // Ensure your UserProgress entity defines a primary key.
    // If UserProgress has a known fixed ID (e.g. UserProgress.DEFAULT_ID), use that.
    // If it's auto-generated, you'd need to fetch it first to know its ID for targeted updates.
    // This query resets all rows if there's no WHERE clause.
    // If only one row exists, this is fine.
    @Query("UPDATE user_progress SET firstCheckTimestamp = NULL, totalChecksCount = 0, lastTenCheckNotificationCount = 0, sevenDayNotificationSent = 0, fiftyStreakAchieved = 0, fiftyStreakFeeling = NULL")
    suspend fun resetProgress()
}
