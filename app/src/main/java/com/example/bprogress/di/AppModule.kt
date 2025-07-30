package com.example.bprogress.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.example.bprogress.App
import com.example.bprogress.AppDatabase
// BuildConfig will be used by NetworkModule, not directly here for network parts
import com.example.bprogress.R
import com.example.bprogress.ActivityDao
import com.example.bprogress.UserProgressDao
// OpenAiApiService is now provided by NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
// OkHttp, Retrofit, HttpLoggingInterceptor providers are moved
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "bprogress_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideActivityDao(appDatabase: AppDatabase): ActivityDao {
        return appDatabase.activityDao()
    }

    @Provides
    @Singleton
    fun provideUserProgressDao(appDatabase: AppDatabase): UserProgressDao {
        return appDatabase.userProgressDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext appContext: Context): SharedPreferences {
        // Using App.PREFS_NAME ensures consistency if you defined it in App.kt
        return appContext.getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE)
    }
    @Provides
    @Named("DailyReminderChannelID")
    fun provideNotificationChannelId(): String = App.DAILY_REMINDER_CHANNEL_ID

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext appContext: Context): NotificationManager {
        return appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideDailyReminderNotificationChannel( // Renamed for clarity, good practice!
        @ApplicationContext appContext: Context,
        notificationManager: NotificationManager,
        @Named("DailyReminderChannelID") channelId: String
    ): NotificationChannel { // <--- CORRECT: Explicitly returns NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val name = appContext.getString(R.string.notification_channel_daily_reminder_name)
                val descriptionText = appContext.getString(R.string.notification_channel_daily_reminder_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = descriptionText
                }
                notificationManager.createNotificationChannel(channel)
                Log.i("AppModule", "Notification channel '$channelId' created.")
                return channel // <--- CORRECT: Returns the created channel
            } else {
                Log.d("AppModule", "Notification channel '$channelId' already exists.")
                return existingChannel // <--- CORRECT: Returns the existing channel
            }
        } else {
            // This part needs careful consideration for dependency injection.
            // If this provider is injected somewhere that might run on pre-O devices,
            // throwing an exception here will crash the app during Dagger's graph construction
            // or when the dependency is first requested.
            // A better approach for pre-O might be to return a "no-op" or placeholder if
            // NotificationChannel is strictly required as a type, or make the injection
            // of NotificationChannel itself conditional or optional where it's used.
            // However, for the specific purpose of *creating* a channel, this check is fine,
            // but the dependency graph needs to handle it.
            // A common pattern is that code using NotificationChannel directly is also guarded
            // by SDK_INT checks.
            //
            // If a NotificationChannel instance *must* be provided for Dagger to satisfy
            // the graph even on pre-O (where it's not used by the system),
            // you might return a dummy/placeholder, or make this provider more complex.
            // But since NotificationChannel itself is an O+ concept, it's often better
            // to ensure that anything *injecting* a NotificationChannel is only doing so
            // on O+ devices or handles its absence gracefully.
            //
            // For now, let's assume consumers will handle the SDK version.
            // If a NotificationChannel is *always* expected to be injectable,
            // this else branch is problematic for Dagger's graph on pre-O.
            // A typical way to handle this is that this provider isn't actually
            // *called* or its result *used* on pre-O for system channel creation.
            // If you need to inject *something* of type NotificationChannel on pre-O,
            // you'd need a different strategy.
            //
            // Given that NotificationChannel itself is an O+ API, it's likely that
            // anything injecting this NotificationChannel will also have SDK_INT >= O checks.
            // So, if this provider is only ever resolved on O+ devices, the throw is okay
            // as a safeguard, though ideally Dagger wouldn't even try to resolve it pre-O
            // if its consumers are version-aware.
            //
            // A safer way if NotificationChannel is injected unconditionally:
            // Log a warning and return a dummy object, though this is a bit of a hack.
            // Or, better, don't inject NotificationChannel directly if it might not be valid.
            // Instead, inject the NotificationManager and channel ID, and let the consumer
            // create the channel if needed.
            //
            // For simplicity, sticking to your provided logic:
            Log.w("AppModule", "Notification channels are only for Android O and above. This provider should ideally not be called on older versions if injecting NotificationChannel.")
            // To satisfy Dagger's need for a return type *if* it attempts to call this pre-O:
            // You can't return null if the return type is non-nullable NotificationChannel.
            // This is a design challenge with providing O+ specific APIs via Dagger unconditionally.
            //
            // OPTION 1: Stick with the throw, assuming consumers are O+ aware.
            throw IllegalStateException("Notification channels require Android O+. Dagger should not resolve this on older versions if NotificationChannel is injected.")

            // OPTION 2: If NotificationChannel MUST be injectable even pre-O (e.g. for a common interface),
            // this is tricky because the class itself might not exist or behave well.
            // However, since NotificationChannel IS an O class, let's assume the graph
            // is constructed such that this isn't an issue. The throw is an assertion.
            // throw IllegalStateException("Notification channels can only be created on Android O and above. This provider was called inappropriately.")
        }
    }


    // --- Network related providers (OkHttp, Retrofit, ApiService, Interceptors) are MOVED to NetworkModule.kt ---
}