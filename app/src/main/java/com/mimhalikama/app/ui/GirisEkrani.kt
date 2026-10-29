package com.mimhalikama.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimhalikama.app.data.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GirisEkrani(basarili: (String, String) -> Unit) {
    var kullanici by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var gizle by remember { mutableStateOf(true) }
    var hata by remember { mutableStateOf("") }
    var calisiyor by remember { mutableStateOf(false) }
    val kapsam = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = Navy) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🧼", style = MaterialTheme.typography.displayLarge)
            Text("Mim Halı Yıkama",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary)
            Text("İşletme yönetim sistemi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f))
            Spacer(Modifier.height(28.dp))

            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(22.dp)) {
                    OutlinedTextField(
                        value = kullanici,
                        onValueChange = { kullanici = it },
                        label = { Text("Kullanıcı adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sifre,
                        onValueChange = { sifre = it },
                        label = { Text("Şifre") },
                        singleLine = true,
                        visualTransformation = if (gizle) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { gizle = !gizle }) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = if (gizle) "Şifreyi göster" else "Şifreyi gizle",
                                    tint = if (gizle) Soluk else Teal,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (hata.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(hata, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (kullanici.isBlank() || sifre.isBlank()) {
                                hata = "Kullanıcı adı ve şifre gerekli."
                                return@Button
                            }
                            hata = ""
                            calisiyor = true
                            kapsam.launch {
                                try {
                                    val cevap = withContext(Dispatchers.IO) {
                                        Api.giris(kullanici.trim(), sifre)
                                    }
                                    val token = cevap.getString("token")
                                    val ad = cevap.optJSONObject("user")?.optString("ad").orEmpty()
                                    basarili(token, ad)
                                } catch (e: Api.ApiHata) {
                                    hata = e.mesaj
                                } catch (e: Exception) {
                                    hata = "Beklenmeyen hata: ${e.message}"
                                } finally {
                                    calisiyor = false
                                }
                            }
                        },
                        enabled = !calisiyor,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        if (calisiyor) CircularProgressIndicator(
                            Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary
                        ) else Text("Giriş Yap", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Mim Halı Yıkama · Saha uygulaması",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .55f),
                textAlign = TextAlign.Center)
        }
    }
}
