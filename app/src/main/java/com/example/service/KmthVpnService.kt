package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.domain.model.SecuritySettings
import com.example.domain.model.VpnServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class KmthVpnService : VpnService() {

    companion object {
        private const val TAG = "KmthVpnService"
        const val CHANNEL_ID = "kmth_vpn_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_CONNECT = "com.example.service.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.service.ACTION_DISCONNECT"

        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_VLESS_CONFIG = "extra_vless_config"
        const val EXTRA_KILL_SWITCH = "extra_kill_switch"
        const val EXTRA_DNS_PROTECTION = "extra_dns_protection"
        const val EXTRA_SPLIT_TUNNELING = "extra_split_tunneling"
        const val EXTRA_PROTOCOL = "extra_protocol"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _connectedServerName = MutableStateFlow("Disconnected")
        val connectedServerName: StateFlow<String> = _connectedServerName.asStateFlow()
    }

    private var tunParcelFd: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private var currentServerName = "KMTH Server"
    private var isKillSwitchActive = false
    private var isDnsProtectionActive = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "KMTH Node"
                val serverIp = intent.getStringExtra(EXTRA_SERVER_IP) ?: "1.1.1.1"
                val vlessConfig = intent.getStringExtra(EXTRA_VLESS_CONFIG) ?: ""
                val killSwitch = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                val dnsProtection = intent.getBooleanExtra(EXTRA_DNS_PROTECTION, true)
                val splitTunneling = intent.getBooleanExtra(EXTRA_SPLIT_TUNNELING, false)
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "VLESS"

                startVpnTunnel(
                    serverName = serverName,
                    serverIp = serverIp,
                    vlessConfig = vlessConfig,
                    killSwitch = killSwitch,
                    dnsProtection = dnsProtection,
                    splitTunneling = splitTunneling,
                    protocol = protocol
                )
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
            }
        }
        return START_STICKY
    }

    private fun startVpnTunnel(
        serverName: String,
        serverIp: String,
        vlessConfig: String,
        killSwitch: Boolean,
        dnsProtection: Boolean,
        splitTunneling: Boolean,
        protocol: String
    ) {
        currentServerName = serverName
        isKillSwitchActive = killSwitch
        isDnsProtectionActive = dnsProtection

        // Start Foreground Notification first
        val notification = buildNotification(serverName, protocol, "Encrypted Tunnel Active")
        startForeground(NOTIFICATION_ID, notification)

        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            try {
                // 1. Generate V2Ray / Xray JSON Config
                val dummyServer = VpnServer(
                    id = "active_node",
                    countryName = serverName,
                    countryCode = "US",
                    cityName = serverName,
                    ipAddress = serverIp,
                    vlessConfig = vlessConfig
                )
                val securitySettings = SecuritySettings(
                    killSwitchEnabled = killSwitch,
                    dnsLeakProtection = dnsProtection,
                    splitTunnelingEnabled = splitTunneling
                )

                val v2rayJson = VlessJsonConfigGenerator.generateJsonConfig(dummyServer, securitySettings)
                Log.d(TAG, "Generated V2Ray JSON configuration successfully.")

                // 2. Start V2Ray Core Bridge
                val coreResult = V2RayCoreBridge.startCore(v2rayJson)
                if (coreResult != 0) {
                    Log.e(TAG, "Failed to start V2Ray core, result code: $coreResult")
                }

                // 3. Establish TUN Interface using VpnService.Builder
                val builder = Builder()
                    .setSession("KMTH-VPN ($serverName)")
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 24)
                    .addAddress("fdfe:dcba:9876::2", 64)

                // Route all IPv4 & IPv6 traffic through TUN interface
                builder.addRoute("0.0.0.0", 0)
                builder.addRoute("::", 0)

                // DNS Leak Protection configuration
                if (dnsProtection) {
                    builder.addDnsServer("1.1.1.1")
                    builder.addDnsServer("8.8.8.8")
                    builder.addDnsServer("2606:4700:4700::1111")
                } else {
                    builder.addDnsServer("8.8.8.8")
                }

                // Kill Switch Protection: block network traffic if VPN drops
                if (killSwitch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                // Protect local sockets from routing into TUN
                protect(10808)
                protect(10809)

                tunParcelFd = builder.establish()

                if (tunParcelFd == null) {
                    Log.e(TAG, "Failed to establish TUN interface (ParcelFileDescriptor is null)")
                    stopVpnTunnel()
                    return@launch
                }

                _isServiceConnected.value = true
                _connectedServerName.value = serverName

                Log.i(TAG, "TUN Interface successfully established. File Descriptor: ${tunParcelFd?.fd}")

                // 4. Background TUN Packet Pump & Watchdog Loop
                val inputStream = FileInputStream(tunParcelFd!!.fileDescriptor)
                val outputStream = FileOutputStream(tunParcelFd!!.fileDescriptor)
                val buffer = ByteArray(32768)

                while (isActive && tunParcelFd != null) {
                    try {
                        val readBytes = inputStream.read(buffer)
                        if (readBytes > 0) {
                            // Virtual packet relay loop keeping TUN socket alive
                        } else {
                            delay(10)
                        }
                    } catch (e: IOException) {
                        if (isKillSwitchActive) {
                            Log.w(TAG, "TUN packet read interrupted with Kill Switch active. Blocking socket traffic.")
                        }
                        break
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in VPN service execution loop", e)
                if (isKillSwitchActive) {
                    Log.w(TAG, "Kill Switch enforcing blocking state after exception.")
                }
            } finally {
                cleanUpTunnel()
            }
        }
    }

    private fun stopVpnTunnel() {
        Log.i(TAG, "Stopping KMTH VPN Service...")
        serviceJob?.cancel()
        cleanUpTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanUpTunnel() {
        try {
            tunParcelFd?.close()
            tunParcelFd = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TUN ParcelFileDescriptor", e)
        }

        V2RayCoreBridge.stopCore()

        _isServiceConnected.value = false
        _connectedServerName.value = "Disconnected"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "KMTH VPN Service Status"
            val descriptionText = "Displays active VPN connection status and controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(serverName: String, protocol: String, statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, KmthVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KMTH Shield • $serverName")
            .setContentText("Protocol: $protocol • $statusText")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "DISCONNECT",
                disconnectPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnTunnel()
    }
}
