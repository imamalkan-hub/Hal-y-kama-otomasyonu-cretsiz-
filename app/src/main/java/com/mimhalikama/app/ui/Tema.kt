package com.mimhalikama.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Marka renkleri — web arayüzüyle birebir
val Navy = Color(0xFF0F2438)
val Teal = Color(0xFF0D9488)
val TealAcik = Color(0xFF14B8A6)
val Zemin = Color(0xFFF1F5F9)
val Kart = Color(0xFFFFFFFF)
val Metin = Color(0xFF1E293B)
val Soluk = Color(0xFF64748B)
val Cizgi = Color(0xFFE2E8F0)
val Kirmizi = Color(0xFFB91C1C)
val Yesil = Color(0xFF15803D)
val Amber = Color(0xFFB45309)

private val AcikRenkler = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Navy,
    background = Zemin,
    surface = Kart,
    onSurface = Metin,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Soluk,
    outline = Cizgi,
    error = Kirmizi,
)

private val KoyuRenkler = darkColorScheme(
    primary = TealAcik,
    onPrimary = Color(0xFF04211F),
    secondary = Color(0xFF1E3A5F),
    background = Color(0xFF0B1220),
    surface = Color(0xFF141F31),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1B2A44),
    onSurfaceVariant = Color(0xFF93A6BA),
    outline = Color(0xFF263650),
    error = Color(0xFFF87171),
)

@Composable
fun MimTema(koyu: Boolean = isSystemInDarkTheme(), icerik: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (koyu) KoyuRenkler else AcikRenkler,
        content = icerik,
    )
}
