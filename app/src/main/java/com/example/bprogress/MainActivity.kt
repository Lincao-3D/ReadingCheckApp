package com.example.bprogress

// AndroidX imports
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
// WorkManager is no longer directly used in MainActivity for scheduling this specific worker
// import androidx.work.ExistingWorkPolicy
// import androidx.work.OneTimeWorkRequestBuilder
// import androidx.work.WorkManager
import com.example.bprogress.databinding.ActivityMainBinding
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest // Ensure Manifest is imported
import androidx.activity.result.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ActivitiesViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission GRANTED after request.")
                // Optional: If it was the first launch and permission was just granted,
                // you could potentially trigger the debug notification logic here if it didn't run.
                // However, the App class's logic should handle it on the next app start
                // if it missed the first window due to missing permissions.
                // For simplicity, we'll rely on the App class.
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission DENIED after request.")
                // TODO: Consider showing a dialog explaining why notifications are important
                // for daily reminders and how the user can enable them in app settings.
                // For example:
                // showPermissionDeniedDialog()
            }
        }

    private val localeChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == App.ACTION_LOCALE_CHANGED) {
                Log.i("MainActivity", "Locale changed broadcast received. Recreating activity.")
                recreate()
            }
        }
    }

    // BaseActivity is defined within MainActivity, which is unusual.
    // Usually, BaseActivity would be a separate file if other activities inherit from it.
    // If only MainActivity uses this logic, `attachBaseContext` can be directly in MainActivity.
    // For now, keeping your structure.
    abstract class BaseActivity : AppCompatActivity() {
        override fun attachBaseContext(newBase: Context) {
            super.attachBaseContext(App.wrapContextWithLocale(newBase))
        }
    }
    // If MainActivity should use the attachBaseContext logic, it needs to inherit from this BaseActivity,
    // or this method should be moved directly into MainActivity.
    // Let's assume MainActivity should be applying this:
    // **Correction**: BaseActivity should be a superclass, or its logic moved.
    // For now, I'll move `attachBaseContext` directly into MainActivity.

    override fun attachBaseContext(newBase: Context) {
        // Apply the saved locale before the Activity's context is fully initialized
        super.attachBaseContext(App.wrapContextWithLocale(newBase))
        Log.d("MainActivity", "attachBaseContext called, locale wrapped.")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started.")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Request notification permission
        checkAndRequestNotificationPermission()

        // The initial worker scheduling is now handled by App.kt
        // if (savedInstanceState == null) {
        //    scheduleInitialFiveMinuteWorker() // REMOVED
        // }

        val intentFilter = IntentFilter(App.ACTION_LOCALE_CHANGED)
        LocalBroadcastManager.getInstance(this).registerReceiver(localeChangeReceiver, intentFilter)
        Log.d("MainActivity", "LocaleChangeReceiver registered. ViewModel instance: $viewModel")
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localeChangeReceiver)
        Log.d("MainActivity", "LocaleChangeReceiver unregistered.")
    }

    private fun checkAndRequestNotificationPermission() {
        // Check if the Android version is Tiramisu (API 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS // Use Manifest.permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You have permission
                    Log.i("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why you need the permission.
                    // Show a dialog or a snackbar.
                    Log.i("MainActivity", "Showing rationale for POST_NOTIFICATIONS permission.")
                    // TODO: Show a user-friendly dialog explaining why the permission is needed,
                    // then launch the permission request.
                    // For example:
                    // showRationaleDialog {
                    //    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // }
                    // For now, directly launching:
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly ask for the permission.
                    Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission directly.")
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Notification permission is not required or handled differently on older versions
            Log.d("MainActivity", "Notification permission not runtime-managed for SDK < 33.")
        }
    }

    // Removed scheduleInitialFiveMinuteWorker() as its logic is now in App.kt
    // private fun scheduleInitialFiveMinuteWorker() { ... }
}
