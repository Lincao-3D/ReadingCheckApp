package com.example.bprogress
import androidx.room.Database
import androidx.room.RoomDatabase
// These imports are fine if ActivityItem and UserProgress are in the same package
import com.example.bprogress.ActivityItem
import com.example.bprogress.UserProgress

@Database(
    entities = [ActivityItem::class, UserProgress::class],
    version = 1,
    exportSchema = false // For development, this is okay. For production, set to true.
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun userProgressDao(): UserProgressDao
}