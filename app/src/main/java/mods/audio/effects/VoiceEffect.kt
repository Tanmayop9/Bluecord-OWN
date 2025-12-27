package mods.audio.effects

/**
 * Enum representing different voice effects that can be applied to audio
 */
enum class VoiceEffect(val displayName: String, val pitchFactor: Float, val speedFactor: Float) {
    NONE("Normal Voice", 1.0f, 1.0f),
    HIGH_PITCH("Chipmunk", 1.5f, 1.0f),
    LOW_PITCH("Deep Voice", 0.7f, 1.0f),
    ROBOT("Robot", 1.0f, 1.0f),
    ECHO("Echo", 1.0f, 1.0f),
    FAST("Fast", 1.0f, 1.3f),
    SLOW("Slow", 1.0f, 0.8f),
    HELIUM("Helium", 1.8f, 1.0f),
    GIANT("Giant", 0.5f, 1.0f);

    companion object {
        fun fromOrdinal(ordinal: Int): VoiceEffect {
            return values().getOrNull(ordinal) ?: NONE
        }
    }
}
