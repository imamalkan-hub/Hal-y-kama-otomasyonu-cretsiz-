package com.mimhalikama.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mimhalikama.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetayEkrani(id: Long, geri: () -> Unit, detaySonrasi: () -> Unit) {
    var detay by remember { mutableStateOf<SiparisDetay?>(null) }
    var hata by remember { mutableStateOf("") }
    var islem by remember { mutableStateOf(false) }
    var menuAcik by remember { mutableStateOf(false) }
    val kapsam = rememberCoroutineScope()
    val baglam = LocalContext.current

    suspend fun yukle() {
        hata = ""
        try {
            detay = withContext(Dispatchers.IO) {
                val j = Api.siparis(id)
                SiparisDetay(
                    siparis = Siparis.from(j.getJSONObject("siparis")),
                    kalemler = j.getJSONArray("kalemler").mapObj { Kalem.from(it) },
                    foto = j.getJSONArray("foto").mapObj { FotoNesne.from(it) },
                    sonrakiDurum = j.optJSONArray("sonraki_durum")?.let { a ->
                        (0 until a.length()).map { a.optString(it) }
                    } ?: emptyList(),
                    durumlar = j.getJSONObject("durumlar").let { d ->
                        d.keys().asSequence().associateWith { d.getString(it) }
                    },
                )
            }
        } catch (e: Api.ApiHata) {
            hata = e.mesaj
        } catch (e: Exception) {
            hata = "Bağlantı hatası: ${e.message}"
        }
    }

    LaunchedEffect(id) { yukle() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detay?.siparis?.kod ?: "Sipariş", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = geri) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { pad ->
        val s = detay?.siparis
        Box(Modifier.padding(pad)) {
            if (detay == null && hata.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal)
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (hata.isNotEmpty()) UyariKarti(hata, Kirmizi)

                    s?.let { sip ->
                        // --- durum + müşteri kartı
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.background(hexRenk(sip.durumRenk), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)) {
                                        Text(sip.durumEtiket, color = Color.White,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                    if (sip.gecikti) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("GECİKMİŞ", color = Kirmizi,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                HorizontalDivider(color = Cizgi, thickness = 1.dp)
                                Text(sip.musteri, style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold)
                                if (sip.ilce.isNotEmpty() || sip.adres.isNotEmpty()) {
                                    Text(listOf(sip.adres, sip.ilce).filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                        color = Soluk, style = MaterialTheme.typography.bodySmall)
                                }
                                if (sip.telefon.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                            onClick = {
                                                baglam.startActivity(Intent(Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${sip.telefon}")))
                                            },
                                            label = { Text("Ara") },
                                            leadingIcon = { Icon(Icons.Outlined.Call, null, Modifier.height(18.dp)) },
                                        )
                                        AssistChip(
                                            onClick = {
                                                val no = sip.telefon.replace(Regex("[^0-9+]"), "")
                                                baglam.startActivity(Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("https://wa.me/$no")))
                                            },
                                            label = { Text("WhatsApp") },
                                            leadingIcon = { Icon(Icons.Outlined.Email, null, Modifier.height(18.dp)) },
                                        )
                                        if (sip.adres.isNotBlank()) {
                                            AssistChip(
                                                onClick = {
                                                    val a = Uri.encode(sip.adres + " " + sip.ilce)
                                                    baglam.startActivity(Intent(Intent.ACTION_VIEW,
                                                        Uri.parse("geo:0,0?q=$a")))
                                                },
                                                label = { Text("Harita") },
                                                leadingIcon = { Icon(Icons.Outlined.Place, null, Modifier.height(18.dp)) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- durum ilerletme
                        if (detay!!.sonrakiDurum.isNotEmpty() || sip.durum != "teslim") {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Durum", style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    val sonraki = detay!!.sonrakiDurum.firstOrNull()
                                    if (sonraki != null) {
                                        Button(
                                            onClick = {
                                                islem = true
                                                kapsam.launch {
                                                    try {
                                                        withContext(Dispatchers.IO) {
                                                            Api.durumDegistir(id, sonraki)
                                                        }
                                                        yukle()
                                                    } catch (e: Api.ApiHata) {
                                                        hata = e.mesaj
                                                    } finally { islem = false }
                                                }
                                            },
                                            enabled = !islem,
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                        ) {
                                            Text("→ ${detay!!.durumlar[sonraki] ?: sonraki}",
                                                fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text("Bu sipariş akışın sonunda.",
                                            color = Soluk, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // --- kalemler
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Parçalar (${detay!!.kalemler.sumOf { it.adet }})",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                detay!!.kalemler.forEach { k ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(k.tur, fontWeight = FontWeight.SemiBold)
                                            if (k.olcu.isNotBlank()) Text(k.olcu, color = Soluk,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text("${k.adet}×", fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(12.dp))
                                        Text(tlFormatla(k.tutarKurus),
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                    HorizontalDivider(color = Cizgi.copy(alpha = .6f))
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Toplam", fontWeight = FontWeight.Bold)
                                    Text(tlFormatla(sip.toplamKurus - sip.indirimKurus),
                                        fontWeight = FontWeight.ExtraBold)
                                }
                                if (sip.odendiKurus > 0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ödenen", color = Yesil)
                                        Text(tlFormatla(sip.odendiKurus), color = Yesil)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Kalan", color = if (sip.kalanKurus > 0) Amber else Yesil,
                                            fontWeight = FontWeight.SemiBold)
                                        Text(tlFormatla(sip.kalanKurus),
                                            color = if (sip.kalanKurus > 0) Amber else Yesil,
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // --- fotoğraflar
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Fotoğraflar (${detay!!.foto.size})",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                if (detay!!.foto.isEmpty()) {
                                    Text("Henüz fotoğraf yok.", color = Soluk,
                                        style = MaterialTheme.typography.bodySmall)
                                } else {
                                    detay!!.foto.take(6).forEach { f ->
                                        Text("• ${f.asamaEtiket}" +
                                                if (f.not.isNotBlank()) " — ${f.not}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Soluk, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
