package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.ui.theme.NimazTheme

// Data classes and states
data class QiadaLetter(
    val arabic: String,
    val pronunciation: String,
    val description: String,
    val examples: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiadaScreen(
    modifier: Modifier = Modifier,
) {
    var currentLetter by remember { mutableStateOf(0) }
    var showExamples by remember { mutableStateOf(false) }
    var practiceMode by remember { mutableStateOf(false) }

    val letters = listOf(
        QiadaLetter("ا", "Alif", "First letter of Arabic alphabet", listOf("أَحَد", "إِيمَان", "أُمّ")),
        QiadaLetter("ب", "Ba", "Second letter", listOf("بَاب", "بِنْت", "بُستَان")),
        QiadaLetter("ت", "Ta", "Third letter", listOf("تَمْر", "تِين", "تُوت")),
        // Add more letters...
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LinearProgressIndicator(
            progress = (currentLetter + 1) / letters.size.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        AnimatedVisibility(
            visible = !practiceMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            LearningSection(
                letter = letters[currentLetter],
                showExamples = showExamples,
                onToggleExamples = { showExamples = !showExamples }
            )
        }

        AnimatedVisibility(
            visible = practiceMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            PracticeSection(
                letter = letters[currentLetter],
                onExitPractice = { practiceMode = false }
            )
        }

        NavigationControls(
            currentIndex = currentLetter,
            totalItems = letters.size,
            onPrevious = { if (currentLetter > 0) currentLetter-- },
            onNext = { if (currentLetter < letters.size - 1) currentLetter++ }
        )

        if (!practiceMode) {
            OutlinedButton(
                onClick = { practiceMode = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Practice Writing")
            }
        }
    }
}

@Composable
fun LearningSection(
    letter: QiadaLetter,
    showExamples: Boolean,
    onToggleExamples: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter.arabic,
                style = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = letter.pronunciation,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium)
        )

        Text(
            text = letter.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        if (showExamples) {
            ExamplesSection(examples = letter.examples)
        }

        TextButton(onClick = onToggleExamples) {
            Text(if (showExamples) "Hide Examples" else "Show Examples")
        }
    }
}

@Composable
fun PracticeSection(
    letter: QiadaLetter,
    onExitPractice: () -> Unit
) {
    var drawingPath by remember { mutableStateOf(Path()) }

    val drawingColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Practice writing:", style = MaterialTheme.typography.titleLarge)

        Canvas(
            modifier = Modifier
                .size(300.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        drawingPath = drawingPath.apply {
                            lineTo(
                                change.position.x,
                                change.position.y
                            )
                        }
                    }
                }
        ) {
            drawPath(
                drawingPath,
                color = drawingColor,
                style = Stroke(width = 5f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { drawingPath = Path() }) {
                Text("Clear")
            }
            Button(onClick = onExitPractice) {
                Text("Exit Practice")
            }
        }
    }
}

@Composable
fun ExamplesSection(examples: List<String>) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Examples:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        examples.forEach { example ->
            Text(
                text = example,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun NavigationControls(
    currentIndex: Int,
    totalItems: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onPrevious,
            enabled = currentIndex > 0
        ) {
            Text("Previous")
        }
        Button(
            onClick = onNext,
            enabled = currentIndex < totalItems - 1
        ) {
            Text("Next")
        }
    }
}
@Preview
@Composable
fun PreviewQiadaInterface() {
    NimazTheme {
        QiadaScreen()
    }
}