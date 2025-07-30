package com.example.bprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    // Assuming table name from ActivityItem entity is 'activity_items'
    @Query("SELECT * FROM activity_items ORDER BY orderIndex ASC")
    fun getAllActivitiesFlow(): Flow<List<ActivityItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllActivities(activities: List<ActivityItem>)

    @Update
    suspend fun updateActivity(activity: ActivityItem)

    @Query("DELETE FROM activity_items")
    suspend fun deleteAllActivities()

    @Query("SELECT COUNT(id) FROM activity_items")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM activity_items WHERE isChecked = 0 ORDER BY orderIndex ASC")
    suspend fun getUncheckedActivitiesList(): List<ActivityItem>

    @Query("UPDATE activity_items SET isChecked = :isChecked, lastCheckedDate = CASE WHEN :isChecked = 1 THEN :timestamp ELSE lastCheckedDate END WHERE id = :id")
    suspend fun updateCheckedStatus(id: String, isChecked: Boolean, timestamp: Long?) // Added timestamp

    // New DAO method to get the most recently checked activity
    @Query("SELECT * FROM activity_items WHERE isChecked = 1 AND lastCheckedDate IS NOT NULL ORDER BY lastCheckedDate DESC LIMIT 1")
    suspend fun getMostRecentlyCheckedActivity(): ActivityItem?

    // If you want any activity that was checked, regardless of its current checked status
    // (e.g. user checked then unchecked it, but it was the last interaction)
    // you might need a different timestamp strategy or a more complex query.
    // For now, "most recently that IS CURRENTLY checked" is simpler.
}
