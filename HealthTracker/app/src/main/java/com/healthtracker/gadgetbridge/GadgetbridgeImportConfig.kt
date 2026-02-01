package com.healthtracker.gadgetbridge

import android.content.Context
import android.net.Uri

class GadgetbridgeImportConfig(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getExportUri(): Uri? = prefs.getString(KEY_EXPORT_URI, null)?.let(Uri::parse)

    fun setExportUri(uri: Uri) {
        prefs.edit().putString(KEY_EXPORT_URI, uri.toString()).apply()
    }

    fun getDeviceMatch(): String? = prefs.getString(KEY_DEVICE_MATCH, null)

    fun setDeviceMatch(value: String?) {
        val sanitized = value?.trim().orEmpty()
        if (sanitized.isEmpty()) {
            prefs.edit().remove(KEY_DEVICE_MATCH).apply()
        } else {
            prefs.edit().putString(KEY_DEVICE_MATCH, sanitized).apply()
        }
    }

    fun getLastImportedStartTime(): Long = prefs.getLong(KEY_LAST_START_TIME, 0L)

    fun setLastImportedStartTime(value: Long) {
        prefs.edit().putLong(KEY_LAST_START_TIME, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "gadgetbridge_import"
        private const val KEY_EXPORT_URI = "export_uri"
        private const val KEY_DEVICE_MATCH = "device_match"
        private const val KEY_LAST_START_TIME = "last_start_time"
    }
}
