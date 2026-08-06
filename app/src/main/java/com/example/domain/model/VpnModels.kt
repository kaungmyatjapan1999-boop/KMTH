package com.example.domain.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    RECONNECTING
}

enum class VpnProtocol(val displayName: String, val description: String) {
    OPENVPN_UDP("OpenVPN (UDP)", "High speed & low latency for gaming/streaming"),
    OPENVPN_TCP("OpenVPN (TCP)", "Ultra-reliable for strict firewall bypass"),
    WIREGUARD("WireGuard®", "Next-gen lightweight crypto protocol"),
    KMTH_SPEED("KMTH-Turbo", "Proprietary multi-path obfuscated protocol")
}

enum class ServerCategory(val label: String) {
    ALL("All Servers"),
    FASTEST("Fastest"),
    GAMING("Gaming (Low Ping)"),
    STREAMING("Streaming 4K"),
    P2P("P2P / Torrenting"),
    FAVORITES("Favorites")
}

data class VpnServer(
    val id: String,
    val countryName: String,
    val countryCode: String,
    val cityName: String,
    val ipAddress: String,
    val flagEmoji: String,
    val pingMs: Int,
    val serverLoadPercentage: Int,
    val isPremium: Boolean = false,
    val isFavorite: Boolean = false,
    val category: ServerCategory = ServerCategory.FASTEST,
    val availableProtocols: List<VpnProtocol> = VpnProtocol.values().toList()
)

data class TrafficStats(
    val uploadSpeedKbps: Float = 0f,
    val downloadSpeedKbps: Float = 0f,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L
)

data class SecuritySettings(
    val killSwitchEnabled: Boolean = true,
    val autoConnectOnWifi: Boolean = true,
    val dnsLeakProtection: Boolean = true,
    val splitTunnelingEnabled: Boolean = false,
    val bypassApps: List<String> = listOf("com.google.android.youtube", "com.spotify.music"),
    val selectedProtocol: VpnProtocol = VpnProtocol.OPENVPN_UDP
)

data class ConnectionSessionLog(
    val id: Long = 0,
    val serverName: String,
    val countryCode: String,
    val connectedAtTimestamp: Long,
    val durationSeconds: Long,
    val bytesDownloaded: Long,
    val bytesUploaded: Long,
    val protocolUsed: String
)
