# MediKasir - Aplikasi POS Apotek Android

Aplikasi Android (Kotlin + Jetpack Compose) yang merupakan migrasi dari repository [apotekpos](https://github.com/el-dapoyyy/apotekpos).

## Stack Teknologi

| Layer | Teknologi |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt (Dagger) |
| Network | Retrofit 2 + OkHttp |
| Auth Storage | DataStore Preferences |
| State | ViewModel + StateFlow |
| Language | Kotlin |

## Fitur yang Dimigrasi

✅ Aktivasi Lisensi  
✅ Login dengan PIN  
✅ Dashboard (revenue, transaksi, stok rendah, peringatan kadaluarsa)  
✅ Kasir / POS (tambah ke keranjang, diskon, multi-metode pembayaran, struk)  
✅ Manajemen Stok (produk + batch FEFO)  
✅ Riwayat Transaksi  
✅ Pengaturan & Ganti PIN  

## Cara Membuka di Android Studio

1. **Clone / extract** proyek ini
2. Buka **Android Studio** → **Open** → pilih folder `ApotekPOS`
3. Tunggu Gradle sync selesai
4. Edit `BASE_URL` di `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "BASE_URL", "\"http://YOUR_SERVER_IP:8001/api/\"")
   ```
   - Untuk emulator: `http://10.0.2.2:8001/api/`
   - Untuk device fisik: gunakan IP lokal server, misal `http://192.168.1.100:8001/api/`

## Menjalankan Backend

Backend Python FastAPI dari repo asli tetap digunakan. Jalankan dengan:

```bash
cd backend
pip install -r requirements.txt
python server.py
```

Kemudian seed data demo:
```
POST http://localhost:8001/api/seed
```

Kredensial demo:
- **Lisensi**: `APOTEK-DEMO-2024-1234`
- **Admin**: username `admin`, PIN `1234`
- **Kasir**: username `kasir1`, PIN `5678`

## Struktur Proyek

```
app/src/main/java/com/mediakasir/apotekpos/
├── data/
│   ├── model/        # Data classes (Models.kt)
│   ├── network/      # Retrofit ApiService + AuthInterceptor
│   └── repository/   # SessionRepository (DataStore)
├── di/               # Hilt module (AppModule.kt)
├── ui/
│   ├── auth/         # LicenseScreen, LoginScreen
│   ├── main/
│   │   ├── dashboard/ # Dashboard
│   │   ├── pos/       # Kasir / POS
│   │   ├── stok/      # Manajemen Stok
│   │   ├── history/   # Riwayat Transaksi
│   │   └── settings/  # Pengaturan
│   └── theme/         # Warna & tema Material 3
├── utils/             # FormatUtils (IDR, tanggal)
├── ApotekApp.kt       # Hilt Application
├── MainActivity.kt    # Entry point + Nav graph
└── Screen.kt          # Route definitions
```

## Catatan

- `minSdk = 26` (Android 8.0+)
- Backend URL dikonfigurasi via `BuildConfig.BASE_URL`
- Token JWT disimpan di DataStore (aman, bukan SharedPreferences biasa)
- Semua screen pakai Hilt ViewModel untuk state management
