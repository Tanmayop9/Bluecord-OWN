# Voice Changer Feature

## Overview
Bluecord now includes an in-built voice changer that allows you to modify your voice in real-time with various effects during voice message recording.

## Features

### Available Voice Effects
1. **Normal Voice** - No effect applied
2. **Chipmunk** - High-pitched voice (1.5x pitch)
3. **Deep Voice** - Low-pitched voice (0.7x pitch)
4. **Helium** - Very high-pitched voice (1.8x pitch)
5. **Giant** - Very deep voice (0.5x pitch)
6. **Robot** - Robotic voice effect using ring modulation
7. **Echo** - Echo effect with 150ms delay
8. **Fast** - Faster speech (planned for future implementation)
9. **Slow** - Slower speech (planned for future implementation)

## How to Use

### Enabling Voice Effects
1. Open Bluecord settings/preferences
2. Find "Voice Effect Settings" preference
3. Toggle the "Enable Voice Effects" switch
4. Select your desired effect from the list
5. Click "Apply"

### Recording with Voice Effects
1. Enable voice effects in settings
2. Go to any Discord channel where you can send messages
3. Start recording a voice message as usual
4. Your voice will be processed in real-time with the selected effect
5. Send the voice message - it will have the effect applied

## Technical Details

### Architecture
The voice changer feature consists of:

- **VoiceEffect.kt**: Defines all available effects
- **VoiceEffectProcessor.kt**: Processes audio files with effects
- **RealtimeVoiceProcessor.kt**: Processes audio buffers in real-time
- **VoiceEffectManager.kt**: Manages settings and preferences
- **Pcm16AudioRecorder.kt**: Integrated with audio recording pipeline

### How It Works
1. Audio is captured from the microphone at 48kHz (Discord's standard)
2. Audio samples are converted from bytes to shorts (PCM16 format)
3. The RealtimeVoiceProcessor applies the selected effect to each buffer
4. Processed audio is converted back to bytes and saved
5. The file is encoded to OGG format and uploaded as a voice message

### Voice Channel Support
**Current Status**: Voice effects work for voice message recording.

**Voice Channel Limitations**: 
- Discord's voice channels use WebRTC with encrypted streams
- WebRTC audio pipeline in Bluecord uses native/obfuscated code
- Direct real-time processing of voice channel audio requires deeper integration
- This would need access to the audio pipeline before encryption

**Potential Solutions**:
- Use a virtual audio device to route audio through the processor
- Hook into the WebRTC native library (complex, requires reverse engineering)
- Use the jurihock/voicesmith library with full native integration

## Performance

### Processing Latency
- **Voice Messages**: Near real-time processing during recording
- **Buffer Size**: Processed in small chunks (typically 512-2048 samples)
- **CPU Usage**: Moderate - simple effects like pitch shift use basic resampling
- **Memory**: Minimal additional memory usage

### Quality
- **Sample Rate**: 48kHz (Discord standard)
- **Bit Depth**: 16-bit PCM
- **Channels**: Mono (as used by Discord voice messages)

## Known Limitations

1. **Voice Channels**: Currently only works for voice message recording, not live voice channels
2. **Quality**: Pitch shifting uses simple resampling which may reduce quality
3. **Speed Effects**: Fast/Slow effects not yet implemented for real-time
4. **Latency**: Some processing latency during recording (typically <50ms)

## Future Improvements

### Planned Features
- Integration with jurihock/voicesmith for higher quality pitch shifting
- Real-time voice channel support (if WebRTC access becomes available)
- Custom effect parameters (adjustable pitch, echo delay, etc.)
- Voice presets (save favorite effect combinations)
- AI-based voice cloning (resource permitting)

### Advanced Integration
For advanced users who want better quality:
- The jurihock/voicesmith project can be used separately as a virtual audio device
- Run voicesmith alongside Bluecord for system-wide voice effects
- Use apps like "Audio Router" to route Bluecord audio through voicesmith

## Credits

This feature was inspired by and references:
- **jurihock/voicesmith** - Open source Android voice transformer
  - GitHub: https://github.com/jurihock/voicesmith
  - Uses stftPitchShift for high-quality pitch/timbre shifting
  - Oboe for low-latency audio on Android

## Troubleshooting

### Effects Not Working
- Ensure "Enable Voice Effects" is toggled ON in settings
- Try restarting Bluecord after enabling effects
- Check that you have microphone permissions granted
- Verify you're recording voice messages (not using voice channels yet)

### Poor Quality
- Some effects may degrade audio quality, especially extreme pitch changes
- Try different effects to find one that works best for your voice
- Ensure good microphone input quality

### Performance Issues
- If experiencing lag, try disabling complex effects like Echo or Robot
- Close other apps to free up CPU resources
- Some older devices may struggle with real-time processing

## Privacy & Security

- All voice processing happens locally on your device
- No audio data is sent to external servers for processing
- Settings are stored locally in SharedPreferences
- Processed voice messages are uploaded to Discord as normal

## License

This feature is part of Bluecord and follows the project's license.
Voice processing algorithms are open source implementations.
