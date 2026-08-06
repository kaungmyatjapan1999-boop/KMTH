package com.example.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KmthDatabase
import com.example.data.remote.IpCheckResponse
import com.example.data.remote.SpeedTestResultDto
import com.example.data.repository.ServerSyncState
import com.example.data.repository.VpnRepository
import com.example.domain.model.ConnectionSessionLog
import com.example.domain.model.ConnectionState
import com.example.domain.model.SecuritySettings
import com.example.domain.model.ServerCategory
import com.example.domain.model.TrafficStats
import com.example.domain.model.VpnProtocol
import com.example.domain.model.VpnServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KmthDatabase.getDatabase(application)
    private val repository = VpnRepository(db.vpnDao())

    private val _serverSyncState = MutableStateFlow<ServerSyncState>(ServerSyncState.Idle)
    val serverSyncState: StateFlow<ServerSyncState> = _serverSyncState.asStateFlow()

    val allServers: StateFlow<List<VpnServer>> = repository.getAllServers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sessionLogs: StateFlow<List<ConnectionSessionLog>> = repository.getConnectionLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ServerCategory.ALL)
    val selectedCategory: StateFlow<ServerCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<Float>>(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val speedHistory: StateFlow<List<Float>> = _speedHistory.asStateFlow()

    private val _connectionTimerSeconds = MutableStateFlow(0L)
    val connectionTimerSeconds: StateFlow<Long> = _connectionTimerSeconds.asStateFlow()

    private val _securitySettings = MutableStateFlow(SecuritySettings())
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()

    private val _ipCheckResponse = MutableStateFlow(
        IpCheckResponse(
            ip = "198.51.100.14",
            country = "Unprotected Node",
            city = "Public Connection",
            org = "Cellular Broadband",
            secure = false
        )
    )
    val ipCheckResponse: StateFlow<IpCheckResponse> = _ipCheckResponse.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<SpeedTestResultDto?>(null)
    val diagnosticResult: StateFlow<SpeedTestResultDto?> = _diagnosticResult.asStateFlow()

    private val _isTestingDiagnostics = MutableStateFlow(false)
    val isTestingDiagnostics: StateFlow<Boolean> = _isTestingDiagnostics.asStateFlow()

    private var connectionJob: Job? = null
    private var tickerJob: Job? = null
    private var sessionStartTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            refreshServers()
            // Observe servers and set initial selected server once loaded
            allServers.collect { list ->
                if (_selectedServer.value == null && list.isNotEmpty()) {
                    _selectedServer.value = list.firstOrNull { it.id == "us_ny_01" } ?: list.first()
                }
            }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            _serverSyncState.value = ServerSyncState.Syncing
            val state = repository.fetchAndSyncServers(getApplication())
            _serverSyncState.value = state
        }
    }

    fun toggleConnection() {
        when (_connectionState.value) {
            ConnectionState.DISCONNECTED -> startConnectionFlow()
            ConnectionState.CONNECTED -> stopConnectionFlow()
            ConnectionState.CONNECTING -> stopConnectionFlow()
            else -> {}
        }
    }

    private fun startConnectionFlow() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            _connectionState.value = ConnectionState.CONNECTING

            val currentServer = _selectedServer.value
            val security = _securitySettings.value

            // Launch KmthVpnService with VLESS config and Kill Switch settings
            val context = getApplication<Application>()
            val intent = Intent(context, com.example.service.KmthVpnService::class.java).apply {
                action = com.example.service.KmthVpnService.ACTION_CONNECT
                putExtra(com.example.service.KmthVpnService.EXTRA_SERVER_NAME, currentServer?.cityName ?: "KMTH Shield")
                putExtra(com.example.service.KmthVpnService.EXTRA_SERVER_IP, currentServer?.ipAddress ?: "1.1.1.1")
                putExtra(com.example.service.KmthVpnService.EXTRA_VLESS_CONFIG, currentServer?.vlessConfig ?: "")
                putExtra(com.example.service.KmthVpnService.EXTRA_KILL_SWITCH, security.killSwitchEnabled)
                putExtra(com.example.service.KmthVpnService.EXTRA_DNS_PROTECTION, security.dnsLeakProtection)
                putExtra(com.example.service.KmthVpnService.EXTRA_SPLIT_TUNNELING, security.splitTunnelingEnabled)
                putExtra(com.example.service.KmthVpnService.EXTRA_PROTOCOL, security.selectedProtocol.displayName)
            }

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Simulate VLESS Reality handshake and key exchange
            delay(1200)

            _connectionState.value = ConnectionState.CONNECTED
            sessionStartTimestamp = System.currentTimeMillis()
            _connectionTimerSeconds.value = 0L

            _ipCheckResponse.value = repository.simulateIpLookup(currentServer)

            startTrafficTicker()
        }
    }

    private fun stopConnectionFlow() {
        tickerJob?.cancel()
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTING

            // Stop KmthVpnService
            val context = getApplication<Application>()
            val intent = Intent(context, com.example.service.KmthVpnService::class.java).apply {
                action = com.example.service.KmthVpnService.ACTION_DISCONNECT
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            delay(600)

            val durationSecs = _connectionTimerSeconds.value
            val currentServer = _selectedServer.value

            if (durationSecs > 0 && currentServer != null) {
                repository.logSession(
                    ConnectionSessionLog(
                        serverName = "${currentServer.countryName} (${currentServer.cityName})",
                        countryCode = currentServer.countryCode,
                        connectedAtTimestamp = sessionStartTimestamp,
                        durationSeconds = durationSecs,
                        bytesDownloaded = _trafficStats.value.totalBytesReceived,
                        bytesUploaded = _trafficStats.value.totalBytesSent,
                        protocolUsed = _securitySettings.value.selectedProtocol.displayName
                    )
                )
            }

            _connectionState.value = ConnectionState.DISCONNECTED
            _trafficStats.value = TrafficStats()
            _connectionTimerSeconds.value = 0L
            _speedHistory.value = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            _ipCheckResponse.value = repository.simulateIpLookup(null)
        }
    }

    private fun startTrafficTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            var totalDownBytes = 0L
            var totalUpBytes = 0L

            while (_connectionState.value == ConnectionState.CONNECTED) {
                delay(1000)
                _connectionTimerSeconds.value += 1

                val activeServer = _selectedServer.value
                val basePing = activeServer?.pingMs ?: 30
                val protocolMultiplier = when (_securitySettings.value.selectedProtocol) {
                    VpnProtocol.KMTH_SPEED -> 1.4f
                    VpnProtocol.WIREGUARD -> 1.25f
                    VpnProtocol.OPENVPN_UDP -> 1.0f
                    VpnProtocol.OPENVPN_TCP -> 0.85f
                }

                // Simulate active network traffic fluctuations
                val liveDownMbps = ((80f + (100 - basePing).coerceAtLeast(10)) * protocolMultiplier * (0.85f + Math.random() * 0.3f)).toFloat()
                val liveUpMbps = ((40f + (80 - basePing).coerceAtLeast(5)) * protocolMultiplier * (0.85f + Math.random() * 0.3f)).toFloat()

                val tickDownBytes = (liveDownMbps * 1024 * 1024 / 8).toLong()
                val tickUpBytes = (liveUpMbps * 1024 * 1024 / 8).toLong()

                totalDownBytes += tickDownBytes
                totalUpBytes += tickUpBytes

                _trafficStats.value = TrafficStats(
                    downloadSpeedKbps = liveDownMbps,
                    uploadSpeedKbps = liveUpMbps,
                    totalBytesReceived = totalDownBytes,
                    totalBytesSent = totalUpBytes
                )

                // Push speed history point
                val currentHistory = _speedHistory.value.toMutableList()
                if (currentHistory.size >= 10) currentHistory.removeAt(0)
                currentHistory.add(liveDownMbps)
                _speedHistory.value = currentHistory
            }
        }
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
        if (_connectionState.value == ConnectionState.CONNECTED) {
            // Reconnect to new server
            startConnectionFlow()
        }
    }

    fun toggleFavorite(server: VpnServer) {
        viewModelScope.launch {
            repository.toggleFavorite(server.id, !server.isFavorite)
        }
    }

    fun setSelectedCategory(category: ServerCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProtocol(protocol: VpnProtocol) {
        _securitySettings.value = _securitySettings.value.copy(selectedProtocol = protocol)
    }

    fun toggleKillSwitch(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(killSwitchEnabled = enabled)
    }

    fun toggleAutoConnect(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(autoConnectOnWifi = enabled)
    }

    fun toggleDnsLeakProtection(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(dnsLeakProtection = enabled)
    }

    fun toggleSplitTunneling(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(splitTunnelingEnabled = enabled)
    }

    fun runDiagnostics() {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            _isTestingDiagnostics.value = true
            _diagnosticResult.value = null
            delay(1800)
            _diagnosticResult.value = repository.runSpeedDiagnostics(server, _securitySettings.value.selectedProtocol)
            _isTestingDiagnostics.value = false
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
