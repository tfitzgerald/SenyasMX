package com.lsm.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lsm.translator.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SenyasMXTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun SenyasMXTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary          = Color(0xFF00BCD4),
            onPrimary        = Color.Black,
            secondary        = Color(0xFF80CBC4),
            tertiary         = Color(0xFF4CAF50),
            background       = Color(0xFF121212),
            surface          = Color(0xFF1E1E1E),
            onBackground     = Color.White,
            onSurface        = Color.White,
            error            = Color(0xFFCF6679)
        )
    } else {
        lightColorScheme(
            primary          = Color(0xFF00838F),
            onPrimary        = Color.White,
            secondary        = Color(0xFF00695C),
            tertiary         = Color(0xFF2E7D32),
            background       = Color(0xFFF5F5F5),
            surface          = Color.White,
            onBackground     = Color(0xFF212121),
            onSurface        = Color(0xFF212121),
            error            = Color(0xFFB00020)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
