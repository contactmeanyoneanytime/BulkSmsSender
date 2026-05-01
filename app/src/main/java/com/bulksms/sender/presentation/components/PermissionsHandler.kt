// Save at: app/src/main/java/com/bulksms/sender/presentation/components/PermissionsHandler.kt

package com.bulksms.sender.presentation.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.*

@Composable
fun PermissionsHandler(
    onPermissionsGranted: () -> Unit
) {
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE
            )
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    if (!permissionState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permissions Required") },
            text = {
                Text("This app needs SMS permissions to send messages. Without these permissions, the app cannot function.")
            },
            confirmButton = {
                Button(
                    onClick = { permissionState.launchMultiplePermissionRequest() }
                ) {
                    Text("Grant Permissions")
                }
            }
        )
    }
}