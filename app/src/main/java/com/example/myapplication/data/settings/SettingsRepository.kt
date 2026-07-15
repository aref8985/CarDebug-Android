package com.example.myapplication.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme { LIGHT, DARK, SYSTEM }
enum class AppLanguage { EN, FA }
enum class UnitSystem { METRIC, IMPERIAL }

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val UNITS = stringPreferencesKey("units")
        val RPM_THRESHOLD = intPreferencesKey("rpm_threshold")
        val TEMP_THRESHOLD = intPreferencesKey("temp_threshold")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        AppTheme.valueOf(preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name)
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        AppLanguage.valueOf(preferences[PreferencesKeys.LANGUAGE] ?: AppLanguage.EN.name)
    }

    val unitsFlow: Flow<UnitSystem> = context.dataStore.data.map { preferences ->
        UnitSystem.valueOf(preferences[PreferencesKeys.UNITS] ?: UnitSystem.METRIC.name)
    }

    val rpmThresholdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.RPM_THRESHOLD] ?: 6000
    }

    val tempThresholdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TEMP_THRESHOLD] ?: 105
    }

    val autoConnectFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_CONNECT] ?: false
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language.name
        }
    }

    suspend fun updateUnits(units: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNITS] = units.name
        }
    }

    suspend fun updateRpmThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RPM_THRESHOLD] = threshold
        }
    }

    suspend fun updateTempThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEMP_THRESHOLD] = threshold
        }
    }

    suspend fun updateAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CONNECT] = enabled
        }
    }
}
