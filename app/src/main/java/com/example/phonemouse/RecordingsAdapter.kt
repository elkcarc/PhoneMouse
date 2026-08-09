package com.example.phonemouse

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.format.DateFormat
import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.phonemouse.databinding.ItemRecordingBinding
import java.util.Locale
import com.google.android.material.R as MaterialR

/** Manages the display and user interaction for the list of input recordings. */
class RecordingsAdapter(
    private var list: List<InputRecording>,
    private var selectedIndex: Int,
    private val onClick: (Int) -> Unit,
    private val onDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    /** Synchronizes the list content and selection state. */
    fun update(newList: List<InputRecording>, newSelected: Int) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = list[o].timestamp == newList[n].timestamp
            override fun areContentsTheSame(o: Int, n: Int) = list[o] == newList[n]
        })
        list = newList
        selectedIndex = newSelected
        diff.dispatchUpdatesTo(this)
        @SuppressLint("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemRecordingBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(ItemRecordingBinding.inflate(LayoutInflater.from(p.context), p, false))

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val rec = list[pos]
        val ctx = h.itemView.context
        
        val tvTrackpad = android.util.TypedValue()
        ctx.theme.resolveAttribute(R.attr.trackpadBackgroundColor, tvTrackpad, true)
        
        val tvPrimary = android.util.TypedValue()
        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, tvPrimary, true)
        
        val tvOnPrimary = android.util.TypedValue()
        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, tvOnPrimary, true)
        
        val tvTextPrimary = android.util.TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, tvTextPrimary, true)

        val isSelected = pos == selectedIndex
        
        // Background and Stroke
        h.binding.root.setCardBackgroundColor(if (isSelected) tvPrimary.data else tvTrackpad.data)
        h.binding.root.strokeColor = if (isSelected) tvPrimary.data else tvTrackpad.data
        h.binding.root.strokeWidth = if (isSelected) 6 else 2

        // Text Colors
        val contentColor = if (isSelected) tvOnPrimary.data else tvTextPrimary.data
        h.binding.recordingName.setTextColor(contentColor)
        h.binding.recordingDetails.setTextColor(contentColor)
        h.binding.recordingDetails.alpha = if (isSelected) 0.9f else 0.7f
        
        // Icon Tints
        h.binding.dragHandle.imageTintList = android.content.res.ColorStateList.valueOf(contentColor)
        
        h.binding.recordingName.text = rec.name
        
        val date = DateFormat.format("MM/dd HH:mm", rec.timestamp).toString()
        val duration = String.format(Locale.US, "%.1fs", rec.durationMs / 1000f)
        h.binding.recordingDetails.text = ctx.getString(R.string.recording_details, date, duration, rec.clickCount)

        h.binding.dragHandle.setOnTouchListener { v, e -> if (e.action == MotionEvent.ACTION_DOWN) { v.performClick(); onDrag(h) }; false }
        h.binding.root.setOnClickListener { onClick(h.adapterPosition) }
    }

    override fun getItemCount() = list.size
}