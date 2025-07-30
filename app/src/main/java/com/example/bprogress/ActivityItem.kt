package com.example.bprogress

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_items")
data class ActivityItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val column1Data: String?,
    val column2Data: String,
    var isChecked: Boolean = false,
    var isImportant: Boolean = false,
    val orderIndex: Int, // No default, assuming it's always provided from CSV or creation logic
    val lastCheckedDate: Long? = null
)