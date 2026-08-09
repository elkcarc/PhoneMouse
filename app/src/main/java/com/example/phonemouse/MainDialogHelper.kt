package com.example.phonemouse

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial

/** Helper class to manage complex dialogs for profile editing and recordings. */
class MainDialogHelper(private val context: Context, private val viewModel: MainViewModel) {
    private val inflater = LayoutInflater.from(context)

    /** Shows the unified add/edit dialog for autoclicker profiles. */
    fun showEditProfileDialog(index: Int?) {
        val cfg = index?.let { viewModel.uiState.value.configs.getOrNull(it) }
        val defaultName = if (index == null) viewModel.generateNextProfileName() else ""
        
        val dialogView = inflater.inflate(R.layout.dialog_edit_profile, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editName).apply { setText(cfg?.name ?: defaultName) }
        val minI = dialogView.findViewById<EditText>(R.id.editMinInt).apply { setText(cfg?.minInterval?.toString() ?: "100") }
        val maxI = dialogView.findViewById<EditText>(R.id.editMaxInt).apply { setText(cfg?.maxInterval?.toString() ?: "300") }
        val minP = dialogView.findViewById<EditText>(R.id.editMinPress).apply { setText(cfg?.minPressDuration?.toString() ?: "50") }
        val maxP = dialogView.findViewById<EditText>(R.id.editMaxPress).apply { setText(cfg?.maxPressDuration?.toString() ?: "150") }
        val minB = dialogView.findViewById<EditText>(R.id.editMinBreak).apply { setText(cfg?.minBreakDelay?.toString() ?: "3000") }
        val maxB = dialogView.findViewById<EditText>(R.id.editMaxBreak).apply { setText(cfg?.maxBreakDelay?.toString() ?: "60000") }
        val freq = dialogView.findViewById<EditText>(R.id.editFreq).apply { setText(cfg?.delayFrequency?.toString() ?: "500") }

        AlertDialog.Builder(context).setTitle(if (index == null) R.string.add_new_profile else R.string.edit_profile).setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString()
                val mi = minI.text.toString().toIntOrNull() ?: 100
                val ma = maxI.text.toString().toIntOrNull() ?: 300
                val mp = minP.text.toString().toIntOrNull() ?: 50
                val mpa = maxP.text.toString().toIntOrNull() ?: 150
                val mb = minB.text.toString().toIntOrNull() ?: 3000
                val mba = maxB.text.toString().toIntOrNull() ?: 60000
                val f = freq.text.toString().toIntOrNull() ?: 500
                
                if (index != null) {
                    viewModel.updateConfig(index, name, mi, ma, mp, mpa, mb, mba, f)
                } else {
                    viewModel.addConfig(name, mi, ma, mp, mpa, mb, mba, f)
                }
            }.setNegativeButton(R.string.cancel, null).show()
    }

    /** Shows the dialog for renaming a macro and toggling loop playback. */
    fun showEditRecordingDialog(index: Int) {
        val rec = viewModel.uiState.value.recordings.getOrNull(index) ?: return
        val dialogView = inflater.inflate(R.layout.dialog_edit_recording, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editRecName).apply { setText(rec.name) }
        val loopToggle = dialogView.findViewById<SwitchMaterial>(R.id.loopPlaybackToggle).apply { isChecked = rec.loopPlayback }

        AlertDialog.Builder(context).setTitle(R.string.rename_recording).setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.renameRecording(index, nameInput.text.toString())
                viewModel.updateRecordingLoop(index, loopToggle.isChecked)
            }.setNegativeButton(R.string.cancel, null).show()
    }

    /** Shows a deletion confirmation prompt. */
    fun confirmDelete(onConfirm: () -> Unit) {
        if (!viewModel.uiState.value.confirmDelete) return onConfirm()
        AlertDialog.Builder(context).setTitle(R.string.confirm_delete_title).setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }.setNegativeButton(R.string.cancel, null).show()
    }
}