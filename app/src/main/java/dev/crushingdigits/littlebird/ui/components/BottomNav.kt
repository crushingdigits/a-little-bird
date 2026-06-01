package dev.crushingdigits.littlebird.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BottomNav(selected: String, onSelect: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == "feed",
            onClick = { onSelect("feed") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Feed") },
        )
        NavigationBarItem(
            selected = selected == "settings",
            onClick = { onSelect("settings") },
            icon = { Icon(Icons.Default.Tune, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}
