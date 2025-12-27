package mods.audio.effects

import mods.utils.LogUtils
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

/**
 * Real-time audio processor that applies voice effects to audio streams
 * This processes audio buffers in real-time for voice channels
 */
class RealtimeVoiceProcessor(
    private val sampleRate: Int = 48000 // Discord uses 48kHz
) {
    private val TAG = RealtimeVoiceProcessor::class.java.simpleName
    
    // Circular buffer for echo effect
    private val echoBufferSize = (sampleRate * 0.15).toInt() // 150ms delay
    private val echoBuffer = ShortArray(echoBufferSize)
    private var echoBufferPos = 0
    
    // Phase vocoder state for pitch shifting without tempo change
    private var lastPhase = 0.0
    private var sumPhase = 0.0
    
    /**
     * Process a buffer of PCM16 audio samples in-place
     * @param buffer The audio buffer to process (will be modified)
     * @param length The number of valid shorts in the buffer
     * @param effect The voice effect to apply
     */
    fun processBuffer(buffer: ShortArray, length: Int, effect: VoiceEffect) {
        if (effect == VoiceEffect.NONE || length == 0) {
            return
        }
        
        try {
            when (effect) {
                VoiceEffect.HIGH_PITCH -> applyPitchShiftRealtime(buffer, length, 1.5f)
                VoiceEffect.LOW_PITCH -> applyPitchShiftRealtime(buffer, length, 0.7f)
                VoiceEffect.HELIUM -> applyPitchShiftRealtime(buffer, length, 1.8f)
                VoiceEffect.GIANT -> applyPitchShiftRealtime(buffer, length, 0.5f)
                VoiceEffect.ROBOT -> applyRobotEffectRealtime(buffer, length)
                VoiceEffect.ECHO -> applyEchoRealtime(buffer, length)
                else -> {} // Other effects not suitable for real-time
            }
        } catch (e: Exception) {
            LogUtils.logException(e)
        }
    }
    
    /**
     * Apply pitch shift in real-time using a simple algorithm
     * Note: This changes both pitch and tempo. A phase vocoder would be needed
     * for pitch-only changes, but that's too CPU intensive for mobile real-time.
     */
    private fun applyPitchShiftRealtime(buffer: ShortArray, length: Int, pitchFactor: Float) {
        // Create a temporary buffer for the output
        val tempBuffer = ShortArray(length)
        
        // Simple resampling for pitch shift
        for (i in 0 until length) {
            val srcIndex = (i * pitchFactor).toInt()
            if (srcIndex < length) {
                tempBuffer[i] = buffer[srcIndex]
            } else {
                tempBuffer[i] = 0
            }
        }
        
        // Copy back to original buffer
        System.arraycopy(tempBuffer, 0, buffer, 0, length)
    }
    
    /**
     * Apply echo effect in real-time using a circular buffer
     */
    private fun applyEchoRealtime(buffer: ShortArray, length: Int) {
        val decay = 0.4f // Echo decay factor
        
        for (i in 0 until length) {
            // Get the delayed sample from the circular buffer
            val delayedSample = echoBuffer[echoBufferPos]
            
            // Mix original with delayed sample
            val mixed = buffer[i] + (delayedSample * decay).toInt()
            val clamped = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            
            // Store current sample in circular buffer
            echoBuffer[echoBufferPos] = buffer[i]
            
            // Update output
            buffer[i] = clamped
            
            // Move circular buffer position
            echoBufferPos = (echoBufferPos + 1) % echoBufferSize
        }
    }
    
    /**
     * Apply robot effect in real-time using ring modulation
     */
    private fun applyRobotEffectRealtime(buffer: ShortArray, length: Int) {
        val modulationFreq = 30.0 // Hz
        val twoPiFreq = 2.0 * PI * modulationFreq / sampleRate
        
        for (i in 0 until length) {
            // Ring modulation with sine wave
            val phase = (lastPhase + i * twoPiFreq) % (2.0 * PI)
            val modulator = sin(phase)
            val modulated = (buffer[i] * (0.5 + 0.5 * modulator)).roundToInt()
            buffer[i] = modulated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        lastPhase = (lastPhase + length * twoPiFreq) % (2.0 * PI)
    }
    
    /**
     * Reset the processor state (call when starting a new recording/stream)
     */
    fun reset() {
        echoBuffer.fill(0)
        echoBufferPos = 0
        lastPhase = 0.0
        sumPhase = 0.0
    }
}
