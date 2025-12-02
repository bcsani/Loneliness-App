package fi.tuni.lonelinessapp.domain.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import fi.tuni.lonelinessapp.domain.model.DeviceInfo

class BluetoothProximityManager(
    private val context: Context,
    private val onDeviceDetected: (DeviceInfo) -> Unit,
    private val onScanStatusChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false
    private val scannedDevices = mutableSetOf<String>()

    companion object {
        private const val PROXIMITY_RSSI_THRESHOLD = -70
        private const val SCAN_PERIOD: Long = 100000
    }

    private val leScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            handleScanResult(result)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            results.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            onError("Bluetooth scan failed with error: $errorCode")
        }
    }

    init {
        initializeBluetooth()
    }

    private fun initializeBluetooth() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            onError("Bluetooth is not enabled")
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        if (!hasRequiredPermissions()) {
            return
        }

        if (scanning) return

        scannedDevices.clear()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner.startScan(emptyList(), scanSettings, leScanCallback)
        scanning = true
        onScanStatusChanged(true)

        // Auto-stop after scan period
        Handler(Looper.getMainLooper()).postDelayed({
            stopScanning()
        }, SCAN_PERIOD)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        if (scanning) {
            bluetoothLeScanner.stopScan(leScanCallback)
            scanning = false
            onScanStatusChanged(false)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi

        if (rssi >= PROXIMITY_RSSI_THRESHOLD && !scannedDevices.contains(device.address)) {
            scannedDevices.add(device.address)

            val distance = calculateDistance(rssi)
            val deviceInfo = DeviceInfo(
                name = device.name ?: "Unknown",
                address = device.address,
                rssi = rssi,
                distance = distance
            )

            onDeviceDetected(deviceInfo)
        }
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 // Reference RSSI at 1 meter
        return if (rssi == 0) -1.0 else {
            val ratio = rssi * 1.0 / txPower
            if (ratio < 1.0) Math.pow(ratio, 10.0)
            else (0.89976) * Math.pow(ratio, 7.7095) + 0.111
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun cleanup() {
        stopScanning()
    }
}