package com.example.phonemouse

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.example.phonemouse.databinding.ItemConfigBinding

/** Manages the display and user interaction for the list of automation variations. */
class ConfigsAdapter(
    private var list: List<String>,
    private var selectedIndex: Int,
    private val onSelected: (Int) -> Unit,
    private val onDeleted: (Int) -> Unit,
    private val onDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<ConfigsAdapter.ViewHolder>() {

    /** Synchronizes the list content and selection state with minimal UI disruption. */
    fun update(newList: List<String>, newSelected: Int) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = list[o] == newList[n]
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

    /** Binds config data to the row UI and sets up drag/click/delete listeners. */
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val p = list[pos].split(",")
        val tv = android.util.TypedValue()
        h.itemView.context.theme.resolveAttribute(R.attr.trackpadBackgroundColor, tv, true)
        h.binding.root.setBackgroundColor(if (pos == selectedIndex) tv.data else Color.TRANSPARENT)
        h.binding.configText.text = h.itemView.context.getString(R.string.config_format, p[0], p.getOrElse(1){p[0]}, p[2], p.getOrElse(3){p[2]}, p[4], p.getOrElse(5){p[4]}, p[6])
        h.binding.dragHandle.setOnTouchListener { v, e -> if (e.action == MotionEvent.ACTION_DOWN) { v.performClick(); onDrag(h) }; false }
        h.binding.root.setOnClickListener { onSelected(h.adapterPosition) }
        h.binding.deleteBtn.setOnClickListener { onDeleted(h.adapterPosition) }
    }

    override fun getItemCount() = list.size
}