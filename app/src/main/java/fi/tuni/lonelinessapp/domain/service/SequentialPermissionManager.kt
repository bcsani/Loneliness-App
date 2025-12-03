package fi.tuni.lonelinessapp.domain.service

import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SequentialPermissionManager(
    private val activity: ComponentActivity
) {
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        currentRequest?.let { request ->
            if (isGranted) {
                request.onGranted()
            } else {
                request.onDenied()
            }
            processNext()
        }
    }

    private val permissionQueue = mutableListOf<PermissionRequest>()
    private var currentRequest: PermissionRequest? = null
    private var isProcessing = false

    data class PermissionRequest(
        val permission: String,
        val onGranted: () -> Unit,
        val onDenied: () -> Unit,
        val rationaleMessage: String? = null
    )

    fun addPermission(
        permission: String,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {},
        rationaleMessage: String? = null
    ) {
        permissionQueue.add(PermissionRequest(permission, onGranted, onDenied, rationaleMessage))
    }

    fun start() {
        if (!isProcessing) {
            isProcessing = true
            processNext()
        }
    }

    private fun processNext() {
        if (permissionQueue.isEmpty()) {
            isProcessing = false
            return
        }

        currentRequest = permissionQueue.removeAt(0)
        val request = currentRequest!!

        when {
            ContextCompat.checkSelfPermission(activity, request.permission) == PackageManager.PERMISSION_GRANTED -> {
                request.onGranted()
                processNext()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(activity, request.permission) -> {
                // Show rationale and then request permission
                showRationale(request)
            }
            else -> {
                permissionLauncher.launch(request.permission)
            }
        }
    }

    private fun showRationale(request: PermissionRequest) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Needed")
            .setMessage(request.rationaleMessage ?: "This permission is required to use this feature.")
            .setPositiveButton("OK") { _, _ ->
                permissionLauncher.launch(request.permission)
            }
            .setNegativeButton("Cancel") { _, _ ->
                request.onDenied()
                processNext()
            }
            .show()
    }
}