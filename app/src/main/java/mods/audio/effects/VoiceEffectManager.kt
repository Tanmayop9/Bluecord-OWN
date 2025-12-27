package mods.audio.effects

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.NoiseSuppressor
import mods.preference.Prefs
import mods.utils.LogUtils

/**
 * Manages voice effects settings and state
 */
object VoiceEffectManager {
    private val TAG = VoiceEffectManager::class.java.simpleName
    
    const val PREF_VOICE_EFFECT_ENABLED = "blue.voice.effect.enabled"
    const val PREF_VOICE_EFFECT_TYPE = "blue.voice.effect.type"
    const val PREF_VOICE_EFFECT_PITCH = "blue.voice.effect.pitch"
    
    /**
     * Check if voice effects are enabled
     */
    fun isEnabled(): Boolean {
        return Prefs.getBoolean(PREF_VOICE_EFFECT_ENABLED, false)
    }
    
    /**
     * Enable or disable voice effects
     */
    fun setEnabled(enabled: Boolean) {
        Prefs.setBoolean(PREF_VOICE_EFFECT_ENABLED, enabled)
        LogUtils.log(TAG, "Voice effects ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get the current voice effect
     */
    fun getCurrentEffect(): VoiceEffect {
        val ordinal = Prefs.getInt(PREF_VOICE_EFFECT_TYPE, VoiceEffect.NONE.ordinal)
        return VoiceEffect.fromOrdinal(ordinal)
    }
    
    /**
     * Set the current voice effect
     */
    fun setCurrentEffect(effect: VoiceEffect) {
        Prefs.setInt(PREF_VOICE_EFFECT_TYPE, effect.ordinal)
        LogUtils.log(TAG, "Voice effect set to: ${effect.displayName}")
    }
    
    /**
     * Get custom pitch value (0.5 to 2.0)
     */
    fun getCustomPitch(): Float {
        return Prefs.getFloat(PREF_VOICE_EFFECT_PITCH, 1.0f).coerceIn(0.5f, 2.0f)
    }
    
    /**
     * Set custom pitch value (0.5 to 2.0)
     */
    fun setCustomPitch(pitch: Float) {
        Prefs.setFloat(PREF_VOICE_EFFECT_PITCH, pitch.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Try to enable noise suppression for the given audio session
     */
    fun enableNoiseSuppression(audioSessionId: Int): NoiseSuppressor? {
        return try {
            if (NoiseSuppressor.isAvailable()) {
                val suppressor = NoiseSuppressor.create(audioSessionId)
                suppressor?.enabled = true
                LogUtils.log(TAG, "Noise suppressor enabled for session $audioSessionId")
                suppressor
            } else {
                LogUtils.log(TAG, "Noise suppressor not available")
                null
            }
        } catch (e: Exception) {
            LogUtils.logException(e)
            null
        }
    }
    
    /**
     * Try to enable acoustic echo canceler for the given audio session
     */
    fun enableAcousticEchoCanceler(audioSessionId: Int): AcousticEchoCanceler? {
        return try {
            if (AcousticEchoCanceler.isAvailable()) {
                val aec = AcousticEchoCanceler.create(audioSessionId)
                aec?.enabled = true
                LogUtils.log(TAG, "Acoustic echo canceler enabled for session $audioSessionId")
                aec
            } else {
                LogUtils.log(TAG, "Acoustic echo canceler not available")
                null
            }
        } catch (e: Exception) {
            LogUtils.logException(e)
            null
        }
    }
}
