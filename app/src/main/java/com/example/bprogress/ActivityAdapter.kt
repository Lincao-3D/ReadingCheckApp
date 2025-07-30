// --- ActivityAdapter.kt ---
package com.example.bprogress

import android.content.res.Configuration
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bprogress.databinding.ItemActivityBinding

class ActivityAdapter(
    private val onDoubleClick: (ActivityItem) -> Unit,
    private val onLongClick: (ActivityItem) -> Unit,
    private val onSingleClickWarn: (ActivityItem) -> Unit
) : ListAdapter<ActivityItem, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) { // Still uses the nested class

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding, onDoubleClick, onLongClick, onSingleClickWarn)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = getItem(position)
        // It's good practice to also log item details if possible, or at least confirm item is not null
        Log.d("ActivityAdapter", "Binding item at position $position with ID: ${item?.id ?: "null item"}")
        item?.let { holder.bind(it) } // Defensive: only bind if item is not null
    }

    class ActivityViewHolder(
        private val binding: ItemActivityBinding,
        private val onDoubleClickCallback: (ActivityItem) -> Unit,
        private val onLongClickCallback: (ActivityItem) -> Unit,
        private val onSingleClickWarnCallback: (ActivityItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var lastClickTime: Long = 0
        private val doubleClickTimeDelta: Long = 300 // Consider making this a companion object const

        private var currentItem: ActivityItem? = null // Store the current item to avoid issues if item changes during click

        init {
            binding.activityItemLayout.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                currentItem?.let { item -> // Use stored currentItem
                    if (lastClickTime != 0L && (currentTime - lastClickTime < doubleClickTimeDelta)) {
                        Log.d("ActivityViewHolder", "Double-click on item ID: ${item.id}")
                        onDoubleClickCallback(item)
                        lastClickTime = 0 // Reset for next interaction
                    } else {
                        Log.d("ActivityViewHolder", "Single-click registered for item ID: ${item.id}. Warning will be shown.")
                        onSingleClickWarnCallback(item) // Fire the warning callback
                        lastClickTime = currentTime
                    }
                }
            }

            binding.activityItemLayout.setOnLongClickListener {
                currentItem?.let { item -> // Use stored currentItem
                    Log.d("ActivityViewHolder", "Long-click on item ID: ${item.id}")
                    onLongClickCallback(item)
                }
                true // Consume the long click
            }
        }

        fun bind(item: ActivityItem) {
            this.currentItem = item // Store the item
            Log.d("ActivityViewHolder", "Binding item ID: ${item.id}, isChecked: ${item.isChecked}, isImportant: ${item.isImportant}")

            binding.textViewColumn1.text = item.column1Data
            binding.textViewColumn2.text = item.column2Data
            binding.itemCheckbox.isChecked = item.isChecked // Directly set from item

            // No need to listen to checkbox changes here if the ViewModel handles the state update
            // and the list is resubmitted. The binding will update from the new item state.
            // binding.itemCheckbox.setOnCheckedChangeListener(null) // Clear previous listeners
            // binding.itemCheckbox.isChecked = item.isChecked // Already done above

            if (item.isChecked) {
                binding.textViewColumn1.paintFlags = binding.textViewColumn1.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textViewColumn2.paintFlags = binding.textViewColumn2.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewColumn1.paintFlags = binding.textViewColumn1.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textViewColumn2.paintFlags = binding.textViewColumn2.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            val context = binding.root.context
            // Consider moving color logic to a helper or constants if it gets complex
            val highlightColor = ContextCompat.getColor(context, R.color.yellow_marker)
            // It's safer to get the default color from a reliable source or define it explicitly
            // val defaultCardBackgroundColor = ContextCompat.getColor(context, R.color.your_default_card_color)
            // For simplicity, assuming cardViewActivity.cardBackgroundColor.defaultColor works as intended.
            val defaultCardBackgroundColor = binding.cardViewActivity.cardBackgroundColor.defaultColor


            val cardBgColor = if (item.isImportant) highlightColor else defaultCardBackgroundColor
            binding.cardViewActivity.setCardBackgroundColor(cardBgColor)
            Log.d("ActivityViewHolder", "Item ID: ${item.id}, isImportant: ${item.isImportant}, Card Color: $cardBgColor")
        }
    }

    // --- NESTED DiffUtil.ItemCallback ---
    private class ActivityDiffCallback : DiffUtil.ItemCallback<ActivityItem>() {
        override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            Log.d("DiffCallback", "areItemsTheSame: Old ID ${oldItem.id}, New ID ${newItem.id} -> ${oldItem.id == newItem.id}")
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            val same = oldItem == newItem
            Log.d("DiffCallback", "areContentsTheSame for ID ${oldItem.id}: Old $oldItem, New $newItem -> $same")
            // If 'same' is false, it's useful to log which specific fields differ if ActivityItem becomes complex.
            // For example: if (!same) Log.d("DiffCallback", "Differences for ID ${oldItem.id}: checked ${oldItem.isChecked} vs ${newItem.isChecked}, important ${oldItem.isImportant} vs ${newItem.isImportant}")
            return same
        }
    }
}
