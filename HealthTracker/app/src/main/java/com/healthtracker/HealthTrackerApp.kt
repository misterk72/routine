package com.healthtracker

import android.app.Application

import android.content.res.Configuration
import android.os.Build
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import com.healthtracker.data.DefaultLocationsProvider
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class HealthTrackerApp : Application(), WorkConfiguration.Provider {
    
    companion object {}
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var defaultLocationsProvider: DefaultLocationsProvider
    
    override fun onCreate() {
        super.onCreate()
        
        // Force French locale for the entire application
        setLocale()
        

        
        // Set a StrictMode policy to work around the PendingIntent issue
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val policy = StrictMode.VmPolicy.Builder()
                .detectUnsafeIntentLaunch()
                .penaltyLog()
                .build()
            StrictMode.setVmPolicy(policy)
        }
        
        // Insertion uniquement des localisations par défaut
        defaultLocationsProvider.insertDefaultLocationsIfNeeded()
    }
    
    private fun setLocale() {
        val locale = Locale.FRENCH
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    

    
    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
