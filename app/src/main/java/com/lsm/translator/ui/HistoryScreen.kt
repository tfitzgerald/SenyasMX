package com.lsm.translator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsm.translator.viewmodel.RecognitionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    vm: RecognitionViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {

        // ── Top bar ────────────────────────────────────────────────────────
        TopAppBar(
            title   = { Text("Historial") },
            actions = {
                if (state.history.isNotEmpty()) {
                    TextButton(onClick = { vm.clearHistory() }) {
                        Text(
                            "Limpiar todo",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )

        // ── Empty state ────────────────────────────────────────────────────
        if (state.history.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier           = Modifier.size(64.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Sin historial aún",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Las frases reconocidas aparecerán aquí",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {

            // ── History list ───────────────────────────────────────────────
            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                items(state.history, key = { it.id }) { item ->

                    val dotColor = when {
                        item.confidence >= 0.85f -> Color(0xFF4CAF50)
                        item.confidence >= 0.65f -> Color(0xFF00BCD4)
                        else                     -> Color(0xFFFF9800)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Colour dot indicating confidence level
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape    = CircleShape,
                            color    = dotColor
                        ) {}

                        // Phrase text + timestamp
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.phrase.spanish,
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            val fmt = SimpleDateFormat("HH:mm:ss", Locale("es", "MX"))
                            Text(
                                fmt.format(Date(item.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Confidence badge
                        Surface(
                            color = dotColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "${(item.confidence * 100).toInt()}%",
                                color      = Color.White,
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical   = 3.dp
                                )
                            )
                        }

                        // Speak button
                        IconButton(
                            onClick  = { vm.speakPhrase(item.phrase.spanish) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Escuchar ${item.phrase.spanish}",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
