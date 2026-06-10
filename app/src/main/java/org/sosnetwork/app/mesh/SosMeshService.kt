package org.sosnetwork.app.mesh

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.ui.MainActivity
import java.util.concurrent.ConcurrentHashMap

/**
 * Decentralized mesh using Google Nearby Connections (P2P over Bluetooth/Wi-Fi).
 * Alerts propagate hop-by-hop per SOS-DP without a central server.
 */
class SosMeshService : Service() {

    private lateinit var connectionsClient: ConnectionsClient
    private val connectedEndpoints = ConcurrentHashMap.newKeySet<String>()
    private var coordinator: SosMeshCoordinator? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            payload.asBytes()?.let { bytes ->
                coordinator?.onPayloadReceived(bytes) ?: pendingInbound.add(bytes)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpoints.add(endpointId)
                flushPending()
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(localName, endpointId, lifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        connectionsClient = Nearby.getConnectionsClient(this)
        coordinator = (application as SosApplication).container.meshCoordinator
        coordinator?.meshService = this
        startForeground(NOTIFICATION_ID, buildNotification())
        startMesh()
        coordinator?.onMeshStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pendingBroadcast?.let {
            broadcast(it)
            pendingBroadcast = null
        }
        flushPending()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        coordinator?.meshService = null
        super.onDestroy()
    }

    private fun startMesh() {
        val strategy = Strategy.P2P_CLUSTER
        connectionsClient.startAdvertising(
            localName,
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(strategy).build(),
        ).addOnSuccessListener {
            connectionsClient.startDiscovery(
                SERVICE_ID,
                discoveryCallback,
                DiscoveryOptions.Builder().setStrategy(strategy).build(),
            )
        }
    }

    fun broadcast(bytes: ByteArray) {
        if (connectedEndpoints.isEmpty()) {
            pendingBroadcast = bytes
            return
        }
        sendToAll(bytes)
    }

    fun relay(messageId: String, bytes: ByteArray) {
        if (!relayedIds.add(messageId)) return
        sendToAll(bytes)
    }

    private fun sendToAll(bytes: ByteArray) {
        if (connectedEndpoints.isEmpty()) return
        val payload = Payload.fromBytes(bytes)
        connectedEndpoints.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private fun flushPending() {
        if (connectedEndpoints.isEmpty()) return
        val inbound = pendingInbound.toList()
        pendingInbound.clear()
        inbound.forEach { coordinator?.onPayloadReceived(it) }
        pendingBroadcast?.let {
            sendToAll(it)
            pendingBroadcast = null
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, SosApplication.CHANNEL_MESH)
            .setContentTitle(getString(R.string.mesh_notification_title))
            .setContentText(getString(R.string.mesh_notification_body))
            .setSmallIcon(R.drawable.ic_sos)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val SERVICE_ID = "org.sosnetwork.app.SOS_DP_V1"
        private const val NOTIFICATION_ID = 1001
        private val localName = "SOS-${android.os.Build.MODEL.take(12)}"
        var pendingBroadcast: ByteArray? = null
        private val pendingInbound = mutableListOf<ByteArray>()
        private val relayedIds = ConcurrentHashMap.newKeySet<String>()
    }
}
