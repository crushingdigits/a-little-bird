package dev.crushingdigits.littlebird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.crushingdigits.littlebird.ui.components.BottomNav
import dev.crushingdigits.littlebird.ui.feed.FeedScreen
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LittleBirdTheme {
                var tab by remember { mutableStateOf("feed") }
                Scaffold(bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) }) { padding ->
                    when (tab) {
                        "feed" -> FeedScreen(modifier = Modifier.padding(padding))
                        else -> SettingsPlaceholder(Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings — coming soon", style = MaterialTheme.typography.titleLarge)
    }
}
