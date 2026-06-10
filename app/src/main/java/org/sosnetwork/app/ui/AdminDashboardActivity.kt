package org.sosnetwork.app.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.data.local.VerificationRequestEntity
import org.sosnetwork.app.databinding.ActivityAdminBinding
import org.sosnetwork.protocol.AttestationType
import org.sosnetwork.protocol.CryptoUtils

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private val container by lazy { (application as SosApplication).container }
    private lateinit var adapter: VerificationRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = VerificationRequestAdapter(
            onVerifyId = { req -> confirmAndVerify(req, physical = false) },
            onVerifyPhysical = { req -> confirmAndVerify(req, physical = true) },
            onViewDocument = { req -> viewDocument(req) },
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.btnEnableAdmin.setOnClickListener {
            lifecycleScope.launch {
                val peerId = container.identityRepository.getIdentity()?.peerId ?: return@launch
                container.identityRepository.promoteToAdmin(peerId)
                Toast.makeText(this@AdminDashboardActivity, R.string.admin_enabled, Toast.LENGTH_SHORT).show()
                refreshAdminUi()
            }
        }

        lifecycleScope.launch {
            container.verificationRepository.observePending().collect { list ->
                adapter.submitList(list)
                binding.emptyText.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch { refreshAdminUi() }
    }

    private suspend fun refreshAdminUi() {
        val identity = container.identityRepository.getIdentity() ?: run {
            finish()
            return
        }
        val isAdmin = identity.isAdmin
        binding.btnEnableAdmin.visibility = if (isAdmin) View.GONE else View.VISIBLE
        binding.adminPanel.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.nonAdminHint.visibility = if (isAdmin) View.GONE else View.VISIBLE

        if (isAdmin) {
            container.identityRepository.addTrustedAdmin(identity.publicKeyBase64)
            binding.adminPublicKey.text = getString(
                R.string.admin_public_key_label,
                identity.publicKeyBase64,
            )
        }
    }

    private fun viewDocument(req: VerificationRequestEntity) {
        val path = req.encryptedDocumentPath
        if (path.isBlank()) {
            Toast.makeText(this, R.string.document_not_on_device, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            val bytes = container.verificationRepository.loadEncryptedDocument(path)
            if (bytes == null) {
                Toast.makeText(this@AdminDashboardActivity, R.string.document_load_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                Toast.makeText(this@AdminDashboardActivity, R.string.document_not_image, Toast.LENGTH_LONG).show()
                return@launch
            }
            val imageView = ImageView(this@AdminDashboardActivity).apply {
                setImageBitmap(bitmap)
                adjustViewBounds = true
            }
            AlertDialog.Builder(this@AdminDashboardActivity)
                .setTitle(req.displayName)
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun confirmAndVerify(req: VerificationRequestEntity, physical: Boolean) {
        val title = if (physical) R.string.confirm_physical else R.string.confirm_id
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(getString(R.string.verify_confirm_message, req.displayName))
            .setPositiveButton(R.string.confirm) { _, _ ->
                lifecycleScope.launch { performVerification(req, physical) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private suspend fun performVerification(req: VerificationRequestEntity, physical: Boolean) {
        val admin = container.identityRepository.getIdentity() ?: return
        if (!admin.isAdmin) {
            Toast.makeText(this, R.string.admin_only, Toast.LENGTH_SHORT).show()
            return
        }
        val privateKey = CryptoUtils.privateKeyFromBase64(admin.privateKeyBase64)

        val att = if (physical) {
            container.verificationRepository.adminVerifyPhysicalPresence(
                req.peerId,
                "Physically verified in person",
            )
            container.verificationRepository.createAdminAttestation(
                admin.peerId,
                admin.publicKeyBase64,
                privateKey,
                req.peerId,
                AttestationType.PHYSICAL_PRESENCE_VERIFIED,
                "In-person verification completed",
            )
        } else {
            container.verificationRepository.adminVerifyGovernmentId(
                req.peerId,
                "Government ID reviewed",
            )
            container.verificationRepository.createAdminAttestation(
                admin.peerId,
                admin.publicKeyBase64,
                privateKey,
                req.peerId,
                AttestationType.GOVERNMENT_ID_VERIFIED,
                "Document matches holder",
            )
        }
        container.meshCoordinator.broadcastAttestation(att)
        Toast.makeText(this, R.string.verification_complete, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
