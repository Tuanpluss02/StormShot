package com.stormx.shot.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast

// Kiểm tra Accessibility Service đã bật chưa
fun Context.isAccessibilityServiceEnabled(): Boolean {
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices =
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.id == "$packageName/.ScreenshotService" }
}

// Yêu cầu bật Accessibility
fun Context.requestAccessibilityPermission() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    startActivity(intent)
    Toast.makeText(this, "Vui lòng bật Accessibility cho StormShot", Toast.LENGTH_LONG).show()
}

// Yêu cầu bật Overlay
fun Context.requestOverlayPermission() {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
    )
    startActivity(intent)
    Toast.makeText(this, "Vui lòng bật quyền Overlay", Toast.LENGTH_LONG).show()
}