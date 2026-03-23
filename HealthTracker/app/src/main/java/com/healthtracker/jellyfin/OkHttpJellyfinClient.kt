package com.healthtracker.jellyfin

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpJellyfinClient @Inject constructor() : JellyfinClient {

    private val httpClient = OkHttpClient()

    override suspend fun testServerConnection(serverUrl: String, apiKey: String) {
        val request = Request.Builder()
            .url(buildUrl(serverUrl, "System", "Info"))
            .headers(buildHeaders(apiKey))
            .get()
            .build()

        executeJson(request)
    }

    override suspend fun resolveUserId(serverUrl: String, apiKey: String, username: String): String {
        val request = Request.Builder()
            .url(buildUrl(serverUrl, "Users"))
            .headers(buildHeaders(apiKey))
            .get()
            .build()

        val payload = executeJson(request)
        val users = payload.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        val match = users.firstOrNull { element ->
            element.asJsonObject.getString("Name")?.equals(username, ignoreCase = true) == true
        }?.asJsonObject

        return match?.getString("Id")
            ?: throw IOException("Utilisateur Jellyfin introuvable: $username")
    }

    override suspend fun fetchPlayedMedia(
        serverUrl: String,
        apiKey: String,
        userId: String
    ): List<WatchedMediaDuringSession> {
        val url = buildUrl(
            serverUrl,
            "System",
            "ActivityLog",
            "Entries"
        ).newBuilder()
            .addQueryParameter("Limit", "200")
            .build()

        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders(apiKey))
            .get()
            .build()

        val payload = executeJson(request).asJsonObject
        val entries = payload.getAsJsonArray("Items") ?: JsonArray()
        val detailsByItemId = linkedMapOf<String, JsonObject?>()
        return entries.mapNotNull { entry ->
            parseActivityEntry(
                entry = entry.asJsonObject,
                expectedUserId = userId,
                serverUrl = serverUrl,
                apiKey = apiKey,
                userIdForItemLookup = userId,
                detailsByItemId = detailsByItemId
            )
        }
            .sortedByDescending { it.playedAt }
    }

    private fun parseActivityEntry(
        entry: JsonObject,
        expectedUserId: String,
        serverUrl: String,
        apiKey: String,
        userIdForItemLookup: String,
        detailsByItemId: MutableMap<String, JsonObject?>
    ): WatchedMediaDuringSession? {
        if (entry.getString("UserId") != expectedUserId) {
            return null
        }

        val activityType = entry.getString("Type") ?: return null
        if (activityType != "VideoPlayback") {
            return null
        }

        val itemId = entry.getString("ItemId") ?: return null
        val playedAt = entry.getString("Date")?.let(::parseInstant) ?: return null
        val item = detailsByItemId.getOrPut(itemId) {
            fetchItemDetails(serverUrl, apiKey, userIdForItemLookup, itemId)
        } ?: return null

        val type = item.getString("Type") ?: return null
        return when (type) {
            "Movie" -> {
                val title = item.getString("Name") ?: return null
                WatchedMediaDuringSession(
                    title = title,
                    type = JellyfinMediaType.MOVIE,
                    releaseYear = item.getInt("ProductionYear") ?: parseReleaseYear(item.getString("PremiereDate")),
                    playedAt = playedAt,
                    itemId = itemId
                )
            }
            "Episode" -> {
                val seriesName = item.getString("SeriesName") ?: item.getString("Name") ?: return null
                WatchedMediaDuringSession(
                    title = item.getString("Name") ?: seriesName,
                    type = JellyfinMediaType.EPISODE,
                    seriesName = seriesName,
                    seasonNumber = item.getInt("ParentIndexNumber"),
                    playedAt = playedAt,
                    itemId = itemId
                )
            }
            else -> null
        }
    }

    private fun fetchItemDetails(
        serverUrl: String,
        apiKey: String,
        userId: String,
        itemId: String
    ): JsonObject? {
        val url = buildUrl(
            serverUrl,
            "Users",
            userId,
            "Items",
            itemId
        ).newBuilder()
            .addQueryParameter("Fields", "SeriesName,ParentIndexNumber")
            .build()

        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders(apiKey))
            .get()
            .build()

        return runCatching { executeJson(request).asJsonObject }.getOrNull()
    }

    private fun parseReleaseYear(value: String?): Int? {
        val parsed = value?.let(::parseInstant) ?: return null
        return parsed.atZone(ZoneId.systemDefault()).year
    }

    private fun parseInstant(value: String): Instant? {
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
            }.getOrNull()
    }

    private fun buildHeaders(apiKey: String) = okhttp3.Headers.Builder()
        .add("Accept", "application/json")
        .add("X-Emby-Token", apiKey)
        .add(
            "Authorization",
            "MediaBrowser Client=\"HealthTracker\", Device=\"Android\", DeviceId=\"healthtracker-android\", Version=\"1.0\", Token=\"$apiKey\""
        )
        .build()

    private fun buildUrl(serverUrl: String, vararg segments: String): HttpUrl {
        val baseUrl = serverUrl.toHttpUrlOrNull()
            ?: throw IOException("URL Jellyfin invalide")
        val builder = baseUrl.newBuilder()
        segments.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    private fun executeJson(request: Request): JsonElement {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Erreur Jellyfin ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw IOException("Réponse Jellyfin vide")
            }
            return JsonParser.parseString(body)
        }
    }

    private fun JsonObject.getString(name: String): String? {
        val value = get(name) ?: return null
        return if (value.isJsonNull) null else value.asString
    }

    private fun JsonObject.getInt(name: String): Int? {
        val value = get(name) ?: return null
        return if (value.isJsonNull) null else value.asInt
    }
}
