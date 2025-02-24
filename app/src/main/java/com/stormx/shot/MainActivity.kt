package com.stormx.shot

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        var isServiceInitialized by remember { mutableStateOf(ScreenshotService.isRunning(context)) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (!isServiceInitialized) {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                        Toast.makeText(
                            context,
                            "Vui lòng bật ScreenshotService trong Accessibility",
                            Toast.LENGTH_LONG
                        ).show()

                        val floatingIntent = Intent(context, FloatingWindowService::class.java)
                        context.startService(floatingIntent)
                    } else {
                        Toast.makeText(context, "Dùng nút nổi để chụp ảnh!", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Text(if (isServiceInitialized) "Đã sẵn sàng" else "Khởi động dịch vụ")
            }

            LaunchedEffect(Unit) {
                while (true) {
                    isServiceInitialized = ScreenshotService.isRunning(context)
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
    }
}