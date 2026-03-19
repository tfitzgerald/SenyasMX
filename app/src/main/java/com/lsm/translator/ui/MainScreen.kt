package com.lsm.translator.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Filled.Videocam, contentDescription = null) },
                    label    = { Text("Señas → Texto") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                    label    = { Text("Texto → Señas") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    icon     = { Icon(Icons.Filled.History, contentDescription = null) },
                    label    = { Text("Historial") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> RecognitionScreen(modifier = Modifier.padding(innerPadding))
            1 -> PlaybackScreen(modifier = Modifier.padding(innerPadding))
            2 -> HistoryScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
