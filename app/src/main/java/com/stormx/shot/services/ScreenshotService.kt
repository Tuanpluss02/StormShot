package com.stormx.shot.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenshotService : AccessibilityService() {
    companion object {
        const val ACTION_START = "com.stormx.shot.START"
        const val ACTION_TAKE_SCREENSHOT = "com.stormx.shot.TAKE_SCREENSHOT"

        private var instance: ScreenshotService? = null
        private var isServiceRunning = false

        fun isRunning(context: Context): Boolean = isServiceRunning

        fun takeScreenshot() {
            instance?.performScreenshot()
        }
    }

    override fun onServiceConnected() {
        instance = this
        isServiceRunning = true

        val floatingIntent = Intent(this, FloatingWindowService::class.java)
        startService(floatingIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {}
            ACTION_TAKE_SCREENSHOT -> {
                performScreenshot()
            }
        }
        return START_STICKY
    }

    private fun performScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hideIntent = Intent(this, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_HIDE
            }
            startService(hideIntent)

            Handler(Looper.getMainLooper()).postDelayed({
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val bitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            bitmap?.let { saveScreenshot(it) }

                            Handler(Looper.getMainLooper()).post {
                                val showIntent = Intent(
                                    this@ScreenshotService,
                                    FloatingWindowService::class.java
                                ).apply {
                                    action = FloatingWindowService.ACTION_SHOW
                                }
                                startService(showIntent)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Handler(Looper.getMainLooper()).post {
                                val showIntent = Intent(
                                    this@ScreenshotService,
                                    FloatingWindowService::class.java
                                ).apply {
                                    action = FloatingWindowService.ACTION_SHOW
                                }
                                startService(showIntent)
                            }
                        }
                    })
            }, 100)
        }
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Screenshot_$timeStamp.png"
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { imageUri ->
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    }

    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        instance = null
    }
}