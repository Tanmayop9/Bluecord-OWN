package mods.preference

import android.app.AlertDialog
import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import com.bluecord.R
import mods.audio.effects.VoiceEffect
import mods.audio.effects.VoiceEffectManager
import mods.utils.ToastUtil

/**
 * Preference for selecting and configuring voice effects
 */
class VoiceEffectPreference(context: Context, attrs: AttributeSet) :
    Preference(context, attrs), Preference.OnPreferenceClickListener {

    init {
        onPreferenceClickListener = this
        updateSummary()
    }

    private fun updateSummary() {
        summary = if (VoiceEffectManager.isEnabled()) {
            "Enabled: ${VoiceEffectManager.getCurrentEffect().displayName}"
        } else {
            "Disabled - Click to configure voice effects"
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        showVoiceEffectDialog()
        return true
    }

    private fun showVoiceEffectDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_voice_effect, null)
        
        val enableSwitch = dialogView.findViewById<Switch>(R.id.voice_effect_enable_switch)
        val effectGroup = dialogView.findViewById<RadioGroup>(R.id.voice_effect_radio_group)
        
        // Set current state
        enableSwitch?.isChecked = VoiceEffectManager.isEnabled()
        val isEnabled = enableSwitch?.isChecked ?: false
        
        // Create radio buttons for each effect
        val currentEffect = VoiceEffectManager.getCurrentEffect()
        VoiceEffect.values().forEach { effect ->
            val radioButton = RadioButton(context).apply {
                text = effect.displayName
                id = effect.ordinal
                isChecked = effect == currentEffect
                this.isEnabled = isEnabled
            }
            effectGroup?.addView(radioButton)
        }
        
        // Handle switch toggle
        enableSwitch?.setOnCheckedChangeListener { _, isChecked ->
            effectGroup?.let { group ->
                for (i in 0 until group.childCount) {
                    group.getChildAt(i)?.isEnabled = isChecked
                }
            }
        }
        
        AlertDialog.Builder(context)
            .setTitle("Voice Effect Settings")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val enabled = enableSwitch?.isChecked ?: false
                VoiceEffectManager.setEnabled(enabled)
                
                if (enabled) {
                    val selectedId = effectGroup?.checkedRadioButtonId ?: -1
                    if (selectedId != -1) {
                        val selectedEffect = VoiceEffect.fromOrdinal(selectedId)
                        VoiceEffectManager.setCurrentEffect(selectedEffect)
                        ToastUtil.toastShort("Voice effect: ${selectedEffect.displayName}")
                    }
                } else {
                    ToastUtil.toastShort("Voice effects disabled")
                }
                
                updateSummary()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
