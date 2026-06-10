package org.sosnetwork.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.sosnetwork.app.data.local.ReceivedAlertEntity
import org.sosnetwork.app.databinding.ItemReceivedAlertBinding
import java.text.DateFormat
import java.util.Date

class ReceivedAlertAdapter(
    private val onOpen: (ReceivedAlertEntity) -> Unit,
) : ListAdapter<ReceivedAlertEntity, ReceivedAlertAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReceivedAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemReceivedAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReceivedAlertEntity) {
            binding.title.text = item.senderDisplayName ?: item.senderPeerId.take(8)
            binding.subtitle.text = DateFormat.getDateTimeInstance().format(Date(item.receivedAtEpochMs))
            binding.acknowledged.isChecked = item.acknowledged
            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReceivedAlertEntity>() {
            override fun areItemsTheSame(a: ReceivedAlertEntity, b: ReceivedAlertEntity) =
                a.alertId == b.alertId

            override fun areContentsTheSame(a: ReceivedAlertEntity, b: ReceivedAlertEntity) =
                a == b
        }
    }
}
