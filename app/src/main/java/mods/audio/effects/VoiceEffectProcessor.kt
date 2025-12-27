package mods.audio.effects

import mods.audio.converters.AudioConstants
import mods.utils.LogUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Processes audio files to apply voice effects
 */
object VoiceEffectProcessor {
    private val TAG = VoiceEffectProcessor::class.java.simpleName

    /**
     * Apply a voice effect to a PCM16 audio file
     * @param inputFile The input PCM16 audio file
     * @param outputFile The output PCM16 audio file with effects applied
     * @param effect The voice effect to apply
     * @return true if successful, false otherwise
     */
    fun applyEffect(inputFile: File, outputFile: File, effect: VoiceEffect): Boolean {
        if (effect == VoiceEffect.NONE) {
            // No effect, just copy the file
            inputFile.copyTo(outputFile, overwrite = true)
            return true
        }

        return try {
            when (effect) {
                VoiceEffect.ECHO -> applyEcho(inputFile, outputFile)
                VoiceEffect.ROBOT -> applyRobotEffect(inputFile, outputFile)
                else -> applyPitchShift(inputFile, outputFile, effect.pitchFactor)
            }
            true
        } catch (e: Exception) {
            LogUtils.logException(e)
            // On error, copy original file
            inputFile.copyTo(outputFile, overwrite = true)
            false
        }
    }

    /**
     * Apply pitch shifting to audio
     */
    private fun applyPitchShift(inputFile: File, outputFile: File, pitchFactor: Float) {
        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(4096)
                val sampleBuffer = ShortArray(2048)
                
                // Simple pitch shift by resampling
                // This is a basic implementation - more sophisticated algorithms like PSOLA would be better
                var bytesRead: Int
                val pitchBuffer = mutableListOf<Short>()
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    // Convert bytes to shorts (PCM16)
                    for (i in 0 until bytesRead step 2) {
                        if (i + 1 < bytesRead) {
                            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                            pitchBuffer.add(sample)
                        }
                    }
                }

                // Resample to change pitch
                val outputSamples = mutableListOf<Short>()
                val inputLength = pitchBuffer.size
                val outputLength = (inputLength / pitchFactor).toInt()

                for (i in 0 until outputLength) {
                    val srcIndex = (i * pitchFactor).toFloat()
                    val index1 = srcIndex.toInt()
                    val index2 = min(index1 + 1, inputLength - 1)
                    val fraction = srcIndex - index1

                    if (index1 < inputLength) {
                        // Linear interpolation
                        val sample1 = pitchBuffer[index1].toFloat()
                        val sample2 = pitchBuffer[index2].toFloat()
                        val interpolated = (sample1 * (1 - fraction) + sample2 * fraction).roundToInt().toShort()
                        outputSamples.add(interpolated)
                    }
                }

                // Convert shorts back to bytes and write
                for (sample in outputSamples) {
                    output.write(sample.toInt() and 0xFF)
                    output.write((sample.toInt() shr 8) and 0xFF)
                }
            }
        }
    }

    /**
     * Apply echo effect to audio
     */
    private fun applyEcho(inputFile: File, outputFile: File) {
        val delayMs = 150 // Echo delay in milliseconds
        val delaySamples = (AudioConstants.SAMPLE_RATE * delayMs / 1000)
        val decay = 0.5f // Echo decay factor

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val allSamples = mutableListOf<Short>()
                val buffer = ByteArray(4096)
                var bytesRead: Int

                // Read all samples
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    for (i in 0 until bytesRead step 2) {
                        if (i + 1 < bytesRead) {
                            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                            allSamples.add(sample)
                        }
                    }
                }

                // Apply echo
                for (i in allSamples.indices) {
                    var outputSample = allSamples[i].toFloat()
                    
                    // Add delayed sample
                    if (i >= delaySamples) {
                        outputSample += allSamples[i - delaySamples] * decay
                    }

                    // Clamp to short range
                    val clampedSample = outputSample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
                    
                    output.write(clampedSample.toInt() and 0xFF)
                    output.write((clampedSample.toInt() shr 8) and 0xFF)
                }
            }
        }
    }

    /**
     * Apply robot effect to audio (simple ring modulation)
     */
    private fun applyRobotEffect(inputFile: File, outputFile: File) {
        val modulationFreq = 30.0 // Hz
        
        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var sampleIndex = 0

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    for (i in 0 until bytesRead step 2) {
                        if (i + 1 < bytesRead) {
                            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                            
                            // Ring modulation
                            val t = sampleIndex.toDouble() / AudioConstants.SAMPLE_RATE
                            val modulator = Math.sin(2.0 * Math.PI * modulationFreq * t)
                            val modulated = (sample * (0.5 + 0.5 * modulator)).toInt().toShort()
                            
                            output.write(modulated.toInt() and 0xFF)
                            output.write((modulated.toInt() shr 8) and 0xFF)
                            
                            sampleIndex++
                        }
                    }
                }
            }
        }
    }
}
