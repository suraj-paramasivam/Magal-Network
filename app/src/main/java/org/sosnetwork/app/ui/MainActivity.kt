package org.sosnetwork.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.databinding.ActivityMainBinding
import org.sosnetwork.app.location.LocationHelper
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissions: PermissionHelper
    private var recorder: MediaRecorder? = null
    private var voiceFile: File? = null

    private val container by lazy { (application as SosApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissions = PermissionHelper(this)

        lifecycleScope.launch {
            if (!container.identityRepository.isRegistered()) {
                startActivity(android.content.Intent(this@MainActivity, RegistrationActivity::class.java))
                finish()
                return@launch
            }
            binding.statusText.text = getString(R.string.mesh_active_hint)
            permissions.requestAll { container.meshCoordinator.startMesh() }
        }

        binding.btnRecord.setOnClickListener { toggleVoiceRecording() }
        binding.btnSos.setOnClickListener { sendSos() }
        binding.btnVerifyId.setOnClickListener {
            startActivity(android.content.Intent(this, RegistrationActivity::class.java).apply {
                putExtra(RegistrationActivity.EXTRA_ID_ONLY, true)
            })
        }
        binding.btnAdmin.setOnClickListener {
            startActivity(android.content.Intent(this, AdminDashboardActivity::class.java))
        }
        binding.btnHistory.setOnClickListener {
            startActivity(android.content.Intent(this, AlertHistoryActivity::class.java))
        }

        lifecycleScope.launch {
            container.identityRepository.identityFlow.collect { identity ->
                binding.verificationBadge.text =
                    identity?.verificationLevel?.replace('_', ' ') ?: ""
            }
        }
    }

    private fun sendSos() {
        permissions.requestAll {
            binding.btnSos.isEnabled = false
            binding.progressSos.isVisible = true
            lifecycleScope.launch {
                try {
                    val location = LocationHelper.getCurrentLocation(this@MainActivity)
                    val message = binding.inputMessage.text?.toString()
                    val voiceBytes = voiceFile?.takeIf { it.exists() }?.readBytes()
                    container.meshCoordinator.broadcastSos(location, message, voiceBytes)
                    Toast.makeText(this@MainActivity, R.string.sos_sent, Toast.LENGTH_LONG).show()
                    voiceFile?.delete()
                    voiceFile = null
                    binding.inputMessage.text?.clear()
                    binding.btnRecord.text = getString(R.string.record_voice)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.sos_failed, e.message ?: "error"),
                        Toast.LENGTH_LONG,
                    ).show()
                } finally {
                    binding.btnSos.isEnabled = true
                    binding.progressSos.isVisible = false
                }
            }
        }
    }

    private fun toggleVoiceRecording() {
        if (recorder != null) {
            stopRecording()
            binding.btnRecord.text = getString(R.string.record_voice)
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.requestAll { toggleVoiceRecording() }
            return
        }
        try {
            val file = File(cacheDir, "sos_voice_${System.currentTimeMillis()}.m4a")
            voiceFile = file
            recorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            binding.btnRecord.text = getString(R.string.stop_recording)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.record_failed, e.message), Toast.LENGTH_SHORT).show()
            stopRecording()
        }
    }

    private fun stopRecording() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
