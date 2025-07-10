package hr.ferit.typelearner.model.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.typelearner.model.StatsData
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.model.TestResultData
import hr.ferit.typelearner.model.UserData
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import kotlin.jvm.java

class ModelRepository{
    private val users = mutableListOf<UserData>()
    private val statistics = mutableListOf<StatsData>()
    private val tests = mutableListOf<TestData>()
    private val testResults = mutableListOf<TestResultData>()
    private val db = FirebaseFirestore.getInstance()

    suspend fun addUser(user: UserData, password: String): Result<Unit> {
        return try {
            // Check if username or email already exists
            val usernameExists = db.collection("users")
                .whereEqualTo("username", user.username)
                .get()
                .await()
                .documents.isNotEmpty()
            val emailExists = db.collection("users")
                .whereEqualTo("email", user.email)
                .get()
                .await()
                .documents.isNotEmpty()

            if (usernameExists) return Result.failure(Exception("Username already taken"))
            if (emailExists) return Result.failure(Exception("Email already registered"))

            val userDoc = mapOf(
                "id" to user.id,
                "username" to user.username,
                "email" to user.email,
                "password" to password, // Note: In production, hash passwords
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.id).set(userDoc).await()
//            users.add(user)
            val statsDoc = mapOf(
                "userId" to user.id,
                "wpm" to 0.0f,
                "accuracy" to 0.0f,
                "topWpm" to 0.0f,
                "testsFinished" to 0
            )
//            statistics.add(StatsData(userId = user.id))
            db.collection("statistics").document(user.id).set(statsDoc).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticateUser(username: String, password: String): Result<UserData> {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password) // Note: In production, compare hashed passwords
                .get()
                .await()
            val userDoc = querySnapshot.documents.firstOrNull()
            if (userDoc != null) {
                val user = userDoc.toObject(UserData::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data invalid"))
                }
            } else {
                Result.failure(Exception("Invalid username or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUser(userId: String): UserData? = users.find { it.id == userId }

    suspend fun getStatistics(userId: String): Result<StatsData>{
        return try {
            val document = db.collection("statistics").document(userId).get().await()
            if (document.exists()) {
                val stats = document.toObject(StatsData::class.java)
                if (stats != null) {
                    Result.success(stats)
                } else {
                    Result.failure(Exception("Statistics data invalid"))
                }
            } else {
                Result.failure(Exception("No statistics found for user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatistics(userId: String, wpm: Float, accuracy: Float): Result<Unit> {
        return try {

            val docRef = db.collection("statistics").document(userId)
            val document = docRef.get().await()
            if (document.exists()) {
                Log.d("TypingVM", "Update statistics: ${userId},${wpm},${accuracy}")
                val stats = document.toObject(StatsData::class.java)
                if (stats != null) {
                    val newTestsFinished = stats.testsFinished + 1
                    val newWpm = (stats.wpm * stats.testsFinished + wpm) / newTestsFinished
                    val newAccuracy = (stats.accuracy * stats.testsFinished + accuracy) / newTestsFinished
                    val newTopWpm = maxOf(stats.topWpm, wpm)

                    val updatedStats = mapOf(
                        "wpm" to newWpm,
                        "accuracy" to newAccuracy,
                        "topWpm" to newTopWpm,
                        "testsFinished" to newTestsFinished
                    )
                    docRef.update(updatedStats).await()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Statistics data invalid"))
                }
            } else {
                Result.failure(Exception("No statistics found for user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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