package org.sosnetwork.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.data.local.ReceivedAlertEntity
import org.sosnetwork.app.databinding.ActivityAlertHistoryBinding

class AlertHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlertHistoryBinding
    private val container by lazy { (application as SosApplication).container }
    private lateinit var adapter: ReceivedAlertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ReceivedAlertAdapter { alert -> openAlert(alert) }
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            container.alertRepository.observeAlerts().collect { alerts ->
                adapter.submitList(alerts)
                binding.emptyText.visibility =
                    if (alerts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun openAlert(alert: ReceivedAlertEntity) {
        startActivity(
            Intent(this, SosAlertActivity::class.java).apply {
                putExtra(SosAlertActivity.EXTRA_ALERT_ID, alert.alertId)
                putExtra(SosAlertActivity.EXTRA_SENDER, alert.senderDisplayName)
                putExtra(SosAlertActivity.EXTRA_LAT, alert.latitude)
                putExtra(SosAlertActivity.EXTRA_LNG, alert.longitude)
                putExtra(SosAlertActivity.EXTRA_MESSAGE, alert.messageText)
                putExtra(SosAlertActivity.EXTRA_VOICE_PATH, alert.voiceNotePath)
                putExtra(SosAlertActivity.EXTRA_VERIFICATION, alert.verificationLevel)
            },
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
