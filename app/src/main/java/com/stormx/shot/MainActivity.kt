package com.stormx.shot

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenshotApp()
                }
            }
        }
    }

    @Composable
    fun ScreenshotApp() {
        val context = LocalContext.current
        val isOverlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
        val isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
        var isServiceInitialized by remember { mutableStateOf(ScreenshotService.isRunning(context)) }

        LaunchedEffect(Unit) {
            if (isAccessibilityEnabled && !isServiceInitialized && isOverlayEnabled) {
                val floatingIntent = Intent(context, FloatingWindowService::class.java)
                context.startService(floatingIntent)
                isServiceInitialized = true
            }
            while (true) {
                isServiceInitialized = ScreenshotService.isRunning(context)
                kotlinx.coroutines.delay(1000)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Phần 1: Trạng thái Overlay + Nút bật
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quyền hiển thị trên ứng dụng khác: ${if (isOverlayEnabled) "Đã bật" else "Chưa bật"}",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            context.startActivity(intent)
                            Toast.makeText(context, "Vui lòng bật quyền Overlay", Toast.LENGTH_LONG).show()
                        },
                        enabled = !isOverlayEnabled
                    ) {
                        Text("Bật quyền Overlay")
                    }
                }
            }

            // Phần 2: Trạng thái Accessibility + Nút bật
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quyền Accessibility: ${if (isAccessibilityEnabled) "Đã bật" else "Chưa bật"}",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val accessibilityIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(accessibilityIntent)
                            Toast.makeText(context, "Vui lòng bật ScreenshotService", Toast.LENGTH_LONG).show()
                        },
                        enabled = !isAccessibilityEnabled
                    ) {
                        Text("Bật Accessibility")
                    }
                }
            }

            // Phần 3: Nút ẩn/hiện Floating Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Điều khiển nút nổi",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                val hideIntent = Intent(context, FloatingWindowService::class.java).apply {
                                    action = FloatingWindowService.ACTION_HIDE
                                }
                                context.startService(hideIntent)
                            },
                            enabled = isServiceInitialized
                        ) {
                            Text("Ẩn nút nổi")
                        }
                        Button(
                            onClick = {
                                val showIntent = Intent(context, FloatingWindowService::class.java).apply {
                                    action = FloatingWindowService.ACTION_SHOW
                                }
                                context.startService(showIntent)
                            },
                            enabled = isServiceInitialized
                        ) {
                            Text("Hiện nút nổi")
                        }
                    }
                }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id == "$packageName/.ScreenshotService" }
    }
}