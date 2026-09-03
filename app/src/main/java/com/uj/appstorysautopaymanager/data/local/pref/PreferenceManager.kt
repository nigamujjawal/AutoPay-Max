package com.uj.appstorysautopaymanager.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_pref")

@Singleton
class PreferenceManager @Inject constructor(
    private val context: Context
) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val CURRENCY_CODE_KEY = stringPreferencesKey("currency_code")
        val PIN_CODE_KEY = stringPreferencesKey("pin_code")
        val IS_BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("is_biometric_enabled")
        val IS_VOICE_ALERTS_ENABLED_KEY = booleanPreferencesKey("is_voice_alerts_enabled")
        val SPEECH_SPEED_KEY = floatPreferencesKey("speech_speed")
        val SPEECH_LANGUAGE_KEY = stringPreferencesKey("speech_language")
        val DEFAULT_REMINDER_DAYS_KEY = intPreferencesKey("default_reminder_days")
        val IS_ONBOARDED_KEY = booleanPreferencesKey("is_onboarded")
        val VOICE_ENGINE_KEY = stringPreferencesKey("voice_engine")
        val VOICE_VOLUME_KEY = floatPreferencesKey("voice_volume")
        val PLAY_CHIME_FIRST_KEY = booleanPreferencesKey("play_chime_first")
        val ALERT_TONE_KEY = stringPreferencesKey("alert_tone")
        val IS_PUSH_NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("is_push_notifications_enabled")
        val IS_AUTOPAY_REMINDERS_ENABLED_KEY = booleanPreferencesKey("is_autopay_reminders_enabled")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "Light" }
    val currencyFlow: Flow<String> = context.dataStore.data.map { it[CURRENCY_KEY] ?: "₹" }
    // ISO 4217 code, e.g. for PaymentSyncWorker's backend `currency` field - currencyFlow's
    // symbol alone is ambiguous ("$" is US/Australia/Mexico), this isn't.
    val currencyCodeFlow: Flow<String> = context.dataStore.data.map { it[CURRENCY_CODE_KEY] ?: "INR" }
    val pinCodeFlow: Flow<String> = context.dataStore.data.map { it[PIN_CODE_KEY] ?: "" }
    val isBiometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_BIOMETRIC_ENABLED_KEY] ?: false }
    val isVoiceAlertsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_VOICE_ALERTS_ENABLED_KEY] ?: true }
    val speechSpeedFlow: Flow<Float> = context.dataStore.data.map { it[SPEECH_SPEED_KEY] ?: 1.0f }
    val speechLanguageFlow: Flow<String> = context.dataStore.data.map { it[SPEECH_LANGUAGE_KEY] ?: "en" }
    val defaultReminderDaysFlow: Flow<Int> = context.dataStore.data.map { it[DEFAULT_REMINDER_DAYS_KEY] ?: 1 }
    val isOnboardedFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_ONBOARDED_KEY] ?: false }
    val voiceEngineFlow: Flow<String> = context.dataStore.data.map { it[VOICE_ENGINE_KEY] ?: "Male Voice" }
    val voiceVolumeFlow: Flow<Float> = context.dataStore.data.map { it[VOICE_VOLUME_KEY] ?: 100f }
    val playChimeFirstFlow: Flow<Boolean> = context.dataStore.data.map { it[PLAY_CHIME_FIRST_KEY] ?: true }
    val alertToneFlow: Flow<String> = context.dataStore.data.map { it[ALERT_TONE_KEY] ?: "Cashier" }
    val isPushNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_PUSH_NOTIFICATIONS_ENABLED_KEY] ?: true }
    val isAutopayRemindersEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_AUTOPAY_REMINDERS_ENABLED_KEY] ?: true }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[CURRENCY_KEY] = currency }
    }

    suspend fun setCurrencyCode(currencyCode: String) {
        context.dataStore.edit { it[CURRENCY_CODE_KEY] = currencyCode }
    }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { it[PIN_CODE_KEY] = pin }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun setVoiceAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_VOICE_ALERTS_ENABLED_KEY] = enabled }
    }

    suspend fun setSpeechSpeed(speed: Float) {
        context.dataStore.edit { it[SPEECH_SPEED_KEY] = speed }
    }

    suspend fun setSpeechLanguage(lang: String) {
        context.dataStore.edit { it[SPEECH_LANGUAGE_KEY] = lang }
    }

    suspend fun setDefaultReminderDays(days: Int) {
        context.dataStore.edit { it[DEFAULT_REMINDER_DAYS_KEY] = days }
    }

    suspend fun setIsOnboarded(onboarded: Boolean) {
        context.dataStore.edit { it[IS_ONBOARDED_KEY] = onboarded }
    }

    suspend fun setVoiceEngine(engine: String) {
        context.dataStore.edit { it[VOICE_ENGINE_KEY] = engine }
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.dataStore.edit { it[VOICE_VOLUME_KEY] = volume }
    }

    suspend fun setPlayChimeFirst(enabled: Boolean) {
        context.dataStore.edit { it[PLAY_CHIME_FIRST_KEY] = enabled }
    }

    suspend fun setAlertTone(tone: String) {
        context.dataStore.edit { it[ALERT_TONE_KEY] = tone }
    }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_PUSH_NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    suspend fun setAutopayRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_AUTOPAY_REMINDERS_ENABLED_KEY] = enabled }
    }
}
