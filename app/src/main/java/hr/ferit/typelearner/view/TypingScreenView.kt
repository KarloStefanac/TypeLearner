package hr.ferit.typelearner.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import hr.ferit.typelearner.viewmodel.TypingViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.ferit.typelearner.WordStatus
import androidx.compose.ui.text.TextStyle
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.model.repository.ModelRepository
import java.util.UUID


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypingScreenView(
    viewModel: TypingViewModel,
    userId: String,
    isCustom: Boolean,
    customText: String?,
    timeLimit: Float?,
    minAccuracy: Float?,
    testId: String?,
    test: TestData? = null,
    onComplete: (Float, Float, Float) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(userId, isCustom, customText, timeLimit, minAccuracy, testId, test) {
        if (test != null && testId != null) {
            Log.d("TypingVM", "Going into initializeTimedTest")
            viewModel.initializeTimedTest(test, userId)
        } else {
            Log.d("TypingVM", "Going into initializeTest")
            viewModel.initializeTest(
                isCustom = isCustom,
                customText = customText,
                timeLimit = timeLimit,
                minAccuracy = minAccuracy,
                userId = userId,
                testId = testId ?: UUID.randomUUID().toString()
            )
        }
    }

//    // Initialize test only once when the screen is first composed or parameters change
//    LaunchedEffect(userId, isCustom, customText, timeLimit, minAccuracy) {
//        Log.d("TypingVM", "${uiState.isCompleted}")
//        viewModel.initializeTest(isCustom, customText, timeLimit, minAccuracy, userId)
//    }

    // Update time left periodically
    LaunchedEffect(uiState.isStarted, uiState.timeLeft) {
        if (uiState.isStarted && uiState.timeLimit != null && uiState.timeLeft > 0) {
            Log.d("TypingVM", "Going into updateTimeLeft")
            kotlinx.coroutines.delay(1000L)
            viewModel.updateTimeLeft()
        }
    }

    // Trigger navigation on test completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onComplete(uiState.wpm, uiState.accuracy, uiState.elapsedTime)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5E0F27))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.padding(13.dp))
        if (uiState.timeLimit != null) {
            Text(
                text = "Time Left: ${String.format("%.0f", uiState.timeLeft)} seconds",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp),
                color = Color.White
            )
        }
        FlowRow(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            uiState.words.forEachIndexed { index, word ->
//                Log.d("TypingVM", "Selected words: $word")
                Text(
                    text = "$word ",
                    fontSize = 20.sp,
                    color = when (uiState.wordStatuses.getOrNull(index)) {
                        WordStatus.CORRECT -> Color.Green
                        WordStatus.INCORRECT -> Color.Red
                        else -> Color.White
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        BasicTextField(
            value = uiState.typedText,
            onValueChange = { viewModel.updateTypedText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(16.dp),
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = Color.White // 👈 Set text color to white
            ),
            enabled = uiState.timeLeft > 0 && !uiState.isCompleted,
        )
    }
}