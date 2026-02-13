package com.app.idisplaynew.ui.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import com.google.android.datatransport.BuildConfig
import java.net.NetworkInterface

/**
 * Device info used to build [LoginPayload]: values are read from the device at runtime.
 */
data class DeviceInfo(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val resolution: String,
    val orientation: String,
    val appVersion: String,
    val osVersion: String
)

fun Context.getDeviceInfo(): DeviceInfo {
    val dm = resources.displayMetrics
    val width = dm.widthPixels
    val height = dm.heightPixels
    val orientation = when (resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        else -> "unknown"
    }
    return DeviceInfo(
        deviceId = getAndroidId(),
        name = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "DisplayHub Device" },
        ipAddress = getLocalIpAddress(),
        resolution = "${width}x${height}",
        orientation = orientation,
        appVersion = BuildConfig.VERSION_NAME ?: "1.0",
        osVersion = "Android ${Build.VERSION.RELEASE}"
    )
}

private fun Context.getAndroidId(): String {
    return try {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}

private fun getLocalIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.firstOrNull { ni ->
            !ni.isLoopback && ni.isUp
        }?.inetAddresses?.toList()?.firstOrNull { addr ->
            !addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false
        }?.hostAddress ?: "0.0.0.0"
    } catch (e: Exception) {
        "0.0.0.0"
    }
}
