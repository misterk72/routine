package com.healthtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import com.healthtracker.data.SampleDataProvider
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class HealthTrackerApp : Application(), WorkConfiguration.Provider {
    
    companion object {
        // Create a channel ID for notifications
        const val CHANNEL_ID = "health_tracker_channel"
    }
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var sampleDataProvider: SampleDataProvider
    
    override fun onCreate() {
        super.onCreate()
        
        // Force French locale for the entire application
        setLocale()
        
        // Create notification channel for Android O and above
        createNotificationChannel()
        
        // Set a StrictMode policy to work around the PendingIntent issue
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val policy = StrictMode.VmPolicy.Builder()
                .detectUnsafeIntentLaunch()
                .penaltyLog()
                .build()
            StrictMode.setVmPolicy(policy)
        }
        
        // Insert sample data for testing
        sampleDataProvider.insertSampleDataIfNeeded()
    }
    
    private fun setLocale() {
        val locale = Locale.FRENCH
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.notifications_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
