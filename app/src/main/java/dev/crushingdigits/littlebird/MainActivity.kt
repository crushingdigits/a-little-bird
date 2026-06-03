package dev.crushingdigits.littlebird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.crushingdigits.littlebird.ui.navigation.AppNavHost
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LittleBirdTheme { AppNavHost() }
        }
    }
}
