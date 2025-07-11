package hr.ferit.typelearner.model.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.typelearner.model.StatsData
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.model.TestResultData
import hr.ferit.typelearner.model.UserData
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import kotlin.jvm.java

class ModelRepository(){
    private val users = mutableListOf<UserData>()
    private val statistics = mutableListOf<StatsData>()
    private val tests = mutableListOf<TestData>()
    private val testResults = mutableListOf<TestResultData>()
    private val db = FirebaseFirestore.getInstance()

    private val auth = FirebaseAuth.getInstance()

    suspend fun addUser(userData: UserData, password: String): Result<Unit> {
        return try {
            // Check if username or email already exists
            val usernameExists = db.collection("users")
                .whereEqualTo("username", userData.username)
                .get()
                .await()
                .documents.isNotEmpty()
            val emailExists = db.collection("users")
                .whereEqualTo("email", userData.email)
                .get()
                .await()
                .documents.isNotEmpty()

            if (usernameExists) return Result.failure(Exception("Username already taken"))
            if (emailExists) return Result.failure(Exception("Email already registered"))

            // Create user with Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(userData.email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Failed to create user"))
            val user = UserData(
                id = firebaseUser.uid,
                username = userData.username,
                email = userData.email
            )

            val userDoc = mapOf(
                "id" to user.id,
                "username" to user.username,
                "email" to user.email,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.id).set(userDoc).await()
            users.add(user)

            val stats = StatsData(userId = user.id)
            db.collection("statistics").document(user.id).set(stats).await()
            statistics.add(stats)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticateUser(email: String, password: String): Result<UserData> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Invalid credentials"))

            val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
            val user = userDoc.toObject(UserData::class.java)
            if (user != null) {
                users.add(user)
                // Ensure statistics exist in Firestore
                val statsDoc = db.collection("statistics").document(user.id).get().await()
                if (!statsDoc.exists()) {
                    val stats = StatsData(userId = user.id)
                    db.collection("statistics").document(user.id).set(stats).await()
                    statistics.add(stats)
                } else {
                    val stats = statsDoc.toObject(StatsData::class.java)
                    if (stats != null && !statistics.any { it.userId == user.id }) {
                        statistics.add(stats)
                    }
                }
                Result.success(user)
            } else {
                Result.failure(Exception("User data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<UserData?> {
        return try {
            Log.d("TypingTestRepository", "Fetching user from Firestore: userId=$userId")
            val document = db.collection("users").document(userId).get().await()
            val user = document.toObject(UserData::class.java)
            if (user != null) {
                Log.d("TypingTestRepository", "User fetched successfully: ${user.username}")
                users.removeAll { it.id == userId } // Update in-memory cache
                users.add(user)
                Result.success(user)
            } else {
                Log.w("TypingTestRepository", "No user found in Firestore for userId=$userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("TypingTestRepository", "Error fetching user from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user signed in"))
            // Delete user document
            db.collection("users").document(userId).delete().await()
            // Delete user statistics
            db.collection("statistics").document(userId).delete().await()
            // Delete all test results for the user
            val testResultsSnapshot = db.collection("testResults")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            for (doc in testResultsSnapshot.documents) {
                doc.reference.delete().await()
            }
            // Delete user from Firebase Authentication
            auth.currentUser?.delete()?.await()
            // Clear local caches
            users.removeAll { it.id == userId }
            statistics.removeAll { it.userId == userId }
            testResults.removeAll { it.userId == userId }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStatistics(userId: String): StatsData?{
        return try {
            val document = db.collection("statistics").document(userId).get().await()
            val stats = document.toObject(StatsData::class.java)
            if (stats != null) {
                // Update local cache
                statistics.removeAll { it.userId == userId }
                statistics.add(stats)
            }
            stats
        } catch (e: Exception) {
            statistics.find { it.userId == userId }
        }
    }

    suspend fun updateStatistics(userId: String, wpm: Float, accuracy: Float, context: Context){
        try {
            val document = db.collection("statistics").document(userId).get().await()
            val stats = document.toObject(StatsData::class.java)
            val notificationManager = NotificationManagerCompat.from(context)
            val milestones = setOf(10, 20, 50, 100, 500, 1000)
            val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (stats != null) {
                Log.d("TypingVM", "Found statistics: ${stats.userId}: ${stats.testsFinished}")
                val newTestsFinished = stats.testsFinished + 1
                val newWpm = (stats.wpm * stats.testsFinished + wpm) / newTestsFinished
                val newAccuracy = (stats.accuracy * stats.testsFinished + accuracy) / newTestsFinished
                val newTopWpm = maxOf(stats.topWpm, wpm)
                val updatedStats = stats.copy(
                    wpm = newWpm,
                    accuracy = newAccuracy,
                    topWpm = newTopWpm,
                    testsFinished = newTestsFinished
                )
                // Update Firestore
                db.collection("statistics").document(userId).set(updatedStats).await()
                // Update local cache
                statistics.removeAll { it.userId == userId }
                statistics.add(updatedStats)

                // Send notification for new top WPM
                if (wpm > stats.topWpm) {
                    val notification = NotificationCompat.Builder(context, "typing_test_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("New Top WPM!")
                        .setContentText("Congratulations! You've achieved a new top WPM of ${wpm.toInt()}!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(1, notification)
                }

                // Send notification for test milestones
                if (newTestsFinished in milestones) {
                    val notification = NotificationCompat.Builder(context, "typing_test_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Test Milestone Reached!")
                        .setContentText("You've completed $newTestsFinished typing tests!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(2, notification)
                }
            } else {
                Log.d("TypingVM", "No statistics")

                // Create new statistics if none exist
                val newStats = StatsData(
                    userId = userId,
                    wpm = wpm,
                    accuracy = accuracy,
                    topWpm = wpm,
                    testsFinished = 1
                )
                db.collection("statistics").document(userId).set(newStats).await()
                statistics.removeAll { it.userId == userId }
                statistics.add(newStats)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addTest(test: TestData): Result<Unit> {
        return try {
            db.collection("tests").document(test.id).set(test).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addTestResult(result: TestResultData) {
        testResults.add(result)
        db.collection("testResults").document(result.id).set(result)
    }

    suspend fun getAllTests(): Result<List<TestData>>{
        return try {
            val querySnapshot = db.collection("tests")
                .get()
                .await()

            val firestoreTests = querySnapshot.documents.mapNotNull { document ->
                try {
                    TestData(
                        id = document.id,  // Use document ID as id
                        userId = document.getString("userId") ?: "",
                        text = document.getString("text") ?: "",
                        minAccuracy = (document.get("minAccuracy") as? Number)?.toFloat() ?: 0f,
                        time = (document.get("time") as? Number)?.toFloat() ?: 0f,
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val allTests = (tests + firestoreTests).distinctBy { it.id }

            Log.d("TypingVM", "Tests: ${allTests}")

            Result.success(allTests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTestById(testId: String): Result<TestData?> {
        return try {
            val localTest = tests.find { it.id == testId }
            if (localTest != null) {
                Result.success(localTest)
            } else {
                val document = db.collection("tests").document(testId).get().await()
                val test = TestData(
                    id = document.id,  // Use document ID as id
                    userId = document.getString("userId") ?: "",
                    text = document.getString("text") ?: "",
                    minAccuracy = (document.get("minAccuracy") as? Number)?.toFloat() ?: 0f,
                    time = (document.get("time") as? Number)?.toFloat() ?: 0f,
                )
                if (test != null) {
                    tests.add(test)
                }
                Result.success(test)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTestResults(userId: String): Result<List<TestResultData>> {
        return try {
            val querySnapshot = db.collection("testResults")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val results = querySnapshot.documents.mapNotNull { it.toObject(TestResultData::class.java) }
            testResults.removeAll { it.userId == userId }
            testResults.addAll(results)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadWordsFromAssets(context: Context): List<String> {
        val json: String = context.assets.open("words.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(json)
        val wordList = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            wordList.add(jsonArray.getString(i))
        }

        return wordList
    }

}