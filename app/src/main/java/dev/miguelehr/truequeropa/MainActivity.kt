package dev.miguelehr.truequeropa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import dev.miguelehr.truequeropa.nav.AppNav
import dev.miguelehr.truequeropa.ui.theme.TruequeRopaTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TruequeRopaTheme {
                AppNav() // ← ahora tu app arranca en el flujo de navegación
            }
        }
    }
}