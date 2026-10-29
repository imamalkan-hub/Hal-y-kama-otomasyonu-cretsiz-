# Mim Halı Yıkama — Android Uygulaması

Saha personeli için **native Android uygulaması** (WebView değil).
Kotlin + Jetpack Compose + Material3 ile yazılmıştır; arayüz tamamen
native Compose'dur, uzak sunucu sayfası gösterilmez.

## Ne yapar

| Ekran | İçerik |
|---|---|
| Giriş | Kullanıcı adı / şifre → sunucudan bearer jetonu alır, şifrelenmiş saklar |
| Panel | Bugünkü alım/teslim, gecikmiş, açık bakiye, iş akışı sayacları, günlük kasa |
| Siparişler | Arama (kod / müşteri / telefon), durum rengi, kalan bakiye |
| Alım / Teslimat / Gecikmiş | Saha listeleri |
| Sipariş detay | Müşteri, durum rozeti, parçalar, ödemeler, fotoğraflar, durum ilerletme |

Ek olarak: müşteriyi ara, WhatsApp'tan yaz, adrese harita aç.

## Mimari

```
app/src/main/java/com/mimhalikama/app/
├── MainActivity.kt        — giriş ekranı ↔ ana ekran geçişi
├── data/
│   ├── Api.kt             — HttpURLConnection tabanlı API istemcisi + multipart yükleyici
│   ├── Models.kt          — JSON → Kotlin model dönüşümleri, kuruş→TL biçimlendirme
│   └── TokenStore.kt      — EncryptedSharedPreferences ile güvenli jeton saklama
└── ui/
    ├── Tema.kt            — marka renkleri (web paneliyle aynı), açık/koyu tema
    ├── GirisEkrani.kt
    ├── AnaEkrani.kt       — alt navigasyon, panel, liste ekranları
    └── DetayEkrani.kt     — sipariş detayı ve durum ilerletme
```

Sunucu tarafı API: `https://mimhaliyikama.com/sistem/?p=api&api=<uç>`
Kimlik doğrulama `Authorization: Bearer <jeton>` ile yapılır. Jeton bir
kullanıcıya bağlıdır; **yetkiler web paneliyle birebir aynıdır** — ayrı bir
yetki modeli yoktur. Para birimi her zaman kuruş (integer) olarak taşınır.

## Derleme

Gereksinimler: JDK 17, Android SDK (platform 34, build-tools 34.0.0).

```bash
export ANDROID_HOME=/root/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
gradle assembleRelease --no-daemon
# → app/build/outputs/apk/release/app-release.apk
```

### İmzalama

İmzalama bilgileri **ortam değişkenlerinden** gelir (repoya anahtar girmez):

```bash
export MIM_STORE_FILE=/root/mim-project/android/keys/mim-release.jks
export MIM_STORE_PASSWORD=…
export MIM_KEY_ALIAS=mim
export MIM_KEY_PASSWORD=…
```

`keys/` klasörü `.gitignore`'dadır. Keystore yoksa derleme yine çalışır ama
APK imzasızdır.

### Sürüm

```bash
gradle assembleRelease -PversionCode=7 -PversionName=1.2
```

## GitHub Actions

`.github/workflows/build-apk.yml` — her çalıştırmada:

1. JDK 17 + Android SDK kurar
2. `MIM_STORE_PASSWORD` / `MIM_KEY_PASSWORD` secret'larından keystore üretir
3. Release APK'yı derler ve `apksigner` ile doğrular
4. Artefakt olarak yükler; etiketle tetiklenirse GitHub Release açar

Repo → **Settings → Secrets and variables → Actions**:

| Secret | Açıklama |
|---|---|
| `MIM_STORE_PASSWORD` | keystore şifresi |
| `MIM_KEY_ALIAS` | anahtar takma adı (`mim`) |
| `MIM_KEY_PASSWORD` | anahtar şifresi |

## Canlıya dağıtım

APK panelden indirilebilir: `/sistem/?p=apk` → **Mobil Uygulama** menüsü.
Dosya sunucuda `app/apk/MimHaliYikama.apk` yolunda durur.
