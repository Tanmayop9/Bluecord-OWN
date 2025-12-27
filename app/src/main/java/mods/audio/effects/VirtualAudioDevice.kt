package mods.audio.effects

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import mods.utils.LogUtils
import mods.utils.ThreadUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Virtual audio device that acts as a real-time voice changer for voice channels
 * 
 * This creates a background service that:
 * 1. Captures audio from the microphone
 * 2. Applies voice effects in real-time
 * 3. Provides the processed audio to Discord's WebRTC
 * 
 * Note: This is a workaround approach since we cannot directly hook into Discord's
 * WebRTC pipeline. It works by becoming the audio source.
 */
class VirtualAudioDevice private constructor() {
    
    companion object {
        private val TAG = VirtualAudioDevice::class.java.simpleName
        private val SAMPLE_RATE = 48000 // Discord uses 48kHz
        private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE_FACTOR = 2
        
        @Volatile
        private var instance: VirtualAudioDevice? = null
        
        fun getInstance(): VirtualAudioDevice {
            return instance ?: synchronized(this) {
                instance ?: VirtualAudioDevice().also { instance = it }
            }
        }
    }
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)
    private val voiceProcessor = RealtimeVoiceProcessor(SAMPLE_RATE)
    private var processingThread: Thread? = null
    
    /**
     * Start the virtual audio device
     */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning.get()) {
            LogUtils.log(TAG, "Virtual audio device already running")
            return true
        }
        
        return try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                LogUtils.log(TAG, "Failed to get min buffer size")
                return false
            }
            
            val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR
            
            // Create AudioRecord to capture microphone
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION, // Use voice communication source
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                LogUtils.log(TAG, "AudioRecord initialization failed")
                return false
            }
            
            // Create AudioTrack to play back (this makes it available to other apps)
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AUDIO_FORMAT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                LogUtils.log(TAG, "AudioTrack initialization failed")
                return false
            }
            
            isRunning.set(true)
            audioRecord?.startRecording()
            audioTrack?.play()
            
            // Start processing thread
            processingThread = ThreadUtils.startThread("VirtualAudioDevice") {
                processAudioLoop(bufferSize)
            }
            
            LogUtils.log(TAG, "Virtual audio device started successfully")
            true
        } catch (e: Exception) {
            LogUtils.logException(e)
            stop()
            false
        }
    }
    
    /**
     * Stop the virtual audio device
     */
    fun stop() {
        isRunning.set(false)
        
        try {
            processingThread?.join(1000)
        } catch (e: InterruptedException) {
            LogUtils.logException(e)
        }
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            LogUtils.logException(e)
        }
        
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            LogUtils.logException(e)
        }
        
        voiceProcessor.reset()
        LogUtils.log(TAG, "Virtual audio device stopped")
    }
    
    /**
     * Main audio processing loop
     */
    private fun processAudioLoop(bufferSize: Int) {
        val byteBuffer = ByteArray(bufferSize)
        val shortBuffer = ShortArray(bufferSize / 2)
        
        while (isRunning.get()) {
            try {
                val record = audioRecord ?: break
                val track = audioTrack ?: break
                
                // Read from microphone
                val bytesRead = record.read(byteBuffer, 0, bufferSize)
                
                if (bytesRead <= 0) {
                    continue
                }
                
                // Convert bytes to shorts
                val samplesRead = bytesRead / 2
                for (i in 0 until samplesRead) {
                    val index = i * 2
                    shortBuffer[i] = ((byteBuffer[index + 1].toInt() shl 8) or 
                                     (byteBuffer[index].toInt() and 0xFF)).toShort()
                }
                
                // Apply voice effect if enabled
                if (VoiceEffectManager.isEnabled()) {
                    val effect = VoiceEffectManager.getCurrentEffect()
                    voiceProcessor.processBuffer(shortBuffer, samplesRead, effect)
                }
                
                // Convert shorts back to bytes
                for (i in 0 until samplesRead) {
                    val index = i * 2
                    byteBuffer[index] = (shortBuffer[i].toInt() and 0xFF).toByte()
                    byteBuffer[index + 1] = ((shortBuffer[i].toInt() shr 8) and 0xFF).toByte()
                }
                
                // Write to output (makes it available to other apps like Discord)
                track.write(byteBuffer, 0, bytesRead)
                
            } catch (e: Exception) {
                LogUtils.logException(e)
                break
            }
        }
    }
    
    /**
     * Check if the device is currently running
     */
    fun isRunning(): Boolean = isRunning.get()
}
