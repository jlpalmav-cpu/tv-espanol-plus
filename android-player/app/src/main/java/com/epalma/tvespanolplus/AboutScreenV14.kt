package com.epalma.tvespanolplus

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TVEspanolAboutV14() {
    val context = LocalContext.current
    val phone = !remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    MaterialTheme {
        Surface(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            color = Color(0xFF101114)
        ) {
            Column(
                Modifier.fillMaxSize().padding(if (phone) 20.dp else 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 56.dp else 82.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TV Español+", color = Color.White, fontWeight = FontWeight.Black, fontSize = if (phone) 24.sp else 38.sp)
                }
                Text("Versión 1.4.0", color = Color(0xFF19B5A5), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
                Text("Android TV · Google TV · Android móvil · Kotlin · Media3", color = Color(0xFFB8BBC3), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Text("M3U · EPG · Súper Búsqueda · audio/subtítulos · Vista doble · listas seguras", color = Color(0xFFB8BBC3), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("© epalma", color = Color(0xFFB8BBC3), fontSize = 12.sp)
                    Text("+504 99461582 - Tegucigalpa - Honduras", color = Color(0xFFB8BBC3), fontSize = 11.sp)
                }
            }
        }
    }
}
