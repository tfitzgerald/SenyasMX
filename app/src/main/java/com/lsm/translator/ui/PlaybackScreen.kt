package com.lsm.translator.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsm.translator.model.Phrase
import com.lsm.translator.viewmodel.PlaybackViewModel

@Composable
fun PlaybackScreen(
    modifier: Modifier = Modifier,
    vm: PlaybackViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {

        // ── Search bar ─────────────────────────────────────────────────────
        OutlinedTextField(
            value           = state.searchQuery,
            onValueChange   = vm::onSearchQueryChanged,
            modifier        = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder     = { Text("Buscar frases en español…") },
            leadingIcon     = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon    = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vm.onSearchQueryChanged("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp)
        )

        // ── Category filter chips ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick  = { vm.onCategorySelected(null) },
                label    = { Text("Todas") }
            )
            state.categories.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat.id,
                    onClick  = { vm.onCategorySelected(cat.id) },
                    label    = { Text(cat.name) }
                )
            }
        }

        // ── Video player card (appears when a phrase is selected) ──────────
        state.selectedPhrase?.let { phrase ->
            VideoPlayerCard(
                videoUri       = vm.videoUri(phrase),
                phraseLabel    = phrase.spanish,
                playbackRate   = state.playbackRate,
                onRateChange   = vm::onPlaybackRateChanged,
                onReplay       = { vm.onPhraseSelected(phrase) }
            )
        }

        // ── Phrase list ────────────────────────────────────────────────────
        if (state.displayedPhrases.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sin resultados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.displayedPhrases, key = { it.id }) { phrase ->
                    PhraseRow(
                        phrase     = phrase,
                        isSelected = state.selectedPhrase?.id == phrase.id,
                        onClick    = { vm.onPhraseSelected(phrase) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ── Video player card ─────────────────────────────────────────────────────────

@Composable
private fun VideoPlayerCard(
    videoUri:     String,
    phraseLabel:  String,
    playbackRate: Float,
    onRateChange: (Float) -> Unit,
    onReplay:     () -> Unit
) {
    // hasError is tracked OUTSIDE AndroidView so Compose can react to it
    var hasError by remember(videoUri) { mutableStateOf(false) }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {

            // ── Video area ─────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasError) {
                    // Only shown when the video file is genuinely missing
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            modifier           = Modifier.size(48.dp),
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Video próximamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // VideoView fills the box and starts playing as soon as
                    // the media is prepared — no overlay blocks it
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory  = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(videoUri))
                                setOnPreparedListener { mp ->
                                    try {
                                        mp.playbackParams =
                                            mp.playbackParams.setSpeed(playbackRate)
                                    } catch (_: Exception) {}
                                    start()
                                }
                                setOnErrorListener { _, _, _ ->
                                    hasError = true   // triggers recompose → shows placeholder
                                    true
                                }
                            }
                        },
                        update = { videoView ->
                            // Called when playbackRate chip is tapped
                            if (videoView.isPlaying) {
                                try {
                                    videoView.setPlaybackSpeed(playbackRate)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }

            // ── Controls row ───────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = phraseLabel,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.titleMedium,
                    modifier   = Modifier.weight(1f)
                )

                // Replay button
                IconButton(onClick = onReplay) {
                    Icon(
                        Icons.Filled.Replay,
                        contentDescription = "Repetir",
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }

                // Speed chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5f, 1.0f, 1.5f).forEach { rate ->
                        FilterChip(
                            selected = playbackRate == rate,
                            onClick  = { onRateChange(rate) },
                            label    = {
                                Text(
                                    "${rate}×",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Single phrase row ─────────────────────────────────────────────────────────

@Composable
private fun PhraseRow(
    phrase:     Phrase,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                phrase.spanish,
                fontWeight = FontWeight.Medium,
                style      = MaterialTheme.typography.bodyLarge
            )
            if (phrase.synonyms.isNotEmpty()) {
                Text(
                    phrase.synonyms.first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Filled.PlayCircle,
            contentDescription = "Reproducir ${phrase.spanish}",
            tint               = if (isSelected) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(24.dp)
        )
    }
}
