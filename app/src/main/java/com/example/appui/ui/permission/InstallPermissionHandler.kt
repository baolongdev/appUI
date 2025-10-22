package com.example.appui.ui.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun InstallPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        } else {
            onPermissionGranted()
        }
        showDialog = false
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onPermissionDenied()
            },
            icon = {
                Icon(
                    Icons.Default.InstallMobile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Cần quyền cài đặt ứng dụng")
            },
            text = {
                Text(
                    "Để cài đặt bản cập nhật, ứng dụng cần quyền cài đặt từ nguồn không xác định. " +
                            "Bạn sẽ được chuyển đến Cài đặt để cấp quyền."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")
                            )
                            settingsLauncher.launch(intent)
                        } else {
                            onPermissionGranted()
                            showDialog = false
                        }
                    }
                ) {
                    Text("Đến Cài đặt")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onPermissionDenied()
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
