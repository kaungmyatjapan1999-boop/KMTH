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

        private const val TAG = "KMTH_XRAY"
    }

    /*
     * Protect Xray's own outbound sockets
     * from being routed back into the VPN tunnel.
     */
    private val dialerController = object : DialerController {

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
                    "protectFd exception",
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
            "Starting server: $serverName"
        )

        Log.d(
            TAG,
            "Server type: $serverType"
        )

        lastStatusWasError = false

        sendVpnStatus(
            VPN_STATUS_CONNECTING
        )

        try {

            /*
             * Register Android socket protection
             * before starting Xray.
             */
            libXray.LibXray.registerDialerController(
                dialerController
            )

            /*
             * Configure Xray DNS.
             */
            libXray.LibXray.setDNS(
                dialerController,
                "1.1.1.1:53"
            )

            Log.d(
                TAG,
                "libXray DialerController registered"
            )

            Log.d(
                TAG,
                "libXray DNS configured"
            )

            /*
             * Create Android VPN/TUN interface.
             */
            val builder = Builder()

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
                    "Could not establish Android VPN interface"
                )

                return START_NOT_STICKY
            }

            Log.d(
                TAG,
                "Android VPN interface established"
            )

            /*
             * Start Xray using the TUN file descriptor.
             */
            startXray(
                serverConfig
            )

            return START_STICKY

        } catch (e: Exception) {

            Log.e(
                TAG,
                "VPN startup exception",
                e
            )

            failVpn(
                e.message
                    ?: "VPN startup failed"
            )

            return START_NOT_STICKY
        }
    }

    private fun startXray(
        serverConfig: String
    ) {

        val tunFd =
            vpnInterface?.fd

        if (tunFd == null) {

            failVpn(
                "VPN TUN file descriptor is unavailable"
            )

            return
        }

        Thread {

            try {

                Log.d(
                    TAG,
                    "Starting VLESS conversion..."
                )

                /*
                 * Convert the Xray share link
                 * into Xray outbound JSON.
                 */
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
                    "Share-link conversion response received"
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
                            "Share-link conversion failed"
                        )

                    Log.e(
                        TAG,
                        "Share-link conversion failed: $error"
                    )

                    failVpn(error)

                    return@Thread
                }

                val data =
                    convertJson.optJSONObject(
                        "data"
                    )

                val sourceOutbounds =
                    data?.optJSONArray(
                        "outbounds"
                    )

                if (
                    sourceOutbounds == null ||
                    sourceOutbounds.length() == 0
                ) {

                    failVpn(
                        "Share-link conversion returned no outbound"
                    )

                    return@Thread
                }

                Log.d(
                    TAG,
                    "Outbound count: ${sourceOutbounds.length()}"
                )

                /*
                 * Build Xray configuration.
                 */
                val xrayConfig =
                    JSONObject()

                /*
                 * Pass Android TUN FD to Xray.
                 */
                xrayConfig.put(
                    "env",
                    JSONObject().apply {

                        put(
                            "xray.tun.fd",
                            tunFd
                        )
                    }
                )

                /*
                 * Xray TUN inbound.
                 */
                xrayConfig.put(
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

                /*
                 * Use converted outbound(s).
                 */
                xrayConfig.put(
                    "outbounds",
                    sourceOutbounds
                )

                Log.d(
                    TAG,
                    "Xray configuration created"
                )

                /*
                 * Start Xray.
                 */
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
                    "Calling libXray.runXray..."
                )

                val runResult =
                    libXray.LibXray.invoke(
                        runRequest.toString()
                    )

                val runJson =
                    JSONObject(
                        runResult
                    )

                if (
                    runJson.optBoolean(
                        "success",
                        false
                    )
                ) {

                    xrayStarted = true
                    lastStatusWasError = false

                    Log.d(
                        TAG,
                        "================================"
                    )

                    Log.d(
                        TAG,
                        "XRAY STARTED SUCCESSFULLY"
                    )

                    Log.d(
                        TAG,
                        "================================"
                    )

                    /*
                     * IMPORTANT:
                     * Only now report CONNECTED.
                     */
                    sendVpnStatus(
                        VPN_STATUS_CONNECTED
                    )

                } else {

                    val error =
                        runJson.optString(
                            "error",
                            "Xray failed to start"
                        )

                    Log.e(
                        TAG,
                        "Xray start failed: $error"
                    )

                    failVpn(error)
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Xray start exception",
                    e
                )

                failVpn(
                    e.message
                        ?: "Xray start exception"
                )
            }

        }.start()
    }

    /*
     * Send VPN state to MainActivity.
     */
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

                    if (!error.isNullOrBlank()) {

                        putExtra(
                            EXTRA_VPN_ERROR,
                            error
                        )
                    }
                }

            sendBroadcast(intent)

            Log.d(
                TAG,
                "VPN status sent: $status"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Could not send VPN status",
                e
            )
        }
    }

    /*
     * Report an error and stop the service.
     */
    private fun failVpn(
        error: String
    ) {

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
            "Stopping KMTH VPN..."
        )

        try {

            if (xrayStarted) {

                val request =
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
                    request.toString()
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

        /*
         * Reset libXray DNS configuration.
         */
        try {

            libXray.LibXray.resetDNS()

            Log.d(
                TAG,
                "libXray DNS reset"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "DNS reset failed",
                e
            )
        }

        /*
         * Close Android TUN interface.
         */
        try {

            vpnInterface?.close()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "VPN interface close failed",
                e
            )
        }

        vpnInterface = null

        /*
         * Only report DISCONNECTED when this
         * was not an error shutdown.
         */
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
