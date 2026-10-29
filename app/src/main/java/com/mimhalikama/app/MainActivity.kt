package com.mimhalikama.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mimhalikama.app.data.TokenStore
import com.mimhalikama.app.ui.MimTema
import com.mimhalikama.app.ui.GirisEkrani
import com.mimhalikama.app.ui.AnaEkrani
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val store = TokenStore(this)
        setContent {
            MimTema {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var girisYapildi by remember { mutableStateOf(false) }
                    var kontrolEdildi by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        val (t, _) = store.yukle()
                        girisYapildi = t.isNotEmpty()
                        kontrolEdildi = true
                    }
                    if (!kontrolEdildi) return@Surface
                    if (girisYapildi) {
                        AnaEkrani(cikisYap = {
                            store.sil()
                            girisYapildi = false
                        })
                    } else {
                        GirisEkrani(basarili = { token, ad ->
                            store.kaydet(token, ad)
                            girisYapildi = true
                        })
                    }
                }
            }
        }
    }
}
