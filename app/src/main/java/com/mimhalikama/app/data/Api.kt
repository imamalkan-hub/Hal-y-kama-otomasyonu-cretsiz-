package com.mimhalikama.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Sunucu API istemcisi. Tüm çağrılar arka planda çalışır (MainActivity
 * executor kullanır); bu sınıf yalnız senkron HTTP yapar.
 *
 * Temel adres: https://mimhaliyikama.com/sistem/  uç: ?p=api&api=<uç>
 */
object Api {
    const val BASE = "https://mimhaliyikama.com/sistem/"
    const val TIMEOUT_MS = 20_000

    @Volatile var token: String = ""

    class ApiHata(val mesaj: String, val kod: Int) : Exception(mesaj)

    private fun istek(
        yol: String,
        metot: String = "GET",
        govde: JSONObject? = null,
        multipart: Multipart? = null,
    ): JSONObject {
        val sb = StringBuilder(BASE).append("?p=api&api=").append(URLEncoder.encode(yol, "UTF-8"))
        val conn = URL(sb.toString()).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.requestMethod = metot
        conn.setRequestProperty("Accept", "application/json")
        if (token.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
        try {
            if (multipart != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=${multipart.sinir}")
                conn.setFixedLengthStreamingMode(multipart.boyut)
                multipart.yaz(conn.outputStream)
            } else if (govde != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(govde.toString().toByteArray(Charsets.UTF_8)) }
            }
            val kod = conn.responseCode
            val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
            val metin = akis?.readBytes()?.toString(Charsets.UTF_8) ?: ""
            if (metin.isEmpty()) throw ApiHata("Sunucu boş yanıt (HTTP $kod)", kod)
            val json = JSONObject(metin)
            if (kod !in 200..299) {
                throw ApiHata(json.optString("hata", "Sunucu hatası (HTTP $kod)"), kod)
            }
            return json
        } catch (e: ApiHata) {
            throw e
        } catch (e: Exception) {
            throw ApiHata("Bağlantı kurulamadı: ${e.message ?: "bilinmeyen"}", 0)
        } finally {
            conn.disconnect()
        }
    }

    fun giris(kullanici: String, sifre: String): JSONObject {
        val b = JSONObject().put("kullanici", kullanici).put("sifre", sifre).put("platform", "android")
        return istek("giris", "POST", b)
    }

    fun ozet(): JSONObject = istek("ozet")
    fun siparisler(q: String = "", durum: String = "", sayfa: Int = 1): JSONObject {
        val ek = buildString {
            if (q.isNotEmpty()) append("&q=").append(URLEncoder.encode(q, "UTF-8"))
            if (durum.isNotEmpty()) append("&durum=").append(URLEncoder.encode(durum, "UTF-8"))
            append("&sayfa=").append(sayfa)
        }
        return istek("siparisler$ek")
    }

    fun siparis(id: Long): JSONObject = istek("siparis/$id")
    fun durumDegistir(id: Long, durum: String, gerekce: String = ""): JSONObject =
        istek("siparis/$id/durum", "POST", JSONObject().put("durum", durum).put("gerekce", gerekce))

    fun fotoYukle(id: Long, dosyaAdi: String, mime: String, baytlar: ByteArray, asama: String): JSONObject {
        val mp = Multipart()
            .alan("stage", asama)
            .alan("foto_not", "")
            .dosya("foto[]", dosyaAdi, mime, baytlar)
        return istek("siparis/$id/foto", "POST", multipart = mp)
    }

    fun saha(tur: String): JSONObject = istek("saha&tur=$tur")
    fun musteriler(q: String = ""): JSONObject =
        istek(if (q.isEmpty()) "musteriler" else "musteriler&q=" + URLEncoder.encode(q, "UTF-8"))

    fun sozluk(): JSONObject = istek("sozluk")

    /** Multipart/form-data gövde üretici (JPEG yükleme için). */
    class Multipart(val sinir: String) {
        constructor() : this("mim" + System.currentTimeMillis())

        private val cikti = java.io.ByteArrayOutputStream()

        private val bitisBayt: ByteArray
            get() = "--$sinir--\r\n".toByteArray(Charsets.US_ASCII)

        /** Toplam gövde boyutu (başlık + veri + bitiş işareti). */
        val boyut: Int get() = cikti.size() + bitisBayt.size

        fun alan(ad: String, deger: String): Multipart {
            cikti.write("--$sinir\r\n".toByteArray())
            cikti.write("Content-Disposition: form-data; name=\"$ad\"\r\n\r\n".toByteArray())
            cikti.write("$deger\r\n".toByteArray())
            return this
        }

        fun dosya(ad: String, dosyaAdi: String, mime: String, baytlar: ByteArray): Multipart {
            cikti.write("--$sinir\r\n".toByteArray())
            cikti.write("Content-Disposition: form-data; name=\"$ad\"; filename=\"$dosyaAdi\"\r\n".toByteArray())
            cikti.write("Content-Type: $mime\r\n\r\n".toByteArray())
            cikti.write(baytlar)
            cikti.write("\r\n".toByteArray())
            return this
        }

        fun yaz(out: java.io.OutputStream) {
            out.write(cikti.toByteArray())
            out.write(bitisBayt)
            out.flush()
        }
    }
}

/** Jetonu güvenli saklama (EncryptedSharedPreferences). */
class TokenStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "mim_tokenlar",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun kaydet(token: String, ad: String) {
        prefs.edit().putString("t", token).putString("ad", ad).apply()
        Api.token = token
    }

    fun yukle(): Pair<String, String> {
        val t = prefs.getString("t", "") ?: ""
        val a = prefs.getString("ad", "") ?: ""
        Api.token = t
        return t to a
    }

    fun sil() {
        prefs.edit().clear().apply()
        Api.token = ""
    }
}
