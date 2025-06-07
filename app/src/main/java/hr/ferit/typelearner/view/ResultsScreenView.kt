package hr.ferit.typelearner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import hr.ferit.typelearner.viewmodel.ResultsViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.ferit.typelearner.viewmodel.TypingViewModel


@Composable
fun ResultsScreenView(
    viewModel: ResultsViewModel,
    wpm: Float,
    accuracy: Float,
    time: Float,
    userId: String,
    onRestart: () -> Unit,
    typingViewModel: TypingViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(wpm, accuracy, time, userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5E0F27))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Results",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp),
            color = Color.White
        )

        Text(
            text = "Words per Minute: ${uiState.wpm.toInt()}",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.White
        )

        Text(
            text = "Accuracy: ${uiState.accuracy.toInt()}%",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.White
        )

        Text(
            text = "Time: ${String.format("%.2f", time)} seconds",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp),
            color = Color.White
        )

        Button(
            onClick = onRestart,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Try Again")
        }
    }
}