package fi.tuni.lonelinessapp.domain.model

data class DeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val distance: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun toString(): String {
        return "Name: $name\nAddress: $address\nRSSI: $rssi\nDistance: ${String.format("%.2f", distance)}m"
    }
}