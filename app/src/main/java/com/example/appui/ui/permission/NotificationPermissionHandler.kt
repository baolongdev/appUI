package com.example.appui.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appui.ui.theme.Spacing
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )

        LaunchedEffect(permissionState.status) {
            when {
                permissionState.status.isGranted -> {
                    onPermissionGranted()
                }
                permissionState.status.shouldShowRationale -> {
                    // Show rationale
                }
                !permissionState.status.isGranted &&
                        permissionState.status is PermissionStatus.Denied -> {
                    // Permission denied
                }
            }
        }

        if (!permissionState.status.isGranted) {
            NotificationPermissionDialog(
                onRequestPermission = {
                    permissionState.launchPermissionRequest()
                },
                onDismiss = {
                    onPermissionDenied()
                },
                shouldShowRationale = permissionState.status.shouldShowRationale
            )
        }
    } else {
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
    }
}

@Composable
private fun NotificationPermissionDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    shouldShowRationale: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Cho phép thông báo",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text(
                    text = if (shouldShowRationale) {
                        "Ứng dụng cần quyền thông báo để hiển thị tiến trình tải xuống cập nhật.\n\n" +
                                "Bạn có thể bật quyền này trong Cài đặt > Ứng dụng > Quyền."
                    } else {
                        "Để theo dõi tiến trình tải xuống, ứng dụng cần quyền hiển thị thông báo."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRequestPermission
            ) {
                Text("Cho phép")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bỏ qua")
            }
        }
    )
}
