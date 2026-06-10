package org.sosnetwork.app.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.databinding.ActivitySosAlertBinding
import java.io.File

class SosAlertActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySosAlertBinding
    private var mediaPlayer: MediaPlayer? = null
    private var alertId: String = ""
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    private val container by lazy { (application as SosApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alertId = intent.getStringExtra(EXTRA_ALERT_ID).orEmpty()

        lifecycleScope.launch {
            val fromDb = if (alertId.isNotEmpty()) container.alertRepository.getAlert(alertId) else null
            val sender = intent.getStringExtra(EXTRA_SENDER)
                ?: fromDb?.senderDisplayName
                ?: getString(R.string.unknown_sender)
            lat = if (intent.hasExtra(EXTRA_LAT)) intent.getDoubleExtra(EXTRA_LAT, 0.0)
            else fromDb?.latitude ?: 0.0
            lng = if (intent.hasExtra(EXTRA_LNG)) intent.getDoubleExtra(EXTRA_LNG, 0.0)
            else fromDb?.longitude ?: 0.0
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: fromDb?.messageText
            val voicePath = intent.getStringExtra(EXTRA_VOICE_PATH) ?: fromDb?.voiceNotePath
            val verification = intent.getStringExtra(EXTRA_VERIFICATION)
                ?: fromDb?.verificationLevel

            bindUi(sender, lat, lng, message, voicePath, verification)
        }

        binding.btnDismiss.setOnClickListener { finish() }
    }

    private fun bindUi(
        sender: String,
        lat: Double,
        lng: Double,
        message: String?,
        voicePath: String?,
        verification: String?,
    ) {
        binding.alertTitle.text = getString(R.string.incoming_sos_title)
        binding.sender.text = sender
        binding.location.text = String.format("%.5f, %.5f", lat, lng)
        binding.message.text = message?.takeIf { it.isNotBlank() }
            ?: getString(R.string.no_message)
        binding.verification.text = verification?.replace('_', ' ') ?: ""

        val file = voicePath?.let { File(it) }?.takeIf { it.exists() }
        binding.btnPlayVoice.isVisible = file != null
        binding.btnPlayVoice.setOnClickListener {
            file ?: return@setOnClickListener
            stopVoice()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopVoice() }
            }
        }

        binding.btnMaps.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(SOS)")))
        }

        binding.btnRespond.setOnClickListener {
            lifecycleScope.launch {
                container.meshCoordinator.sendSosAck(alertId, getString(R.string.responder_on_way))
                Toast.makeText(this@SosAlertActivity, R.string.ack_sent, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun stopVoice() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopVoice()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_VOICE_PATH = "voice_path"
        const val EXTRA_VERIFICATION = "verification"
    }
}
