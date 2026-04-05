package com.mediakasir.apotekpos.ui

import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo

/**
 * ID cabang dari respons login (disimpan di user/license) — untuk **partisi cache Room** dan label UI,
 * bukan untuk mengganti scoping server. Data POS/transaksi dari API sudah difilter backend lewat token.
 */
fun effectiveBranchId(license: LicenseInfo?, user: UserInfo?): String =
    license?.branchId?.takeIf { it.isNotBlank() } ?: user?.branchId.orEmpty()

fun effectiveBranchName(license: LicenseInfo?, user: UserInfo?): String =
    license?.branchName?.takeIf { it.isNotBlank() } ?: user?.branchName.orEmpty()
