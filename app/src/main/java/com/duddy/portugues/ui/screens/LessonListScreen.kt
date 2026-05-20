package com.duddy.portugues.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duddy.portugues.data.model.Lesson
import com.duddy.portugues.data.model.Phrase

@Composable
fun LessonListScreen(
    lessons: List<Lesson>,
    searchQuery: String,
    searchResults: List<Phrase>,
    favoritePhraseIds: Set<String>,
    onSearchQueryChanged: (String) -> Unit,
    onLessonSelected: (Lesson) -> Unit,
    onLessonListenSelected: (Lesson) -> Unit,
    onPhraseSelected: (Phrase) -> Unit,
    onFavoriteToggled: (Phrase) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Lessons",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose a category and practice its phrases.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                label = { Text("Search phrases") },
                placeholder = { Text("coffee, taxi, help, bathroom") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = if (searchResults.isEmpty()) "No phrase matches" else "Phrase matches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(searchResults, key = { phrase -> phrase.id }) { phrase ->
                PhraseSearchRow(
                    phrase = phrase,
                    isFavorite = phrase.id in favoritePhraseIds,
                    onPractice = { onPhraseSelected(phrase) },
                    onToggleFavorite = { onFavoriteToggled(phrase) }
                )
            }
        }

        item {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(lessons, key = { lesson -> lesson.id }) { lesson ->
            LessonRow(
                lesson = lesson,
                onPractice = { onLessonSelected(lesson) },
                onListen = { onLessonListenSelected(lesson) }
            )
        }
    }
}

@Composable
private fun PhraseSearchRow(
    phrase: Phrase,
    isFavorite: Boolean,
    onPractice: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = phrase.portuguese,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = phrase.english,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${phrase.category.displayName} · ${phrase.pronunciationGuide}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onToggleFavorite) {
                    Text(if (isFavorite) "Saved" else "Save")
                }
                TextButton(onClick = onPractice) {
                    Text("Practice")
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: Lesson,
    onPractice: () -> Unit,
    onListen: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "${lesson.phraseCount} phrases",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Button(onClick = onListen) {
                    Text("Listen")
                }
                OutlinedButton(onClick = onPractice) {
                    Text("Practice")
                }
            }
        }
    }
}
