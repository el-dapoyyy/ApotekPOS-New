# 🔍 Audit Endpoint API — ApotekPOS

> **Tanggal:** 12 April 2026
> **Sumber Backend:** `c:\Users\Daffa\apoapps\routes\api.php` (Laravel 11)
> **Sumber Android:** `ApiService.kt`, `Models.kt`, `PosApiModels.kt`
> **Dokumentasi Referensi:** `docs/API_DOKUMENTASI_ANDROID.md` (v2.1, 4 April 2026)

---

## 📊 Ringkasan

| Kategori | Total Backend | Di Dokumentasi | Di Android `ApiService.kt` | Selisih |
|----------|:---:|:---:|:---:|:---:|
| **Auth** | 4 | 4 | 4 (+2 ekstra) | ⚠️ |
| **POS - Products** | 5 | 5 | 3 | ❌ 2 belum |
| **POS - Transactions** | 3 | 3 | 3 | ✅ |
| **POS - Returns** | 3 | 3 | 2 | ❌ 1 belum |
| **POS - Customers** | 4 | 4 | 0 | ❌ 4 belum |
| **POS - Doctors** | 3 | 3 | 0 | ❌ 3 belum |
| **POS - Prescriptions** | 3 | 3 | 0 | ❌ 3 belum |
| **Settings - Partners** | 5 | 5 | 0 | ❌ 5 belum |
| **Settings - Branches** | 6 | 6 | 0 | ❌ 6 belum |
| **Alerts - Combined** | 1 | 1 | 1 | ✅ |
| **Alerts - Expiry** | 7 | 7 | 1 | ❌ 6 belum |
| **Alerts - Stock** | 7 | 7 | 1 | ❌ 6 belum |
| **TOTAL** | **51** | **51** | **15** | **36 belum** |

> [!WARNING]
> Hanya **15 dari 51 endpoint** di `routes/api.php` yang sudah terimplementasi di Android `ApiService.kt`. Sisanya **36 endpoint** belum ada mapping Retrofit-nya.

---

## ✅ Endpoint yang Sudah Terimplementasi di Android

### Auth (4 endpoint + 2 ekstra)

| # | Method | Backend Route | Android `ApiService.kt` | Status |
|---|--------|--------------|------------------------|--------|
| 1 | `POST` | `/api/auth/login` | `fun login()` | ✅ Match |
| 2 | `GET` | `/api/auth/me` | `fun authMe()` | ✅ Match |
| 3 | `POST` | `/api/auth/logout` | — | ❌ Belum |
| 4 | `POST` | `/api/auth/logout-all` | — | ❌ Belum |
| — | `POST` | ❌ *Tidak ada di backend* | `fun loginWithGoogle()` → `auth/google` | ⚠️ Extra |
| — | `POST` | ❌ *Tidak ada di backend* | `fun startShift()` → `auth/shift/start` | ⚠️ Extra |

> [!IMPORTANT]
> **2 endpoint di Android tidak ada di backend:**
> - `POST auth/google` — Google OAuth login, belum ada route/controller di Laravel
> - `POST auth/shift/start` — Buka shift, belum ada route/controller di Laravel

### POS - Products (3 dari 5)

| # | Method | Backend Route | Android `ApiService.kt` | Status |
|---|--------|--------------|------------------------|--------|
| 5 | `GET` | `/api/pos/products/sync` | `fun syncProducts()` | ✅ Match |
| 6 | `GET` | `/api/pos/products/search` | `fun searchProducts()` | ✅ Match |
| 7 | `GET` | `/api/pos/products/{id}` | `fun getProductDetail()` | ✅ Match |
| 8 | `GET` | `/api/pos/products/categories` | — | ❌ Belum |
| 9 | `GET` | `/api/pos/products/units` | — | ❌ Belum |

### POS - Transactions (3 dari 3) ✅

| # | Method | Backend Route | Android `ApiService.kt` | Status |
|---|--------|--------------|------------------------|--------|
| 10 | `POST` | `/api/pos/transactions/sync` | `fun syncTransactions()` | ✅ Match |
| 11 | `GET` | `/api/pos/transactions/history` | `fun getTransactionHistory()` | ✅ Match |
| 12 | `GET` | `/api/pos/transactions/{id}` | `fun getTransactionDetail()` | ✅ Match |

### POS - Returns (2 dari 3)

| # | Method | Backend Route | Android `ApiService.kt` | Status |
|---|--------|--------------|------------------------|--------|
| 13 | `GET` | `/api/pos/returns` | `fun listReturns()` | ✅ Match |
| 14 | `POST` | `/api/pos/returns` | `fun createReturn()` | ✅ Match |
| 15 | `GET` | `/api/pos/returns/{id}` | — | ❌ Belum |

### Alerts (3 dari 15)

| # | Method | Backend Route | Android `ApiService.kt` | Status |
|---|--------|--------------|------------------------|--------|
| 19 | `GET` | `/api/alerts/summary` | `fun getAlertsSummary()` | ✅ Match |
| 20 | `GET` | `/api/alerts/expiry` | `fun getExpiryAlerts()` | ✅ Match |
| 26 | `GET` | `/api/alerts/stock` | `fun getStockAlerts()` | ✅ Match |

---

## ❌ Endpoint yang Belum Terimplementasi di Android

### POS - Customers (0 dari 4)

| Method | Route | Dokumentasi § | Deskripsi |
|--------|-------|:---:|-----------|
| `GET` | `/api/pos/customers` | §33 | Daftar pelanggan (search, filter type, paginated) |
| `GET` | `/api/pos/customers/{id}` | §34 | Detail pelanggan |
| `POST` | `/api/pos/customers` | §35 | Tambah pelanggan baru (Quick Add) |
| `PUT` | `/api/pos/customers/{id}` | §36 | Update data pelanggan |

### POS - Doctors (0 dari 3)

| Method | Route | Dokumentasi § | Deskripsi |
|--------|-------|:---:|-----------|
| `GET` | `/api/pos/doctors` | §37 | Daftar dokter (search, paginated) |
| `GET` | `/api/pos/doctors/{id}` | §38 | Detail dokter |
| `POST` | `/api/pos/doctors` | §39 | Tambah dokter baru (Quick Add) |

### POS - Prescriptions (0 dari 3)

| Method | Route | Dokumentasi § | Deskripsi |
|--------|-------|:---:|-----------|
| `GET` | `/api/pos/prescriptions` | §40 | Daftar resep (search, filter status, paginated) |
| `GET` | `/api/pos/prescriptions/{id}` | §41 | Detail resep |
| `POST` | `/api/pos/prescriptions` | §42 | Buat resep baru |

### Settings - Partners / Branches (11 endpoint)

| Method | Route | Deskripsi |
|--------|-------|-----------|
| `GET` | `/api/settings/partners` | Daftar partners |
| `POST` | `/api/settings/partners` | Buat partner |
| `GET` | `/api/settings/partners/{id}` | Detail partner |
| `PUT` | `/api/settings/partners/{id}` | Update partner |
| `DELETE` | `/api/settings/partners/{id}` | Hapus partner |
| `GET` | `/api/settings/branches` | §16 — Daftar branch |
| `POST` | `/api/settings/branches` | Buat branch |
| `GET` | `/api/settings/branches/{id}` | §17 — Detail branch + statistik |
| `PUT` | `/api/settings/branches/{id}` | Update branch |
| `DELETE` | `/api/settings/branches/{id}` | Hapus branch |
| `GET` | `/api/settings/branches/check/expiring` | §18 — Cek lisensi hampir expired |

### Alerts - Expiry (6 dari 7 belum)

| Method | Route | Dokumentasi § | Deskripsi |
|--------|-------|:---:|-----------|
| `GET` | `/api/alerts/expiry/summary` | — | Ringkasan expiry alert |
| `POST` | `/api/alerts/expiry/generate` | §23 | Generate alert manual |
| `GET` | `/api/alerts/expiry/{id}` | §24 | Detail alert kadaluarsa |
| `POST` | `/api/alerts/expiry/{id}/acknowledge` | §21 | Acknowledge single |
| `POST` | `/api/alerts/expiry/acknowledge-multiple` | §22 | Acknowledge batch |
| `DELETE` | `/api/alerts/expiry/{id}` | §25 | Hapus alert |

### Alerts - Stock (6 dari 7 belum)

| Method | Route | Dokumentasi § | Deskripsi |
|--------|-------|:---:|-----------|
| `GET` | `/api/alerts/stock/summary` | §30 | Ringkasan stock alert |
| `POST` | `/api/alerts/stock/generate` | §29 | Generate alert manual |
| `GET` | `/api/alerts/stock/{id}` | §31 | Detail alert stok |
| `POST` | `/api/alerts/stock/{id}/acknowledge` | §27 | Acknowledge single |
| `POST` | `/api/alerts/stock/acknowledge-multiple` | §28 | Acknowledge batch |
| `DELETE` | `/api/alerts/stock/{id}` | §32 | Hapus alert |

---

## ⚠️ Inkonsistensi yang Ditemukan

### 1. Android punya endpoint yang tidak ada di Backend

| Android Endpoint | Keterangan |
|-----------------|------------|
| `POST auth/google` | Google OAuth — tidak ada route maupun controller di Laravel |
| `POST auth/shift/start` | Buka shift — tidak ada route maupun controller di Laravel |

> [!CAUTION]
> Kedua endpoint ini akan **selalu gagal (404)** jika dipanggil ke backend saat ini. Perlu ditambahkan route + controller di Laravel, atau dihapus dari Android jika tidak dipakai.

### 2. Dokumentasi outdated notes

Dokumentasi `API_DOKUMENTASI_ANDROID.md` masih mencantumkan:
- "⚠️ Route perlu di-uncomment" pada Products, Transactions, Returns — padahal di `routes/api.php` **semua sudah aktif (tidak di-comment)**.

### 3. Model mismatch antara Legacy dan POS API

File `Models.kt` berisi model lama (Product, Transaction) dengan field berbeda dari respons API yang sebenarnya (yang di-handle oleh `PosApiModels.kt`). Contoh:
- `Product.sellPrice` vs `PosProductDto.sellingPrice`
- `Transaction.totalAmount` / `grandTotal`
- Konversi bridge dilakukan via extension functions (`toProduct()`, `toHistoryRowTransaction()`, dll.)

### 4. Logout belum diimplementasi

`POST /api/auth/logout` dan `POST /api/auth/logout-all` tidak ada di `ApiService.kt`. Artinya saat user logout dari Android, token Sanctum **tidak di-revoke** di server.

---

## 📋 Prioritas Implementasi (Rekomendasi)

| Prioritas | Endpoint | Dampak |
|:---------:|----------|--------|
| 🔴 **P0** | `POST auth/logout` | Security: token harus di-revoke saat logout |
| 🔴 **P0** | Customers CRUD (4 endpoint) | Dibutuhkan untuk membuat transaksi non-anonymous |
| 🟡 **P1** | Doctors + Prescriptions (6 endpoint) | Dibutuhkan untuk fitur resep dokter |
| 🟡 **P1** | Alert acknowledge (4 endpoint) | User perlu bisa acknowledge alert dari Android |
| 🟢 **P2** | `GET products/categories`, `GET products/units` | Untuk filter/dropdown di UI |
| 🟢 **P2** | `GET returns/{id}` | Detail retur |
| 🔵 **P3** | Settings (Partners, Branches) | Biasanya admin-only, bisa via web dashboard |

---

## 📂 File Referensi

| File | Lokasi |
|------|--------|
| Backend Routes | [api.php](file:///c:/Users/Daffa/apoapps/routes/api.php) |
| API Dokumentasi | [API_DOKUMENTASI_ANDROID.md](file:///c:/Users/Daffa/apoapps/docs/API_DOKUMENTASI_ANDROID.md) |
| Android API Service | [ApiService.kt](file:///c:/Users/Daffa/AndroidStudioProjects/ApotekPOS/app/src/main/java/com/mediakasir/apotekpos/data/network/ApiService.kt) |
| Android Models | [Models.kt](file:///c:/Users/Daffa/AndroidStudioProjects/ApotekPOS/app/src/main/java/com/mediakasir/apotekpos/data/model/Models.kt) |
| Android POS Models | [PosApiModels.kt](file:///c:/Users/Daffa/AndroidStudioProjects/ApotekPOS/app/src/main/java/com/mediakasir/apotekpos/data/model/PosApiModels.kt) |
