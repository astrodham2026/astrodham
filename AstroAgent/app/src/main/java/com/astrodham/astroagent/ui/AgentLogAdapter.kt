package com.astrodham.astroagent.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.astrodham.astroagent.R
import com.astrodham.astroagent.util.Logger

/**
 * RecyclerView adapter for displaying agent log entries in real time.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class AgentLogAdapter : ListAdapter<Logger.LogEntry, AgentLogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLevel: TextView = itemView.findViewById(R.id.tvLogLevel)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvLogTimestamp)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvLogMessage)

        fun bind(entry: Logger.LogEntry) {
            tvLevel.text = entry.level.name
            tvTimestamp.text = entry.formattedTime
            tvMessage.text = entry.message

            // Color-code by log level
            val levelColor = when (entry.level) {
                Logger.Level.DEBUG -> R.color.log_debug
                Logger.Level.INFO -> R.color.log_info
                Logger.Level.WARN -> R.color.log_warn
                Logger.Level.ERROR -> R.color.log_error
            }
            tvLevel.setTextColor(ContextCompat.getColor(itemView.context, levelColor))
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<Logger.LogEntry>() {
        override fun areItemsTheSame(oldItem: Logger.LogEntry, newItem: Logger.LogEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.message == newItem.message
        }

        override fun areContentsTheSame(oldItem: Logger.LogEntry, newItem: Logger.LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
