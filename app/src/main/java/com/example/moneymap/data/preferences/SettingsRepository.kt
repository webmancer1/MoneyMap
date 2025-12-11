package com.example.moneymap.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.SharedPreferencesMigration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val SETTINGS_DATASTORE_NAME = "user_settings"

private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
private val CURRENCY_KEY = stringPreferencesKey("currency")
private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
private val BIOMETRIC_KEY = booleanPreferencesKey("biometric_lock")

data class SettingsPreferences(
    val darkTheme: Boolean = false,
    val currency: String = "KES",
    val notificationsEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = listOf(SharedPreferencesMigration(context, SETTINGS_DATASTORE_NAME)),
        produceFile = { context.preferencesDataStoreFile(SETTINGS_DATASTORE_NAME) }
    )

    val settingsFlow: Flow<SettingsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SettingsPreferences(
                darkTheme = preferences[DARK_THEME_KEY] ?: false,
                currency = preferences[CURRENCY_KEY] ?: "KES",
                notificationsEnabled = preferences[NOTIFICATIONS_KEY] ?: true,
                biometricLockEnabled = preferences[BIOMETRIC_KEY] ?: false
            )
        }

    suspend fun updateDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = enabled
        }
    }

    suspend fun updateCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currencyCode
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    suspend fun updateBiometricLock(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_KEY] = enabled
        }
    }
}
