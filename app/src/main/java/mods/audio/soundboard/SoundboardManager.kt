package mods.audio.soundboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import mods.utils.LogUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Soundboard manager for playing sound effects during voice calls
 * Similar to Voicemod's soundboard feature
 */
object SoundboardManager {
    private val TAG = SoundboardManager::class.java.simpleName
    
    data class SoundEffect(
        val id: String,
        val name: String,
        val filePath: String,
        val category: String = "Custom",
        val volume: Float = 1.0f,
        val loop: Boolean = false
    )
    
    enum class SoundCategory {
        MEMES,
        REACTIONS,
        MUSIC,
        AMBIENT,
        CUSTOM
    }
    
    private val soundEffects = ConcurrentHashMap<String, SoundEffect>()
    private val activePlayers = ConcurrentHashMap<String, MediaPlayer>()
    private var masterVolume: Float = 1.0f
    
    /**
     * Initialize soundboard with default sounds
     */
    fun initialize(context: Context) {
        LogUtils.log(TAG, "Initializing soundboard")
        loadDefaultSounds(context)
    }
    
    /**
     * Load default sound effects
     */
    private fun loadDefaultSounds(context: Context) {
        // Add some default sounds
        addSoundEffect(
            SoundEffect(
                id = "airhorn",
                name = "Air Horn",
                filePath = "sounds/airhorn.mp3",
                category = SoundCategory.MEMES.name
            )
        )
        
        addSoundEffect(
            SoundEffect(
                id = "applause",
                name = "Applause",
                filePath = "sounds/applause.mp3",
                category = SoundCategory.REACTIONS.name
            )
        )
        
        addSoundEffect(
            SoundEffect(
                id = "crickets",
                name = "Crickets",
                filePath = "sounds/crickets.mp3",
                category = SoundCategory.REACTIONS.name
            )
        )
    }
    
    /**
     * Add a sound effect to the soundboard
     */
    fun addSoundEffect(sound: SoundEffect) {
        soundEffects[sound.id] = sound
        LogUtils.log(TAG, "Added sound effect: ${sound.name}")
    }
    
    /**
     * Play a sound effect
     */
    fun playSound(context: Context, soundId: String) {
        val sound = soundEffects[soundId]
        if (sound == null) {
            LogUtils.log(TAG, "Sound not found: $soundId")
            return
        }
        
        try {
            stopSound(soundId)
            
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build()
                )
                
                val soundFile = File(context.filesDir, sound.filePath)
                if (soundFile.exists()) {
                    setDataSource(soundFile.absolutePath)
                } else {
                    val afd = context.assets.openFd(sound.filePath)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                
                setVolume(sound.volume * masterVolume, sound.volume * masterVolume)
                isLooping = sound.loop
                
                setOnCompletionListener {
                    activePlayers.remove(soundId)
                    release()
                }
                
                prepare()
                start()
            }
            
            activePlayers[soundId] = mediaPlayer
            LogUtils.log(TAG, "Playing sound: ${sound.name}")
            
        } catch (e: Exception) {
            LogUtils.logException(e)
        }
    }
    
    /**
     * Stop a specific sound
     */
    fun stopSound(soundId: String) {
        activePlayers.remove(soundId)?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                LogUtils.logException(e)
            }
        }
    }
    
    /**
     * Stop all sounds
     */
    fun stopAllSounds() {
        activePlayers.keys.toList().forEach { soundId ->
            stopSound(soundId)
        }
    }
    
    /**
     * Get all sound effects
     */
    fun getAllSounds(): List<SoundEffect> {
        return soundEffects.values.toList()
    }
    
    /**
     * Import custom sound from file
     */
    fun importCustomSound(context: Context, uri: Uri, name: String): String? {
        return try {
            val soundId = "custom_${System.currentTimeMillis()}"
            val fileName = "$soundId.mp3"
            val destFile = File(context.filesDir, "sounds/$fileName")
            
            destFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            addSoundEffect(
                SoundEffect(
                    id = soundId,
                    name = name,
                    filePath = "sounds/$fileName",
                    category = SoundCategory.CUSTOM.name
                )
            )
            
            LogUtils.log(TAG, "Imported custom sound: $name")
            soundId
        } catch (e: Exception) {
            LogUtils.logException(e)
            null
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopAllSounds()
        soundEffects.clear()
        LogUtils.log(TAG, "Soundboard cleaned up")
    }
}
