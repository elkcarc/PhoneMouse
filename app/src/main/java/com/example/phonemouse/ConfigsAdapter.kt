package com.example.phonemouse

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.example.phonemouse.databinding.ItemConfigBinding

/** Manages the display and user interaction for the list of autoclicker profiles. */
class ConfigsAdapter(
    private var list: List<AutomationConfig>,
    private var selectedIndex: Int,
    private val onSelected: (Int) -> Unit,
    private val onDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<ConfigsAdapter.ViewHolder>() {

    /** Synchronizes the list content and selection state. */
    fun update(newList: List<AutomationConfig>, newSelected: Int) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = list[o].toJson() == newList[n].toJson()
            override fun areContentsTheSame(o: Int, n: Int) = list[o] == newList[n]
        })
        list = newList
        selectedIndex = newSelected
        diff.dispatchUpdatesTo(this)
        @SuppressLint("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemConfigBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(ItemConfigBinding.inflate(LayoutInflater.from(p.context), p, false))

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val cfg = list[pos]
        val ctx = h.itemView.context
        
        val tv = android.util.TypedValue()
        ctx.theme.resolveAttribute(R.attr.trackpadBackgroundColor, tv, true)
        h.binding.root.setBackgroundColor(if (pos == selectedIndex) tv.data else Color.TRANSPARENT)
        
        h.binding.configName.text = cfg.name
        h.binding.configText.text = ctx.getString(R.string.config_format, 
            cfg.minInterval.toString(), cfg.maxInterval.toString(), 
            cfg.minPressDuration.toString(), cfg.maxPressDuration.toString(), 
            cfg.minBreakDelay.toString(), cfg.maxBreakDelay.toString(), 
            cfg.delayFrequency.toString())
        
        h.binding.dragHandle.setOnTouchListener { v, e -> if (e.action == MotionEvent.ACTION_DOWN) { v.performClick(); onDrag(h) }; false }
        h.binding.root.setOnClickListener { onSelected(h.adapterPosition) }
    }

    override fun getItemCount() = list.size
}