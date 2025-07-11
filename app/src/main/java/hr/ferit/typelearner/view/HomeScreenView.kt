package hr.ferit.typelearner.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.ferit.typelearner.viewmodel.HomeViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import hr.ferit.typelearner.R


@Composable
fun HomeScreenView(
    viewModel: HomeViewModel,
    onStartTyping: () -> Unit,
    onCustomTest: () -> Unit,
    onLogin: () -> Unit,
    onTestsScreen: () -> Unit,
    onResults: () -> Unit,
    onProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5E0F27)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo4x),
            contentDescription = "Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(top = 15.dp)
        )
        Button(
            onClick = onStartTyping,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = "Quick test",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.padding(10.dp))
        Button(
            onClick = onTestsScreen,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = "Timed test",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.padding(10.dp))
        Button(
            onClick = onCustomTest,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = "Custom test",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.padding(30.dp))

        Button(
            onClick = onResults,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = "Results",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.padding(10.dp))

        Button(
            onClick = onProfile,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = "Profile",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.padding(10.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = if (uiState.user == null) "Login" else "Switch User",
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = if (uiState.user == null) "" else uiState.user!!.username,
            color = Color.White,
            fontSize = 18.sp
        )
    }
}