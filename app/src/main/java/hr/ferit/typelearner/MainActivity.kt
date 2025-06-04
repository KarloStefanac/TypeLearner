package hr.ferit.typelearner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.typelearner.model.UserData
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.view.HomeScreenView
import hr.ferit.typelearner.view.LoginScreenView
import hr.ferit.typelearner.view.RegisterScreenView
import hr.ferit.typelearner.view.TypingScreenView
import hr.ferit.typelearner.view.factory.LoginViewModelFactory
import hr.ferit.typelearner.view.factory.RegisterViewModelFactory
import hr.ferit.typelearner.view.factory.TypingViewModelFactory
import hr.ferit.typelearner.viewmodel.HomeViewModel
import hr.ferit.typelearner.viewmodel.LoginViewModel
import hr.ferit.typelearner.viewmodel.RegisterViewModel
import hr.ferit.typelearner.viewmodel.TypingViewModel
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = ModelRepository()
    private val loginViewModel: LoginViewModel by viewModels { LoginViewModelFactory(repository) }
    private val registerViewModel: RegisterViewModel by viewModels { RegisterViewModelFactory(repository) }
    private val homeViewModel: HomeViewModel by viewModels()
    private val typingViewModel: TypingViewModel by viewModels { TypingViewModelFactory(repository, applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TypingTestApp(
                loginViewModel = loginViewModel,
                registerViewModel = registerViewModel,
                homeViewModel = homeViewModel,
                typingViewModel = typingViewModel
            )
        }
    }
}

@Composable
fun TypingTestApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel,
    typingViewModel: TypingViewModel
)  {
    val navController = rememberNavController()
    var user by remember { mutableStateOf<UserData?>(null) }
    MaterialTheme {
        NavHost(navController = navController, startDestination = "home") {
            composable("login") {
                LoginScreenView(
                    viewModel = loginViewModel,
                    onLoginSuccess = { loggedInUser ->
                        user = loggedInUser
                        homeViewModel.setUser(loggedInUser)
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreenView(
                    viewModel = registerViewModel,
                    onRegisterSuccess = { registeredUser ->
                        user = registeredUser
                        homeViewModel.setUser(registeredUser)
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreenView(
                    viewModel = homeViewModel,
                    onStartTyping = {
                        if (user != null) {
                            navController.navigate("typing")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onCustomTest = {
                        if (user != null) {
                            navController.navigate("custom")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onLogin = { navController.navigate("login") }
                )
            }
            composable("typing") {
                user?.let { currentUser ->
                    TypingScreenView(
                        viewModel = typingViewModel,
                        userId = currentUser.id,
                        isCustom = false,
                        customText = null,
                        timeLimit = null,
                        minAccuracy = null,
                        onComplete = { wpm, accuracy, time ->
                            navController.navigate("results/$wpm/$accuracy/$time")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("custom") {
                CustomTestScreen { text, timeLimit, minAccuracy ->
                    navController.navigate("typing?customText=$text&timeLimit=$timeLimit&minAccuracy=$minAccuracy")
                }
            }
            composable("typing?customText={customText}&timeLimit={timeLimit}&minAccuracy={minAccuracy}") { backStackEntry ->
                user?.let { currentUser ->
                    val customText = backStackEntry.arguments?.getString("customText") ?: ""
                    val timeLimit = backStackEntry.arguments?.getString("timeLimit")?.toFloatOrNull()
                    val minAccuracy = backStackEntry.arguments?.getString("minAccuracy")?.toFloatOrNull()
                    TypingScreenView(
                        viewModel = typingViewModel,
                        userId = currentUser.id,
                        isCustom = true,
                        customText = customText,
                        timeLimit = timeLimit,
                        minAccuracy = minAccuracy,
                        onComplete = { wpm, accuracy, time ->
                            navController.navigate("results/$wpm/$accuracy/$time")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("results/{wpm}/{accuracy}/{time}") { backStackEntry ->
                val wpm = backStackEntry.arguments?.getString("wpm")?.toFloatOrNull() ?: 0f
                val accuracy =
                    backStackEntry.arguments?.getString("accuracy")?.toFloatOrNull() ?: 0f
                val time = backStackEntry.arguments?.getString("time")?.toFloatOrNull() ?: 0f
                ResultsScreen(wpm, accuracy, time) {
                    navController.popBackStack("home", inclusive = false)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onStartTyping: () -> Unit, onCustomTest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5E0F27)),
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
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
            onClick = onStartTyping,
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
            onClick = onStartTyping,
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
            onClick = onStartTyping,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = "My tests",
                fontSize = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypingScreen(
    isCustom: Boolean,
    customText: String?,
    timeLimit: Float?,
    minAccuracy: Float?,
    onComplete: (Float, Float, Float) -> Unit
) {
    val repository = ModelRepository()
    var randomWords = repository.loadWordsFromAssets(LocalContext.current)
    val words by remember {
        mutableStateOf(
            if (isCustom && customText != null && customText.isNotBlank()) {
                customText.trim().split("\\s+".toRegex())
            } else {
                randomWords.toList().shuffled(Random).take(15)
            }
        )
    }
    var typedText by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(0L) }
    var wordStatuses by remember { mutableStateOf(List(words.size) { WordStatus.NOT_TYPED }) }
    var isStarted by remember { mutableStateOf(false) }
    var currentWordIndex by remember { mutableStateOf(0) }
    var typedWords by remember { mutableStateOf(mutableListOf<String>()) }
    var timeLeft by remember { mutableStateOf(timeLimit ?: Float.MAX_VALUE) }

    LaunchedEffect(isStarted, timeLeft) {
        if (isStarted && timeLimit != null && timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft -= 1f
            if (timeLeft <= 0) {
                val timeSeconds = (System.currentTimeMillis() - startTime) / 1000f
                val correctWords = wordStatuses.count { it == WordStatus.CORRECT }
            val wpm = if (timeSeconds > 0) (correctWords / timeSeconds * 60).roundToInt().toFloat() else 0f
                val accuracy = (correctWords.toFloat() / words.size * 100).roundToInt().toFloat()
                onComplete(wpm, accuracy, timeSeconds)
            }
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

        if (timeLimit != null) {
            Text(
                text = "Time Left: ${String.format("%.0f", timeLeft)} seconds",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp),
                color = Color.White
            )
        }

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
                        else -> Color.White
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
                            it[currentWordIndex] =
                                if (isCorrect) WordStatus.CORRECT else WordStatus.INCORRECT
                        }
                    }

                    // Clear input and move to next word
                    typedText = ""
                    currentWordIndex++

                    // Check if test is complete
                    if (currentWordIndex >= words.size) {
                        val timeSeconds = (System.currentTimeMillis() - startTime) / 1000f
                        val correctWords = wordStatuses.count { it == WordStatus.CORRECT }
                        val wpm =
                            if (timeSeconds > 0) (correctWords / timeSeconds * 60).roundToInt()
                                .toFloat() else 0f
                        val accuracy =
                            (correctWords.toFloat() / words.size * 100).roundToInt().toFloat()
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
            textStyle = TextStyle(fontSize = 18.sp),
            enabled = timeLeft > 0
        )
    }
}

@Composable
fun ResultsScreen(wpm: Float, accuracy: Float, time: Float, onRestart: () -> Unit) {
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
            text = "Words per Minute: ${wpm.toInt()}",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.White
        )

        Text(
            text = "Accuracy: ${accuracy.toInt()}%",
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

@Composable
fun CustomTestScreen(onStartCustomTest: (String, Float?, Float?) -> Unit) {
    var customText by remember { mutableStateOf("") }
    var timeLimit by remember { mutableStateOf("") }
    var minAccuracy by remember { mutableStateOf("") }

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
            value = customText,
            onValueChange = { customText = it },
            label = { Text("Enter text to type") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 16.dp),
            textStyle = TextStyle(fontSize = 16.sp),
            maxLines = 5
        )

        // Time limit input
        OutlinedTextField(
            value = timeLimit,
            onValueChange = { timeLimit = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Time limit (seconds, optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 16.sp)
        )

        // Minimum accuracy input
        OutlinedTextField(
            value = minAccuracy,
            onValueChange = { minAccuracy = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Minimum accuracy (%, optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 16.sp)
        )
        Button(
            onClick = {
                onStartCustomTest(
                    customText,
                    timeLimit.toFloatOrNull(),
                    minAccuracy.toFloatOrNull()
                )
            },
            enabled = customText.isNotBlank(),
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

enum class WordStatus {
    NOT_TYPED,
    CORRECT,
    INCORRECT
}