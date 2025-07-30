// src/main/java/com/example/bprogress/Activity.kt
package com.example.bprogress

data class Activity(
    val id: String,
    val name: String,
    var isDone: Boolean = false
)
