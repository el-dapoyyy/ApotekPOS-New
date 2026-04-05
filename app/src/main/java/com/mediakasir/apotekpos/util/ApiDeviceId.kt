package com.mediakasir.apotekpos.util

import java.util.zip.CRC32

/**
 * Header `X-Device-ID` dan field login `device_id` dipakai backend untuk `sync_logs` dll.
 *
 * - Jika kolom DB bertipe **integer**, string hex (mis. `d838e919`) memicu SQLSTATE 1366.
 * - Jika kolom terlalu pendek untuk ANDROID_ID penuh, bisa memicu truncation.
 *
 * Kita kirim **angka desimal stabil** (0..2^31-1) dari CRC32(ANDROID_ID), aman untuk kolom INT/BIGINT.
 */
object ApiDeviceId {

    /** Nilai positif yang muat di MySQL `INT` bertanda (max 2147483647). */
    fun fromAndroidId(androidId: String): String {
        val raw = androidId.ifBlank { "unknown" }
        val crc = CRC32()
        crc.update(raw.toByteArray(Charsets.UTF_8))
        var n = crc.value and 0x7FFF_FFFFL
        if (n == 0L) n = 1L
        return n.toString()
    }
}
