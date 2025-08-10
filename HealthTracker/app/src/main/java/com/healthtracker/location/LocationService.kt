package com.healthtracker.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import android.annotation.SuppressLint
import com.google.android.gms.location.*
import com.healthtracker.data.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    private val context: Context,
    private val locationRepository: LocationRepository
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    private val locationRequest = LocationRequest.create().apply {
        interval = 10000 // Intervalle de mise à jour (10 secondes)
        fastestInterval = 5000 // Intervalle le plus rapide (5 secondes)
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    
    private var locationCallback: LocationCallback? = null
    
    // Vérifie si les permissions de localisation sont accordées
    fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Démarre les mises à jour de localisation
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationDetected: (com.healthtracker.data.Location?) -> Unit) {
        if (!hasLocationPermissions()) {
            onLocationDetected(null)
            return
        }
        
        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        processLocation(location, onLocationDetected)
                    }
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            onLocationDetected(null)
        }
    }
    
    // Arrête les mises à jour de localisation
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
    
    // Récupère la dernière localisation connue
    @SuppressLint("MissingPermission")
    fun getLastLocation(onLocationDetected: (com.healthtracker.data.Location?) -> Unit) {
        if (!hasLocationPermissions()) {
            onLocationDetected(null)
            return
        }
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        processLocation(location, onLocationDetected)
                    } else {
                        onLocationDetected(null)
                    }
                }
                .addOnFailureListener {
                    onLocationDetected(null)
                }
        } catch (e: SecurityException) {
            onLocationDetected(null)
        }
    }
    
    // Traite la localisation et trouve la localisation connue la plus proche
    private fun processLocation(
        location: Location,
        onLocationDetected: (com.healthtracker.data.Location?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val nearbyLocations = withContext(Dispatchers.IO) {
                locationRepository.getNearbyLocations(
                    location.latitude,
                    location.longitude
                )
            }
            
            // Trouve la localisation connue la plus proche
            val closestLocation = nearbyLocations.minByOrNull { knownLocation ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    knownLocation.latitude, knownLocation.longitude,
                    results
                )
                results[0]
            }
            
            // Vérifie si la localisation est dans le rayon défini
            if (closestLocation != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    closestLocation.latitude, closestLocation.longitude,
                    results
                )
                
                if (results[0] <= closestLocation.radius) {
                    onLocationDetected(closestLocation)
                } else {
                    onLocationDetected(null)
                }
            } else {
                onLocationDetected(null)
            }
        }
    }
}
