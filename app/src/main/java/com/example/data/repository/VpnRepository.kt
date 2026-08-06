package com.example.data.repository

import com.example.data.local.ConnectionLogEntity
import com.example.data.local.VpnDao
import com.example.data.local.VpnServerEntity
import com.example.data.remote.IpCheckResponse
import com.example.data.remote.SpeedTestResultDto
import com.example.data.remote.VpnApiService
import com.example.domain.model.ConnectionSessionLog
import com.example.domain.model.ServerCategory
import com.example.domain.model.VpnProtocol
import com.example.domain.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VpnRepository(
    private val vpnDao: VpnDao,
    private val apiService: VpnApiService? = null
) {

    // Seed default servers if empty
    suspend fun seedInitialServersIfEmpty() = withContext(Dispatchers.IO) {
        val defaultList = listOf(
            VpnServer(
                id = "us_ny_01",
                countryName = "United States",
                countryCode = "US",
                cityName = "New York (East Coast)",
                ipAddress = "104.28.19.42",
                flagEmoji = "🇺🇸",
                pingMs = 24,
                serverLoadPercentage = 38,
                isPremium = false,
                isFavorite = true,
                category = ServerCategory.FASTEST
            ),
            VpnServer(
                id = "us_la_02",
                countryName = "United States",
                countryCode = "US",
                cityName = "Los Angeles (West)",
                ipAddress = "192.241.182.90",
                flagEmoji = "🇺🇸",
                pingMs = 32,
                serverLoadPercentage = 45,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.GAMING
            ),
            VpnServer(
                id = "jp_tokyo_01",
                countryName = "Japan",
                countryCode = "JP",
                cityName = "Tokyo (Ultra-Speed)",
                ipAddress = "103.15.22.101",
                flagEmoji = "🇯🇵",
                pingMs = 18,
                serverLoadPercentage = 22,
                isPremium = false,
                isFavorite = true,
                category = ServerCategory.FASTEST
            ),
            VpnServer(
                id = "sg_singapore_01",
                countryName = "Singapore",
                countryCode = "SG",
                cityName = "Jurong West",
                ipAddress = "128.199.201.8",
                flagEmoji = "🇸🇬",
                pingMs = 12,
                serverLoadPercentage = 28,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.FASTEST
            ),
            VpnServer(
                id = "de_frankfurt_01",
                countryName = "Germany",
                countryCode = "DE",
                cityName = "Frankfurt (Core)",
                ipAddress = "46.101.112.50",
                flagEmoji = "🇩🇪",
                pingMs = 29,
                serverLoadPercentage = 51,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.STREAMING
            ),
            VpnServer(
                id = "uk_london_01",
                countryName = "United Kingdom",
                countryCode = "GB",
                cityName = "London (Docklands)",
                ipAddress = "178.62.14.99",
                flagEmoji = "🇬🇧",
                pingMs = 35,
                serverLoadPercentage = 62,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.STREAMING
            ),
            VpnServer(
                id = "nl_amsterdam_01",
                countryName = "Netherlands",
                countryCode = "NL",
                cityName = "Amsterdam (P2P Mesh)",
                ipAddress = "188.166.42.12",
                flagEmoji = "🇳🇱",
                pingMs = 31,
                serverLoadPercentage = 41,
                isPremium = false,
                isFavorite = true,
                category = ServerCategory.P2P
            ),
            VpnServer(
                id = "kr_seoul_01",
                countryName = "South Korea",
                countryCode = "KR",
                cityName = "Seoul (Gangnam Node)",
                ipAddress = "211.233.15.80",
                flagEmoji = "🇰🇷",
                pingMs = 21,
                serverLoadPercentage = 30,
                isPremium = true,
                isFavorite = false,
                category = ServerCategory.GAMING
            ),
            VpnServer(
                id = "ca_toronto_01",
                countryName = "Canada",
                countryCode = "CA",
                cityName = "Toronto Central",
                ipAddress = "159.203.45.190",
                flagEmoji = "🇨🇦",
                pingMs = 42,
                serverLoadPercentage = 33,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.FASTEST
            ),
            VpnServer(
                id = "au_sydney_01",
                countryName = "Australia",
                countryCode = "AU",
                cityName = "Sydney (Coastal)",
                ipAddress = "139.59.250.11",
                flagEmoji = "🇦🇺",
                pingMs = 88,
                serverLoadPercentage = 49,
                isPremium = true,
                isFavorite = false,
                category = ServerCategory.STREAMING
            ),
            VpnServer(
                id = "se_stockholm_01",
                countryName = "Sweden",
                countryCode = "SE",
                cityName = "Stockholm Freedom",
                ipAddress = "185.228.84.10",
                flagEmoji = "🇸🇪",
                pingMs = 38,
                serverLoadPercentage = 19,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.P2P
            ),
            VpnServer(
                id = "br_saopaulo_01",
                countryName = "Brazil",
                countryCode = "BR",
                cityName = "São Paulo Hub",
                ipAddress = "191.232.190.5",
                flagEmoji = "🇧🇷",
                pingMs = 110,
                serverLoadPercentage = 54,
                isPremium = false,
                isFavorite = false,
                category = ServerCategory.FASTEST
            )
        )

        val entities = defaultList.map { VpnServerEntity.fromDomainModel(it) }
        vpnDao.insertServers(entities)
    }

    fun getAllServers(): Flow<List<VpnServer>> {
        return vpnDao.getAllServers().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun toggleFavorite(serverId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateFavoriteStatus(serverId, isFavorite)
    }

    fun getConnectionLogs(): Flow<List<ConnectionSessionLog>> {
        return vpnDao.getConnectionLogs().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun logSession(log: ConnectionSessionLog) = withContext(Dispatchers.IO) {
        vpnDao.insertConnectionLog(ConnectionLogEntity.fromDomainModel(log))
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        vpnDao.clearLogs()
    }

    suspend fun simulateIpLookup(connectedServer: VpnServer?): IpCheckResponse = withContext(Dispatchers.IO) {
        if (connectedServer != null) {
            IpCheckResponse(
                ip = connectedServer.ipAddress,
                country = connectedServer.countryName,
                city = connectedServer.cityName,
                org = "KMTH Encrypted Subnet (${connectedServer.countryCode})",
                secure = true
            )
        } else {
            IpCheckResponse(
                ip = "198.51.100.14",
                country = "Your Local ISP",
                city = "Unprotected Node",
                org = "Public Cellular / Broadband",
                secure = false
            )
        }
    }

    suspend fun runSpeedDiagnostics(server: VpnServer, protocol: VpnProtocol): SpeedTestResultDto = withContext(Dispatchers.IO) {
        val protocolMultiplier = when (protocol) {
            VpnProtocol.KMTH_SPEED -> 1.35f
            VpnProtocol.WIREGUARD -> 1.20f
            VpnProtocol.OPENVPN_UDP -> 1.0f
            VpnProtocol.OPENVPN_TCP -> 0.85f
        }
        val baseDownload = (85f + (100 - server.pingMs).coerceAtLeast(10)) * protocolMultiplier
        val baseUpload = (45f + (80 - server.pingMs).coerceAtLeast(5)) * protocolMultiplier

        SpeedTestResultDto(
            pingMs = (server.pingMs * (0.9f + Math.random() * 0.2f)).toInt().coerceAtLeast(8),
            downloadSpeedMbps = (baseDownload * (0.95f + Math.random() * 0.1f)).toFloat(),
            uploadSpeedMbps = (baseUpload * (0.95f + Math.random() * 0.1f)).toFloat(),
            jitterMs = (2 + Math.random() * 4).toInt(),
            serverName = "${server.countryName} - ${server.cityName}"
        )
    }
}
