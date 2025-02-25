package com.stormx.shot.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.stormx.shot.ui.FloatingButton

class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatView: ComposeView
    private var isVisible = true
    private val TAG = "FloatingWindowService"

    companion object {
        const val ACTION_HIDE = "com.stormx.shot.HIDE"
        const val ACTION_SHOW = "com.stormx.shot.SHOW"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        setupFloatingWindow()
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = createWindowParams()

        floatView = ComposeView(this).apply {
            val lifecycleOwner = object : LifecycleOwner, SavedStateRegistryOwner {
                private val lifecycleRegistry = LifecycleRegistry(this)
                private val savedStateRegistryController = SavedStateRegistryController.create(this)

                override val lifecycle: Lifecycle
                    get() = lifecycleRegistry

                override val savedStateRegistry: SavedStateRegistry
                    get() = savedStateRegistryController.savedStateRegistry

                init {
                    lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
                    savedStateRegistryController.performRestore(null)
                    lifecycleRegistry.currentState = Lifecycle.State.STARTED
                }
            }

            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { FloatingButton(onClick = { takeScreenshot() }) }
            setOnTouchListener(createTouchListener(params))
        }

        try {
            windowManager.addView(floatView, params)
            Log.d(TAG, "Floating button added")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating button: ${e.message}")
        }
    }

    private fun createWindowParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
    }

    private fun createTouchListener(params: WindowManager.LayoutParams): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatView, params)
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun takeScreenshot() {
        val intent = Intent(this, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_TAKE_SCREENSHOT
        }
        startForegroundService(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_HIDE -> hideButton()
            ACTION_SHOW -> showButton()
        }
        return START_NOT_STICKY
    }

    private fun hideButton() {
        if (isVisible) {
            floatView.visibility = View.GONE
            isVisible = false
            Log.d(TAG, "Floating button hidden")
        }
    }

    private fun showButton() {
        if (!isVisible) {
            floatView.visibility = View.VISIBLE
            isVisible = true
            Log.d(TAG, "Floating button shown")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatView)
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}