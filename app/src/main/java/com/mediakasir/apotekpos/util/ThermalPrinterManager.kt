package com.mediakasir.apotekpos.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.utils.formatDateTime
import com.mediakasir.apotekpos.utils.formatIDR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth and USB thermal printer connection and ESC/POS receipt printing.
 * Supports standard 58mm and 80mm ESC/POS compatible printers.
 */
object ThermalPrinterManager {

    private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    const val ACTION_USB_PERMISSION = "com.mediakasir.apotekpos.USB_PERMISSION"

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)                    // Initialize
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)     // Center align
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)       // Left align
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)          // Bold on
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)         // Bold off
    private val ESC_DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10) // Double height
    private val ESC_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)           // Normal size
    private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00)              // Full cut
    private val ESC_FEED_3 = byteArrayOf(0x1B, 0x64, 0x03)           // Feed 3 lines

    private var connectedSocket: BluetoothSocket? = null
    private var connectedDeviceAddress: String? = null

    /** Char width for 58mm printer (32 chars). Adjust for 80mm → 48. */
    private const val RECEIPT_WIDTH = 32

    // ── Bluetooth ────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices?.toList().orEmpty()
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        context: Context,
        device: BluetoothDevice,
        transaction: Transaction,
        pharmacyName: String,
        address: String,
        phone: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = connectDevice(device)
            val out = socket.outputStream
            val data = buildReceiptBytes(transaction, pharmacyName, address, phone)
            out.write(data)
            out.flush()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(device: BluetoothDevice): BluetoothSocket {
        connectedSocket?.let { socket ->
            if (socket.isConnected && device.address == connectedDeviceAddress) {
                return socket
            }
            runCatching { socket.close() }
        }
        val uuid = UUID.fromString(SPP_UUID)
        val socket = device.createRfcommSocketToServiceRecord(uuid)
        socket.connect()
        connectedSocket = socket
        connectedDeviceAddress = device.address
        return socket
    }

    fun disconnect() {
        runCatching { connectedSocket?.close() }
        connectedSocket = null
        connectedDeviceAddress = null
    }

    // ── USB ──────────────────────────────────────────────────────────────────

    fun getUsbPrinters(context: Context): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return emptyList()
        return usbManager.deviceList.values.toList()
    }

    fun hasUsbPermission(context: Context, device: UsbDevice): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return false
        return usbManager.hasPermission(device)
    }

    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val intent = android.content.Intent(ACTION_USB_PERMISSION)
        val pendingIntent = android.app.PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager.requestPermission(device, pendingIntent)
    }

    suspend fun printReceiptUsb(
        context: Context,
        device: UsbDevice,
        transaction: Transaction,
        pharmacyName: String,
        address: String,
        phone: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            if (!usbManager.hasPermission(device)) {
                throw SecurityException("Izin USB belum diberikan")
            }
            val connection = usbManager.openDevice(device)
                ?: throw IllegalStateException("Gagal membuka perangkat USB")
            try {
                var bulkOutEndpoint: android.hardware.usb.UsbEndpoint? = null
                var claimedInterface: android.hardware.usb.UsbInterface? = null
                outer@ for (i in 0 until device.interfaceCount) {
                    val intf = device.getInterface(i)
                    for (j in 0 until intf.endpointCount) {
                        val ep = intf.getEndpoint(j)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                            ep.direction == UsbConstants.USB_DIR_OUT
                        ) {
                            bulkOutEndpoint = ep
                            connection.claimInterface(intf, true)
                            claimedInterface = intf
                            break@outer
                        }
                    }
                }
                val ep = bulkOutEndpoint
                    ?: throw IllegalStateException("Endpoint bulk-out tidak ditemukan pada printer")
                val data = buildReceiptBytes(transaction, pharmacyName, address, phone)
                val sent = connection.bulkTransfer(ep, data, data.size, 5000)
                if (sent < 0) throw IllegalStateException("Gagal mengirim data ke printer USB")
            } finally {
                connection.close()
            }
        }
    }

    // ── Shared ESC/POS receipt builder ───────────────────────────────────────

    private fun buildReceiptBytes(
        transaction: Transaction,
        pharmacyName: String,
        address: String,
        phone: String,
    ): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(ESC_INIT)

        // Header
        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write(ESC_DOUBLE_HEIGHT_ON)
        out.printLine(pharmacyName)
        out.write(ESC_NORMAL)
        out.write(ESC_BOLD_OFF)
        out.printLine(address)
        if (phone.isNotBlank()) out.printLine("Telp: $phone")
        out.printLine("================================")

        out.write(ESC_ALIGN_LEFT)

        // Transaction info
        if (transaction.isPendingSync) {
            out.printLine("*** Menunggu sinkron ***")
        }
        out.printLine("No : ${transaction.transactionNumber}")
        out.printLine("Tgl: ${formatDateTime(transaction.createdAt)}")
        out.printLine("Kasir: ${transaction.cashierName}")
        if (transaction.cashierName != transaction.branchName && transaction.branchName.isNotBlank()) {
            out.printLine("Cab: ${transaction.branchName}")
        }
        out.printLine("--------------------------------")

        // Items
        for (item in transaction.items) {
            out.printLine(item.productName)
            val detail = "  ${item.qty} ${item.unit} x ${formatIDR(item.sellPrice)}"
            out.printLine(padLine(detail, formatIDR(item.subtotal)))
        }
        out.printLine("--------------------------------")

        // Totals
        out.printLine(padLine("Subtotal", formatIDR(transaction.subtotal)))
        if (transaction.discount > 0) {
            out.printLine(padLine("Diskon", "-${formatIDR(transaction.discount)}"))
        }
        out.write(ESC_BOLD_ON)
        out.printLine(padLine("TOTAL", formatIDR(transaction.totalAmount)))
        out.write(ESC_BOLD_OFF)
        out.printLine("--------------------------------")

        // Payment details
        for (p in transaction.paymentDetails) {
            out.printLine(padLine(p.method.uppercase(), formatIDR(p.amount)))
        }
        out.printLine(padLine("Kembalian", formatIDR(transaction.change)))

        // Footer
        out.printLine("================================")
        out.write(ESC_ALIGN_CENTER)
        out.printLine("Terima Kasih!")
        out.printLine("")

        out.write(ESC_FEED_3)
        out.write(ESC_CUT)

        return out.toByteArray()
    }

    private fun OutputStream.printLine(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
        write("\n".toByteArray())
    }

    private fun padLine(left: String, right: String): String {
        val gap = RECEIPT_WIDTH - left.length - right.length
        return if (gap > 0) left + " ".repeat(gap) + right else "$left $right"
    }

    // ── Shift Report ─────────────────────────────────────────────────────────

    /**
     * Lightweight model carrying all fields needed to print a shift closing report.
     * Populated from [com.mediakasir.apotekpos.ui.main.pos.ShiftSummaryData] in the UI layer.
     */
    data class ShiftReportData(
        val pharmacyName: String,
        val shiftType: String,
        val cashierName: String,
        val branchName: String,
        val startedAt: String,
        val endedAt: String,
        val startingCash: Double,
        val endingCash: Double,
        val expectedCash: Double,
        val difference: Double,
        val totalSales: Double,
        val totalCashSales: Double,
        val totalNonCashSales: Double,
        val totalTransactions: Int,
    )

    @SuppressLint("MissingPermission")
    suspend fun printShiftReport(
        context: Context,
        device: BluetoothDevice,
        report: ShiftReportData,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val socket = bm.adapter
                .getRemoteDevice(device.address)
                .createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
            socket.connect()
            try {
                val data = buildShiftReportBytes(report)
                socket.outputStream.write(data)
                socket.outputStream.flush()
            } finally {
                socket.close()
            }
        }
    }

    suspend fun printShiftReportUsb(
        context: Context,
        device: UsbDevice,
        report: ShiftReportData,
    ) = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val connection = usbManager.openDevice(device)
            ?: throw IllegalStateException("Tidak bisa membuka koneksi USB printer")
        var bulkOutEndpoint: android.hardware.usb.UsbEndpoint? = null
        var claimedInterface: android.hardware.usb.UsbInterface? = null
        outer@ for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    ep.direction == UsbConstants.USB_DIR_OUT
                ) {
                    connection.claimInterface(intf, true)
                    bulkOutEndpoint = ep
                    claimedInterface = intf
                    break@outer
                }
            }
        }
        try {
            val ep = bulkOutEndpoint
                ?: throw IllegalStateException("Endpoint bulk-out tidak ditemukan pada printer USB")
            val data = buildShiftReportBytes(report)
            val sent = connection.bulkTransfer(ep, data, data.size, 5000)
            if (sent < 0) throw IllegalStateException("Gagal mengirim laporan shift ke printer USB")
        } finally {
            connection.close()
        }
    }

    private fun buildShiftReportBytes(report: ShiftReportData): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(ESC_INIT)

        // Header
        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write(ESC_DOUBLE_HEIGHT_ON)
        out.printLine(report.pharmacyName)
        out.write(ESC_NORMAL)
        out.write(ESC_BOLD_OFF)
        out.printLine("LAPORAN TUTUP SHIFT")
        out.printLine("================================")

        // Shift identity
        out.write(ESC_ALIGN_LEFT)
        val label = report.shiftType.replaceFirstChar { it.uppercase() }
        out.printLine("Shift    : $label")
        out.printLine("Kasir    : ${report.cashierName}")
        if (report.branchName.isNotBlank()) out.printLine("Cabang   : ${report.branchName}")
        out.printLine("Mulai    : ${formatDateTime(report.startedAt)}")
        out.printLine("Selesai  : ${formatDateTime(report.endedAt)}")

        // Duration
        runCatching {
            val start = java.time.Instant.parse(report.startedAt)
            val end = java.time.Instant.parse(report.endedAt)
            val mins = java.time.Duration.between(start, end).toMinutes()
            val h = mins / 60; val m = mins % 60
            out.printLine("Durasi   : ${h}j ${m}m")
        }
        out.printLine("--------------------------------")

        // Sales summary
        out.write(ESC_BOLD_ON)
        out.printLine("RINGKASAN PENJUALAN")
        out.write(ESC_BOLD_OFF)
        out.printLine(padLine("Total Transaksi", "${report.totalTransactions}"))
        out.printLine(padLine("Total Penjualan", formatIDR(report.totalSales)))
        out.printLine(padLine("  Tunai", formatIDR(report.totalCashSales)))
        out.printLine(padLine("  Non-Tunai", formatIDR(report.totalNonCashSales)))
        out.printLine("--------------------------------")

        // Cash reconciliation
        out.write(ESC_BOLD_ON)
        out.printLine("REKAP KAS")
        out.write(ESC_BOLD_OFF)
        out.printLine(padLine("Kas Awal", formatIDR(report.startingCash)))
        out.printLine(padLine("Pemasukan Tunai", formatIDR(report.totalCashSales)))
        out.printLine(padLine("Kas Expected", formatIDR(report.expectedCash)))
        out.printLine(padLine("Kas Aktual", formatIDR(report.endingCash)))
        out.write(ESC_BOLD_ON)
        val diffLabel = if (report.difference >= 0) "Selisih (Lebih)" else "Selisih (Kurang)"
        val diffPrefix = if (report.difference > 0) "+" else ""
        out.printLine(padLine(diffLabel, "$diffPrefix${formatIDR(report.difference)}"))
        out.write(ESC_BOLD_OFF)

        // Footer
        out.printLine("================================")
        out.write(ESC_ALIGN_CENTER)
        out.printLine("Laporan otomatis dari sistem")
        out.printLine("ApotekPOS")
        out.printLine("")

        out.write(ESC_FEED_3)
        out.write(ESC_CUT)

        return out.toByteArray()
    }
}
