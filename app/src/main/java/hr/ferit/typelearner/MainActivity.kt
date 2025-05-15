package hr.ferit.typelearner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.math.roundToInt
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TypingTestApp()
        }
    }
}

@Composable
fun TypingTestApp() {
    val navController = rememberNavController()
    MaterialTheme {
        NavHost(navController = navController, startDestination = "typing") {
            composable("typing") {
                TypingScreen { wpm, accuracy, time ->
                    navController.navigate("results/$wpm/$accuracy/$time")
                }
            }
            composable("results/{wpm}/{accuracy}/{time}") { backStackEntry ->
                val wpm = backStackEntry.arguments?.getString("wpm")?.toFloatOrNull() ?: 0f
                val accuracy = backStackEntry.arguments?.getString("accuracy")?.toFloatOrNull() ?: 0f
                val time = backStackEntry.arguments?.getString("time")?.toFloatOrNull() ?: 0f
                ResultsScreen(wpm, accuracy, time) {
                    navController.popBackStack("typing", inclusive = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypingScreen(onComplete: (Float, Float, Float) -> Unit) {
    var randomWords = arrayOf("mouse", "keyboard", "dog", "laptop", "class", "faculty", "brain", "the", "quick", "brown", "fox", "jumps", "tablet", "charger", "over")
    val words by remember {
        mutableStateOf(
            randomWords.toList().shuffled(Random).take(20)
        )
    }
    var typedText by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(0L) }
    var wordStatuses by remember { mutableStateOf(List(words.size) { WordStatus.NOT_TYPED }) }
    var isStarted by remember { mutableStateOf(false) }
    var currentWordIndex by remember { mutableStateOf(0) }
    var typedWords by remember { mutableStateOf(mutableListOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.padding(10.dp))
        // Display sample text with colored words
        FlowRow(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            words.forEachIndexed { index, word ->
                Text(
                    text = "$word ",
                    fontSize = 20.sp,
                    color = when (wordStatuses.getOrNull(index)) {
                        WordStatus.CORRECT -> Color.Green
                        WordStatus.INCORRECT -> Color.Red
                        else -> Color.Black
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Text input field
        BasicTextField(
            value = typedText,
            onValueChange = { newText ->
                if (!isStarted) {
                    startTime = System.currentTimeMillis()
                    isStarted = true
                }

                if (newText.endsWith(" ") && newText.trim().isNotEmpty()) {
                    // Word completed (space pressed)
                    val typedWord = newText.trim()
                    typedWords.add(typedWord)

                    // Check if word is correct
                    val isCorrect = typedWord == words.getOrNull(currentWordIndex)
                    wordStatuses = wordStatuses.toMutableList().also {
                        if (currentWordIndex < it.size) {
                            it[currentWordIndex] = if (isCorrect) WordStatus.CORRECT else WordStatus.INCORRECT
                        }
                    }

                    // Clear input and move to next word
                    typedText = ""
                    currentWordIndex++

                    // Check if test is complete
                    if (currentWordIndex >= words.size) {
                        val timeSeconds = (System.currentTimeMillis() - startTime) / 1000f
                        val correctWords = wordStatuses.count { it == WordStatus.CORRECT }
                        val wpm = if (timeSeconds > 0) (correctWords / timeSeconds * 60).roundToInt().toFloat() else 0f
                        val accuracy = (correctWords.toFloat() / words.size * 100).roundToInt().toFloat()
                        onComplete(wpm, accuracy, timeSeconds)
                    }
                } else {
                    typedText = newText
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(16.dp),
            textStyle = TextStyle(fontSize = 18.sp)
        )
    }
}

@Composable
fun ResultsScreen(wpm: Float, accuracy: Float, time: Float, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Results",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Words per Minute: ${wpm.toInt()}",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Accuracy: ${accuracy.toInt()}%",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Time: ${String.format("%.2f", time)} seconds",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onRestart,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Try Again")
        }
    }
}

enum class WordStatus {
    NOT_TYPED,
    CORRECT,
    INCORRECT
}