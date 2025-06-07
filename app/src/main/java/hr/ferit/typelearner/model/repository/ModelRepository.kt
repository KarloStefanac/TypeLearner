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
            users.add(user)
            statistics.add(StatsData(userId = user.id))
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
                    users.add(user)
                    if (statistics.none { it.userId == user.id }) {
                        statistics.add(StatsData(userId = user.id))
                    }
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data invalid"))
                }
            } else {
                Result.failure(Exception("Invalid username or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<UserData>
    }

    fun getUser(userId: String): UserData? = users.find { it.id == userId }

    fun getStatistics(userId: String): StatsData? = statistics.find { it.userId == userId }

    fun updateStatistics(userId: String, wpm: Float, accuracy: Float) {
        val stats = statistics.find { it.userId == userId } ?: return
        val newTestsFinished = stats.testsFinished + 1
        val newWpm = (stats.wpm * stats.testsFinished + wpm) / newTestsFinished
        val newAccuracy = (stats.accuracy * stats.testsFinished + accuracy) / newTestsFinished
        val newTopWpm = if (stats.topWpm > wpm) stats.topWpm else wpm
        statistics.remove(stats)
        statistics.add(
            stats.copy(
                wpm = newWpm.toFloat(),
                accuracy = newAccuracy.toFloat(),
                topWpm = newTopWpm.toFloat(),
                testsFinished = newTestsFinished
            )
        )
    }

    fun addTest(test: TestData) {
        tests.add(test)
        db.collection("tests").document(test.id).set(test)
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
                        time = document.getLong("time") ?: 0L
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