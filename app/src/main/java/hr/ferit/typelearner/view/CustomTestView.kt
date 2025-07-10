package hr.ferit.typelearner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.ferit.typelearner.viewmodel.CustomTestViewModel

@Composable
fun CustomTestView(
    viewModel: CustomTestViewModel,
    onStartCustomTest: (String, Float?, Float?) -> Unit){

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5E0F27))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = "Create Custom Test",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
            color = Color.White
        )

        // Custom text input
        OutlinedTextField(
            value = uiState.customText,
            onValueChange = { viewModel.updateCustomText(it) },
            label = { Text("Enter text to type") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 16.dp),
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
            maxLines = 5
        )

        // Time limit input
        OutlinedTextField(
            value = uiState.timeLimit,
            onValueChange = { viewModel.updateTimeLimit(it.filter { char -> char.isDigit() || char == '.' }) },
            label = { Text("Time limit (seconds, optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 16.sp,color = Color.White)
        )

        // Minimum accuracy input
        OutlinedTextField(
            value = uiState.minAccuracy,
            onValueChange = { viewModel.updateMinAccuracy(it.filter { char -> char.isDigit() || char == '.' }) },
            label = { Text("Minimum accuracy (%, optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White)
        )
        Button(
            onClick = {
                onStartCustomTest(
                    uiState.customText,
                    uiState.timeLimit.toFloatOrNull(),
                    uiState.minAccuracy.toFloatOrNull()
                )
            },
            enabled = uiState.customText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp)
        ) {
            Text(
                text = "Start Custom Test",
                fontSize = 18.sp
            )
        }
    }
}