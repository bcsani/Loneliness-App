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
import fi.tuni.lonelinessapp.data.repository.DayRepository
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothProximityManager(
    private val context: Context,
    private val onScanStatusChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
    private val dayRepository: DayRepository,
    private val analysisViewModel: AnalysisViewModel
) {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false
    private val scannedDevices = mutableSetOf<String>()
    private var totalDevices: Int = 0
    private var totalDuration: Float = 0.0f
    private val deviceDetectionMap = mutableMapOf<String, Long>()
    private val deviceTimeoutMap = mutableMapOf<String, Runnable>()
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val PROXIMITY_RSSI_THRESHOLD = -70
        private const val SCAN_PERIOD: Long = 10000L
        private const val DEVICE_TIMEOUT_MS: Long = 3000L
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

        if (!::bluetoothAdapter.isInitialized || !bluetoothAdapter.isEnabled) {
            initializeBluetooth()
        }

        // Check if scanner is available
        if (!bluetoothAdapter.isEnabled) {
            onError("Please enable Bluetooth to start scanning")
            return
        }

        // Initialize scanner if not already done
        if (!::bluetoothLeScanner.isInitialized) {
            try {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            } catch (e: Exception) {
                onError("Failed to initialize Bluetooth scanner: ${e.message}")
                return
            }
        }

        scannedDevices.clear()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bluetoothLeScanner.startScan(emptyList(), scanSettings, leScanCallback)
            scanning = true
            onScanStatusChanged(true)

            // Auto-stop after scan period
            Handler(Looper.getMainLooper()).postDelayed({
                stopScanning()
            }, SCAN_PERIOD)
        } catch (e: SecurityException) {
            onError("Bluetooth permission denied: ${e.message}")
        } catch (e: IllegalStateException) {
            onError("Bluetooth is not available: ${e.message}")
        } catch (e: Exception) {
            onError("Failed to start scan: ${e.message}")
        }

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        if (scanning) {
            // Handle all remaining devices as timed out
            val currentTime = System.currentTimeMillis()
            deviceDetectionMap.forEach { (deviceAddress, startTime) ->
                val duration = (currentTime - startTime) / 1000.0f
                totalDuration += duration
            }


            // Add total duration to the database
            coroutineScope.launch {
                println("totalDevices: $totalDevices")
                println("totalDuration: $totalDuration")
                dayRepository.addSignalToday(signal = totalDuration.toInt())
                dayRepository.addSignalAmountToday(signalAmount = totalDevices)
                analysisViewModel.updateSignalDuration()
            }

            // Clear all tracking data
            deviceDetectionMap.clear()
            deviceTimeoutMap.clear()
            scannedDevices.clear()

            // Remove any pending callbacks
            handler.removeCallbacksAndMessages(null)

            bluetoothLeScanner.stopScan(leScanCallback)
            scanning = false
            onScanStatusChanged(false)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi

        if (rssi >= PROXIMITY_RSSI_THRESHOLD) {
            // Check if this is a new detection
            val isNewDevice = !scannedDevices.contains(device.address)

            if (isNewDevice) {
                scannedDevices.add(device.address)
                totalDevices += 1
            }

            // Track detection time
            handleDeviceDetection(device.address)
        } else {
            // Device is out of range
            handleDeviceTimeout(device.address)
        }
    }

    private fun handleDeviceDetection(deviceAddress: String){
        val currentTime = System.currentTimeMillis()

        // If device was not previously detected, start tracking
        if (!deviceDetectionMap.containsKey(deviceAddress)) {
            deviceDetectionMap[deviceAddress] = currentTime
        }

        // Cancel any existing timeout for this device
        deviceTimeoutMap[deviceAddress]?.let {
            handler.removeCallbacks(it)
            deviceTimeoutMap.remove(deviceAddress)
        }

        // Schedule a new timeout to detect when device goes out of range
        val timeoutRunnable = Runnable {
            handleDeviceTimeout(deviceAddress)
        }

        handler.postDelayed(timeoutRunnable, DEVICE_TIMEOUT_MS)
        deviceTimeoutMap[deviceAddress] = timeoutRunnable
    }

    private fun handleDeviceTimeout(deviceAddress: String) {
        deviceDetectionMap[deviceAddress]?.let { startTime ->
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000.0f

            // Add to total duration
            totalDuration += duration

            // Remove from tracking maps
            deviceDetectionMap.remove(deviceAddress)
            deviceTimeoutMap.remove(deviceAddress)
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

    fun getTotalDuration(): Float {
        val currentTime = System.currentTimeMillis()
        var ongoingDuration = 0.0f

        deviceDetectionMap.forEach { (_, startTime) ->
            ongoingDuration += (currentTime - startTime) / 1000.0f
        }

        return totalDuration + ongoingDuration
    }

    fun getTotalDevice(): Int {
        return totalDevices
    }
}