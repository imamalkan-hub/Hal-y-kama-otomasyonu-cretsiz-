package com.mimhalikama.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimhalikama.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Sekme(val etiket: String, val ikon: @Composable () -> Unit) {
    Panel("Panel", { Icon(Icons.Outlined.Home, null) }),
    Siparisler("Sipariş", { Icon(Icons.Outlined.List, null) }),
    Alim("Alım", { Icon(Icons.Outlined.List, null) }),
    Teslimat("Teslimat", { Icon(Icons.Outlined.Place, null) }),
    Gecikmis("Gecikmiş", { Icon(Icons.Outlined.Warning, null) }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnaEkrani(cikisYap: () -> Unit) {
    var sekme by remember { mutableStateOf(Sekme.Panel) }
    var detayId by remember { mutableStateOf<Long?>(null) }

    detayId?.let { id ->
        DetayEkrani(
            id = id,
            geri = { detayId = null },
            detaySonrasi = cikisYap,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mim Halı Yıkama", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = cikisYap) {
                        Icon(Icons.Outlined.ExitToApp, contentDescription = "Çıkış")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Kart) {
                Sekme.entries.forEach { s ->
                    NavigationBarItem(
                        selected = sekme == s,
                        onClick = { sekme = s },
                        icon = s.ikon,
                        label = { Text(s.etiket, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Teal,
                            selectedTextColor = Teal,
                            indicatorColor = Teal.copy(alpha = .14f),
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (sekme) {
                Sekme.Panel -> PanelEkrani(siparisAc = { detayId = it })
                Sekme.Siparisler -> ListeEkrani(tur = "tumu", siparisAc = { detayId = it })
                Sekme.Alim -> ListeEkrani(tur = "alim", siparisAc = { detayId = it })
                Sekme.Teslimat -> ListeEkrani(tur = "teslimat", siparisAc = { detayId = it })
                Sekme.Gecikmis -> ListeEkrani(tur = "gecikmis", siparisAc = { detayId = it })
            }
        }
    }
}

// ================================================================ PANEL
@Composable
fun PanelEkrani(siparisAc: (Long) -> Unit) {
    var ozet by remember { mutableStateOf<Ozet?>(null) }
    var hata by remember { mutableStateOf("") }
    var yeniliyor by remember { mutableStateOf(false) }
    val kapsam = rememberCoroutineScope()

    suspend fun yukle() {
        yeniliyor = true; hata = ""
        try {
            ozet = withContext(Dispatchers.IO) { Ozet.from(Api.ozet()) }
        } catch (e: Api.ApiHata) {
            hata = e.mesaj
        } catch (e: Exception) {
            hata = "Bağlantı hatası: ${e.message}"
        } finally { yeniliyor = false }
    }

    LaunchedEffect(Unit) { yukle() }

    PullYenile(yeniliyor, onYenile = { kapsam.launch { yukle() } }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            if (hata.isNotEmpty()) {
                UyariKarti(hata, Kirmizi)
                Spacer(Modifier.height(10.dp))
            }
            val o = ozet
            if (o != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiKarti("Bugün Alım", o.alimBugun.toString(), Teal, Modifier.weight(1f))
                    KpiKarti("Bugün Teslim", o.teslimBugun.toString(), Navy, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiKarti("Gecikmiş", o.gecikmis.toString(), if (o.gecikmis > 0) Kirmizi else Yesil, Modifier.weight(1f))
                    KpiKarti("Açık Bakiye", tlFormatla(o.acikHesapKurus), Amber, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text("İş Akışı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(6.dp)) {
                        AkisSatiri("Yıkama bekleyen", o.yikamaBekleyen)
                        AkisSatiri("İşlemde", o.islemde)
                        AkisSatiri("Hazır", o.hazir)
                        AkisSatiri("Dağıtımda", o.dagitimda)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Bugünün Parası", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ParaSatiri("Tahsilat", o.tahsilatKurus, Yesil)
                        ParaSatiri("Gider", o.giderKurus, Kirmizi)
                        HorizontalDivider(color = Cizgi.copy(alpha = .6f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net", fontWeight = FontWeight.Bold)
                            Text(tlFormatla(o.tahsilatKurus - o.giderKurus),
                                fontWeight = FontWeight.Bold,
                                color = if (o.tahsilatKurus - o.giderKurus >= 0) Yesil else Kirmizi)
                        }
                    }
                }
            } else if (hata.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal)
                }
            }
        }
    }
}

@Composable
private fun AkisSatiri(etiket: String, adet: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(etiket)
        Badge(containerColor = if (adet > 0) Teal.copy(alpha = .15f) else Cizgi.copy(alpha = .5f),
            contentColor = if (adet > 0) Teal else Soluk) {
            Text("$adet", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ParaSatiri(etiket: String, kurus: Long, renk: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiket)
        Text(tlFormatla(kurus), color = renk, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun KpiKarti(etiket: String, deger: String, vurgu: Color, modifier: Modifier = Modifier, tikla: () -> Unit = {}) {
    Card(modifier.clickable(onClick = tikla), shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.fillMaxWidth().background(vurgu.copy(alpha = .06f))) {
            Column(Modifier.padding(14.dp)) {
                Text(deger, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = vurgu)
                Text(etiket.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = Soluk, letterSpacing = 0.8.sp)
            }
        }
    }
}

@Composable
fun UyariKarti(mesaj: String, renk: Color) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = renk.copy(alpha = .08f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = renk)
            Spacer(Modifier.width(10.dp))
            Text(mesaj, color = renk, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Basit çek-yenile benzetimi: üstte "Yenile" düğmesi + içerik. */
@Composable
fun PullYenile(yeniliyor: Boolean, onYenile: () -> Unit, icerik: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        icerik()
        if (yeniliyor) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter), color = Teal)
        }
    }
}

// ================================================================ LİSTE
@Composable
fun ListeEkrani(tur: String, siparisAc: (Long) -> Unit) {
    var liste by remember { mutableStateOf<List<Siparis>>(emptyList()) }
    var hata by remember { mutableStateOf("") }
    var arama by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(true) }
    val kapsam = rememberCoroutineScope()

    suspend fun yukle(q: String) {
        yukleniyor = true
        hata = ""
        try {
            val veri = withContext(Dispatchers.IO) {
                if (tur == "tumu") {
                    Api.siparisler(q).getJSONArray("siparisler").mapObj { Siparis.from(it) }
                } else {
                    Api.saha(tur).getJSONArray("siparisler").mapObj { Siparis.from(it) }
                }
            }
            liste = veri
        } catch (e: Api.ApiHata) {
            hata = e.mesaj
        } catch (e: Exception) {
            hata = "Bağlantı hatası: ${e.message}"
        } finally {
            yukleniyor = false
        }
    }

    LaunchedEffect(arama) {
        kotlinx.coroutines.delay(350)   // yazarken her tuşta istek atma
        yukle(arama)
    }

    Column(Modifier.fillMaxSize()) {
        if (tur == "tumu") {
            OutlinedTextField(
                value = arama,
                onValueChange = { arama = it },
                placeholder = { Text("Kod, müşteri, telefon ara…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(14.dp),
            )
        }
        if (hata.isNotEmpty()) {
            UyariKarti(hata, Kirmizi)
            Spacer(Modifier.height(8.dp))
            Modifier.padding(horizontal = 12.dp)
        }
        if (yukleniyor && liste.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal)
            }
        } else if (liste.isEmpty()) {
            BosEkran("Kayıt bulunamadı", "Bu listede henüz sipariş yok.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(liste, key = { it.id }) { s ->
                    SiparisSatiri(s) { siparisAc(s.id) }
                }
            }
        }
    }
}

@Composable
fun BosEkran(baslik: String, alt: String) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.List, null, tint = Soluk, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(10.dp))
        Text(baslik, fontWeight = FontWeight.Bold)
        Text(alt, color = Soluk, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SiparisSatiri(s: Siparis, tikla: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = tikla), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(5.dp).height(46.dp)
                    .background(hexRenk(s.durumRenk), RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.musteri, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (s.gecikti) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Warning, "gecikti", tint = Kirmizi,
                            modifier = Modifier.size(16.dp))
                    }
                }
                Text("${s.kod} · ${s.parcaAdedi} parça · ${s.durumEtiket}",
                    style = MaterialTheme.typography.labelMedium, color = Soluk, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(tlFormatla(s.toplamKurus), fontWeight = FontWeight.Bold)
                if (s.kalanKurus > 0) {
                    Text("kalan ${tlFormatla(s.kalanKurus)}", style = MaterialTheme.typography.labelSmall,
                        color = Amber)
                }
            }
        }
    }
}

/** "#rrggbb" → Color */
fun hexRenk(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF64748B)
}
