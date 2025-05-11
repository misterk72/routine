package com.healthtracker.data

import com.healthtracker.data.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fournit les localisations par défaut pour l'application
 */
@Singleton
class DefaultLocationsProvider @Inject constructor(
    private val locationRepository: LocationRepository
) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Insère les localisations par défaut si nécessaire
     */
    fun insertDefaultLocationsIfNeeded() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Vérifier si des localisations existent déjà
                if (locationRepository.getLocationCount() > 0) {
                    return@launch
                }
                
                // Créer des localisations par défaut
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
                
                defaultLocations.forEach { locationRepository.insertLocation(it) }
                
            } catch (e: Exception) {
                // Log exception but don't crash the app
                android.util.Log.e("DefaultLocationsProvider", "Erreur lors de l'insertion des localisations par défaut: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
