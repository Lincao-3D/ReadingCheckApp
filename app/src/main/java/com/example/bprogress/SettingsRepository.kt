package com.example.bprogress

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Or @ActivityRetainedScoped / @ViewModelScoped if preferred
class SettingsRepository @Inject constructor(
    private val userProgressDao: UserProgressDao
) {
    fun getUserProgress(): LiveData<UserProgress?> = userProgressDao.getUserProgressFlow().asLiveData()
    // Ensure getUserProgressFlow() is the correct method name in your UserProgressDao
}