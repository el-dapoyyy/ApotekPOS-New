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

## Fitur

✅ Login (Sanctum) + sesi cabang  
✅ Dashboard (ringkasan alert stok & kadaluarsa)  
✅ Kasir / POS (sinkron transaksi ke server)  
✅ Stok (baca dari API POS; kelola produk/batch lewat web admin)  
✅ Riwayat transaksi  
✅ Pengaturan (logout, reset data lokal)  

## Cara Membuka di Android Studio

1. **Clone / extract** proyek ini
2. Buka **Android Studio** → **Open** → pilih folder `ApotekPOS`
3. Tunggu Gradle sync selesai
4. Edit `BASE_URL` (dan jika perlu `DEV_API_HOST_HEADER`) di `app/build.gradle.kts` agar mengarah ke **Laravel ApoApps** Anda, path API biasanya berakhiran `/api/` (lihat **`API_DOKUMENTASI_ANDROID.md`** di root repo).
   - Emulator ke Laragon / vhost lokal: sering memakai `http://10.0.2.2/...` plus header `Host` sesuai domain virtual (mis. `apoapps.test`).

## Backend

Aplikasi memakai **API Laravel (Sanctum)** sesuai dokumentasi di **`API_DOKUMENTASI_ANDROID.md`**, bukan mock server atau FastAPI demo.

## Struktur Proyek

```
app/src/main/java/com/mediakasir/apotekpos/
├── data/
│   ├── model/        # Data classes (Models.kt)
│   ├── network/      # Retrofit ApiService + AuthInterceptor
│   └── repository/   # SessionRepository (DataStore)
├── di/               # Hilt module (AppModule.kt)
├── ui/
│   ├── auth/         # SplashScreen, LoginScreen
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
