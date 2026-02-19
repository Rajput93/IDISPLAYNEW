package com.app.idisplaynew.data.remote

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.net.NetworkInterface

/**
 * Provides device/OS stats for heartbeat payload.
 * All values are read from the device at runtime.
 */
class DeviceStatsProvider(private val context: Context) {

    fun getIpAddress(): String = getLocalIpAddress()

    /** Android does not expose CPU temperature to normal apps; send 0. */
    fun getTemperature(): Double = 0.0

    /** Approximate CPU usage (0–100). Reads /proc/stat; may be 0 on some devices. */
    fun getCpuUsage(): Double {
        return try {
            val stat1 = readProcStat()
            Thread.sleep(200)
            val stat2 = readProcStat()
            if (stat1 != null && stat2 != null) {
                val total1 = stat1.total
                val total2 = stat2.total
                val idle1 = stat1.idle
                val idle2 = stat2.idle
                val totalDelta = total2 - total1
                val idleDelta = idle2 - idle1
                if (totalDelta > 0) ((totalDelta - idleDelta) * 100.0 / totalDelta).coerceIn(0.0, 100.0)
                else 0.0
            } else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    private data class ProcStat(val total: Long, val idle: Long)

    private fun readProcStat(): ProcStat? {
        return try {
            val line = java.io.File("/proc/stat").readLines().firstOrNull() ?: return null
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 5) return null
            val user = parts[1].toLongOrNull() ?: 0L
            val nice = parts[2].toLongOrNull() ?: 0L
            val system = parts[3].toLongOrNull() ?: 0L
            val idle = parts[4].toLongOrNull() ?: 0L
            ProcStat(total = user + nice + system + idle, idle = idle)
        } catch (_: Exception) {
            null
        }
    }

    /** Memory usage percentage (0–100) for current app. */
    fun getMemoryUsage(): Double {
        return try {
            val runtime = Runtime.getRuntime()
            val used = runtime.totalMemory() - runtime.freeMemory()
            val max = runtime.maxMemory()
            if (max > 0) (used * 100.0 / max).coerceIn(0.0, 100.0) else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    /** Internal storage usage percentage (0–100). */
    fun getStorageUsage(): Double {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - availableBytes
            if (totalBytes > 0) (usedBytes * 100.0 / totalBytes).coerceIn(0.0, 100.0) else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    /** Available internal storage in MB. */
    fun getStorageAvailableMb(): Double {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            (availableBytes / (1024.0 * 1024.0)).coerceAtLeast(0.0)
        } catch (_: Exception) {
            0.0
        }
    }

    fun getAppVersion(): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    fun getOsVersion(): String = "Android ${Build.VERSION.RELEASE}"

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.firstOrNull { ni ->
                !ni.isLoopback && ni.isUp
            }?.inetAddresses?.toList()?.firstOrNull { addr ->
                !addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false
            }?.hostAddress ?: "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }
}
