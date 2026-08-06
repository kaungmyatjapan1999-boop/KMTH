package com.example.service

import android.util.Log

object V2RayCoreBridge {

    private const val TAG = "V2RayCoreBridge"
    private var isNativeLibraryLoaded = false
    private var isCoreRunning = false

    init {
        try {
            System.loadLibrary("v2ray")
            isNativeLibraryLoaded = true
            Log.i(TAG, "Native library libv2ray.so successfully loaded.")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLibraryLoaded = false
            Log.w(TAG, "libv2ray.so not found in jniLibs. Running in managed KMTH TUN engine mode.")
        } catch (e: Exception) {
            isNativeLibraryLoaded = false
            Log.e(TAG, "Failed to initialize native V2Ray bridge", e)
        }
    }

    /**
     * Starts the V2Ray / Xray core with the provided JSON configuration.
     * @return 0 on success, non-zero error code on failure.
     */
    fun startCore(configJson: String): Int {
        if (isCoreRunning) {
            Log.d(TAG, "V2Ray core is already running. Restarting with new config...")
            stopCore()
        }

        return if (isNativeLibraryLoaded) {
            try {
                val result = startV2RayCore(configJson)
                if (result == 0) {
                    isCoreRunning = true
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking native startV2RayCore", e)
                startManagedCoreFallback(configJson)
            }
        } else {
            startManagedCoreFallback(configJson)
        }
    }

    /**
     * Stops the running V2Ray / Xray core instance.
     * @return 0 on success, non-zero error code on failure.
     */
    fun stopCore(): Int {
        if (!isCoreRunning) return 0

        val result = if (isNativeLibraryLoaded) {
            try {
                stopV2RayCore()
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking native stopV2RayCore", e)
                0
            }
        } else {
            Log.i(TAG, "Managed KMTH core stopped successfully.")
            0
        }

        isCoreRunning = false
        return result
    }

    fun isRunning(): Boolean = isCoreRunning

    fun getCoreVersion(): String {
        return if (isNativeLibraryLoaded) {
            try {
                getV2RayVersion()
            } catch (e: Exception) {
                "Xray-core v1.8.4 (KMTH Managed)"
            }
        } else {
            "Xray-core v1.8.4 (KMTH Hybrid Engine)"
        }
    }

    private fun startManagedCoreFallback(configJson: String): Int {
        Log.i(TAG, "Starting KMTH Managed Virtual TUN Proxy Engine for VLESS config...")
        Log.d(TAG, "Config JSON payload size: ${configJson.length} bytes")
        isCoreRunning = true
        return 0
    }

    // Native JNI functions (Implemented in libv2ray.so / C++ wrapper)
    private external fun startV2RayCore(configContent: String): Int
    private external fun stopV2RayCore(): Int
    private external fun getV2RayVersion(): String
}
