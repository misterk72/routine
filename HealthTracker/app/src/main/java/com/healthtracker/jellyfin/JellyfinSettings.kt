package com.healthtracker.jellyfin

interface JellyfinSettings {
    fun getServerUrl(): String?
    fun setServerUrl(value: String?)
    fun getUsername(): String?
    fun setUsername(value: String?)
    fun getApiKey(): String?
    fun setApiKey(value: String?)
    fun isConfigured(): Boolean
}
