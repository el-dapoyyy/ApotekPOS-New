# 📱 API DOKUMENTASI - TIM MOBILE APOAPPS

**Status**: ✅ **BACKEND READY FOR INTEGRATION**
**Versi**: 2.1
**Update**: April 4, 2026
**Base URL Dev**: `http://apoapps.test/api`
**Base URL Prod**: `https://api.apoapps.id/api`

---

## 🎯 BACKEND STATUS

```
✓ Laravel 11 + PHP 8.2
✓ Database: MySQL 8.0
✓ Authentication: Laravel Sanctum (Bearer Token)
✓ Multi-Tenant: Automatic partner_id scoping
✓ RBAC: 5-tier role hierarchy (super_admin, owner, admin, apoteker, kasir)
```

### Modules Tersedia

| Module | Base Path | Endpoints | Status |
|--------|-----------|-----------|--------|
| **Auth** | `/api/auth/*` | 4 | ✅ AKTIF |
| **POS - Products** | `/api/pos/products/*` | 5 | ✅ AKTIF |
| **POS - Transactions** | `/api/pos/transactions/*` | 3 | ✅ AKTIF |
| **POS - Returns** | `/api/pos/returns/*` | 3 | ✅ AKTIF |
| **POS - Customers** | `/api/pos/customers/*` | 4 | ✅ AKTIF |
| **POS - Doctors** | `/api/pos/doctors/*` | 3 | ✅ AKTIF |
| **POS - Prescriptions** | `/api/pos/prescriptions/*` | 3 | ✅ AKTIF |
| **Settings - Partners** | `/api/settings/partners` | 5 | ✅ AKTIF |
| **Settings - Branches** | `/api/settings/branches` | 6 | ✅ AKTIF |
| **Settings - Subscriptions** | `/api/settings/subscriptions` | 5 | ✅ AKTIF |
| **Settings - License Extensions** | `/api/settings/license-extensions` | 5 | ✅ AKTIF |
| **Alerts - Expiry** | `/api/alerts/expiry/*` | 7 | ✅ AKTIF |
| **Alerts - Stock** | `/api/alerts/stock/*` | 7 | ✅ AKTIF |
| **Alerts - Summary** | `/api/alerts/summary` | 1 | ✅ AKTIF |

> ✅ **Semua route sudah aktif.** Total: **56 endpoint** tersedia.

---

## 🔐 AUTENTIKASI

### Headers Wajib

Semua request yang memerlukan autentikasi harus menyertakan:

```
Authorization: Bearer {token}
Content-Type: application/json
Accept: application/json
X-Device-ID: {device_unique_id}
```

### 1. Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| email | string | ✅ | Email user |
| password | string | ✅ | Password user |
| device_id | string | ✅ | Device unique ID (ANDROID_ID) |
| device_name | string | ❌ | Nama device (contoh: Samsung Galaxy S21) |
| device_model | string | ❌ | Model device (contoh: SM-G991B) |

**Contoh Request:**
```json
{
    "email": "kasir@apotek.com",
    "password": "password123",
    "device_id": "abc123def456",
    "device_name": "Samsung Galaxy S21",
    "device_model": "SM-G991B"
}
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Login berhasil",
    "data": {
        "user": {
            "id": 1,
            "name": "Kasir 1",
            "email": "kasir@apotek.com",
            "role": "kasir"
        },
        "branch": {
            "id": 1,
            "name": "Apotek Sehat Cabang Jakarta",
            "address": "Jl. Sudirman No. 123",
            "phone": "021-12345678",
            "sia_number": "SIA.446/2024/DKI",
            "apoteker_name": "Apt. Ahmad Fauzi, S.Farm",
            "sipa_number": "SIPA.123/2024/DKI"
        },
        "partner": {
            "id": 1,
            "name": "PT Apotek Sehat Indonesia"
        },
        "license": {
            "status": "active",
            "expired_at": "2027-02-05",
            "days_remaining": 365,
            "is_trial": false
        },
        "token": "1|abc123xyz789...",
        "expires_at": "2026-05-04T00:00:00.000000Z"
    }
}
```

**Response Error - Kredensial Salah (401):**
```json
{
    "success": false,
    "message": "Email atau password salah",
    "error_code": "INVALID_CREDENTIALS"
}
```

**Response Error - User Tidak Aktif (403):**
```json
{
    "success": false,
    "message": "Akun Anda tidak aktif. Hubungi administrator.",
    "error_code": "USER_INACTIVE"
}
```

**Response Error - Partner Tidak Aktif (403):**
```json
{
    "success": false,
    "message": "Partner tidak aktif",
    "error_code": "PARTNER_INACTIVE"
}
```

**Response Error - User Tidak Punya Branch (403):**
```json
{
    "success": false,
    "message": "User tidak memiliki cabang",
    "error_code": "NO_BRANCH"
}
```

**Response Error - Lisensi Expired (403):**
```json
{
    "success": false,
    "message": "Lisensi cabang telah expired",
    "error_code": "LICENSE_EXPIRED",
    "data": {
        "expired_at": "2025-12-31",
        "days_expired": 95
    }
}
```

---

### 2. Get Current User Info (Me)

**Endpoint:** `GET /api/auth/me`

**Headers:** Memerlukan autentikasi

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "user": {
            "id": 1,
            "name": "Kasir 1",
            "email": "kasir@apotek.com",
            "role": "kasir"
        },
        "branch": {
            "id": 1,
            "name": "Apotek Sehat Cabang Jakarta"
        },
        "active_shift": {
            "id": 5,
            "clock_in": "2026-04-04T08:00:00.000000Z",
            "starting_cash": 500000
        }
    }
}
```

> **Catatan:** `active_shift` akan `null` jika kasir belum clock-in hari ini.

---

### 3. Logout (Current Device)

**Endpoint:** `POST /api/auth/logout`

**Headers:** Memerlukan autentikasi

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Logout berhasil"
}
```

---

### 4. Logout dari Semua Perangkat

**Endpoint:** `POST /api/auth/logout-all`

**Headers:** Memerlukan autentikasi

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Logout dari semua perangkat berhasil"
}
```

---

## 📦 POS - PRODUCTS

> ⚠️ **Status Route:** Controller sudah ada di `App\Http\Controllers\Api\Pos\ProductSyncController`. Route perlu di-uncomment di `routes/api.php`.

### 5. Sync Produk (Download ke Android)

Download produk untuk disimpan di lokal Room Database Android. Mendukung incremental sync.

**Endpoint:** `GET /api/pos/products/sync`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| page | integer | 1 | Halaman saat ini |
| per_page | integer | 100 | Jumlah item per halaman (max 200) |
| updated_since | datetime | null | ISO 8601, untuk incremental sync |

**Contoh Request:**
```
GET /api/pos/products/sync?page=1&per_page=100
GET /api/pos/products/sync?updated_since=2026-03-01T10:00:00Z
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Sync berhasil",
    "data": {
        "products": [
            {
                "id": 1,
                "sku": "PRD001",
                "barcode": "8991234567890",
                "name": "Paracetamol 500mg",
                "generic_name": "Paracetamol",
                "category_id": 1,
                "category_name": "Obat Bebas",
                "unit_id": 1,
                "unit_name": "Tablet",
                "unit_abbreviation": "Tab",
                "brand_name": "Kimia Farma",
                "purchase_price": 500.00,
                "selling_price": 1000.00,
                "min_stock": 100,
                "drug_class": "bebas",
                "requires_prescription": false,
                "dosage_form": "Tablet",
                "strength": "500mg",
                "stock": {
                    "total": 500,
                    "is_low": false
                },
                "nearest_expiry": {
                    "batch_number": "BTH001",
                    "expired_date": "2027-06-30",
                    "quantity": 200
                },
                "updated_at": "2026-02-05T10:30:00.000000Z"
            }
        ],
        "pagination": {
            "current_page": 1,
            "per_page": 100,
            "total_items": 250,
            "total_pages": 3,
            "has_more": true
        },
        "sync_info": {
            "server_time": "2026-04-04T14:30:00.000000Z",
            "next_sync_token": "2026-04-04T14:30:00.000000Z"
        }
    }
}
```

> **Catatan Penting:**
> - Simpan `next_sync_token` sebagai `updated_since` untuk sync berikutnya
> - Loop request sampai `has_more` = false untuk download semua produk
> - `stock.total` = total dari semua batch di branch user yang login
> - `nearest_expiry` = batch dengan tanggal kadaluarsa terdekat (FEFO)

---

### 6. Cari Produk

**Endpoint:** `GET /api/pos/products/search`

**Query Parameters:**

| Parameter | Tipe | Wajib | Keterangan |
|-----------|------|-------|------------|
| q | string | ✅ | Keyword (min 2 karakter). Cari di: nama, SKU, barcode, nama generik |
| limit | integer | ❌ | Jumlah hasil (default 20, max 50) |

**Contoh Request:**
```
GET /api/pos/products/search?q=paracetamol&limit=10
GET /api/pos/products/search?q=8991234567890
```

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "products": [
            {
                "id": 1,
                "sku": "PRD001",
                "barcode": "8991234567890",
                "name": "Paracetamol 500mg",
                "generic_name": "Paracetamol",
                "category_id": 1,
                "category_name": "Obat Bebas",
                "unit_id": 1,
                "unit_name": "Tablet",
                "unit_abbreviation": "Tab",
                "brand_name": "Kimia Farma",
                "purchase_price": 500.00,
                "selling_price": 1000.00,
                "min_stock": 100,
                "drug_class": "bebas",
                "requires_prescription": false,
                "dosage_form": "Tablet",
                "strength": "500mg",
                "stock": {
                    "total": 500,
                    "is_low": false
                },
                "nearest_expiry": {
                    "batch_number": "BTH001",
                    "expired_date": "2027-06-30",
                    "quantity": 200
                },
                "updated_at": "2026-02-05T10:30:00.000000Z"
            }
        ],
        "total": 1
    }
}
```

**Response Error - Keyword Terlalu Pendek (400):**
```json
{
    "success": false,
    "message": "Keyword minimal 2 karakter",
    "error_code": "KEYWORD_TOO_SHORT"
}
```

---

### 7. Detail Produk (dengan semua Batch)

**Endpoint:** `GET /api/pos/products/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "sku": "PRD001",
        "barcode": "8991234567890",
        "name": "Paracetamol 500mg",
        "generic_name": "Paracetamol",
        "category_id": 1,
        "category_name": "Obat Bebas",
        "unit_id": 1,
        "unit_name": "Tablet",
        "unit_abbreviation": "Tab",
        "brand_name": "Kimia Farma",
        "purchase_price": 500.00,
        "selling_price": 1000.00,
        "min_stock": 100,
        "drug_class": "bebas",
        "requires_prescription": false,
        "dosage_form": "Tablet",
        "strength": "500mg",
        "description": "Obat pereda nyeri dan penurun demam",
        "contraindications": "Hipersensitif terhadap paracetamol",
        "side_effects": "Mual, ruam kulit",
        "storage_instructions": "Simpan di tempat kering, suhu ruang",
        "manufacturer": "PT Kimia Farma",
        "stock": {
            "total": 500,
            "is_low": false
        },
        "nearest_expiry": {
            "batch_number": "BTH001",
            "expired_date": "2027-06-30",
            "quantity": 200
        },
        "batches": [
            {
                "id": 1,
                "batch_number": "BTH001",
                "expired_date": "2027-06-30",
                "quantity": 200,
                "purchase_price": 500.00
            },
            {
                "id": 2,
                "batch_number": "BTH002",
                "expired_date": "2027-12-31",
                "quantity": 300,
                "purchase_price": 520.00
            }
        ],
        "unit_conversions": [
            {
                "unit_id": 1,
                "unit_name": "Tablet",
                "conversion_factor": 1.0,
                "selling_price": 1000.00
            },
            {
                "unit_id": 2,
                "unit_name": "Strip",
                "conversion_factor": 10.0,
                "selling_price": 9500.00
            }
        ],
        "updated_at": "2026-02-05T10:30:00.000000Z"
    }
}
```

---

### 8. Daftar Kategori Produk

**Endpoint:** `GET /api/pos/products/categories`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "name": "Obat Bebas",
            "description": "Obat yang dapat dibeli tanpa resep dokter"
        },
        {
            "id": 2,
            "name": "Obat Keras",
            "description": "Obat yang memerlukan resep dokter"
        }
    ]
}
```

---

### 9. Daftar Satuan

**Endpoint:** `GET /api/pos/products/units`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "name": "Tablet",
            "abbreviation": "Tab"
        },
        {
            "id": 2,
            "name": "Strip",
            "abbreviation": "Str"
        },
        {
            "id": 3,
            "name": "Box",
            "abbreviation": "Box"
        }
    ]
}
```

---

## 💳 POS - TRANSACTIONS

> ⚠️ **Status Route:** Controller sudah ada di `App\Http\Controllers\Api\Pos\TransactionSyncController`. Route perlu di-uncomment.

### 10. Upload / Sync Transaksi Offline ⭐

Upload batch transaksi dari Android ke server. Mendukung idempotency (duplikat akan di-skip, bukan error).

**Endpoint:** `POST /api/pos/transactions/sync`

**Request Body:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| transactions | array | ✅ | Array transaksi |
| transactions[].local_transaction_id | string | ✅ | ID unik dari Android (untuk idempotency) |
| transactions[].customer_id | integer | ❌ | ID pelanggan (null = umum/anonymous) |
| transactions[].prescription_id | integer | ❌ | ID resep dokter (jika ada) |
| transactions[].subtotal | decimal | ✅ | Subtotal sebelum pajak/diskon |
| transactions[].discount_amount | decimal | ❌ | Jumlah diskon (default 0) |
| transactions[].tax_amount | decimal | ❌ | Jumlah pajak (default 0) |
| transactions[].tusla_amount | decimal | ❌ | Biaya tusla/embalase (default 0) |
| transactions[].embalse_amount | decimal | ❌ | Biaya embalase (default 0) |
| transactions[].grand_total | decimal | ✅ | Total akhir |
| transactions[].payment_method | string | ✅ | cash/qris/debit/credit/transfer/split |
| transactions[].payment_status | string | ✅ | paid/partial/unpaid |
| transactions[].notes | string | ❌ | Catatan tambahan |
| transactions[].completed_at | datetime | ❌ | ISO 8601, waktu transaksi selesai |
| transactions[].items | array | ✅ | Daftar item produk |
| transactions[].payments | array | ❌ | Detail pembayaran (untuk split payment) |

**Struktur Item:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| product_id | integer | ✅ | ID produk |
| batch_id | integer | ❌ | ID batch spesifik. Jika null, sistem FEFO otomatis |
| quantity | decimal | ✅ | Jumlah |
| unit_price | decimal | ✅ | Harga satuan |
| discount | decimal | ❌ | Diskon per item (default 0) |
| subtotal | decimal | ✅ | Subtotal item |
| is_racikan | boolean | ❌ | Apakah item racikan? (default false) |

**Struktur Payment (split payment):**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| method | string | ✅ | cash/qris/debit/credit/transfer |
| amount | decimal | ✅ | Jumlah pembayaran |
| reference | string | ❌ | Nomor referensi (untuk QRIS/transfer) |

**Contoh Request - Transaksi Tunai Biasa:**
```json
{
    "transactions": [
        {
            "local_transaction_id": "TRX_1707228000_001",
            "customer_id": null,
            "subtotal": 50000,
            "discount_amount": 0,
            "tax_amount": 5500,
            "grand_total": 55500,
            "payment_method": "cash",
            "payment_status": "paid",
            "completed_at": "2026-04-04T14:00:00.000000Z",
            "items": [
                {
                    "product_id": 1,
                    "batch_id": null,
                    "quantity": 10,
                    "unit_price": 1000,
                    "discount": 0,
                    "subtotal": 10000
                },
                {
                    "product_id": 2,
                    "batch_id": 5,
                    "quantity": 2,
                    "unit_price": 20000,
                    "discount": 0,
                    "subtotal": 40000
                }
            ],
            "payments": [
                {
                    "method": "cash",
                    "amount": 55500
                }
            ]
        }
    ]
}
```

**Contoh Request - Split Payment:**
```json
{
    "transactions": [
        {
            "local_transaction_id": "TRX_1707228000_002",
            "customer_id": 1,
            "subtotal": 150000,
            "discount_amount": 0,
            "tax_amount": 16500,
            "grand_total": 166500,
            "payment_method": "split",
            "payment_status": "paid",
            "completed_at": "2026-04-04T14:30:00.000000Z",
            "items": [
                {
                    "product_id": 5,
                    "quantity": 3,
                    "unit_price": 50000,
                    "discount": 0,
                    "subtotal": 150000
                }
            ],
            "payments": [
                {
                    "method": "cash",
                    "amount": 100000
                },
                {
                    "method": "qris",
                    "amount": 66500,
                    "reference": "QRIS123456789"
                }
            ]
        }
    ]
}
```

**Contoh Request - Pembayaran Kredit (Partial):**
```json
{
    "transactions": [
        {
            "local_transaction_id": "TRX_1707228000_003",
            "customer_id": 1,
            "subtotal": 200000,
            "grand_total": 200000,
            "payment_method": "cash",
            "payment_status": "partial",
            "items": [
                {
                    "product_id": 3,
                    "quantity": 4,
                    "unit_price": 50000,
                    "subtotal": 200000
                }
            ],
            "payments": [
                {
                    "method": "cash",
                    "amount": 100000
                }
            ]
        }
    ]
}
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Sync selesai: 2 berhasil, 0 gagal",
    "data": {
        "results": [
            {
                "local_transaction_id": "TRX_1707228000_001",
                "server_transaction_id": 156,
                "invoice_number": "INV/1/20260404/0001",
                "success": true,
                "message": "Synced successfully",
                "status": "created"
            },
            {
                "local_transaction_id": "TRX_1707228000_002",
                "server_transaction_id": 157,
                "invoice_number": "INV/1/20260404/0002",
                "success": true,
                "message": "Synced successfully",
                "status": "created"
            }
        ],
        "summary": {
            "total": 2,
            "success": 2,
            "failed": 0
        },
        "server_time": "2026-04-04T14:35:00.000000Z"
    }
}
```

**Response - Transaksi Sudah Pernah Sync (Duplikat, tetap 200):**
```json
{
    "success": true,
    "message": "Sync selesai: 1 berhasil, 0 gagal",
    "data": {
        "results": [
            {
                "local_transaction_id": "TRX_1707228000_001",
                "server_transaction_id": 156,
                "invoice_number": "INV/1/20260404/0001",
                "success": true,
                "message": "Already synced",
                "status": "duplicate"
            }
        ],
        "summary": {
            "total": 1,
            "success": 1,
            "failed": 0
        }
    }
}
```

**✅ Proses Otomatis Backend:**
1. ✅ Deduplikasi via `local_transaction_id` (idempotent)
2. ✅ FEFO batch selection (jika `batch_id` null)
3. ✅ Multi-batch allocation (jika stok satu batch tidak cukup)
4. ✅ Deduct stock dari batches + stock movement log
5. ✅ Generate `invoice_number` format: `INV/{branch_id}/{YYYYMMDD}/{seq}`
6. ✅ Buat `AccountReceivable` jika payment_status = partial/unpaid
7. ✅ Cek lisensi sebelum sync (error jika expired)

---

### 11. Riwayat Transaksi

**Endpoint:** `GET /api/pos/transactions/history`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| page | integer | 1 | Halaman |
| per_page | integer | 20 | Item per halaman (max 50) |
| start_date | date | null | Filter tanggal mulai (YYYY-MM-DD) |
| end_date | date | null | Filter tanggal akhir (YYYY-MM-DD) |

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "transactions": [
            {
                "id": 156,
                "invoice_number": "INV/1/20260404/0001",
                "local_transaction_id": "TRX_1707228000_001",
                "customer_name": "Umum",
                "grand_total": 55500.00,
                "payment_status": "paid",
                "items_count": 2,
                "completed_at": "2026-04-04T14:00:00.000000Z",
                "synced_at": "2026-04-04T14:35:00.000000Z"
            }
        ],
        "pagination": {
            "current_page": 1,
            "per_page": 20,
            "total_items": 156,
            "total_pages": 8
        }
    }
}
```

---

### 12. Detail Transaksi

**Endpoint:** `GET /api/pos/transactions/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 156,
        "invoice_number": "INV/1/20260404/0001",
        "local_transaction_id": "TRX_1707228000_001",
        "customer": {
            "id": 1,
            "name": "Budi Santoso",
            "phone": "081234567890"
        },
        "prescription": {
            "id": 5,
            "prescription_number": "RX-20260404-0001",
            "doctor_name": "Dr. Ahmad"
        },
        "subtotal": 50000.00,
        "discount_amount": 0.00,
        "tax_amount": 5500.00,
        "grand_total": 55500.00,
        "payment_status": "paid",
        "items": [
            {
                "product_id": 1,
                "product_name": "Paracetamol 500mg",
                "sku": "PRD001",
                "batch_number": "BTH001",
                "expired_date": "2027-06-30",
                "quantity": 10.0,
                "unit_price": 1000.00,
                "discount": 0.00,
                "subtotal": 10000.00
            }
        ],
        "payments": [
            {
                "method": "cash",
                "amount": 55500.00,
                "reference": null
            }
        ],
        "completed_at": "2026-04-04T14:00:00.000000Z",
        "synced_at": "2026-04-04T14:35:00.000000Z",
        "notes": null
    }
}
```

---

## 🔄 POS - RETURNS


### 13. Daftar Retur

**Endpoint:** `GET /api/pos/returns`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| page | integer | 1 | Halaman |
| per_page | integer | 20 | Item per halaman (max 50) |

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "returns": [
            {
                "id": 1,
                "return_number": "RTN/1/20260404/0001",
                "invoice_number": "INV/1/20260403/0050",
                "total_amount": 20000.00,
                "refund_method": "cash",
                "reason": "Obat rusak",
                "status": "completed",
                "items_count": 1,
                "created_at": "2026-04-04T15:00:00.000000Z"
            }
        ],
        "pagination": {
            "current_page": 1,
            "per_page": 20,
            "total_items": 5,
            "total_pages": 1
        }
    }
}
```

---

### 14. Buat Retur ⭐

**Endpoint:** `POST /api/pos/returns`

**Request Body:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| transaction_id | integer | ✅ | ID transaksi asli yang akan diretur |
| reason | string | ✅ | Alasan retur |
| refund_method | string | ❌ | cash/qris/transfer (default: cash) |
| notes | string | ❌ | Catatan tambahan |
| items | array | ✅ | Daftar item yang diretur |

**Struktur Item Retur:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| product_id | integer | ✅ | ID produk yang diretur |
| batch_id | integer | ❌ | ID batch (opsional, gunakan batch dari transaksi asli) |
| quantity | integer | ✅ | Jumlah yang diretur (tidak boleh melebihi qty original) |
| subtotal | decimal | ✅ | Nilai refund |
| condition | string | ❌ | good/damaged/expired (default: good) |
| restock | boolean | ❌ | Kembalikan ke stok? (default: true, hanya jika condition = good) |

**Contoh Request:**
```json
{
    "transaction_id": 156,
    "reason": "Obat rusak dalam pengiriman",
    "refund_method": "cash",
    "notes": "Kemasan penyok",
    "items": [
        {
            "product_id": 1,
            "quantity": 5,
            "subtotal": 5000,
            "condition": "damaged",
            "restock": false
        },
        {
            "product_id": 2,
            "batch_id": 5,
            "quantity": 1,
            "subtotal": 20000,
            "condition": "good",
            "restock": true
        }
    ]
}
```

**Response Sukses (201):**
```json
{
    "success": true,
    "message": "Retur berhasil dibuat",
    "data": {
        "id": 2,
        "return_number": "RTN/1/20260404/0002",
        "total_amount": 25000.00,
        "refund_method": "cash",
        "created_at": "2026-04-04T15:30:00.000000Z"
    }
}
```

**Response Error - Transaksi Sudah Ada Retur (400):**
```json
{
    "success": false,
    "message": "Transaksi ini sudah memiliki retur",
    "error_code": "RETURN_EXISTS",
    "data": {
        "return_id": 1,
        "return_number": "RTN/1/20260404/0001"
    }
}
```

**✅ Proses Otomatis Backend:**
1. ✅ Validasi: quantity retur tidak melebihi quantity original
2. ✅ Restore stok jika `condition = good` dan `restock = true`
3. ✅ Stock movement log (tipe: in)
4. ✅ Generate `return_number` format: `RTN/{branch_id}/{YYYYMMDD}/{seq}`

---

### 15. Detail Retur

**Endpoint:** `GET /api/pos/returns/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 2,
        "return_number": "RTN/1/20260404/0002",
        "transaction": {
            "id": 156,
            "invoice_number": "INV/1/20260404/0001",
            "grand_total": 55500.00
        },
        "total_amount": 25000.00,
        "refund_method": "cash",
        "reason": "Obat rusak dalam pengiriman",
        "status": "completed",
        "notes": "Kemasan penyok",
        "processed_by": "Kasir 1",
        "items": [
            {
                "product_id": 1,
                "product_name": "Paracetamol 500mg",
                "sku": "PRD001",
                "batch_number": "BTH001",
                "quantity": 5,
                "unit_price": 1000.00,
                "subtotal": 5000.00,
                "condition": "damaged",
                "restocked": false
            }
        ],
        "created_at": "2026-04-04T15:30:00.000000Z"
    }
}
```

---

---

## 👥 POS - CUSTOMERS

### 33. Daftar Pelanggan

**Endpoint:** `GET /api/pos/customers`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| search | string | - | Cari nama, HP, atau kode pelanggan |
| customer_type | string | - | regular / member / reseller |
| page | integer | 1 | Halaman |
| per_page | integer | 20 | Max 100 |

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "customers": [
            {
                "id": 1,
                "code": "CST00001",
                "name": "Budi Santoso",
                "phone": "081234567890",
                "customer_type": "member",
                "discount_percentage": 5.00,
                "credit_limit": 5000000.00,
                "current_credit": 1500000.00,
                "available_credit": 3500000.00,
                "is_credit_blocked": false
            }
        ],
        "pagination": {
            "current_page": 1,
            "per_page": 20,
            "total_items": 120,
            "total_pages": 6
        }
    }
}
```

---

### 34. Detail Pelanggan

**Endpoint:** `GET /api/pos/customers/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "code": "CST00001",
        "name": "Budi Santoso",
        "phone": "081234567890",
        "customer_type": "member",
        "discount_percentage": 5.00,
        "credit_limit": 5000000.00,
        "current_credit": 1500000.00,
        "available_credit": 3500000.00,
        "is_credit_blocked": false,
        "email": "budi@email.com",
        "address": "Jl. Mawar No. 10, Jakarta",
        "birth_date": "1990-05-15",
        "age": 35,
        "notes": null
    }
}
```

---

### 35. Tambah Pelanggan Baru (Quick Add)

**Endpoint:** `POST /api/pos/customers`

**Request Body:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| name | string | ✅ | Nama pelanggan |
| phone | string | ❌ | Nomor HP |
| email | string | ❌ | Email |
| address | string | ❌ | Alamat |
| birth_date | date | ❌ | Tanggal lahir (YYYY-MM-DD) |
| customer_type | string | ❌ | regular/member/reseller (default: regular) |
| discount_percentage | decimal | ❌ | Persen diskon (0-100) |
| credit_limit | decimal | ❌ | Limit kredit |
| notes | string | ❌ | Catatan |

**Response Sukses (201):**
```json
{
    "success": true,
    "message": "Pelanggan berhasil ditambahkan",
    "data": {
        "id": 121,
        "code": "CST00121",
        "name": "Siti Rahayu",
        "phone": "085678901234",
        "customer_type": "regular",
        "discount_percentage": 0.00,
        "credit_limit": 0.00,
        "current_credit": 0.00,
        "available_credit": 0.00,
        "is_credit_blocked": false
    }
}
```

---

### 36. Update Data Pelanggan

**Endpoint:** `PUT /api/pos/customers/{id}`

**Request Body:** Sama dengan POST, semua field opsional (`sometimes`)

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Data pelanggan berhasil diperbarui",
    "data": { ... }
}
```

---

## 🩺 POS - DOCTORS

### 37. Daftar Dokter

**Endpoint:** `GET /api/pos/doctors`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| search | string | - | Cari nama, SIP, atau spesialisasi dokter |
| page | integer | 1 | Halaman |
| per_page | integer | 30 | Max 100 |

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "doctors": [
            {
                "id": 1,
                "name": "Dr. Ahmad Fauzi, Sp.PD",
                "sip_number": "SIP.123/2024/DKI",
                "specialization": "Penyakit Dalam"
            }
        ],
        "pagination": {
            "current_page": 1,
            "per_page": 30,
            "total_items": 15,
            "total_pages": 1
        }
    }
}
```

---

### 38. Detail Dokter

**Endpoint:** `GET /api/pos/doctors/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "name": "Dr. Ahmad Fauzi, Sp.PD",
        "sip_number": "SIP.123/2024/DKI",
        "specialization": "Penyakit Dalam",
        "phone": "021-55556789",
        "clinic_name": "Klinik Sehat Sentosa",
        "address": "Jl. Gatot Subroto Kav. 5",
        "is_active": true
    }
}
```

---

### 39. Tambah Dokter Baru (Quick Add)

**Endpoint:** `POST /api/pos/doctors`

**Request Body:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| name | string | ✅ | Nama dokter |
| sip_number | string | ❌ | Nomor SIP |
| specialization | string | ❌ | Spesialisasi |
| phone | string | ❌ | Nomor HP/Telp |
| clinic_name | string | ❌ | Nama klinik/RS |
| address | string | ❌ | Alamat praktik |

**Response Sukses (201):**
```json
{
    "success": true,
    "message": "Dokter berhasil ditambahkan",
    "data": {
        "id": 16,
        "name": "Dr. Siti Aminah, Sp.A",
        "sip_number": "SIP.456/2024/DKI",
        "specialization": "Anak",
        "phone": null,
        "clinic_name": null,
        "address": null,
        "is_active": true
    }
}
```

---

## 💊 POS - PRESCRIPTIONS (RESEP)

### 40. Daftar Resep

**Endpoint:** `GET /api/pos/prescriptions`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| search | string | - | Cari nomor resep, nama pasien, atau nama dokter |
| status | string | - | pending / dispensed / cancelled |
| page | integer | 1 | Halaman |
| per_page | integer | 20 | Max 50 |

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "prescriptions": [
            {
                "id": 1,
                "prescription_number": "RX/1/20260404/0001",
                "prescription_date": "2026-04-04",
                "valid_until": "2026-05-04",
                "status": "pending",
                "doctor": {
                    "id": 1,
                    "name": "Dr. Ahmad Fauzi, Sp.PD",
                    "sip_number": "SIP.123/2024/DKI",
                    "specialization": "Penyakit Dalam"
                },
                "customer": {
                    "id": 5,
                    "code": "CST00005",
                    "name": "Budi Santoso",
                    "phone": "081234567890"
                },
                "items_count": 2
            }
        ],
        "pagination": {
            "current_page": 1,
            "per_page": 20,
            "total_items": 50,
            "total_pages": 3
        }
    }
}
```

---

### 41. Detail Resep

**Endpoint:** `GET /api/pos/prescriptions/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "prescription_number": "RX/1/20260404/0001",
        "prescription_date": "2026-04-04",
        "valid_until": "2026-05-04",
        "status": "pending",
        "diagnosis": "Hipertensi",
        "notes": "Minum setelah makan",
        "doctor": {
            "id": 1,
            "name": "Dr. Ahmad Fauzi, Sp.PD",
            "sip_number": "SIP.123/2024/DKI",
            "specialization": "Penyakit Dalam"
        },
        "customer": {
            "id": 5,
            "code": "CST00005",
            "name": "Budi Santoso",
            "phone": "081234567890"
        },
        "items": [
            {
                "id": 1,
                "product_id": 10,
                "product_name": "Amlodipine 5mg",
                "sku": "PRD010",
                "selling_price": 3500.00,
                "qty_prescribed": 30,
                "dosage": "5mg",
                "frequency": "1x sehari",
                "duration_days": 30,
                "notes": null
            }
        ],
        "transaction": null
    }
}
```

---

### 42. Buat Resep Baru ⭐

**Endpoint:** `POST /api/pos/prescriptions`

**Request Body:**

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| doctor_id | integer | ✅ | ID dokter |
| customer_id | integer | ❌ | ID pelanggan (null = anonim) |
| prescription_date | date | ✅ | Tanggal resep (YYYY-MM-DD) |
| diagnosis | string | ❌ | Diagnosa |
| valid_days | integer | ❌ | Masa berlaku (default: 30 hari) |
| notes | string | ❌ | Catatan |
| items | array | ✅ | Daftar obat |
| items[].product_id | integer | ✅ | ID produk |
| items[].qty_prescribed | decimal | ✅ | Jumlah yang diresepkan |
| items[].dosage | string | ❌ | Dosis (contoh: 10mg) |
| items[].frequency | string | ❌ | Frekuensi (contoh: 2x sehari) |
| items[].duration_days | integer | ❌ | Durasi (hari) |
| items[].notes | string | ❌ | Catatan per obat |

**Contoh Request:**
```json
{
    "doctor_id": 1,
    "customer_id": 5,
    "prescription_date": "2026-04-04",
    "diagnosis": "Hipertensi",
    "valid_days": 30,
    "items": [
        {
            "product_id": 10,
            "qty_prescribed": 30,
            "dosage": "5mg",
            "frequency": "1x sehari",
            "duration_days": 30
        },
        {
            "product_id": 15,
            "qty_prescribed": 60,
            "dosage": "500mg",
            "frequency": "2x sehari",
            "duration_days": 30
        }
    ]
}
```

**Response Sukses (201):**
```json
{
    "success": true,
    "message": "Resep berhasil dibuat",
    "data": {
        "id": 51,
        "prescription_number": "RX/1/20260404/0001",
        "prescription_date": "2026-04-04",
        "valid_until": "2026-05-04",
        "status": "pending",
        "doctor": { "id": 1, "name": "Dr. Ahmad Fauzi, Sp.PD" },
        "customer": { "id": 5, "name": "Budi Santoso" },
        "items": [ ... ]
    }
}
```

> **Nomor Resep Format:** `RX/{branch_id}/{YYYYMMDD}/{seq}` — auto-generated oleh backend.

---

## ⚙️ SETTINGS

### 16. Daftar Branch (Cabang)

**Endpoint:** `GET /api/settings/branches`

**Query Parameters:**

| Parameter | Tipe | Keterangan |
|-----------|------|------------|
| search | string | Cari nama/alamat/SIA/SIPA |
| is_active | boolean | Filter status aktif |
| per_page | integer | Default 15 |
| sort_by | string | Default: created_at |
| sort_order | string | asc/desc (default: desc) |

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Branches retrieved successfully",
    "data": {
        "current_page": 1,
        "data": [
            {
                "id": 1,
                "partner_id": 1,
                "name": "Apotek Sehat Cabang Jakarta",
                "address": "Jl. Sudirman No. 123",
                "phone": "021-12345678",
                "sia_number": "SIA.446/2024/DKI",
                "apoteker_name": "Apt. Ahmad Fauzi, S.Farm",
                "sipa_number": "SIPA.123/2024/DKI",
                "is_active": true,
                "partner": { "id": 1, "name": "PT Apotek Sehat" },
                "subscriptions": [...]
            }
        ],
        "per_page": 15,
        "total": 5
    }
}
```

---

### 17. Detail Branch (dengan Statistik)

**Endpoint:** `GET /api/settings/branches/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Branch retrieved successfully",
    "data": {
        "id": 1,
        "name": "Apotek Sehat Cabang Jakarta",
        "statistics": {
            "total_users": 5,
            "total_transactions": 1200,
            "total_sales_today": 5250000,
            "license_status": "active",
            "days_until_license_expiry": 265,
            "sia_status": "active",
            "days_until_sia_expiry": 180,
            "sipa_status": "active",
            "days_until_sipa_expiry": 180
        }
    }
}
```

---

### 18. Cek Lisensi yang Segera Expired

**Endpoint:** `GET /api/settings/branches/check/expiring`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|------------|
| days | integer | 30 | Cek dalam berapa hari ke depan |

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Expiring licenses checked successfully",
    "data": {
        "critical": [
            {
                "id": 2,
                "name": "Apotek Sehat Cabang Bandung",
                "sia_expired_at": "2026-04-08",
                "sipa_expired_at": "2026-06-30",
                "license_expired_at": "2027-02-05",
                "days_until_expiry": 4
            }
        ],
        "warning": [],
        "notice": []
    },
    "summary": {
        "total_expiring": 1,
        "critical": 1,
        "warning": 0,
        "notice": 0
    }
}
```

> **Kategori:**
> - `critical` = kurang dari 7 hari
> - `warning` = 7–30 hari
> - `notice` = 30–60 hari

---

## 🔔 ALERTS

### 19. Ringkasan Alert (Expiry + Stock)

**Endpoint:** `GET /api/alerts/summary`

**Headers:** Memerlukan autentikasi

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "expiry": {
            "total": 12,
            "critical": 3,
            "warning": 5,
            "notice": 4,
            "unacknowledged": 8
        },
        "stock": {
            "total": 6,
            "out_of_stock": 2,
            "low_stock": 4,
            "unacknowledged": 5
        }
    }
}
```

---

### 20. Daftar Alert Kadaluarsa

**Endpoint:** `GET /api/alerts/expiry`

**Query Parameters:**

| Parameter | Tipe | Keterangan |
|-----------|------|------------|
| branch_id | integer | Filter per cabang |
| alert_level | string | critical/warning/notice |
| level | string | Shortcut: critical/warning/notice |
| acknowledged | string | true/false/1/0 |
| unacknowledged | boolean | Shortcut filter belum-acknowledged |
| date_from | date | Filter tanggal kadaluarsa mulai |
| date_to | date | Filter tanggal kadaluarsa akhir |
| search | string | Cari batch number atau nama produk |
| sort_by | string | Default: expired_date |
| sort_order | string | asc/desc |
| per_page | integer | Default 15 |

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Expiry alerts retrieved successfully",
    "data": {
        "current_page": 1,
        "data": [
            {
                "id": 1,
                "partner_id": 1,
                "branch_id": 1,
                "product_id": 5,
                "product_batch_id": 10,
                "batch_number": "BTH005",
                "expired_date": "2026-04-10",
                "quantity": 50,
                "alert_level": "critical",
                "acknowledged_at": null,
                "acknowledged_by": null,
                "days_until_expiry": 6,
                "is_expired": false,
                "is_critical": true,
                "branch": { "id": 1, "name": "Apotek Sehat Jakarta" },
                "product": { "id": 5, "name": "Amoxicillin 500mg" },
                "product_batch": { "id": 10, "batch_number": "BTH005" }
            }
        ],
        "per_page": 15,
        "total": 12
    }
}
```

---

### 21. Acknowledge Alert Kadaluarsa

**Endpoint:** `POST /api/alerts/expiry/{id}/acknowledge`

**Request Body:**
```json
{
    "notes": "Sudah diperiksa dan akan dimusnahkan"
}
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Expiry alert acknowledged successfully",
    "data": {
        "id": 1,
        "acknowledged_at": "2026-04-04T10:00:00.000000Z",
        "acknowledged_by": { "id": 1, "name": "Kasir 1" }
    }
}
```

---

### 22. Acknowledge Beberapa Alert Kadaluarsa Sekaligus

**Endpoint:** `POST /api/alerts/expiry/acknowledge-multiple`

**Request Body:**
```json
{
    "alert_ids": [1, 2, 3, 5],
    "notes": "Batch batch ini akan dimusnahkan"
}
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Successfully acknowledged 4 expiry alerts",
    "data": {
        "acknowledged_count": 4,
        "total_requested": 4
    }
}
```

---

### 23. Generate Alert Kadaluarsa (Manual)

**Endpoint:** `POST /api/alerts/expiry/generate`

> **Permission:** Hanya role `super_admin`, `owner`, `admin`

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Successfully generated 5 expiry alerts",
    "data": {
        "generated_count": 5
    }
}
```

---

### 24. Detail Alert Kadaluarsa

**Endpoint:** `GET /api/alerts/expiry/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "batch_number": "BTH005",
        "expired_date": "2026-04-10",
        "quantity": 50,
        "alert_level": "critical",
        "days_until_expiry": 6,
        "is_expired": false,
        "is_critical": true,
        "branch": { "id": 1, "name": "Apotek Sehat Jakarta" },
        "product": { "id": 5, "name": "Amoxicillin 500mg" },
        "acknowledged_at": null
    }
}
```

---

### 25. Hapus Alert Kadaluarsa

**Endpoint:** `DELETE /api/alerts/expiry/{id}`

> **Permission:** Hanya role `super_admin`, `owner`, `admin`

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Expiry alert deleted successfully"
}
```

---

### 26. Daftar Alert Stok Menipis

**Endpoint:** `GET /api/alerts/stock`

**Query Parameters:**

| Parameter | Tipe | Keterangan |
|-----------|------|------------|
| branch_id | integer | Filter per cabang |
| alert_type | string | low_stock/out_of_stock |
| type | string | Shortcut: out_of_stock/low_stock |
| acknowledged | string | true/false/1/0 |
| unacknowledged | boolean | Filter belum-acknowledged |
| search | string | Cari nama produk atau SKU |
| sort_by | string | Default: created_at |
| sort_order | string | desc (default) |
| per_page | integer | Default 15 |

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Stock alerts retrieved successfully",
    "data": {
        "current_page": 1,
        "data": [
            {
                "id": 1,
                "product_id": 3,
                "branch_id": 1,
                "alert_type": "low_stock",
                "current_stock": 5,
                "min_stock": 50,
                "acknowledged_at": null,
                "stock_difference": -45,
                "stock_percentage": 10.0,
                "urgency_level": "high",
                "is_out_of_stock": false,
                "branch": { "id": 1, "name": "Apotek Sehat Jakarta" },
                "product": { "id": 3, "name": "Metformin 500mg", "sku": "PRD003" }
            }
        ],
        "per_page": 15,
        "total": 6
    }
}
```

---

### 27. Acknowledge Alert Stok

**Endpoint:** `POST /api/alerts/stock/{id}/acknowledge`

**Request Body:**
```json
{
    "notes": "Sudah order ke supplier"
}
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Stock alert acknowledged successfully",
    "data": { ... }
}
```

---

### 28. Acknowledge Beberapa Alert Stok Sekaligus

**Endpoint:** `POST /api/alerts/stock/acknowledge-multiple`

**Request Body:**
```json
{
    "alert_ids": [1, 2, 3],
    "notes": "Semua sudah diorder ke supplier"
}
```

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Successfully acknowledged 3 stock alerts",
    "data": {
        "acknowledged_count": 3,
        "total_requested": 3
    }
}
```

---

### 29. Generate Alert Stok (Manual)

**Endpoint:** `POST /api/alerts/stock/generate`

> **Permission:** Hanya role `super_admin`, `owner`, `admin`

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Successfully generated 3 stock alerts",
    "data": {
        "generated_count": 3
    }
}
```

---

### 30. Summary Alert Stok

**Endpoint:** `GET /api/alerts/stock/summary`

**Query Parameters:**

| Parameter | Tipe | Keterangan |
|-----------|------|------------|
| branch_id | integer | Filter per cabang |

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Stock alerts summary retrieved successfully",
    "data": {
        "total": 6,
        "out_of_stock": 2,
        "low_stock": 4,
        "unacknowledged": 5
    }
}
```

---

### 31. Detail Alert Stok

**Endpoint:** `GET /api/alerts/stock/{id}`

**Response Sukses (200):**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "product_id": 3,
        "branch_id": 1,
        "alert_type": "low_stock",
        "current_stock": 5,
        "min_stock": 50,
        "stock_difference": -45,
        "stock_percentage": 10.0,
        "urgency_level": "high",
        "is_out_of_stock": false,
        "acknowledged_at": null,
        "branch": { "id": 1, "name": "Apotek Sehat Jakarta" },
        "product": { "id": 3, "name": "Metformin 500mg" }
    }
}
```

---

### 32. Hapus Alert Stok

**Endpoint:** `DELETE /api/alerts/stock/{id}`

> **Permission:** Hanya role `super_admin`, `owner`, `admin`

**Response Sukses (200):**
```json
{
    "success": true,
    "message": "Stock alert deleted successfully"
}
```

---

## 📊 FORMAT RESPONSE STANDAR

### Sukses
```json
{
    "success": true,
    "message": "Pesan sukses",
    "data": { ... }
}
```

### Data Paginated (Laravel Standard)
```json
{
    "success": true,
    "data": {
        "current_page": 1,
        "data": [...],
        "per_page": 15,
        "total": 150,
        "last_page": 10,
        "from": 1,
        "to": 15
    }
}
```

### Data Paginated (POS Custom)
```json
{
    "success": true,
    "data": {
        "products/transactions/returns": [...],
        "pagination": {
            "current_page": 1,
            "per_page": 20,
            "total_items": 150,
            "total_pages": 8,
            "has_more": true
        }
    }
}
```

### Error Validasi (422)
```json
{
    "success": false,
    "message": "Validation error",
    "errors": {
        "email": ["Email wajib diisi"],
        "device_id": ["Device ID wajib diisi"]
    }
}
```

---

## 🚦 ERROR CODES

| Error Code | HTTP Status | Deskripsi |
|------------|-------------|-----------|
| `INVALID_CREDENTIALS` | 401 | Email atau password salah |
| `UNAUTHENTICATED` | 401 | Token tidak valid atau expired |
| `USER_INACTIVE` | 403 | Akun user tidak aktif |
| `PARTNER_INACTIVE` | 403 | Partner/apotek tidak aktif |
| `LICENSE_EXPIRED` | 403 | Lisensi telah expired |
| `NO_BRANCH` | 403 | User tidak memiliki cabang |
| `PRODUCT_NOT_FOUND` | 404 | Produk tidak ditemukan |
| `TRANSACTION_NOT_FOUND` | 404 | Transaksi tidak ditemukan |
| `RETURN_NOT_FOUND` | 404 | Retur tidak ditemukan |
| `RETURN_EXISTS` | 400 | Transaksi sudah memiliki retur |
| `RETURN_FAILED` | 500 | Gagal membuat retur |
| `KEYWORD_TOO_SHORT` | 400 | Keyword kurang dari 2 karakter |
| `VALIDATION_ERROR` | 422 | Data yang dikirim tidak valid |
| `PROCESSING_ERROR` | 500 | Terjadi kesalahan pemrosesan |

---

## 🏗️ QUICK REFERENCE - SEMUA ENDPOINT

```
# AUTH (4 endpoints)
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/logout
POST   /api/auth/logout-all

# POS - PRODUCTS (5 endpoints) ⚠️ route perlu di-uncomment
GET    /api/pos/products/sync
GET    /api/pos/products/search
GET    /api/pos/products/{id}
GET    /api/pos/products/categories
GET    /api/pos/products/units

# POS - TRANSACTIONS (3 endpoints) ⚠️ route perlu di-uncomment
POST   /api/pos/transactions/sync
GET    /api/pos/transactions/history
GET    /api/pos/transactions/{id}

# POS - RETURNS (3 endpoints) ⚠️ route perlu di-uncomment
GET    /api/pos/returns
POST   /api/pos/returns
GET    /api/pos/returns/{id}

# SETTINGS - BRANCHES (5 endpoints)
GET    /api/settings/branches
POST   /api/settings/branches
GET    /api/settings/branches/{id}
PUT    /api/settings/branches/{id}
DELETE /api/settings/branches/{id}
GET    /api/settings/branches/check/expiring

# SETTINGS - PARTNERS (CRUD)
GET    /api/settings/partners
POST   /api/settings/partners
GET    /api/settings/partners/{id}
PUT    /api/settings/partners/{id}
DELETE /api/settings/partners/{id}

# SETTINGS - SUBSCRIPTIONS (CRUD)
GET    /api/settings/subscriptions
POST   /api/settings/subscriptions
GET    /api/settings/subscriptions/{id}
PUT    /api/settings/subscriptions/{id}
DELETE /api/settings/subscriptions/{id}

# SETTINGS - LICENSE EXTENSIONS (CRUD)
GET    /api/settings/license-extensions
POST   /api/settings/license-extensions
GET    /api/settings/license-extensions/{id}
PUT    /api/settings/license-extensions/{id}
DELETE /api/settings/license-extensions/{id}

# ALERTS - COMBINED
GET    /api/alerts/summary

# ALERTS - EXPIRY (7 endpoints)
GET    /api/alerts/expiry
GET    /api/alerts/expiry/summary
POST   /api/alerts/expiry/generate
GET    /api/alerts/expiry/{id}
POST   /api/alerts/expiry/{id}/acknowledge
POST   /api/alerts/expiry/acknowledge-multiple
DELETE /api/alerts/expiry/{id}

# ALERTS - STOCK (7 endpoints)
GET    /api/alerts/stock
GET    /api/alerts/stock/summary
POST   /api/alerts/stock/generate
GET    /api/alerts/stock/{id}
POST   /api/alerts/stock/{id}/acknowledge
POST   /api/alerts/stock/acknowledge-multiple
DELETE /api/alerts/stock/{id}

# POS - CUSTOMERS (4 endpoints)
GET    /api/pos/customers
POST   /api/pos/customers
GET    /api/pos/customers/{id}
PUT    /api/pos/customers/{id}

# POS - DOCTORS (3 endpoints)
GET    /api/pos/doctors
POST   /api/pos/doctors
GET    /api/pos/doctors/{id}

# POS - PRESCRIPTIONS (3 endpoints)
GET    /api/pos/prescriptions
POST   /api/pos/prescriptions
GET    /api/pos/prescriptions/{id}
```

**Total Endpoint Tersedia: 56 endpoint**

---

## 🚀 BEST PRACTICES UNTUK ANDROID

### Token Management
```kotlin
// Simpan di EncryptedSharedPreferences
val sharedPreferences = EncryptedSharedPreferences.create(
    "secure_prefs",
    masterKey,
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
sharedPreferences.edit().putString("auth_token", token).apply()
```

### Error Handling
```kotlin
when (response.code()) {
    200, 201 -> // Success
    400       -> // Bad request (cek error_code)
    401       -> // Token expired → re-login
    403       -> // License/permission error
    422       -> // Validation error (cek errors field)
    500       -> // Server error (retry dengan exponential backoff)
}
```

### Initial Product Sync
```kotlin
suspend fun initialSync() {
    var page = 1
    var hasMore = true

    while (hasMore) {
        val response = api.syncProducts(page = page, perPage = 100)
        localDb.insertProducts(response.data.products)
        hasMore = response.data.pagination.hasMore
        page++
    }
    // Simpan sync token
    preferences.saveLastSyncToken(response.data.syncInfo.nextSyncToken)
}
```

### Incremental Sync
```kotlin
suspend fun incrementalSync() {
    val lastSyncToken = preferences.getLastSyncToken()
    val response = api.syncProducts(updatedSince = lastSyncToken)
    localDb.updateProducts(response.data.products)
    preferences.saveLastSyncToken(response.data.syncInfo.nextSyncToken)
}
```

### Upload Transaksi Offline
```kotlin
suspend fun syncOfflineTransactions() {
    val pendingTxs = localDb.getPendingTransactions()
    if (pendingTxs.isEmpty()) return

    val response = api.syncTransactions(SyncRequest(pendingTxs))

    response.data.results.forEach { result ->
        if (result.success) {
            localDb.markAsSynced(
                localId = result.localTransactionId,
                serverId = result.serverTransactionId,
                invoiceNumber = result.invoiceNumber
            )
        }
    }
}
```

### Generate Local Transaction ID yang Unik
```kotlin
fun generateLocalTransactionId(): String {
    val timestamp = System.currentTimeMillis() / 1000
    val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    val random = (1000..9999).random()
    return "TRX_${timestamp}_${deviceId.takeLast(4)}_$random"
}
```

---

**Terakhir Diperbarui:** April 4, 2026
**Backend Version:** v2.1
**Status:** ✅ Sesuai dengan implementasi backend aktual (56 endpoint aktif)
