package hr.ferit.typelearner.model

import java.util.UUID

data class TestResultData (
    val id : String = UUID.randomUUID().toString(),
    val userId : String = "",
    val testId : String = "",
    val wpm : Float = 0.0f,
    val accuracy : Float = 0.0f,
    val passed : Boolean = false,
    val completedAt : com.google.firebase.Timestamp? = null
)