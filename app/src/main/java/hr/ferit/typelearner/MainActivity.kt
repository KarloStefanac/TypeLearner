package hr.ferit.typelearner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.typelearner.model.UserData
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.view.CustomTestView
import hr.ferit.typelearner.view.HomeScreenView
import hr.ferit.typelearner.view.LoginScreenView
import hr.ferit.typelearner.view.RegisterScreenView
import hr.ferit.typelearner.view.ResultsScreenView
import hr.ferit.typelearner.view.TestsScreenView
import hr.ferit.typelearner.view.TypingScreenView
import hr.ferit.typelearner.view.factory.LoginViewModelFactory
import hr.ferit.typelearner.view.factory.RegisterViewModelFactory
import hr.ferit.typelearner.view.factory.ResultsViewModelFactory
import hr.ferit.typelearner.view.factory.TimedTestViewModelFactory
import hr.ferit.typelearner.view.factory.TypingViewModelFactory
import hr.ferit.typelearner.viewmodel.CustomTestViewModel
import hr.ferit.typelearner.viewmodel.HomeViewModel
import hr.ferit.typelearner.viewmodel.LoginViewModel
import hr.ferit.typelearner.viewmodel.RegisterViewModel
import hr.ferit.typelearner.viewmodel.ResultsViewModel
import hr.ferit.typelearner.viewmodel.TestsViewModel
import hr.ferit.typelearner.viewmodel.TypingViewModel

class MainActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = ModelRepository()
    private val loginViewModel: LoginViewModel by viewModels { LoginViewModelFactory(repository) }
    private val registerViewModel: RegisterViewModel by viewModels { RegisterViewModelFactory(repository) }
    private val homeViewModel: HomeViewModel by viewModels()
    private val typingViewModel: TypingViewModel by viewModels { TypingViewModelFactory(repository, applicationContext) }
    private val resultsViewModel: ResultsViewModel by viewModels { ResultsViewModelFactory(repository) }
    private val customTestViewModel: CustomTestViewModel by viewModels()
    private val testsViewModel: TestsViewModel by viewModels { TimedTestViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TypingTestApp(
                loginViewModel = loginViewModel,
                registerViewModel = registerViewModel,
                homeViewModel = homeViewModel,
                typingViewModel = typingViewModel,
                resultsViewModel = resultsViewModel,
                customTestViewModel = customTestViewModel,
                testsViewModel = testsViewModel
            )
        }
    }
}

@Composable
fun TypingTestApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel,
    typingViewModel: TypingViewModel,
    resultsViewModel: ResultsViewModel,
    customTestViewModel: CustomTestViewModel,
    testsViewModel: TestsViewModel
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
                            navController.navigate("quickTest")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onCustomTest = {
                        if (user != null) {
                            customTestViewModel.resetState()
                            navController.navigate("custom")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onLogin = { navController.navigate("login") },
                    onTestsScreen = {
                        if (user != null) {
                            navController.navigate("tests")
                        }
                        else {
                            navController.navigate("login")
                        }
                    }
                )
            }
            composable("quickTest") {
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
                user?.let {
                    Log.d("TypingVM", "Custom test")
                    CustomTestView(
                        viewModel = customTestViewModel,
                        onStartCustomTest = { text, timeLimit, minAccuracy ->
                            navController.navigate("typing?customText=$text&timeLimit=$timeLimit&minAccuracy=$minAccuracy")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("typing?customText={customText}&timeLimit={timeLimit}&minAccuracy={minAccuracy}") { backStackEntry ->
                user?.let { currentUser ->
                    Log.d("TypingVM", "Typing with parameters")
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
                user?.let { currentUser ->
                    val wpm = backStackEntry.arguments?.getString("wpm")?.toFloatOrNull() ?: 0.0f
                    val accuracy = backStackEntry.arguments?.getString("accuracy")?.toFloatOrNull() ?: 0.0f
                    val time = backStackEntry.arguments?.getString("time")?.toFloatOrNull() ?: 0.0f
                    ResultsScreenView(
                        viewModel = resultsViewModel,
                        wpm = wpm,
                        accuracy = accuracy,
                        time = time,
                        userId = currentUser.id,
                        typingViewModel = typingViewModel,
                        onRestart = {
                            typingViewModel.resetTest()
                            navController.popBackStack("home", inclusive = false) }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("tests") {
                user?.let { currentUser ->
                    TestsScreenView(
                        viewModel = testsViewModel,
                        onBack = {navController.popBackStack()},
                        onTestSelected = { test ->
                            val timeLimit = if (test.time > 0) (test.time) else null
                            val minAccuracy = if (test.minAccuracy > 0) test.minAccuracy else null
                            navController.navigate("typing?customText=${test.text}&timeLimit=$timeLimit&minAccuracy=$minAccuracy")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
        }
    }
}

enum class WordStatus {
    NOT_TYPED,
    CORRECT,
    INCORRECT
}