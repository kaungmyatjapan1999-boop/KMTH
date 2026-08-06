package com.example.service

import com.example.domain.model.VpnServer
import com.example.domain.model.SecuritySettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object VlessJsonConfigGenerator {

    /**
     * Converts a VLESS URL string or VpnServer into a full Xray/V2Ray JSON configuration.
     * Supports VLESS with TLS, REALITY, gRPC, WebSocket, TCP, SNI, Public Key (pbk), Short ID (sid),
     * and custom headers.
     */
    fun generateJsonConfig(
        server: VpnServer,
        securitySettings: SecuritySettings,
        localSocksPort: Int = 10808,
        localHttpPort: Int = 10809
    ): String {
        val rawUrl = server.vlessConfig

        if (rawUrl.startsWith("vless://", ignoreCase = true)) {
            return parseVlessUrlToJson(rawUrl, securitySettings, localSocksPort, localHttpPort)
        }

        // Fallback generator for generic VpnServer objects without raw VLESS link
        return generateFallbackJson(server, securitySettings, localSocksPort, localHttpPort)
    }

    private fun parseVlessUrlToJson(
        vlessUrl: String,
        securitySettings: SecuritySettings,
        localSocksPort: Int,
        localHttpPort: Int
    ): String {
        val urlWithoutPrefix = vlessUrl.substring(8)

        // Split hash (#remark)
        val hashIdx = urlWithoutPrefix.indexOf('#')
        val rawMain = if (hashIdx != -1) urlWithoutPrefix.substring(0, hashIdx) else urlWithoutPrefix
        val remark = if (hashIdx != -1) decodeUrl(urlWithoutPrefix.substring(hashIdx + 1)) else "KMTH-VLESS"

        // Split query (?)
        val queryIdx = rawMain.indexOf('?')
        val authHost = if (queryIdx != -1) rawMain.substring(0, queryIdx) else rawMain
        val rawQuery = if (queryIdx != -1) rawMain.substring(queryIdx + 1) else ""

        // Split @
        val atIdx = authHost.indexOf('@')
        val uuid = if (atIdx != -1) authHost.substring(0, atIdx) else "00000000-0000-0000-0000-000000000000"
        val hostPort = if (atIdx != -1) authHost.substring(atIdx + 1) else authHost

        val colonIdx = hostPort.lastIndexOf(':')
        val address = if (colonIdx != -1) hostPort.substring(0, colonIdx) else hostPort
        val port = if (colonIdx != -1) hostPort.substring(colonIdx + 1).toIntOrNull() ?: 443 else 443

        val queryMap = parseQueryParams(rawQuery)
        val networkType = queryMap["type"] ?: "tcp"
        val security = queryMap["security"] ?: "none"
        val sni = queryMap["sni"] ?: queryMap["host"] ?: address
        val pbk = queryMap["pbk"] ?: ""
        val sid = queryMap["sid"] ?: ""
        val fp = queryMap["fp"] ?: "chrome"
        val path = queryMap["path"] ?: "/"
        val serviceName = queryMap["serviceName"] ?: queryMap["path"] ?: ""
        val headerType = queryMap["headerType"] ?: "none"

        // Build V2Ray Root JSON
        val root = JSONObject()

        // 1. Log Config
        val logObj = JSONObject().apply {
            put("loglevel", "warning")
        }
        root.put("log", logObj)

        // 2. Inbounds (Local SOCKS & HTTP Proxies for TUN adapter)
        val inboundsArray = JSONArray()

        val socksInbound = JSONObject().apply {
            put("tag", "socks-in")
            put("port", localSocksPort)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
            put("sniffing", JSONObject().apply {
                put("enabled", true)
                put("destOverride", JSONArray().put("http").put("tls"))
            })
        }
        inboundsArray.put(socksInbound)

        val httpInbound = JSONObject().apply {
            put("tag", "http-in")
            put("port", localHttpPort)
            put("listen", "127.0.0.1")
            put("protocol", "http")
        }
        inboundsArray.put(httpInbound)

        root.put("inbounds", inboundsArray)

        // 3. Outbounds (VLESS Server Outbound + Direct + Blocked)
        val outboundsArray = JSONArray()

        val vlessOutbound = JSONObject()
        vlessOutbound.put("tag", "proxy")
        vlessOutbound.put("protocol", "vless")

        // VLESS Settings
        val vnextArray = JSONArray()
        val serverNode = JSONObject().apply {
            put("address", address)
            put("port", port)

            val usersArray = JSONArray()
            val user = JSONObject().apply {
                put("id", uuid)
                put("encryption", queryMap["encryption"] ?: "none")
                put("flow", queryMap["flow"] ?: "")
            }
            usersArray.put(user)
            put("users", usersArray)
        }
        vnextArray.put(serverNode)

        val vlessSettings = JSONObject().apply {
            put("vnext", vnextArray)
        }
        vlessOutbound.put("settings", vlessSettings)

        // Stream Settings (Transport, Security TLS/REALITY, gRPC, WS)
        val streamSettings = JSONObject()
        streamSettings.put("network", networkType)
        streamSettings.put("security", security)

        // Security Config (TLS / REALITY)
        if (security.equals("tls", ignoreCase = true)) {
            val tlsSettings = JSONObject().apply {
                put("serverName", sni)
                put("allowInsecure", securitySettings.dnsLeakProtection.not())
                put("fingerprint", fp)
                val alpn = queryMap["alpn"]
                if (!alpn.isNullOrBlank()) {
                    val alpnArray = JSONArray()
                    alpn.split(",").forEach { alpnArray.put(it.trim()) }
                    put("alpn", alpnArray)
                }
            }
            streamSettings.put("tlsSettings", tlsSettings)
        } else if (security.equals("reality", ignoreCase = true)) {
            val realitySettings = JSONObject().apply {
                put("show", false)
                put("dest", "$sni:443")
                put("serverName", sni)
                put("publicKey", pbk)
                put("shortId", sid)
                put("fingerprint", fp)
            }
            streamSettings.put("realitySettings", realitySettings)
        }

        // Transport Network Settings
        when (networkType.lowercase()) {
            "ws", "websocket" -> {
                val wsSettings = JSONObject().apply {
                    put("path", path)
                    val headersObj = JSONObject()
                    if (sni.isNotBlank()) {
                        headersObj.put("Host", sni)
                    }
                    put("headers", headersObj)
                }
                streamSettings.put("wsSettings", wsSettings)
            }
            "grpc" -> {
                val grpcSettings = JSONObject().apply {
                    put("serviceName", serviceName.ifBlank { "vless-grpc" })
                    put("multiMode", false)
                }
                streamSettings.put("grpcSettings", grpcSettings)
            }
            "http", "h2" -> {
                val httpSettings = JSONObject().apply {
                    put("path", path)
                    put("host", JSONArray().put(sni))
                }
                streamSettings.put("httpSettings", httpSettings)
            }
            "tcp" -> {
                if (headerType.equals("http", ignoreCase = true)) {
                    val tcpSettings = JSONObject().apply {
                        put("header", JSONObject().apply {
                            put("type", "http")
                            put("request", JSONObject().apply {
                                put("path", JSONArray().put(path))
                                put("headers", JSONObject().apply {
                                    put("Host", JSONArray().put(sni))
                                })
                            })
                        })
                    }
                    streamSettings.put("tcpSettings", tcpSettings)
                }
            }
        }

        vlessOutbound.put("streamSettings", streamSettings)
        outboundsArray.put(vlessOutbound)

        // Direct Outbound
        val directOutbound = JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
            put("settings", JSONObject())
        }
        outboundsArray.put(directOutbound)

        // Blocked Outbound (Kill Switch / Ads)
        val blockOutbound = JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
            put("settings", JSONObject().apply {
                put("response", JSONObject().apply {
                    put("type", "none")
                })
            })
        }
        outboundsArray.put(blockOutbound)

        root.put("outbounds", outboundsArray)

        // 4. Routing Settings
        val routing = JSONObject()
        routing.put("domainStrategy", "IPIfNonMatch")

        val rulesArray = JSONArray()

        // Block private IP routing if kill switch or DNS leak protection enabled
        if (securitySettings.dnsLeakProtection) {
            val dnsRule = JSONObject().apply {
                put("type", "field")
                put("port", "53")
                put("outboundTag", "proxy")
            }
            rulesArray.put(dnsRule)
        }

        routing.put("rules", rulesArray)
        root.put("routing", routing)

        // 5. DNS Config
        val dnsObj = JSONObject().apply {
            val serversArr = JSONArray()
            serversArr.put("1.1.1.1")
            serversArr.put("8.8.8.8")
            serversArr.put("https://dns.google/dns-query")
            put("servers", serversArr)
        }
        root.put("dns", dnsObj)

        return root.toString(2)
    }

    private fun generateFallbackJson(
        server: VpnServer,
        securitySettings: SecuritySettings,
        localSocksPort: Int,
        localHttpPort: Int
    ): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))

        val inbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks-in")
                put("port", localSocksPort)
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().put("udp", true))
            })
        }
        root.put("inbounds", inbounds)

        val outbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "proxy")
                put("protocol", "vless")
                put("settings", JSONObject().apply {
                    put("vnext", JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", server.ipAddress)
                            put("port", 443)
                            put("users", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("id", "e1f6ac00-358d-042d-58d4-9a8547eccc9b")
                                    put("encryption", "none")
                                })
                            })
                        })
                    })
                })
                put("streamSettings", JSONObject().apply {
                    put("network", "tcp")
                    put("security", "tls")
                })
            })
            put(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
            })
        }
        root.put("outbounds", outbounds)

        return root.toString(2)
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        query.split('&').forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx != -1) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                map[key] = decodeUrl(value)
            }
        }
        return map
    }

    private fun decodeUrl(value: String): String {
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            value
        }
    }
}
