package com.example.data.parser

import android.util.Base64
import com.example.domain.model.ServerCategory
import com.example.domain.model.VpnProtocol
import com.example.domain.model.VpnServer
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object VlessParser {

    /**
     * Parses a raw text config (which may contain raw vless:// links or base64 encoded lines/blobs)
     * into a list of VpnServer objects.
     */
    fun parseConfigText(rawInput: String): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        if (rawInput.isBlank()) return servers

        // Check if the entire input is base64 encoded
        val textToProcess = decodeIfBase64(rawInput.trim())

        val lines = textToProcess.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }

        for ((index, line) in lines.withIndex()) {
            val decodedLine = if (!line.startsWith("vless://", ignoreCase = true) && isBase64(line)) {
                decodeIfBase64(line)
            } else {
                line
            }

            if (decodedLine.startsWith("vless://", ignoreCase = true)) {
                val server = parseVlessUrl(decodedLine, index)
                if (server != null) {
                    servers.add(server)
                }
            }
        }

        return servers
    }

    private fun parseVlessUrl(vlessUrl: String, index: Int): VpnServer? {
        return try {
            val urlWithoutPrefix = vlessUrl.substring(8) // Remove "vless://"

            // Split fragment (#remark)
            val hashIndex = urlWithoutPrefix.indexOf('#')
            val rawMainPart = if (hashIndex != -1) urlWithoutPrefix.substring(0, hashIndex) else urlWithoutPrefix
            val rawRemark = if (hashIndex != -1) urlWithoutPrefix.substring(hashIndex + 1) else ""

            val remark = try {
                URLDecoder.decode(rawRemark, StandardCharsets.UTF_8.name())
            } catch (e: Exception) {
                rawRemark
            }

            // Split query params (?)
            val queryIndex = rawMainPart.indexOf('?')
            val authAndHost = if (queryIndex != -1) rawMainPart.substring(0, queryIndex) else rawMainPart
            val rawQueryParams = if (queryIndex != -1) rawMainPart.substring(queryIndex + 1) else ""

            // Split userInfo @ host:port
            val atIndex = authAndHost.indexOf('@')
            val uuid = if (atIndex != -1) authAndHost.substring(0, atIndex) else "default-uuid"
            val hostAndPort = if (atIndex != -1) authAndHost.substring(atIndex + 1) else authAndHost

            // Extract host & port
            val colonIndex = hostAndPort.lastIndexOf(':')
            val host = if (colonIndex != -1) hostAndPort.substring(0, colonIndex) else hostAndPort
            val port = if (colonIndex != -1) {
                hostAndPort.substring(colonIndex + 1).toIntOrNull() ?: 443
            } else {
                443
            }

            // Parse query parameters (type, security, path, sni)
            val queryMap = parseQueryParameters(rawQueryParams)
            val type = queryMap["type"] ?: "tcp"
            val security = queryMap["security"] ?: "none"

            // Determine Remark Display Name
            val displayName = if (remark.isNotBlank()) remark else "VLESS $host:$port"

            // Extract Flag Emoji and Country Info
            val extractedFlag = extractFlagEmoji(displayName)
            val countryInfo = resolveCountryInfo(displayName, host, extractedFlag)

            val flagEmoji = countryInfo.flagEmoji
            val countryName = countryInfo.countryName
            val countryCode = countryInfo.countryCode
            val cityName = extractCityOrDetails(displayName, host, port, type, security)

            // Category assignment based on remark keywords
            val category = when {
                displayName.contains("Game", ignoreCase = true) || displayName.contains("Gaming", ignoreCase = true) -> ServerCategory.GAMING
                displayName.contains("Stream", ignoreCase = true) || displayName.contains("Netflix", ignoreCase = true) -> ServerCategory.STREAMING
                displayName.contains("Torrent", ignoreCase = true) || displayName.contains("P2P", ignoreCase = true) -> ServerCategory.P2P
                displayName.contains("VIP", ignoreCase = true) || displayName.contains("Fast", ignoreCase = true) -> ServerCategory.FASTEST
                else -> ServerCategory.FASTEST
            }

            // Ping estimation (deterministic per server index/host)
            val estimatedPing = 12 + ((host.hashCode() and 0x7FFFFFFF) % 40)

            val id = "vless_${index}_${host.replace(".", "_")}_$port"

            VpnServer(
                id = id,
                countryName = countryName,
                countryCode = countryCode,
                cityName = cityName,
                ipAddress = host,
                flagEmoji = flagEmoji,
                pingMs = estimatedPing,
                serverLoadPercentage = 15 + ((index * 7) % 50),
                isPremium = displayName.contains("VIP", ignoreCase = true) || displayName.contains("Pro", ignoreCase = true),
                isFavorite = false,
                category = category,
                availableProtocols = listOf(VpnProtocol.KMTH_SPEED, VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP),
                vlessConfig = vlessUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseQueryParameters(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        val pairs = query.split('&')
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx != -1) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                map[key] = try {
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                } catch (e: Exception) {
                    value
                }
            }
        }
        return map
    }

    private fun decodeIfBase64(input: String): String {
        return try {
            val decodedBytes = Base64.decode(input, Base64.DEFAULT)
            val decodedString = String(decodedBytes, StandardCharsets.UTF_8)
            if (decodedString.contains("vless://") || decodedString.lines().size > 1) {
                decodedString
            } else {
                input
            }
        } catch (e: Exception) {
            input
        }
    }

    private fun isBase64(input: String): Boolean {
        if (input.length < 8) return false
        return try {
            Base64.decode(input, Base64.DEFAULT)
            !input.contains(" ") && input.matches(Regex("^[A-Za-z0-9+/=]+$"))
        } catch (e: Exception) {
            false
        }
    }

    private fun extractFlagEmoji(text: String): String? {
        // Regex matching regional indicator pairs (Unicode flag emojis)
        val flagPattern = Pattern.compile("[\\uD83C][\\uDDE6-\\uDDFF][\\uD83C][\\uDDE6-\\uDDFF]")
        val matcher = flagPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }

    private data class CountryInfo(
        val countryName: String,
        val countryCode: String,
        val flagEmoji: String
    )

    private fun resolveCountryInfo(remark: String, host: String, extractedFlag: String?): CountryInfo {
        val lower = remark.lowercase()

        val matchedCode = when {
            lower.contains("us") || lower.contains("united states") || lower.contains("america") -> "US"
            lower.contains("jp") || lower.contains("japan") || lower.contains("tokyo") -> "JP"
            lower.contains("sg") || lower.contains("singapore") -> "SG"
            lower.contains("de") || lower.contains("germany") || lower.contains("frankfurt") -> "DE"
            lower.contains("gb") || lower.contains("uk") || lower.contains("united kingdom") || lower.contains("london") -> "GB"
            lower.contains("hk") || lower.contains("hong kong") -> "HK"
            lower.contains("tw") || lower.contains("taiwan") -> "TW"
            lower.contains("kr") || lower.contains("korea") || lower.contains("seoul") -> "KR"
            lower.contains("ca") || lower.contains("canada") -> "CA"
            lower.contains("au") || lower.contains("australia") -> "AU"
            lower.contains("nl") || lower.contains("netherlands") -> "NL"
            lower.contains("se") || lower.contains("sweden") -> "SE"
            lower.contains("br") || lower.contains("brazil") -> "BR"
            lower.contains("mm") || lower.contains("myanmar") -> "MM"
            lower.contains("fr") || lower.contains("france") -> "FR"
            lower.contains("in") || lower.contains("india") -> "IN"
            lower.contains("th") || lower.contains("thailand") -> "TH"
            lower.contains("vn") || lower.contains("vietnam") -> "VN"
            else -> "US"
        }

        val countryName = when (matchedCode) {
            "US" -> "United States"
            "JP" -> "Japan"
            "SG" -> "Singapore"
            "DE" -> "Germany"
            "GB" -> "United Kingdom"
            "HK" -> "Hong Kong"
            "TW" -> "Taiwan"
            "KR" -> "South Korea"
            "CA" -> "Canada"
            "AU" -> "Australia"
            "NL" -> "Netherlands"
            "SE" -> "Sweden"
            "BR" -> "Brazil"
            "MM" -> "Myanmar"
            "FR" -> "France"
            "IN" -> "India"
            "TH" -> "Thailand"
            "VN" -> "Vietnam"
            else -> "United States"
        }

        val flagEmoji = extractedFlag ?: countryCodeToFlagEmoji(matchedCode)

        return CountryInfo(
            countryName = countryName,
            countryCode = matchedCode,
            flagEmoji = flagEmoji
        )
    }

    private fun extractCityOrDetails(remark: String, host: String, port: Int, type: String, security: String): String {
        val cleanRemark = remark
            .replace(Regex("[\\uD83C][\\uDDE6-\\uDDFF][\\uD83C][\\uDDE6-\\uDDFF]"), "") // remove flag
            .trim()

        if (cleanRemark.isNotBlank()) {
            return "$cleanRemark (VLESS-$type)"
        }
        return "Node $host:$port ($security)"
    }

    fun countryCodeToFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val uppercaseCode = countryCode.uppercase()
        val firstChar = Character.codePointAt(uppercaseCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(uppercaseCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}
