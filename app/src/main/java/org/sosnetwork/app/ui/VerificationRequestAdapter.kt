package org.sosnetwork.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.sosnetwork.app.data.local.VerificationRequestEntity
import org.sosnetwork.app.databinding.ItemVerificationRequestBinding

class VerificationRequestAdapter(
    private val onVerifyId: (VerificationRequestEntity) -> Unit,
    private val onVerifyPhysical: (VerificationRequestEntity) -> Unit,
    private val onViewDocument: (VerificationRequestEntity) -> Unit,
) : ListAdapter<VerificationRequestEntity, VerificationRequestAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVerificationRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemVerificationRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VerificationRequestEntity) {
            binding.name.text = item.displayName
            binding.meta.text = "${item.idDocumentType} · ${item.governmentIdHash.take(16)}…"
            binding.btnVerifyId.isEnabled = !item.idVerified
            binding.btnVerifyPhysical.isEnabled = !item.physicallyVerified
            binding.btnVerifyId.setOnClickListener { onVerifyId(item) }
            binding.btnVerifyPhysical.setOnClickListener { onVerifyPhysical(item) }
            binding.btnViewDocument.visibility =
                if (item.encryptedDocumentPath.isNotBlank()) View.VISIBLE else View.GONE
            binding.btnViewDocument.setOnClickListener { onViewDocument(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VerificationRequestEntity>() {
            override fun areItemsTheSame(a: VerificationRequestEntity, b: VerificationRequestEntity) =
                a.peerId == b.peerId

            override fun areContentsTheSame(a: VerificationRequestEntity, b: VerificationRequestEntity) =
                a == b
        }
    }
}
