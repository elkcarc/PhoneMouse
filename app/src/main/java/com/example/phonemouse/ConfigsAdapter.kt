package com.example.phonemouse

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.phonemouse.databinding.ItemConfigBinding

/**
 * Adapter for the list of automation variations in the side drawer.
 * Supports selection highlighting, deletion, and drag-to-reorder.
 */
class ConfigsAdapter(
    var currentList: List<String>,
    private var selectedIndex: Int,
    private val onConfigSelected: (Int) -> Unit,
    private val onConfigDeleted: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<ConfigsAdapter.ViewHolder>() {

    /**
     * Updates the highlighted item in the list.
     * @param newIndex The index of the item that should be marked as active.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateSelection(newIndex: Int) {
        if (selectedIndex != newIndex) {
            selectedIndex = newIndex
            // Use full reload as background colors of multiple items might change
            notifyDataSetChanged()
        }
    }

    /**
     * ViewHolder for automation variation rows.
     */
    class ViewHolder(val binding: ItemConfigBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = currentList[position]
        val context = holder.itemView.context

        // De-serialize config string for readable display
        val parts = config.split(",")
        val iMin = parts.getOrNull(0) ?: "0"
        val iMax = parts.getOrNull(1) ?: iMin
        val gMin = parts.getOrNull(2) ?: "0"
        val gMax = parts.getOrNull(3) ?: gMin
        val dMin = parts.getOrNull(4) ?: "0"
        val dMax = parts.getOrNull(5) ?: dMin
        val f = parts.getOrNull(6) ?: "0"

        // Highlight selected item using semantic theme attribute
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(R.attr.trackpadBackgroundColor, typedValue, true)
        val bgColor = if (position == selectedIndex) typedValue.data else Color.TRANSPARENT
        holder.binding.root.setBackgroundColor(bgColor)
        
        holder.binding.configText.text = context.getString(
            R.string.config_format, iMin, iMax, gMin, gMax, dMin, dMax, f,
        )

        // Delegate drag initiation to the ItemTouchHelper
        holder.binding.dragHandle.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.performClick()
                onStartDrag(holder)
            }
            false
        }

        holder.binding.root.setOnClickListener {
            onConfigSelected(holder.adapterPosition)
        }

        holder.binding.deleteBtn.setOnClickListener {
            onConfigDeleted(holder.adapterPosition)
        }
    }

    override fun getItemCount() = currentList.size
}