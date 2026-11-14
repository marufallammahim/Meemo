package com.marufallammahim.meemo.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.model.AppThemeMode
import com.marufallammahim.meemo.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.meemoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meemo_user_preferences"
)

@Singleton
class MeemoPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val userPreferencesFlow: Flow<UserPreferences> = context.meemoDataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            themeMode = AppThemeMode.fromName(prefs[KEY_THEME_MODE]),
            languageMode = AppLanguageMode.fromName(prefs[KEY_LANGUAGE_MODE]),
            ttsEnabled = prefs[KEY_TTS_ENABLED] ?: true,
            ttsSpeechRate = prefs[KEY_TTS_RATE] ?: 1.0f,
            saveConversationHistory = prefs[KEY_SAVE_HISTORY] ?: true,
            wakeWordEnabled = prefs[KEY_WAKE_WORD_ENABLED] ?: false,
            notificationReadAloudEnabled = prefs[KEY_NOTIF_READ_ALOUD] ?: false,
            notificationAnnounceSenderOnly = prefs[KEY_NOTIF_SENDER_ONLY] ?: false,
            allowedNotificationPackages = prefs[KEY_ALLOWED_NOTIF_PACKAGES] ?: DEFAULT_MESSAGING_PACKAGES,
            quietHoursEnabled = prefs[KEY_QUIET_HOURS_ENABLED] ?: false,
            quietHoursStartHour = prefs[KEY_QUIET_START] ?: 23,
            quietHoursEndHour = prefs[KEY_QUIET_END] ?: 7,
            aiFallbackEnabled = prefs[KEY_AI_FALLBACK_ENABLED] ?: false,
            aiEndpointUrl = prefs[KEY_AI_ENDPOINT] ?: "https://api.openai.com/v1/chat/completions",
            aiModelName = prefs[KEY_AI_MODEL] ?: "gpt-4o-mini",
            rootFeaturesEnabled = prefs[KEY_ROOT_ENABLED] ?: false,
            preferredBrowserPackage = prefs[KEY_PREFERRED_BROWSER]
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.meemoDataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.meemoDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setLanguageMode(mode: AppLanguageMode) {
        context.meemoDataStore.edit { it[KEY_LANGUAGE_MODE] = mode.name }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.meemoDataStore.edit { it[KEY_TTS_ENABLED] = enabled }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.meemoDataStore.edit { it[KEY_TTS_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setSaveConversationHistory(enabled: Boolean) {
        context.meemoDataStore.edit { it[KEY_SAVE_HISTORY] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.meemoDataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setNotificationReadAloud(enabled: Boolean) {
        context.meemoDataStore.edit { it[KEY_NOTIF_READ_ALOUD] = enabled }
    }

    suspend fun setNotificationAnnounceSenderOnly(senderOnly: Boolean) {
        context.meemoDataStore.edit { it[KEY_NOTIF_SENDER_ONLY] = senderOnly }
    }

    suspend fun setAllowedNotificationPackages(packages: Set<String>) {
        context.meemoDataStore.edit { it[KEY_ALLOWED_NOTIF_PACKAGES] = packages }
    }

    suspend fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        context.meemoDataStore.edit {
            it[KEY_QUIET_HOURS_ENABLED] = enabled
            it[KEY_QUIET_START] = startHour.coerceIn(0, 23)
            it[KEY_QUIET_END] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setAiConfiguration(enabled: Boolean, endpointUrl: String, modelName: String) {
        context.meemoDataStore.edit {
            it[KEY_AI_FALLBACK_ENABLED] = enabled
            it[KEY_AI_ENDPOINT] = endpointUrl.trim()
            it[KEY_AI_MODEL] = modelName.trim()
        }
    }

    suspend fun setRootFeaturesEnabled(enabled: Boolean) {
        context.meemoDataStore.edit { it[KEY_ROOT_ENABLED] = enabled }
    }

    suspend fun setPreferredBrowserPackage(packageName: String?) {
        context.meemoDataStore.edit {
            if (packageName.isNullOrBlank()) {
                it.remove(KEY_PREFERRED_BROWSER)
            } else {
                it[KEY_PREFERRED_BROWSER] = packageName
            }
        }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE_MODE = stringPreferencesKey("language_mode")
        private val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val KEY_TTS_RATE = floatPreferencesKey("tts_rate")
        private val KEY_SAVE_HISTORY = booleanPreferencesKey("save_conversation_history")
        private val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        private val KEY_NOTIF_READ_ALOUD = booleanPreferencesKey("notif_read_aloud")
        private val KEY_NOTIF_SENDER_ONLY = booleanPreferencesKey("notif_sender_only")
        private val KEY_ALLOWED_NOTIF_PACKAGES = stringSetPreferencesKey("allowed_notif_packages")
        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_START = intPreferencesKey("quiet_hours_start")
        private val KEY_QUIET_END = intPreferencesKey("quiet_hours_end")
        private val KEY_AI_FALLBACK_ENABLED = booleanPreferencesKey("ai_fallback_enabled")
        private val KEY_AI_ENDPOINT = stringPreferencesKey("ai_endpoint_url")
        private val KEY_AI_MODEL = stringPreferencesKey("ai_model_name")
        private val KEY_ROOT_ENABLED = booleanPreferencesKey("root_features_enabled")
        private val KEY_PREFERRED_BROWSER = stringPreferencesKey("preferred_browser_package")

        val DEFAULT_MESSAGING_PACKAGES = setOf(
            "com.whatsapp",
            "com.facebook.orca",
            "com.instagram.android",
            "com.google.android.apps.messaging",
            "org.telegram.messenger"
        )
    }
}
