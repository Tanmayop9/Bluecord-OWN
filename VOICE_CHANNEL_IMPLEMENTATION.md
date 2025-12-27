# Real-Time Voice Channel Effects - Implementation Guide

## Current Status

✅ **WORKING**: Voice message recording with real-time effects  
🔄 **IN PROGRESS**: Voice channel real-time effects (multiple approaches implemented)

## The Challenge

Discord voice channels use WebRTC (Web Real-Time Communication) which processes audio in native C++ code. The audio pipeline is:

```
Microphone → AudioRecord (Native) → WebRTC Encoder → Network → Discord Servers
```

To apply voice effects, we need to intercept the audio between AudioRecord and WebRTC Encoder.

## Implemented Approaches

### 1. Modified Pcm16AudioRecorder ✅ WORKING
**File**: `Pcm16AudioRecorder.kt`  
**Status**: Fully functional for voice messages  
**How it works**:
- Directly modifies the audio recording loop
- Applies effects frame-by-frame during capture
- Saves processed audio to file

**Limitations**: Only works for voice message recording, not voice channels

### 2. AudioRecordInterceptor 🔄 TESTING NEEDED
**File**: `AudioRecordInterceptor.kt`  
**Status**: Implemented, needs testing  
**How it works**:
- Wraps AudioRecord.read() calls
- Processes audio data before it's returned
- Can intercept Discord's audio capture

**To Test**:
```kotlin
// In Discord's audio initialization code
val originalAudioRecord = AudioRecord(...)
val wrappedAudioRecord = AudioRecordInterceptor.wrapAudioRecord(originalAudioRecord)
```

**Advantages**:
- Non-invasive
- Works with existing code
- Low overhead

**Challenges**:
- Need to find where Discord creates AudioRecord
- May need bytecode injection

### 3. VoiceEffectAudioRecord 🔄 NEEDS INJECTION
**File**: `AudioRecordInterceptor.kt` (VoiceEffectAudioRecord class)  
**Status**: Implemented, needs injection  
**How it works**:
- Custom AudioRecord subclass
- Overrides read() methods to apply effects
- Automatically processes all audio

**To Use**:
Need to make Discord use VoiceEffectAudioRecord instead of AudioRecord. Options:

a) **Reflection Replacement**:
```kotlin
// Replace AudioRecord constructor
val constructor = AudioRecord::class.java.getConstructor(...)
// Hook to return VoiceEffectAudioRecord instead
```

b) **ClassLoader Hook**:
```kotlin
// Intercept class loading
// Return VoiceEffectAudioRecord when Discord asks for AudioRecord
```

c) **Native Hook** (Most reliable but complex):
```cpp
// Hook AudioRecord JNI methods
// Redirect to our implementation
```

### 4. VirtualAudioDevice ⚠️ LIMITED
**File**: `VirtualAudioDevice.kt`  
**Status**: Implemented but limited usefulness  
**How it works**:
- Creates separate audio capture and playback
- Processes audio in background thread
- Outputs to AudioTrack

**Limitations**:
- Discord won't capture from AudioTrack
- Creates echo if both devices run simultaneously
- High battery usage

**Possible Use**:
- With audio routing apps (e.g., AudioRelay, SoundAbout)
- As system-wide voice changer (if rooted)

### 5. VoiceChannelProcessor ❌ READ-ONLY
**File**: `VoiceChannelProcessor.kt`  
**Status**: Implemented but can't modify audio  
**How it works**:
- Uses Android Visualizer API
- Captures audio from audio session
- Can analyze but not modify

**Why it doesn't work**:
- Visualizer is read-only API
- Can't write modified data back
- Only useful for visualization/analysis

### 6. AudioRecordHook ⚠️ REQUIRES FRAMEWORK
**File**: `AudioRecordHook.kt`  
**Status**: Placeholder implementation  
**How it works**:
- Uses runtime hooking framework (Xposed/LSPosed)
- Intercepts AudioRecord methods at system level
- Can modify any app's audio

**Requirements**:
- Rooted device
- Xposed/LSPosed framework installed
- Create Xposed module

**Implementation**:
```kotlin
// In Xposed module
findAndHookMethod(
    AudioRecord::class.java,
    "read",
    ByteArray::class.java,
    Int::class.java,
    Int::class.java,
    object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val buffer = param.args[0] as ByteArray
            val size = param.result as Int
            AudioRecordInterceptor.processReadData(buffer, 0, size)
        }
    }
)
```

## Recommended Implementation Path

### Phase 1: Make It Work (Choose One)

#### Option A: Find Discord's AudioRecord Creation Point
1. Search Bluecord codebase for AudioRecord instantiation
2. Wrap or replace with VoiceEffectAudioRecord
3. Test in voice channel

**Search for**:
```kotlin
grep -r "AudioRecord" app/src/main --include="*.kt" --include="*.java"
grep -r "MediaRecorder" app/src/main --include="*.kt" --include="*.java"
```

#### Option B: Use Reflection to Hook AudioRecord
1. Find all AudioRecord instances at runtime
2. Use reflection to intercept read() calls
3. Process audio in wrapper

**Implementation**:
```kotlin
// In Application.onCreate()
fun hookAllAudioRecords() {
    val audioRecordClass = AudioRecord::class.java
    val readMethod = audioRecordClass.getMethod("read", ByteArray::class.java, Int::class.java, Int::class.java)
    
    // Use ProxyBuilder or similar to intercept
}
```

#### Option C: Create Xposed Module (Most Reliable)
1. Create separate Xposed module project
2. Hook AudioRecord at system level
3. Works for any app including Bluecord

**Pros**: Most reliable, works everywhere  
**Cons**: Requires root, separate module

### Phase 2: Optimize Performance

Once basic functionality works:

1. **Reduce Latency**:
   - Optimize buffer processing
   - Use native code (JNI) for DSP
   - Reduce buffer copies

2. **Improve Quality**:
   - Implement better pitch shifting (phase vocoder)
   - Add formant preservation
   - Use FFT-based processing

3. **Add Features**:
   - Multiple simultaneous effects
   - Custom effect parameters
   - Effect presets

### Phase 3: Add UI Controls

1. **Quick Toggle**:
   - Floating button during voice calls
   - Quick settings tile
   - Notification controls

2. **Effect Selector**:
   - In-call effect switching
   - Real-time preview
   - Custom parameters

## Testing Guide

### Test Voice Messages (Already Works)

1. Enable voice effects in settings
2. Select an effect (e.g., Chipmunk)
3. Record a voice message
4. Send and verify effect is applied ✅

### Test Voice Channels (Needs Implementation)

1. Join a Discord voice channel
2. Enable voice effects
3. Speak into microphone
4. Ask others if they hear the effect

**Debug Steps**:
```kotlin
// Add logging to see if audio is being processed
LogUtils.log(TAG, "Processing audio: $sampleCount samples")

// Verify effects are enabled
LogUtils.log(TAG, "Effects enabled: ${VoiceEffectManager.isEnabled()}")
LogUtils.log(TAG, "Current effect: ${VoiceEffectManager.getCurrentEffect()}")
```

## Building and Installing

### Build Debug APK

```bash
cd /path/to/Bluecord-OWN
./gradlew assembleDebug
```

APK will be in: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

```bash
./gradlew assembleRelease
```

APK will be in: `app/build/outputs/apk/release/app-release.apk`

### Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or transfer APK to device and install manually.

## Troubleshooting

### Voice Messages Work, Voice Channels Don't

**Cause**: Audio interception not working for WebRTC  
**Solution**: Implement AudioRecordInterceptor approach or use Xposed

### Effects Applied But Audio Quality Is Poor

**Cause**: Simple resampling algorithm  
**Solution**: 
- Reduce pitch change amount
- Implement phase vocoder
- Use external tools (voicesmith)

### High CPU Usage / Battery Drain

**Cause**: Real-time processing overhead  
**Solution**:
- Optimize DSP algorithms
- Use native code (C++)
- Reduce processing complexity

### No Audio in Voice Channels

**Cause**: Permission issues or audio source conflict  
**Solution**:
- Check microphone permissions
- Ensure only one audio capture at a time
- Check audio focus

## Next Steps

### Immediate (To Make Voice Channels Work)

1. **Find AudioRecord Usage**:
   ```bash
   cd app/src/main
   find . -name "*.smali" -o -name "*.dex" | xargs grep -l "AudioRecord"
   ```

2. **Test AudioRecordInterceptor**:
   - Add initialization code
   - Test with voice channel
   - Verify audio processing

3. **Create Xposed Module** (if needed):
   - New project for Xposed hooks
   - Hook AudioRecord.read()
   - Test with Bluecord

### Short Term

- Optimize processing performance
- Add UI toggle for voice channels
- Test with multiple effects
- Get user feedback

### Long Term

- Integrate voicesmith for quality
- Add AI voice cloning (server-based)
- Create effect marketplace
- Support effect plugins

## Code Examples

### Example: Hooking Discord's Audio

```kotlin
// In DiscordTools.kt or similar initialization file
object VoiceChannelHook {
    fun initialize() {
        try {
            // Find WebRTC audio classes
            val webRtcAudioRecord = Class.forName("org.webrtc.audio.WebRtcAudioRecord")
            
            // Hook the audio processing method
            hookMethod(webRtcAudioRecord, "nativeDataIsRecorded") { method, args ->
                val audioData = args[1] as ByteArray
                val size = args[2] as Int
                
                // Process audio
                AudioRecordInterceptor.processReadData(audioData, 0, size)
                
                // Call original method
                method.invoke(args[0], *args.copyOfRange(1, args.size))
            }
            
            LogUtils.log(TAG, "Voice channel hook installed")
        } catch (e: Exception) {
            LogUtils.logException(e)
        }
    }
}
```

### Example: Testing Audio Processing

```kotlin
// Test in isolation
fun testAudioProcessing() {
    val sampleRate = 48000
    val duration = 1.0 // seconds
    val sampleCount = (sampleRate * duration).toInt()
    
    // Generate test tone
    val testAudio = ShortArray(sampleCount) { i ->
        (32767 * Math.sin(2 * Math.PI * 440 * i / sampleRate)).toInt().toShort()
    }
    
    // Apply effect
    val processor = RealtimeVoiceProcessor(sampleRate)
    processor.processBuffer(testAudio, sampleCount, VoiceEffect.HIGH_PITCH)
    
    // testAudio now contains processed audio
    // Save to file or play to verify
}
```

## Resources

- **Android AudioRecord**: https://developer.android.com/reference/android/media/AudioRecord
- **WebRTC**: https://webrtc.org/
- **Xposed Framework**: https://repo.xposed.info/
- **LSPosed**: https://github.com/LSPosed/LSPosed
- **Voicesmith**: https://github.com/jurihock/voicesmith

## Contributing

To help implement voice channel support:

1. Find where Discord creates AudioRecord for voice channels
2. Test AudioRecordInterceptor approach
3. Report results in GitHub issues
4. Submit PRs with improvements

## Support

For issues:
- GitHub Issues: Tag with "voice-changer" and "voice-channels"
- Provide logs from LogUtils
- Specify device and Android version
- Note if rooted/Xposed available
