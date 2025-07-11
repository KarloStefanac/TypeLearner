package hr.ferit.typelearner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.model.UserData
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.view.CustomTestView
import hr.ferit.typelearner.view.HomeScreenView
import hr.ferit.typelearner.view.LoginScreenView
import hr.ferit.typelearner.view.ProfileScreenView
import hr.ferit.typelearner.view.RegisterScreenView
import hr.ferit.typelearner.view.ResultsHistoryScreenView
import hr.ferit.typelearner.view.ResultsScreenView
import hr.ferit.typelearner.view.TestsScreenView
import hr.ferit.typelearner.view.TypingScreenView
import hr.ferit.typelearner.view.factory.LoginViewModelFactory
import hr.ferit.typelearner.view.factory.ProfileViewModelFactory
import hr.ferit.typelearner.view.factory.RegisterViewModelFactory
import hr.ferit.typelearner.view.factory.ResultsViewModelFactory
import hr.ferit.typelearner.view.factory.TestResultsViewModelFactory
import hr.ferit.typelearner.view.factory.TimedTestViewModelFactory
import hr.ferit.typelearner.view.factory.TypingViewModelFactory
import hr.ferit.typelearner.viewmodel.CustomTestViewModel
import hr.ferit.typelearner.viewmodel.HomeViewModel
import hr.ferit.typelearner.viewmodel.LoginViewModel
import hr.ferit.typelearner.viewmodel.ProfileViewModel
import hr.ferit.typelearner.viewmodel.RegisterViewModel
import hr.ferit.typelearner.viewmodel.ResultsViewModel
import hr.ferit.typelearner.viewmodel.TestResultsViewModel
import hr.ferit.typelearner.viewmodel.TestsViewModel
import hr.ferit.typelearner.viewmodel.TypingViewModel
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = ModelRepository()
    private val loginViewModel: LoginViewModel by viewModels { LoginViewModelFactory(repository) }
    private val registerViewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(
            repository
        )
    }
    private val homeViewModel: HomeViewModel by viewModels()
    private val typingViewModel: TypingViewModel by viewModels {
        TypingViewModelFactory(
            repository,
            applicationContext
        )
    }
    private val resultsViewModel: ResultsViewModel by viewModels {
        ResultsViewModelFactory(
            repository
        )
    }
    private val customTestViewModel: CustomTestViewModel by viewModels()
    private val testsViewModel: TestsViewModel by viewModels { TimedTestViewModelFactory(repository) }
    private val testResultsViewModel: TestResultsViewModel by viewModels {
        TestResultsViewModelFactory(
            repository
        )
    }
    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            repository
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Optionally handle permission denial
            if (!isGranted) {
                // You can log this or show a message to the user
                // For simplicity, we'll proceed without notifications if denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            val channel = NotificationChannel(
                "typing_test_channel",
                "Typing Test Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for typing test achievements"
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        setContent {
            TypingTestApp(
                loginViewModel = loginViewModel,
                registerViewModel = registerViewModel,
                homeViewModel = homeViewModel,
                typingViewModel = typingViewModel,
                resultsViewModel = resultsViewModel,
                customTestViewModel = customTestViewModel,
                testsViewModel = testsViewModel,
                testResultsViewModel = testResultsViewModel,
                profileViewModel = profileViewModel
            )
        }
    }
    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d("MainActivity", "Auth state changed: user=${user?.uid}")
            if (user == null) {
                loginViewModel.resetState()
                homeViewModel.clearUser()
            }
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
    testsViewModel: TestsViewModel,
    testResultsViewModel: TestResultsViewModel,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()
    var user by remember { mutableStateOf<UserData?>(null) }
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val userDoc =
                FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid).get()
                    .await()
            val firestoreUser = userDoc.toObject(UserData::class.java)
            if (firestoreUser != null) {
                user = firestoreUser
                homeViewModel.setUser(firestoreUser)
            }
        }
    }

    MaterialTheme {
        NavHost(
            navController = navController,
            startDestination = if (auth.currentUser != null) "home" else "login"
        ) {
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
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreenView(
                    viewModel = homeViewModel,
                    onStartTyping = {
                        if (auth.currentUser != null) {
                            navController.navigate("typing")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onCustomTest = {
                        if (auth.currentUser != null) {
                            customTestViewModel.resetState()
                            navController.navigate("custom")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onLogin = {
                        auth.signOut()
                        user = null
                        homeViewModel.clearUser()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onTestsScreen = {
                        if (auth.currentUser != null) {
                            navController.navigate("tests")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onResults = {
                        if (auth.currentUser != null) {
                            navController.navigate("resultsHistory")
                        } else {
                            navController.navigate("login")
                        }
                    },
                    onProfile = {
                        if (auth.currentUser != null) {
                            navController.navigate("profile")
                        } else {
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
                        testId = null,
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
                    CustomTestView(
                        viewModel = customTestViewModel,
                        onStartCustomTest = { text, timeLimit, minAccuracy ->
                            Log.d("TypingVM", "custom: ${text},${timeLimit},${minAccuracy}")
                            navController.navigate("typing?customText=$text&timeLimit=$timeLimit&minAccuracy=$minAccuracy")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("typing?customText={customText}&timeLimit={timeLimit}&minAccuracy={minAccuracy}") { backStackEntry ->
                user?.let { currentUser ->
                    val customText = backStackEntry.arguments?.getString("customText") ?: ""
                    val timeLimit =
                        backStackEntry.arguments?.getString("timeLimit")?.toFloatOrNull()
                    val minAccuracy =
                        backStackEntry.arguments?.getString("minAccuracy")?.toFloatOrNull()
                    Log.d(
                        "TypingVM",
                        "typing?customText= ${customText},${timeLimit},${minAccuracy}"
                    )

                    TypingScreenView(
                        viewModel = typingViewModel,
                        userId = currentUser.id,
                        isCustom = true,
                        customText = customText,
                        timeLimit = timeLimit,
                        minAccuracy = minAccuracy,
                        testId = null,
                        onComplete = { wpm, accuracy, time ->
                            navController.navigate("results/$wpm/$accuracy/$time")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("typing/timed/{testId}") { backStackEntry ->
                user?.let { currentUser ->
                    val testId = backStackEntry.arguments?.getString("testId") ?: ""
                    var test by remember { mutableStateOf<TestData?>(null) }
                    LaunchedEffect(testId) {
                        val result = typingViewModel.loadTestById(testId)
                        if (result.isSuccess) {
                            test = result.getOrNull()
                        }
                    }
                    if (test != null) {
                        TypingScreenView(
                            viewModel = typingViewModel,
                            userId = currentUser.id,
                            isCustom = false,
                            customText = null,
                            timeLimit = null,
                            minAccuracy = null,
                            testId = testId,
                            test = test,
                            onComplete = { wpm, accuracy, duration ->
                                navController.navigate("results/$wpm/$accuracy/$duration")
                            }
                        )
                    } else {
                        Text("Loading test or test not found")
                    }
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("results/{wpm}/{accuracy}/{time}") { backStackEntry ->
                user?.let { currentUser ->
                    val wpm = backStackEntry.arguments?.getString("wpm")?.toFloatOrNull() ?: 0.0f
                    val accuracy =
                        backStackEntry.arguments?.getString("accuracy")?.toFloatOrNull() ?: 0.0f
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
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("tests") {
                user?.let { currentUser ->
                    TestsScreenView(
                        viewModel = testsViewModel,
                        onBack = { navController.popBackStack() },
                        onTestSelected = { test ->
                            navController.navigate("typing/timed/${test.id}")
                        }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("resultsHistory") {
                user?.let { currentUser ->
                    ResultsHistoryScreenView(
                        viewModel = testResultsViewModel,
                        userId = currentUser.id,
                        onBack = { navController.popBackStack() }
                    )
                } ?: run {
                    navController.navigate("login")
                }
            }
            composable("profile") {
                user?.let { currentUser ->
                    ProfileScreenView(
                        viewModel = profileViewModel,
                        userId = currentUser.id,
                        onBack = { navController.popBackStack() },
                        onDeleteAccount = {
                            Log.d("TypingTestApp", "Initiating account deletion")
                            profileViewModel.deleteAccount(
                                onSuccess = {
                                    Log.d("TypingTestApp", "Account deletion successful, waiting for sign-out")
                                    // Navigation handled by auth state listener in MainActivity
                                },
                                onError = { errorMessage ->
                                    Log.e("TypingTestApp", "Account deletion failed: $errorMessage")
                                }
                            )
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