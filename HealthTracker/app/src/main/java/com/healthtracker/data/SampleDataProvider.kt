package com.healthtracker.data

import com.healthtracker.data.repository.HealthEntryRepository
import com.healthtracker.data.repository.LocationRepository
import com.healthtracker.data.repository.MetricRepository
import com.healthtracker.data.repository.MetricTypeRepository
import com.healthtracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides sample data for testing and demonstration purposes
 */
@Singleton
class SampleDataProvider @Inject constructor(
    private val healthEntryRepository: HealthEntryRepository,
    private val metricRepository: MetricRepository,
    private val metricTypeRepository: MetricTypeRepository,
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository
) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Insert sample data if the database is empty
     */
    fun insertSampleDataIfNeeded() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
            // Check if we already have entries
            if (healthEntryRepository.getEntryCount() > 0) {
                return@launch
            }
            
            // 1. Créer un utilisateur par défaut
            val defaultUser = User(
                name = "Utilisateur par défaut",
                isDefault = true
            )
            val defaultUserId = userRepository.insertUser(defaultUser)
            
            // 2. Créer des localisations par défaut
            val defaultLocations = listOf(
                Location(
                    name = "Domène",
                    latitude = 45.2028,
                    longitude = 5.8417,
                    radius = 100f,
                    isDefault = true
                ),
                Location(
                    name = "Avon",
                    latitude = 48.4167,
                    longitude = 2.7333,
                    radius = 100f,
                    isDefault = false
                ),
                Location(
                    name = "La Roche-de-Glun",
                    latitude = 45.0167,
                    longitude = 4.8333,
                    radius = 100f,
                    isDefault = false
                )
            )
            
            val locationIds = defaultLocations.map { locationRepository.insertLocation(it) }
            
            // 3. Create sample metric types
            val metricTypes = listOf(
                MetricType(name = "Blood Pressure", unit = "mmHg", 
                          description = "Systolic blood pressure", 
                          minValue = 90.0, maxValue = 200.0, stepSize = 1.0),
                MetricType(name = "Heart Rate", unit = "bpm", 
                          description = "Resting heart rate", 
                          minValue = 40.0, maxValue = 200.0, stepSize = 1.0),
                MetricType(name = "Sleep", unit = "hours", 
                          description = "Sleep duration", 
                          minValue = 0.0, maxValue = 24.0, stepSize = 0.25),
                MetricType(name = "Mood", unit = "scale 1-5", 
                          description = "Subjective mood rating", 
                          minValue = 1.0, maxValue = 5.0, stepSize = 1.0),
                MetricType(name = "Steps", unit = "count", 
                          description = "Daily step count", 
                          minValue = 0.0, maxValue = 50000.0, stepSize = 10.0),
                MetricType(name = "Body Fat", unit = "%", 
                          description = "Body fat percentage", 
                          minValue = 5.0, maxValue = 50.0, stepSize = 0.1)
            )
            
            metricTypes.forEach { metricTypeRepository.insertMetricType(it) }
            
            // Create sample entries for the past week
            val now = LocalDateTime.now()
            
            val entries = (0..6).map { daysAgo ->
                val date = now.minusDays(daysAgo.toLong())
                HealthEntry(
                    userId = defaultUserId,
                    timestamp = date,
                    weight = 70f + (Math.random() * 2 - 1).toFloat(),
                    waistMeasurement = 80f + (Math.random() * 2 - 1).toFloat(),
                    bodyFat = 15f + (Math.random() * 5 - 2.5).toFloat(),
                    notes = if (daysAgo % 3 == 0) "Je me sens bien aujourd'hui" else null,
                    locationId = if (daysAgo % 3 == 0) locationIds[0] else if (daysAgo % 3 == 1) locationIds[1] else locationIds[2]
                )
            }
            
            // Insert entries and their metrics
            entries.forEach { entry ->
                val entryId = healthEntryRepository.insertEntry(entry)
                
                // Add some random metrics for each entry
                val metricValues = mutableListOf<MetricValue>()
                
                // Blood pressure (for every entry)
                metricValues.add(
                    MetricValue(
                        entryId = entryId,
                        metricType = "Blood Pressure",
                        value = 120.0 + (Math.random() * 20 - 10),
                        unit = "mmHg"
                    )
                )
                
                // Heart rate (for every entry)
                metricValues.add(
                    MetricValue(
                        entryId = entryId,
                        metricType = "Heart Rate",
                        value = 65.0 + (Math.random() * 10 - 5),
                        unit = "bpm"
                    )
                )
                
                // Sleep (for some entries)
                if (Math.random() > 0.3) {
                    metricValues.add(
                        MetricValue(
                            entryId = entryId,
                            metricType = "Sleep",
                            value = 7.0 + (Math.random() * 2 - 1),
                            unit = "hours"
                        )
                    )
                }
                
                // Mood (for some entries)
                if (Math.random() > 0.4) {
                    metricValues.add(
                        MetricValue(
                            entryId = entryId,
                            metricType = "Mood",
                            value = (1 + (Math.random() * 4).toInt()).toDouble(),
                            unit = "scale 1-5"
                        )
                    )
                }
                
                // Steps (for some entries)
                if (Math.random() > 0.5) {
                    metricValues.add(
                        MetricValue(
                            entryId = entryId,
                            metricType = "Steps",
                            value = 5000.0 + (Math.random() * 5000),
                            unit = "count"
                        )
                    )
                }
                
                // Body Fat (for some entries)
                if (Math.random() > 0.4) {
                    metricValues.add(
                        MetricValue(
                            entryId = entryId,
                            metricType = "Body Fat",
                            value = 15.0 + (Math.random() * 10),
                            unit = "%"
                        )
                    )
                }
                
                metricRepository.insertMetricValues(metricValues)
            }
            } catch (e: Exception) {
                // Log exception but don't crash the app
                android.util.Log.e("SampleDataProvider", "Error inserting sample data: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
