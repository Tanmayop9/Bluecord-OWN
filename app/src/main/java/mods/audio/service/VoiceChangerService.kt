package mods.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bluecord.R
import mods.audio.effects.RealtimeVoiceProcessor
import mods.audio.effects.VoiceEffect
import mods.audio.effects.VoiceEffectManager
import mods.utils.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Voice Changer Service - Similar to Voicemod
 * Runs as foreground service for real-time voice effects
 */
class VoiceChangerService : Service() {
    
    companion object {
        private val TAG = VoiceChangerService::class.java.simpleName
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_changer_service"
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        const val ACTION_START = "com.bluecord.voice.START"
        const val ACTION_STOP = "com.bluecord.voice.STOP"
        const val ACTION_TOGGLE_EFFECT = "com.bluecord.voice.TOGGLE_EFFECT"
        const val EXTRA_EFFECT = "effect"
    }
    
    private val binder = LocalBinder()
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)
    private var processingThread: Thread? = null
    private val voiceProcessor = RealtimeVoiceProcessor(SAMPLE_RATE)
    
    inner class LocalBinder : Binder() {
        fun getService(): VoiceChangerService = this@VoiceChangerService
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        LogUtils.log(TAG, "Voice Changer Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVoiceChanger()
            ACTION_STOP -> stopVoiceChanger()
            ACTION_TOGGLE_EFFECT -> {
                val effectOrdinal = intent.getIntExtra(EXTRA_EFFECT, 0)
                val effect = VoiceEffect.fromOrdinal(effectOrdinal)
                VoiceEffectManager.setCurrentEffect(effect)
            }
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Changer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time voice changing service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, VoiceChangerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        
        val effect = VoiceEffectManager.getCurrentEffect()
        val contentText = if (VoiceEffectManager.isEnabled()) {
            "Active: ${effect.displayName}"
        } else {
            "Inactive"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Changer")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mic_interactivenormal_dark_24dp)
            .addAction(R.drawable.ic_delete_white_24dp, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun startVoiceChanger() {
        if (isRunning.get()) {
            return
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            if (minBufferSize <= 0) {
                stopSelf()
                return
            }
            
            val bufferSize = minBufferSize * 4
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AUDIO_FORMAT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            isRunning.set(true)
            audioRecord?.startRecording()
            audioTrack?.play()
            
            processingThread = Thread {
                processAudioLoop(bufferSize)
            }.apply {
                name = "VoiceChangerThread"
                priority = Thread.MAX_PRIORITY
                start()
            }
            
            LogUtils.log(TAG, "Voice changer started")
            
        } catch (e: Exception) {
            LogUtils.logException(e)
            stopVoiceChanger()
        }
    }
    
    private fun processAudioLoop(bufferSize: Int) {
        val byteBuffer = ByteArray(bufferSize)
        val shortBuffer = ShortArray(bufferSize / 2)
        
        while (isRunning.get()) {
            try {
                val record = audioRecord ?: break
                val track = audioTrack ?: break
                
                val bytesRead = record.read(byteBuffer, 0, bufferSize)
                if (bytesRead <= 0) continue
                
                val samplesRead = bytesRead / 2
                for (i in 0 until samplesRead) {
                    val index = i * 2
                    shortBuffer[i] = ((byteBuffer[index + 1].toInt() shl 8) or 
                                     (byteBuffer[index].toInt() and 0xFF)).toShort()
                }
                
                if (VoiceEffectManager.isEnabled()) {
                    val effect = VoiceEffectManager.getCurrentEffect()
                    voiceProcessor.processBuffer(shortBuffer, samplesRead, effect)
                }
                
                for (i in 0 until samplesRead) {
                    val index = i * 2
                    byteBuffer[index] = (shortBuffer[i].toInt() and 0xFF).toByte()
                    byteBuffer[index + 1] = ((shortBuffer[i].toInt() shr 8) and 0xFF).toByte()
                }
                
                track.write(byteBuffer, 0, bytesRead)
                
            } catch (e: Exception) {
                LogUtils.logException(e)
                break
            }
        }
    }
    
    private fun stopVoiceChanger() {
        isRunning.set(false)
        
        try {
            processingThread?.join(1000)
            audioRecord?.stop()
            audioRecord?.release()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            LogUtils.logException(e)
        }
        
        audioRecord = null
        audioTrack = null
        voiceProcessor.reset()
        
        stopForeground(true)
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVoiceChanger()
    }
}
