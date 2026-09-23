package com.voicebot.alo.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.voicebot.alo.core.filter.RulesBasedSpeakingPolicy
import com.voicebot.alo.core.filter.SpeakingPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes del usuario (Fase 0: SharedPreferences).
 * Alimentan la capa 5 del filtro y la configuración del motor de voz.
 */
class AloSettings(context: Context) {

    data class Snapshot(
        val voiceEnabled: Boolean = true,
        val languageTag: String = "es-PE",
        val readGroups: Boolean = true,
        val onlyWithHeadphones: Boolean = false,
        val pauseDuringCalls: Boolean = true,
        val quietHoursStart: Int? = null,
        val quietHoursEnd: Int? = null,
        val mutedChatIds: Set<String> = emptySet(),
        val retentionDays: Int = 30,
        val maxCharsPerMessage: Int = 320,
        val vibrateInsteadOfSpeak: Boolean = false,
    )

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("alo_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _state.value = load()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun speakingPolicy(): SpeakingPolicy = with(_state.value) {
        RulesBasedSpeakingPolicy(
            voiceEnabled = voiceEnabled,
            mutedChatIds = mutedChatIds,
            readGroups = readGroups,
            onlyWithHeadphones = onlyWithHeadphones,
            pauseDuringCalls = pauseDuringCalls,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
        )
    }

    fun setVoiceEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VOICE, enabled).apply()

    fun setLanguageTag(tag: String) = prefs.edit().putString(KEY_LANGUAGE, tag).apply()

    fun setReadGroups(enabled: Boolean) = prefs.edit().putBoolean(KEY_READ_GROUPS, enabled).apply()

    fun setOnlyWithHeadphones(enabled: Boolean) = prefs.edit().putBoolean(KEY_ONLY_HEADPHONES, enabled).apply()

    fun setPauseDuringCalls(enabled: Boolean) = prefs.edit().putBoolean(KEY_PAUSE_CALLS, enabled).apply()

    fun setQuietHours(start: Int?, end: Int?) {
        prefs.edit()
            .putInt(KEY_QUIET_START, start ?: VALUE_NONE)
            .putInt(KEY_QUIET_END, end ?: VALUE_NONE)
            .apply()
    }

    fun setRetentionDays(days: Int) = prefs.edit().putInt(KEY_RETENTION, days).apply()

    fun setMuted(chatId: String, muted: Boolean) {
        val current = _state.value.mutedChatIds.toMutableSet()
        if (muted) current += chatId else current -= chatId
        prefs.edit().putStringSet(KEY_MUTED, current).apply()
    }

    fun setVibrateInsteadOfSpeak(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_VIBRATE_INSTEAD, enabled).apply()

    private fun load(): Snapshot = with(prefs) {
        Snapshot(
            voiceEnabled = getBoolean(KEY_VOICE, true),
            languageTag = getString(KEY_LANGUAGE, "es-PE") ?: "es-PE",
            readGroups = getBoolean(KEY_READ_GROUPS, true),
            onlyWithHeadphones = getBoolean(KEY_ONLY_HEADPHONES, false),
            pauseDuringCalls = getBoolean(KEY_PAUSE_CALLS, true),
            quietHoursStart = optionalInt(KEY_QUIET_START),
            quietHoursEnd = optionalInt(KEY_QUIET_END),
            mutedChatIds = getStringSet(KEY_MUTED, emptySet())?.toSet() ?: emptySet(),
            retentionDays = getInt(KEY_RETENTION, 30),
            maxCharsPerMessage = getInt(KEY_MAX_CHARS, 320),
            vibrateInsteadOfSpeak = getBoolean(KEY_VIBRATE_INSTEAD, false),
        )
    }

    private fun SharedPreferences.optionalInt(key: String): Int? =
        getInt(key, VALUE_NONE).takeIf { it != VALUE_NONE }

    companion object {
        private const val KEY_VOICE = "voice_enabled"
        private const val KEY_LANGUAGE = "language_tag"
        private const val KEY_READ_GROUPS = "read_groups"
        private const val KEY_ONLY_HEADPHONES = "only_with_headphones"
        private const val KEY_PAUSE_CALLS = "pause_during_calls"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_MUTED = "muted_chats"
        private const val KEY_RETENTION = "retention_days"
        private const val KEY_MAX_CHARS = "max_chars"
        private const val KEY_VIBRATE_INSTEAD = "vibrate_instead"
        private const val VALUE_NONE = -1
    }
}
