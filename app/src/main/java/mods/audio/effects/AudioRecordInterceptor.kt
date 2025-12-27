package mods.audio.effects

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import mods.utils.LogUtils
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * AudioRecord interceptor for real-time voice effects in voice channels
 * 
 * This class intercepts AudioRecord operations to apply voice effects
 * to Discord voice channel audio in real-time.
 * 
 * Approach:
 * 1. Monitor AudioRecord instances created by Discord
 * 2. Wrap the read() methods to process audio
 * 3. Apply voice effects before returning data
 * 
 * Note: This requires the app to have RECORD_AUDIO permission
 */
object AudioRecordInterceptor {
    private val TAG = AudioRecordInterceptor::class.java.simpleName
    
    private val monitoredRecorders = mutableSetOf<AudioRecord>()
    private val voiceProcessor = RealtimeVoiceProcessor(48000)
    
    /**
     * Wrap an AudioRecord instance to intercept its read() calls
     */
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    fun wrapAudioRecord(audioRecord: AudioRecord): AudioRecord {
        // Store for monitoring
        monitoredRecorders.add(audioRecord)
        LogUtils.log(TAG, "Wrapped AudioRecord instance")
        
        // Return the original - actual interception happens via read() wrapper
        return audioRecord
    }
    
    /**
     * Process audio data that was read from AudioRecord
     * Call this after AudioRecord.read() to apply effects
     */
    fun processReadData(buffer: ByteArray, offset: Int, size: Int): Int {
        if (!VoiceEffectManager.isEnabled() || size <= 0) {
            return size
        }
        
        try {
            // Convert bytes to shorts (PCM16)
            val samples = size / 2
            val shortBuffer = ShortArray(samples)
            
            for (i in 0 until samples) {
                val byteIndex = offset + (i * 2)
                if (byteIndex + 1 < buffer.size) {
                    shortBuffer[i] = ((buffer[byteIndex + 1].toInt() shl 8) or 
                                     (buffer[byteIndex].toInt() and 0xFF)).toShort()
                }
            }
            
            // Apply voice effect
            val effect = VoiceEffectManager.getCurrentEffect()
            voiceProcessor.processBuffer(shortBuffer, samples, effect)
            
            // Convert back to bytes
            for (i in 0 until samples) {
                val byteIndex = offset + (i * 2)
                if (byteIndex + 1 < buffer.size) {
                    buffer[byteIndex] = (shortBuffer[i].toInt() and 0xFF).toByte()
                    buffer[byteIndex + 1] = ((shortBuffer[i].toInt() shr 8) and 0xFF).toByte()
                }
            }
            
            return size
        } catch (e: Exception) {
            LogUtils.logException(e)
            return size
        }
    }
    
    /**
     * Process audio data (short array variant)
     */
    fun processReadData(buffer: ShortArray, offset: Int, size: Int): Int {
        if (!VoiceEffectManager.isEnabled() || size <= 0) {
            return size
        }
        
        try {
            val effect = VoiceEffectManager.getCurrentEffect()
            
            // Create a temporary buffer for the range to process
            if (offset == 0 && size == buffer.size) {
                // Process entire buffer in place
                voiceProcessor.processBuffer(buffer, size, effect)
            } else {
                // Process only the specified range
                val tempBuffer = ShortArray(size)
                System.arraycopy(buffer, offset, tempBuffer, 0, size)
                voiceProcessor.processBuffer(tempBuffer, size, effect)
                System.arraycopy(tempBuffer, 0, buffer, offset, size)
            }
            
            return size
        } catch (e: Exception) {
            LogUtils.logException(e)
            return size
        }
    }
    
    /**
     * Clean up monitored recorders
     */
    fun cleanup() {
        monitoredRecorders.clear()
        voiceProcessor.reset()
        LogUtils.log(TAG, "Cleaned up audio record interceptor")
    }
}

/**
 * Custom AudioRecord wrapper that applies voice effects
 * 
 * This can be used to replace Discord's AudioRecord if we can inject it
 */
class VoiceEffectAudioRecord(
    audioSource: Int,
    sampleRateInHz: Int,
    channelConfig: Int,
    audioFormat: Int,
    bufferSizeInBytes: Int
) : AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes) {
    
    private val voiceProcessor = RealtimeVoiceProcessor(sampleRateInHz)
    
    companion object {
        private val TAG = VoiceEffectAudioRecord::class.java.simpleName
    }
    
    override fun read(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int {
        val result = super.read(audioData, offsetInBytes, sizeInBytes)
        
        if (result > 0 && VoiceEffectManager.isEnabled()) {
            try {
                AudioRecordInterceptor.processReadData(audioData, offsetInBytes, result)
            } catch (e: Exception) {
                LogUtils.logException(e)
            }
        }
        
        return result
    }
    
    override fun read(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
        val result = super.read(audioData, offsetInShorts, sizeInShorts)
        
        if (result > 0 && VoiceEffectManager.isEnabled()) {
            try {
                AudioRecordInterceptor.processReadData(audioData, offsetInShorts, result)
            } catch (e: Exception) {
                LogUtils.logException(e)
            }
        }
        
        return result
    }
    
    override fun read(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int, readMode: Int): Int {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            super.read(audioData, offsetInBytes, sizeInBytes, readMode)
        } else {
            super.read(audioData, offsetInBytes, sizeInBytes)
        }
        
        if (result > 0 && VoiceEffectManager.isEnabled()) {
            try {
                AudioRecordInterceptor.processReadData(audioData, offsetInBytes, result)
            } catch (e: Exception) {
                LogUtils.logException(e)
            }
        }
        
        return result
    }
}
