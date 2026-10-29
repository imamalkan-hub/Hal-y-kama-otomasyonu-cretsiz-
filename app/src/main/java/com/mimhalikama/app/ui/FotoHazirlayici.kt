package com.mimhalikama.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Galeriden seçilen fotoğrafı sunucuya yüklemeye hazırlar.
 *
 * Sunucu tarafı sınır: 8192 px (foto.php PHOTO_MAX_EDGE) ve 16 MB.
 * Eski telefon fotoğrafları 4096×3072 olabildiği için, cihazda üretilen
 * büyük görseller sunucuya gönderilmeden önce burada küçültülür.
 *
 * Üretilen dosya: JPEG, en uzun kenar 1600 px (sunucu hedefiyle aynı),
 * kalite 82. Böylece hem 12 MP telefon fotoğrafı sorunsuz yüklenir hem de
 * saha bağlantısı zayıfken veri tutmaz.
 */
object FotoHazirlayici {

    const val HEDEF_KENAR = 1600
    const val KALITE = 82
    private const val ETIKET = "FotoHazirlayici"

    /** Uri'den yükleime hazır JPEG baytları üretir. */
    fun hazirla(context: Context, uri: Uri): Sonuc {
        return try {
            val bitmap = decodeOrnek(context, uri)
                ?: return Sonuc.Hata("Fotoğraf açılamadı.")

            // EXIF yönü: telefon fotoğrafları genelde 90/180/270 derece döndürülmüş
            // olarak saklanır. Dönüş uygulanmazsa sunucuda yan durur.
            val duzeltilmis = exifDuzelt(context, uri, bitmap)

            val kucultulmus = kucult(duzeltilmis, HEDEF_KENAR)

            val cikti = ByteArrayOutputStream()
            kucultulmus.compress(Bitmap.CompressFormat.JPEG, KALITE, cikti)
            kucultulmus.recycle()
            if (duzeltilmis !== kucultulmus) duzeltilmis.recycle()

            val baytlar = cikti.toByteArray()
            if (baytlar.isEmpty()) {
                Sonuc.Hata("Fotoğraf işlenemedi.")
            } else {
                Sonuc.Basarili(
                    baytlar = baytlar,
                    ad = "mim-${System.currentTimeMillis()}.jpg",
                    mime = "image/jpeg",
                    boyut = baytlar.size,
                )
            }
        } catch (e: OutOfMemoryError) {
            Sonuc.Hata("Fotoğraf çok büyük, bellek yetersiz. Küçük bir fotoğraf seçin.")
        } catch (e: Exception) {
            Log.e(ETIKET, "Hazırlama hatası", e)
            Sonuc.Hata("Fotoğraf işlenemedi: ${e.message ?: "bilinmeyen hata"}")
        }
    }

    /**
     * Görseli belleğe almadan önce boyutlarını oku; gerekiyorsa 1/4 ölçekli
     * örnek çöz (decodeBounds). 4096×3072 JPEG ~48 MB ham bellek demek.
     */
    private fun decodeOrnek(context: Context, uri: Uri): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        var ornek = 1
        val enBuyuk = maxOf(opts.outWidth, opts.outHeight)
        while (enBuyuk / (ornek * 2) >= HEDEF_KENAR) {
            ornek *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = ornek }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    /** Uzun kenarı sınıra indirir; kısa kenara göre oran korunur. */
    private fun kucult(bitmap: Bitmap, hedef: Int): Bitmap {
        val enUzun = maxOf(bitmap.width, bitmap.height)
        if (enUzun <= hedef) return bitmap
        val oran = hedef.toFloat() / enUzun.toFloat()
        val yeniGenislik = (bitmap.width * oran).toInt().coerceAtLeast(1)
        val yeniYukseklik = (bitmap.height * oran).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, yeniGenislik, yeniYukseklik, true)
    }

    /** EXIF yön etiketine göre döndürür (1 = döndürme yok). */
    private fun exifDuzelt(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val derece = try {
            context.contentResolver.openInputStream(uri)?.use { akis ->
                ExifInterface(akis).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrisi = when (derece) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
            else -> return bitmap
        }
        return try {
            val sonuc = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrisi, true)
            if (sonuc != bitmap) bitmap.recycle()
            sonuc
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    /** Hazırlık sonucu. */
    sealed class Sonuc {
        data class Basarili(
            val baytlar: ByteArray,
            val ad: String,
            val mime: String,
            val boyut: Int,
        ) : Sonuc()

        data class Hata(val mesaj: String) : Sonuc()
    }
}
