// --- app/src/main/java/com/example/bprogress/SettingsViewModel.kt ---
package com.example.bprogress

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel // Changed from AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel // Marks this ViewModel for Hilt injection
class SettingsViewModel @Inject constructor( // Constructor injection
    private val settingsRepository: SettingsRepository // Inject Repository
) : ViewModel() {

    // Data obtained from the injected repository
    val userProgress: LiveData<UserProgress?> = settingsRepository.getUserProgress()

    init {
        Log.d("SettingsViewModel", "SettingsViewModel initialized with Hilt.")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("SettingsViewModel", "SettingsViewModel cleared.")
    }
}
