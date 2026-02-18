package com.app.idisplaynew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit

/**
 * Starts the app automatically when device boots (and optionally after first unlock).
 * Flow: Power ON → Android Boot → Device unlock → APP AUTO START → Full Screen / Dashboard
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Mark: launch app when user unlocks device
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                    putBoolean(KEY_PENDING_AUTO_START, true)
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                // Device unlocked – auto-start app once (after boot)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (prefs.getBoolean(KEY_PENDING_AUTO_START, false)) {
                    prefs.edit { putBoolean(KEY_PENDING_AUTO_START, false) }
                    startApp(context)
                }
            }
        }
    }

    private fun startApp(context: Context) {
        try {
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
            context.startActivity(launch)
        } catch (e: Exception) {
            Log.e(TAG, "Auto-start failed", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "boot_auto_start"
        private const val KEY_PENDING_AUTO_START = "pending_auto_start"
    }
}
