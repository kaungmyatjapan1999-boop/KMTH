package com.kmth.vpn

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import libXray.DialerController
import org.json.JSONArray
import org.json.JSONObject

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayStarted = false
    private var lastStatusWasError = false

    companion object {

        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_SERVER_TYPE = "server_type"
        const val EXTRA_SERVER_CONFIG = "server_config"

        const val ACTION_VPN_STATUS =
            "com.kmth.vpn.VPN_STATUS"

        const val EXTRA_VPN_STATUS =
            "vpn_status"

        const val EXTRA_VPN_ERROR =
            "vpn_error"

        const val VPN_STATUS_CONNECTING =
            "connecting"

        const val VPN_STATUS_CONNECTED =
            "connected"

        const val VPN_STATUS_DISCONNECTED =
            "disconnected"

        const val VPN_STATUS_ERROR =
            "error"

        private const val TAG = "KMTH_VPN"
    }

    private val dialerController =
        object : DialerController {

            override fun protectFd(fd: Long): Boolean {

                return try {

                    val result =
                        protect(fd.toInt())

                    Log.d(
                        TAG,
                        "protectFd($fd) = $result"
                    )

                    result

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "protectFd failed",
                        e
                    )

                    false
                }
            }
        }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (vpnInterface != null) {

            Log.d(
                TAG,
                "VPN already running"
            )

            return START_STICKY
        }

        val serverName =
            intent?.getStringExtra(
                EXTRA_SERVER_NAME
            ) ?: "KMTH"

        val serverType =
            intent?.getStringExtra(
                EXTRA_SERVER_TYPE
            ) ?: ""

        val serverConfig =
            intent?.getStringExtra(
                EXTRA_SERVER_CONFIG
            ) ?: ""

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "SERVER: $serverName"
        )

        Log.d(
            TAG,
            "TYPE: $serverType"
        )

        Log.d(
            TAG,
            "================================"
        )

        lastStatusWasError = false

        sendVpnStatus(
            VPN_STATUS_CONNECTING
        )

        if (serverConfig.isBlank()) {

            failVpn(
                "Server configuration is empty"
            )

            return START_NOT_STICKY
        }

        try {

            /*
             * SSH_WS is NOT supported by
             * libXray share-link conversion.
             *
             * Do not send ssh:// into
             * convertShareLinksToXrayJson().
             */
            if (
                serverType.equals(
                    "ssh_ws",
                    ignoreCase = true
                )
            ) {

                failVpn(
                    "SSH_WS transport is not enabled yet. " +
                        "VLESS path is ready, but the custom SSH_WS " +
                        "HTTP/WebSocket handshake still needs its own client."
                )

                return START_NOT_STICKY
            }

            if (
                !serverType.equals(
                    "vless",
                    ignoreCase = true
                )
            ) {

                failVpn(
                    "Unsupported server type: $serverType"
                )

                return START_NOT_STICKY
            }

            /*
             * Register socket protection.
             */
            libXray.LibXray.registerDialerController(
                dialerController
            )

            /*
             * Configure DNS.
             */
            libXray.LibXray.setDNS(
                dialerController,
                "1.1.1.1:53"
            )

            Log.d(
                TAG,
                "libXray initialized"
            )

            /*
             * Establish Android VPN interface.
             */
            val builder =
                Builder()

            builder
                .setSession(
                    "KMTH VPN"
                )
                .addAddress(
                    "10.0.0.2",
                    32
                )
                .addRoute(
                    "0.0.0.0",
                    0
                )

            vpnInterface =
                builder.establish()

            if (vpnInterface == null) {

                failVpn(
                    "Android VPN interface could not be established"
                )

                return START_NOT_STICKY
            }

            Log.d(
                TAG,
                "Android TUN established"
            )

            startVlessXray(
                serverConfig
            )

            return START_STICKY

        } catch (e: Exception) {

            Log.e(
                TAG,
                "VPN startup failed",
                e
            )

            failVpn(
                e.message
                    ?: "VPN startup failed"
            )

            return START_NOT_STICKY
        }
    }

    private fun startVlessXray(
        serverConfig: String
    ) {

        val tunFd =
            vpnInterface?.fd

        if (tunFd == null) {

            failVpn(
                "TUN file descriptor is unavailable"
            )

            return
        }

        Thread {

            try {

                Log.d(
                    TAG,
                    "Converting VLESS share link..."
                )

                val convertRequest =
                    JSONObject().apply {

                        put(
                            "apiVersion",
                            3
                        )

                        put(
                            "method",
                            "convertShareLinksToXrayJson"
                        )

                        put(
                            "payload",
                            JSONObject().apply {

                                put(
                                    "text",
                                    serverConfig
                                )
                            }
                        )
                    }

                val convertResult =
                    libXray.LibXray.invoke(
                        convertRequest.toString()
                    )

                Log.d(
                    TAG,
                    "libXray conversion response received"
                )

                val convertJson =
                    JSONObject(
                        convertResult
                    )

                if (
                    !convertJson.optBoolean(
                        "success",
                        false
                    )
                ) {

                    val error =
                        convertJson.optString(
                            "error",
                            "VLESS conversion failed"
                        )

                    failVpn(
                        error
                    )

                    return@Thread
                }

                val data =
                    convertJson.optJSONObject(
                        "data"
                    )

                val outbounds =
                    data?.optJSONArray(
                        "outbounds"
                    )

                if (
                    outbounds == null ||
                    outbounds.length() == 0
                ) {

                    failVpn(
                        "VLESS conversion returned no outbound"
                    )

                    return@Thread
                }

                Log.d(
                    TAG,
                    "Converted outbounds: ${outbounds.length()}"
                )

                /*
                 * Build Xray configuration.
                 *
                 * xray.tun.fd must be inside
                 * the root env object.
                 */
                val xrayConfig =
                    JSONObject().apply {

                        put(
                            "env",
                            JSONObject().apply {

                                put(
                                    "xray.tun.fd",
                                    tunFd
                                )
                            }
                        )

                        put(
                            "inbounds",
                            JSONArray().apply {

                                put(
                                    JSONObject().apply {

                                        put(
                                            "tag",
                                            "kmth-tun"
                                        )

                                        put(
                                            "protocol",
                                            "tun"
                                        )

                                        put(
                                            "settings",
                                            JSONObject().apply {

                                                put(
                                                    "name",
                                                    "kmth-tun"
                                                )

                                                put(
                                                    "mtu",
                                                    1500
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )

                        put(
                            "outbounds",
                            outbounds
                        )

                        put(
                            "routing",
                            JSONObject().apply {

                                put(
                                    "domainStrategy",
                                    "AsIs"
                                )

                                put(
                                    "rules",
                                    JSONArray()
                                )
                            }
                        )
                    }

                Log.d(
                    TAG,
                    "Xray config prepared"
                )

                val runRequest =
                    JSONObject().apply {

                        put(
                            "apiVersion",
                            3
                        )

                        put(
                            "method",
                            "runXray"
                        )

                        put(
                            "payload",
                            JSONObject().apply {

                                put(
                                    "xrayJson",
                                    xrayConfig.toString()
                                )
                            }
                        )
                    }

                Log.d(
                    TAG,
                    "Starting libXray..."
                )

                val runResult =
                    libXray.LibXray.invoke(
                        runRequest.toString()
                    )

                Log.d(
                    TAG,
                    "runXray response received"
                )

                val runJson =
                    JSONObject(
                        runResult
                    )

                if (
                    !runJson.optBoolean(
                        "success",
                        false
                    )
                ) {

                    val error =
                        runJson.optString(
                            "error",
                            "Xray failed to start"
                        )

                    failVpn(
                        error
                    )

                    return@Thread
                }

                xrayStarted = true
                lastStatusWasError = false

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "KMTH VLESS VPN CONNECTED"
                )

                Log.d(
                    TAG,
                    "================================"
                )

                sendVpnStatus(
                    VPN_STATUS_CONNECTED
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "VLESS Xray exception",
                    e
                )

                failVpn(
                    e.message
                        ?: "VLESS Xray startup failed"
                )
            }

        }.start()
    }

    private fun sendVpnStatus(
        status: String,
        error: String? = null
    ) {

        try {

            val intent =
                Intent(
                    ACTION_VPN_STATUS
                ).apply {

                    setPackage(
                        packageName
                    )

                    putExtra(
                        EXTRA_VPN_STATUS,
                        status
                    )

                    if (
                        !error.isNullOrBlank()
                    ) {

                        putExtra(
                            EXTRA_VPN_ERROR,
                            error
                        )
                    }
                }

            sendBroadcast(
                intent
            )

            Log.d(
                TAG,
                "STATUS: $status"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Status broadcast failed",
                e
            )
        }
    }

    private fun failVpn(
        error: String
    ) {

        Log.e(
            TAG,
            "VPN ERROR: $error"
        )

        lastStatusWasError = true

        sendVpnStatus(
            VPN_STATUS_ERROR,
            error
        )

        stopSelf()
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "Stopping KMTH VPN"
        )

        try {

            if (xrayStarted) {

                val stopRequest =
                    JSONObject().apply {

                        put(
                            "apiVersion",
                            3
                        )

                        put(
                            "method",
                            "stopXray"
                        )

                        put(
                            "payload",
                            JSONObject()
                        )
                    }

                libXray.LibXray.invoke(
                    stopRequest.toString()
                )

                xrayStarted = false

                Log.d(
                    TAG,
                    "Xray stopped"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Xray stop failed",
                e
            )
        }

        try {

            libXray.LibXray.resetDNS()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "DNS reset failed",
                e
            )
        }

        try {

            vpnInterface?.close()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "TUN close failed",
                e
            )
        }

        vpnInterface = null

        if (!lastStatusWasError) {

            sendVpnStatus(
                VPN_STATUS_DISCONNECTED
            )
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent
    ): IBinder? {

        return super.onBind(intent)
    }
}
