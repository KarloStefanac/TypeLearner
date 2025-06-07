package hr.ferit.typelearner.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import hr.ferit.typelearner.WordStatus
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.model.TestResultData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt
import kotlin.random.Random

class TypingViewModel(
    private val repository: ModelRepository,
    private val context : Context) : ViewModel() {
    private val _uiState = MutableStateFlow(TypingUiState())
    val uiState: StateFlow<TypingUiState> = _uiState.asStateFlow()

    private val wordBank: List<String> by lazy {
        repository.loadWordsFromAssets(context)
    }

    fun initializeTest(isCustom: Boolean, customText: String?, timeLimit: Float?, minAccuracy: Float?, userId: String) {
        val words = if (isCustom && customText != null && customText.isNotBlank()) {
            customText.trim().split("\\s+".toRegex())
        } else {
            wordBank.shuffled(Random).take(20)
        }

        val test = TestData(
            text = words.joinToString(" "),
            minAccuracy = minAccuracy?.toFloat() ?: 0.0,
            time = (timeLimit?.toLong() ?: 0L),
            userId = userId
        )
        if (isCustom) {
            Log.d("TypingVM", "${isCustom}")
            repository.addTest(test)
        }

        _uiState.value = TypingUiState(
            words = words,
            wordStatuses = List(words.size) { WordStatus.NOT_TYPED },
            testId = test.id,
            userId = userId,
            timeLimit = timeLimit,
            minAccuracy = minAccuracy,
            timeLeft = timeLimit ?: Float.MAX_VALUE
        )
    }

    fun updateTypedText(newText: String) {
        val currentState = _uiState.value
        if (!currentState.isStarted && newText.isNotEmpty()) {
            _uiState.value = currentState.copy(
                isStarted = true,
                startTime = System.currentTimeMillis(),
                typedText = newText
            )
            Log.d("TypingVM", "Test started, startTime=${_uiState.value.startTime}")
//            return
        }

        else if (newText.endsWith(" ") && newText.trim().isNotEmpty()) {
            val typedWord = newText.trim()
            val newTypedWords = currentState.typedWords + typedWord
            val currentWordIndex = currentState.currentWordIndex
            val isCorrect = typedWord == currentState.words.getOrNull(currentWordIndex)
            val newStatuses = currentState.wordStatuses.toMutableList().apply {
                if (currentWordIndex < size) {
                    set(currentWordIndex, if (isCorrect) WordStatus.CORRECT else WordStatus.INCORRECT)
                }
            }

            val newIndex = currentWordIndex + 1
            _uiState.value = currentState.copy(
                typedText = "",
                typedWords = newTypedWords,
                wordStatuses = newStatuses,
                currentWordIndex = newIndex
            )

            if (newIndex >= currentState.words.size) {
                completeTest()
            }
        } else {
            _uiState.value = currentState.copy(typedText = newText)
        }
    }

    fun updateTimeLeft() {
        val currentState = _uiState.value
        if (currentState.isStarted && currentState.timeLimit != null && currentState.timeLeft > 0) {
            val newTimeLeft = currentState.timeLeft - 1f
            _uiState.value = currentState.copy(timeLeft = newTimeLeft)
            if (newTimeLeft <= 0) {
                completeTest()
            }
        }
    }

    private fun completeTest() {
        val currentState = _uiState.value
        val endTime = System.currentTimeMillis()
        val timeSeconds = if (currentState.isStarted) {
            ((endTime - currentState.startTime) / 1000f).coerceAtLeast(0.1f) // Prevent division by zero
        } else {
            0.0f
        }
        val correctWords = currentState.wordStatuses.count { it == WordStatus.CORRECT }
        val wpm = if (timeSeconds > 0) (correctWords / timeSeconds * 60).roundToInt().toFloat() else 0.0f
        val accuracy = (correctWords.toFloat() / currentState.words.size * 100).roundToInt().toFloat()
        val passed = currentState.minAccuracy == null || accuracy >= currentState.minAccuracy

        val result = TestResultData(
            userId = currentState.userId,
            testId = currentState.testId,
            wpm = wpm,
            accuracy = accuracy,
            passed = passed
        )
        repository.addTestResult(result)
        repository.updateStatistics(currentState.userId, wpm, accuracy)

        _uiState.value = currentState.copy(
            wpm = wpm,
            accuracy = accuracy,
            isCompleted = true,
            elapsedTime = timeSeconds
        )
    }

    fun resetTest(){
        _uiState.value = TypingUiState()
    }

    data class TypingUiState(
        val words: List<String> = emptyList(),
        val typedText: String = "",
        val typedWords: List<String> = emptyList(),
        val wordStatuses: List<WordStatus> = emptyList(),
        val currentWordIndex: Int = 0,
        val testId: String = "",
        val userId: String = "",
        val isStarted: Boolean = false,
        val startTime: Long = 0L,
        val timeLimit: Float? = null,
        val timeLeft: Float = Float.MAX_VALUE,
        val minAccuracy: Float? = null,
        val wpm: Float = 0.0f,
        val accuracy: Float = 0.0f,
        val elapsedTime: Float = 0.0f,
        val isCompleted: Boolean = false
    )
}