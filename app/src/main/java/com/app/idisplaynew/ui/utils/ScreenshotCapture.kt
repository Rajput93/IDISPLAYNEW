package com.app.idisplaynew.ui.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.Choreographer
import android.view.PixelCopy
import android.view.View
import android.view.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Must run PixelCopy callback on Main; await latch on IO to avoid deadlock. */
private const val PIXELCOPY_TIMEOUT_SEC: Long = 8

private const val JPEG_QUALITY = 80

/**
 * Suspends until the next frame has been drawn. Call this before capture so the window content is ready.
 * Must be called from Main dispatcher.
 */
suspend fun waitForNextFrame() {
    withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            Choreographer.getInstance().postFrameCallback {
                cont.resume(Unit)
            }
        }
    }
}

/**
 * Waits for the next layout pass (view.post) then captures. Use when immediate capture returns null.
 */
suspend fun captureScreenBitmapAfterPost(activity: Activity?): Bitmap? = withContext(Dispatchers.Main.immediate) {
    val view = activity?.window?.decorView?.rootView ?: return@withContext null
    suspendCancellableCoroutine<Unit> { cont ->
        view.post { cont.resume(Unit) }
    }
    delay(80)
    captureScreenBitmap(activity)
}

/**
 * Captures the current screen (decor view) of the activity.
 * Call from Main; uses IO for waiting so PixelCopy callback can run (avoids deadlock).
 * Returns null if activity/window is invalid or capture fails.
 */
suspend fun captureScreenBitmap(activity: Activity?): Bitmap? = withContext(Dispatchers.Main.immediate) {
    if (activity == null || activity.isFinishing || activity.isDestroyed) return@withContext null
    val window: Window = activity.window ?: return@withContext null
    val view: View = window.decorView.rootView ?: return@withContext null
    var width = view.width
    var height = view.height
    if (width <= 0 || height <= 0) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        width = view.measuredWidth.coerceAtLeast(0)
        height = view.measuredHeight.coerceAtLeast(0)
    }
    if (width <= 0 || height <= 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = window.context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            val bounds = wm?.currentWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                width = bounds.width()
                height = bounds.height()
            }
        }
        if (width <= 0 || height <= 0) {
            val dm = window.context.resources.displayMetrics
            width = dm.widthPixels.coerceAtLeast(1)
            height = dm.heightPixels.coerceAtLeast(1)
        }
    }
    if (width <= 0 || height <= 0) return@withContext null
    captureViaPixelCopySuspend(window, width, height)
}

/** Runs PixelCopy on Main, awaits callback on IO so Main thread is not blocked (fixes deadlock). */
@SuppressLint("NewApi")
private suspend fun captureViaPixelCopySuspend(window: Window, width: Int, height: Int): Bitmap? {
    val bitmap = try {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        return null
    }
    val latch = CountDownLatch(1)
    var success = false
    var requestFailed = false
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val listener: (Int) -> Unit = { result ->
        if (result == PixelCopy.SUCCESS) success = true
        latch.countDown()
    }
    withContext(Dispatchers.Main.immediate) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PixelCopy.request(window, bitmap, listener, handler)
            } else {
                PixelCopy.request(window, Rect(0, 0, width, height), bitmap, listener, handler)
            }
        } catch (e: Exception) {
            latch.countDown()
            bitmap.recycle()
            requestFailed = true
        }
    }
    if (requestFailed) return null
    withContext(Dispatchers.IO) {
        latch.await(PIXELCOPY_TIMEOUT_SEC, TimeUnit.SECONDS)
    }
    return if (success) bitmap else run {
        bitmap.recycle()
        null
    }
}

/**
 * Converts a hardware-backed bitmap to a software bitmap so it can be compressed
 * or drawn to a software Canvas without throwing.
 */
@SuppressLint("NewApi")
private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
        val software = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        bitmap.recycle()
        return software
    }
    return bitmap
}

/**
 * Compresses the bitmap to JPEG at 80% quality.
 * Safe to call from any thread; does I/O so prefer Dispatchers.IO.
 * Converts hardware bitmaps to software first to avoid "Software rendering doesn't support hardware bitmaps".
 */
suspend fun bitmapToJpegBytes(bitmap: Bitmap?, quality: Int = JPEG_QUALITY): ByteArray? =
    withContext(Dispatchers.IO) {
        if (bitmap == null) return@withContext null
        val toCompress = ensureSoftwareBitmap(bitmap) ?: return@withContext null
        try {
            val stream = ByteArrayOutputStream()
            if (!toCompress.compress(Bitmap.CompressFormat.JPEG, quality, stream)) return@withContext null
            stream.toByteArray()
        } finally {
            if (!toCompress.isRecycled) toCompress.recycle()
        }
    }
