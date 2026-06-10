package org.sosnetwork.app.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.databinding.ActivityRegistrationBinding
import org.sosnetwork.protocol.GovernmentIdType

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding
    private var documentBytes: ByteArray? = null

    private val container by lazy { (application as SosApplication).container }

    private val pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        contentResolver.openInputStream(uri)?.use { documentBytes = it.readBytes() }
        binding.idStatus.text = getString(R.string.id_document_selected)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idOnly = intent.getBooleanExtra(EXTRA_ID_ONLY, false)
        binding.registerSection.visibility = if (idOnly) android.view.View.GONE else android.view.View.VISIBLE

        binding.spinnerIdType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            GovernmentIdType.entries.map { it.name.replace('_', ' ') }
        )

        binding.btnPickId.setOnClickListener { pickDocument.launch(arrayOf("image/*", "application/pdf")) }

        binding.btnSubmit.setOnClickListener {
            lifecycleScope.launch { submit(idOnly) }
        }
    }

    private suspend fun submit(idOnly: Boolean) {
        try {
            var identity = container.identityRepository.getIdentity()
            if (!idOnly) {
                val name = binding.inputName.text?.toString()?.trim().orEmpty()
                if (name.length < 2) {
                    Toast.makeText(this, R.string.name_required, Toast.LENGTH_SHORT).show()
                    return
                }
                identity = container.identityRepository.register(name)
                container.meshCoordinator.startMesh()
                Toast.makeText(this, R.string.registered, Toast.LENGTH_SHORT).show()
            }
            identity ?: return

            val bytes = documentBytes
            if (bytes != null) {
                val type = GovernmentIdType.entries[binding.spinnerIdType.selectedItemPosition]
                val hash = container.verificationRepository.submitGovernmentId(
                    identity.peerId,
                    identity.displayName,
                    bytes,
                    type
                )
                container.identityRepository.setGovernmentId(identity.peerId, hash, type)
                container.meshCoordinator.startMesh()
                container.meshCoordinator.announceVerificationRequest(
                    identity.peerId, identity.displayName, hash, type,
                )
                Toast.makeText(this, R.string.id_submitted, Toast.LENGTH_LONG).show()
            }

            binding.inputAdminKey.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { key ->
                container.identityRepository.addTrustedAdmin(key)
            }

            finish()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Error", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_ID_ONLY = "id_only"
    }
}
