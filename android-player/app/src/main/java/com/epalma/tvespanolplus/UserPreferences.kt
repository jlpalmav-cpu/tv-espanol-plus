package com.epalma.tvespanolplus

import android.content.Context
import java.security.MessageDigest

data class UserPreferences(
    val uiTextScale: Float = 1.0f,
    val preferredAudioLanguage: String = "es-419",
    val preferredSubtitleLanguage: String = "es",
    val subtitleTextSizeSp: Float = 20f,
    val parentalEnabled: Boolean = false,
    val parentalPinHash: String = "",
    val lockedChannelIds: Set<String> = emptySet()
)

data class ParentalGate(
    val title: String,
    val message: String,
    val pendingCategory: String? = null,
    val pendingChannel: Channel? = null,
    val pendingStartDual: Boolean = false,
    val pendingDualSide: DualSide? = null
)

class AppPreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("tv_espanol_plus_user_preferences", Context.MODE_PRIVATE)

    fun load(): UserPreferences = UserPreferences(
        uiTextScale = prefs.getFloat(KEY_TEXT_SCALE, 1.0f).coerceIn(0.85f, 1.35f),
        preferredAudioLanguage = prefs.getString(KEY_AUDIO, "es-419").orEmpty().ifBlank { "es-419" },
        preferredSubtitleLanguage = prefs.getString(KEY_SUBTITLE, "es").orEmpty().ifBlank { "es" },
        subtitleTextSizeSp = prefs.getFloat(KEY_SUBTITLE_SIZE, 20f).coerceIn(16f, 30f),
        parentalEnabled = prefs.getBoolean(KEY_PARENTAL_ENABLED, false),
        parentalPinHash = prefs.getString(KEY_PARENTAL_HASH, "").orEmpty(),
        lockedChannelIds = prefs.getStringSet(KEY_LOCKED_IDS, emptySet())?.toSet().orEmpty()
    )

    fun save(value: UserPreferences) {
        prefs.edit()
            .putFloat(KEY_TEXT_SCALE, value.uiTextScale.coerceIn(0.85f, 1.35f))
            .putString(KEY_AUDIO, value.preferredAudioLanguage)
            .putString(KEY_SUBTITLE, value.preferredSubtitleLanguage)
            .putFloat(KEY_SUBTITLE_SIZE, value.subtitleTextSizeSp.coerceIn(16f, 30f))
            .putBoolean(KEY_PARENTAL_ENABLED, value.parentalEnabled)
            .putString(KEY_PARENTAL_HASH, value.parentalPinHash)
            .putStringSet(KEY_LOCKED_IDS, value.lockedChannelIds.toSet())
            .apply()
    }

    companion object {
        private const val KEY_TEXT_SCALE = "ui_text_scale"
        private const val KEY_AUDIO = "preferred_audio_language"
        private const val KEY_SUBTITLE = "preferred_subtitle_language"
        private const val KEY_SUBTITLE_SIZE = "subtitle_text_size_sp"
        private const val KEY_PARENTAL_ENABLED = "parental_enabled"
        private const val KEY_PARENTAL_HASH = "parental_pin_hash"
        private const val KEY_LOCKED_IDS = "parental_locked_channel_ids"

        fun hashPin(pin: String): String = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

object ParentalPolicy {
    private val alwaysRestrictedCategories = setOf("Adultos 18+")

    fun isRestrictedCategory(route: String, prefs: UserPreferences): Boolean {
        if (!prefs.parentalEnabled) return false
        val canonical = when {
            route.startsWith("Subcat:") -> route.removePrefix("Subcat:").substringBefore('|')
            route.startsWith("CountryCat:") -> route.substringAfter('|', "")
            else -> route
        }
        return canonical in alwaysRestrictedCategories
    }

    fun isRestrictedChannel(channel: Channel, prefs: UserPreferences): Boolean {
        if (!prefs.parentalEnabled) return false
        return channel.id in prefs.lockedChannelIds || ChannelClassifier.categoryFor(channel) in alwaysRestrictedCategories
    }

    fun validPinFormat(pin: String): Boolean = pin.length in 4..6 && pin.all(Char::isDigit)
}
