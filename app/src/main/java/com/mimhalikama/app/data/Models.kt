package com.mimhalikama.app.data

import org.json.JSONArray
import org.json.JSONObject

/** API JSON'dan uygulama modellerine çevirim (yalnız ihtiyaç duyulan alanlar). */
data class Siparis(
    val id: Long,
    val kod: String,
    val durum: String,
    val durumEtiket: String,
    val durumRenk: String,
    val musteri: String,
    val telefon: String,
    val ilce: String,
    val adres: String,
    val alimTuru: String,
    val alimTarih: String,
    val teslimTarih: String,
    val toplamKurus: Long,
    val indirimKurus: Long,
    val odendiKurus: Long,
    val kalanKurus: Long,
    val parcaAdedi: Int,
    val not: String,
    val personel: String,
    val gecikti: Boolean,
) {
    companion object {
        fun from(j: JSONObject) = Siparis(
            id = j.getLong("id"),
            kod = j.getString("kod"),
            durum = j.getString("durum"),
            durumEtiket = j.getString("durum_etiket"),
            durumRenk = j.getString("durum_renk"),
            musteri = j.optString("musteri"),
            telefon = j.optString("telefon"),
            ilce = j.optString("ilce"),
            adres = j.optString("adres"),
            alimTuru = j.optString("alim_turu"),
            alimTarih = j.optString("alim_tarih"),
            teslimTarih = j.optString("teslim_tarih"),
            toplamKurus = j.optLong("toplam_kurus"),
            indirimKurus = j.optLong("indirim_kurus"),
            odendiKurus = j.optLong("odendi_kurus"),
            kalanKurus = j.optLong("kalan_kurus"),
            parcaAdedi = j.optInt("parca_adedi"),
            not = j.optString("not"),
            personel = j.optString("personel"),
            gecikti = j.optInt("gecikti") == 1,
        )
    }
}

data class Kalem(
    val id: Long, val kod: String, val tur: String, val olcu: String,
    val adet: Int, val birimKurus: Long, val tutarKurus: Long,
) {
    companion object {
        fun from(j: JSONObject) = Kalem(
            j.getLong("id"), j.optString("kod"), j.getString("tur"), j.optString("olcu"),
            j.optInt("adet"), j.optLong("birim_kurus"), j.optLong("tutar_kurus"),
        )
    }
}

data class FotoNesne(val id: Long, val asama: String, val asamaEtiket: String, val not: String) {
    companion object {
        fun from(j: JSONObject) = FotoNesne(
            j.getLong("id"), j.optString("asama"), j.optString("asama_etiket"), j.optString("not"),
        )
    }
}

data class SiparisDetay(
    val siparis: Siparis,
    val kalemler: List<Kalem>,
    val foto: List<FotoNesne>,
    val sonrakiDurum: List<String>,
    val durumlar: Map<String, String>,
)

data class Ozet(
    val alimBugun: Int, val teslimBugun: Int, val gecikmis: Int, val kritik: Int,
    val yikamaBekleyen: Int, val islemde: Int, val hazir: Int, val dagitimda: Int,
    val ciroKurus: Long, val tahsilatKurus: Long, val giderKurus: Long,
    val acikHesapKurus: Long,
) {
    companion object {
        fun from(j: JSONObject) = Ozet(
            alimBugun = j.optInt("alim_bugun"),
            teslimBugun = j.optInt("teslim_bugun"),
            gecikmis = j.optInt("gecikmis"),
            kritik = j.optInt("kritik"),
            yikamaBekleyen = j.optInt("yikama_bekleyen"),
            islemde = j.optInt("islemde"),
            hazir = j.optInt("hazir"),
            dagitimda = j.optInt("dagitimda"),
            ciroKurus = j.optLong("ciro_kurus"),
            tahsilatKurus = j.optLong("tahsilat_kurus"),
            giderKurus = j.optLong("gider_kurus"),
            acikHesapKurus = j.optLong("acik_hesap_kurus"),
        )
    }
}

data class Musteri(
    val id: Long, val ad: String, val telefon: String, val ilce: String, val adres: String,
) {
    companion object {
        fun from(j: JSONObject) = Musteri(
            j.getLong("id"), j.optString("ad"), j.optString("telefon"),
            j.optString("ilce"), j.optString("adres"),
        )
    }
}

/** JSON dizisi → Kotlin listesi yardımcısı */
fun <T> JSONArray.mapObj(f: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) out.add(f(optJSONObject(i) ?: JSONObject()))
    return out
}

fun tlFormatla(kurus: Long): String {
    val tl = kurus / 100
    val kalan = (kurus % 100).toInt()
    val gruplu = java.text.DecimalFormat("#,##0").format(tl).replace(',', '.')
    return if (kalan == 0) "₺$gruplu" else "₺$gruplu,%02d".format(kalan)
}
