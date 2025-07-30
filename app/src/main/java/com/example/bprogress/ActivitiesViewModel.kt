package com.example.bprogress

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val repository: ActivitiesRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val allActivities: LiveData<List<ActivityItem>> = repository.allActivities.asLiveData()
    val userProgress: LiveData<UserProgress?> = repository.userProgress.asLiveData()

    // Private MutableLiveData that the ViewModel can change
    private val _showStreakDialogEvent = MutableLiveData<Event<Int>>()
    // Public LiveData that the Fragment observes (immutable from the Fragment's perspective)
    val showStreakDialogEvent: LiveData<Event<Int>> get() = _showStreakDialogEvent

    private var lastProcessedMilestoneForDialogInSession = -1 // ViewModel's session-based debounce

    companion object {
        const val MILESTONE_INTERVAL = 3 // Or make it configurable
    }

    // Observer for UserProgress changes to detect milestones.
    private val userProgressObserver = Observer<UserProgress?> { currentProgress ->
        currentProgress?.let { progress ->
            Log.d("ViewModelProgress", "Observed: TotalChecks=${progress.totalChecksCount}, LastDialogShownAt=${progress.lastStreakDialogShownAtCount}")

            val currentChecks = progress.totalChecksCount
            val lastDialogShownForThisCountInDb = progress.lastStreakDialogShownAtCount

            if (currentChecks > 0 && currentChecks % MILESTONE_INTERVAL == 0 && currentChecks > lastDialogShownForThisCountInDb) {
                if (currentChecks != lastProcessedMilestoneForDialogInSession) {
                    Log.i("ViewModelProgress", "New milestone $currentChecks for dialog. Last processed in session: $lastProcessedMilestoneForDialogInSession.")
                    _showStreakDialogEvent.value = Event(currentChecks) // Post event to private LiveData
                    lastProcessedMilestoneForDialogInSession = currentChecks
                } else {
                    Log.d("ViewModelProgress", "Milestone $currentChecks already processed for dialog in this VM session (waiting for DB update/fragment action).")
                }
            } else if (currentChecks > 0 && currentChecks % MILESTONE_INTERVAL == 0) {
                Log.d("ViewModelProgress", "Milestone $currentChecks, but dialog already handled in DB (lastStreakDialogShownAtCount: $lastDialogShownForThisCountInDb).")
            }
        }
    }

    init {
        Log.d("ActivitiesViewModel", "ViewModel initializing...")
        userProgress.observeForever(userProgressObserver) // Start observing user progress
        Log.d("ActivitiesViewModel", "userProgressObserver attached.")
    }

    fun initialDataLoad() {
        if (_isLoading.value == true) {
            Log.d("ActivitiesViewModel", "Initial data load already in progress.")
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d("ActivitiesViewModel", "Ensuring initial data is loaded via repository.")
                repository.ensureInitialDataLoaded()
                Log.i("ActivitiesViewModel", "Repository initial data load process finished.")
            } catch (e: Exception) {
                Log.e("ActivitiesViewModel", "Error during initialDataLoad", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCheckedStatus(item: ActivityItem) {
        viewModelScope.launch {
            try {
                repository.toggleCheckedStatus(item)
            } catch (e: Exception) {
                Log.e("ActivitiesViewModel", "Error toggling checked status for item: ${item.id}", e)
            }
        }
    }

    fun toggleImportantStatus(item: ActivityItem) {
        viewModelScope.launch {
            try {
                repository.toggleImportantStatus(item)
            } catch (e: Exception) {
                Log.e("ActivitiesViewModel", "Error toggling important status for item: ${item.id}", e)
            }
        }
    }

    fun saveUserFeelingForMilestone(feeling: String, totalChecksAtMilestone: Int) {
        Log.d("ViewModelProgress", "ViewModel.saveUserFeelingForMilestone called. Feeling: '$feeling', totalChecksAtMilestone: $totalChecksAtMilestone. Calling repository...")
        viewModelScope.launch {
            try {
                // --->>> THIS IS THE KEY CALL TO REPOSITORY <<<---
                repository.saveUserFeelingAndDialogState(
                    feeling = feeling,
                    totalChecksAtMilestone = totalChecksAtMilestone,
                    // ViewModel is correctly passing totalChecksAtMilestone as milestoneDialogShownAtCount
                    // This is good because the ViewModel determined a dialog *should* be shown for this count
                    milestoneDialogShownAtCount = totalChecksAtMilestone
                )
                Log.d("ActivitiesViewModel", "Repository.saveUserFeelingAndDialogState completed for milestone $totalChecksAtMilestone.") // Changed from "Saved feeling..." to "Repository call completed..."
            } catch (e: Exception) {
                Log.e("ActivitiesViewModel", "Error in repository.saveUserFeelingAndDialogState for milestone $totalChecksAtMilestone", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userProgress.removeObserver(userProgressObserver) // Clean up observer
        Log.d("ActivitiesViewModel", "ViewModel cleared and userProgressObserver removed.")
    }
}
