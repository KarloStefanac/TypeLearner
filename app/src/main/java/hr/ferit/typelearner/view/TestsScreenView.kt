package hr.ferit.typelearner.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.ferit.typelearner.R
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.viewmodel.TestsViewModel

@Composable
fun TestsScreenView(
    viewModel: TestsViewModel,
    onBack: () -> Unit,
    onTestSelected: (TestData) -> Unit
    ){
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5E0F27))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo4x),
            contentDescription = "Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(top = 15.dp)
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = "Available tests",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
            color = Color.White
        )
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else if (uiState.tests.isEmpty()) {
            Text(
                text = "No tests available",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.tests.size) { index ->
                    GridCard(
                        test = uiState.tests[index],
                        onClick = {onTestSelected(uiState.tests[index])})
                }
            }
        }
    }
}

@Composable
fun GridCard(test: TestData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { (onClick()) },
        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column {
                Text(
                    text = test.text.trim()
                        .split("\\s+".toRegex()).count { it.isNotEmpty() }.toString() + " words",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.padding(10.dp))
                Text(
                    text = "Min Accuracy: ${test.minAccuracy}%",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.padding(10.dp))
                Text(
                    text = "Time: ${test.time} seconds",
                    fontSize = 14.sp
                )
            }
        }
    }
}