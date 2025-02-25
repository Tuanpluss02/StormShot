package com.stormx.shot.ui.views

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stormx.shot.services.FloatingWindowService
import com.stormx.shot.services.ScreenshotService
import com.stormx.shot.utils.isAccessibilityServiceEnabled
import com.stormx.shot.utils.requestAccessibilityPermission
import com.stormx.shot.utils.requestOverlayPermission

@Composable
fun ScreenshotScreen(
    modifier: Modifier
) {
    val context = LocalContext.current
    var isOverlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(context.isAccessibilityServiceEnabled()) }
    var isServiceInitialized by remember { mutableStateOf(ScreenshotService.isRunning(context)) }

    LaunchedEffect(isOverlayEnabled, isAccessibilityEnabled, isServiceInitialized) {
        while (true) {
            isOverlayEnabled = Settings.canDrawOverlays(context)
            isAccessibilityEnabled = context.isAccessibilityServiceEnabled()
            isServiceInitialized = ScreenshotService.isRunning(context)
            if (isAccessibilityEnabled && !isServiceInitialized && isOverlayEnabled) {
                context.startService(Intent(context, FloatingWindowService::class.java))
                isServiceInitialized = true
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PermissionCard(
            title = "Quyền hiển thị trên ứng dụng khác",
            isEnabled = isOverlayEnabled,
            onRequestPermission = { context.requestOverlayPermission() }
        )

        PermissionCard(
            title = "Quyền Accessibility",
            isEnabled = isAccessibilityEnabled,
            onRequestPermission = { context.requestAccessibilityPermission() }
        )

        FloatingControlCard(
            isServiceInitialized = isServiceInitialized,
            onHideFloating = {
                context.startService(Intent(context, FloatingWindowService::class.java).apply {
                    action = FloatingWindowService.ACTION_HIDE
                })
            },
            onShowFloating = {
                context.startService(Intent(context, FloatingWindowService::class.java).apply {
                    action = FloatingWindowService.ACTION_SHOW
                })
            }
        )
    }
}

// Card cho Overlay và Accessibility
@Composable
fun PermissionCard(
    title: String,
    isEnabled: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$title: ${if (isEnabled) "Đã bật" else "Chưa bật"}",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                enabled = !isEnabled
            ) {
                Text("Bật $title")
            }
        }
    }
}

// Card cho điều khiển nút nổi
@Composable
fun FloatingControlCard(
    isServiceInitialized: Boolean,
    onHideFloating: () -> Unit,
    onShowFloating: () -> Unit
) {
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
                    onClick = onHideFloating,
                    enabled = isServiceInitialized
                ) {
                    Text("Ẩn nút nổi")
                }
                Button(
                    onClick = onShowFloating,
                    enabled = isServiceInitialized
                ) {
                    Text("Hiện nút nổi")
                }
            }
        }
    }
}