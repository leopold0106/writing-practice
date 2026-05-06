package com.example.writingpractice.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val dailyGoal: Flow<Int> = dataStore.data.map { it[Keys.DAILY_GOAL] ?: 5 }
    val notificationEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.NOTIF_ENABLED] ?: true }
    val notificationHour: Flow<Int> = dataStore.data.map { it[Keys.NOTIF_HOUR] ?: 9 }
    val notificationMinute: Flow<Int> = dataStore.data.map { it[Keys.NOTIF_MINUTE] ?: 0 }
    val apiKey: Flow<String> = dataStore.data.map { it[Keys.API_KEY] ?: "" }
    val apiKeyValidated: Flow<Boolean?> = dataStore.data.map { it[Keys.API_KEY_VALIDATED] }

    suspend fun getApiKey(): String = dataStore.data.first()[Keys.API_KEY] ?: ""
    suspend fun isDbSeeded(): Boolean = dataStore.data.first()[Keys.DB_SEEDED] ?: false

    suspend fun setDbSeeded(value: Boolean) {
        dataStore.edit { it[Keys.DB_SEEDED] = value }
    }

    suspend fun setDailyGoal(goal: Int) {
        dataStore.edit { it[Keys.DAILY_GOAL] = goal }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIF_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.NOTIF_HOUR] = hour
            it[Keys.NOTIF_MINUTE] = minute
        }
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun setApiKeyValidated(valid: Boolean) {
        dataStore.edit { it[Keys.API_KEY_VALIDATED] = valid }
    }

    suspend fun getLastAutoAnalyzedCount(): Int =
        dataStore.data.first()[Keys.LAST_AUTO_ANALYZED_COUNT] ?: 0

    suspend fun setLastAutoAnalyzedCount(count: Int) {
        dataStore.edit { it[Keys.LAST_AUTO_ANALYZED_COUNT] = count }
    }

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_HOUR = intPreferencesKey("notif_hour")
        val NOTIF_MINUTE = intPreferencesKey("notif_minute")
        val API_KEY = stringPreferencesKey("api_key")
        val DB_SEEDED = booleanPreferencesKey("db_seeded")
        val API_KEY_VALIDATED = booleanPreferencesKey("api_key_validated")
        val LAST_AUTO_ANALYZED_COUNT = intPreferencesKey("last_auto_analyzed_count")
    }
}
