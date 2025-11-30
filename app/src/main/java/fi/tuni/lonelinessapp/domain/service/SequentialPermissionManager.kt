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
    private val multiplePermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        currentRequest?.let { request ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                request.onGranted()
            } else {
                request.onDenied()
            }
            processNext()
        }
    }

    private val singlePermissionLauncher = activity.registerForActivityResult(
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
        val permissions: List<String>,
        val onGranted: () -> Unit,
        val onDenied: () -> Unit,
        val rationaleMessage: String? = null,
        val isMultiple: Boolean = false
    )

    fun addPermission(
        permission: String,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {},
        rationaleMessage: String? = null
    ) {
        permissionQueue.add(PermissionRequest(listOf(permission), onGranted, onDenied, rationaleMessage, false))
    }

    fun addMultiplePermissions(
        permissions: List<String>,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {},
        rationaleMessage: String? = null
    ) {
        permissionQueue.add(PermissionRequest(permissions, onGranted, onDenied, rationaleMessage, true))
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

        /*
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
        */

        val allGranted = request.permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            request.onGranted()
            processNext()
        } else {
            // Check if we should show rationale for any permission
            val shouldShowRationale = request.permissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }

            if (shouldShowRationale) {
                showRationale(request)
            } else {
                if (request.isMultiple) {
                    multiplePermissionsLauncher.launch(request.permissions.toTypedArray())
                } else {
                    singlePermissionLauncher.launch(request.permissions.first())
                }
            }
        }
    }

    private fun showRationale(request: PermissionRequest) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Needed")
            .setMessage(request.rationaleMessage ?: "This permission is required to use this feature.")
            .setPositiveButton("OK") { _, _ ->
                if (request.isMultiple) {
                    multiplePermissionsLauncher.launch(request.permissions.toTypedArray())
                } else {
                    singlePermissionLauncher.launch(request.permissions.first())
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                request.onDenied()
                processNext()
            }
            .show()
    }
}