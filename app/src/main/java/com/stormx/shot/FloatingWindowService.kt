package com.stormx.shot

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner


class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatView: ComposeView
    private var isVisible = true

    companion object {
        const val ACTION_HIDE = "com.stormx.shot.HIDE"
        const val ACTION_SHOW = "com.stormx.shot.SHOW"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

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

            setContent {
                FloatingButton(
                    onClick = {
                        val intent = Intent(this@FloatingWindowService, ScreenshotService::class.java).apply {
                            action = ScreenshotService.ACTION_TAKE_SCREENSHOT
                        }
                        startForegroundService(intent)
                    }
                )
            }
        }

        floatView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

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
        })

        windowManager.addView(floatView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        }
    }

    private fun showButton() {
        if (!isVisible) {
            floatView.visibility = View.VISIBLE
            isVisible = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
            .background(Color(0xFF6200EE), CircleShape)
            .padding(16.dp)
    ) {
        Text(
            text = "Chụp",
            color = Color.White
        )
    }
}

