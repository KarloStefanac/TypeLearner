package hr.ferit.typelearner.model

import java.util.UUID

data class TestResultData (
    val id : String = UUID.randomUUID().toString(),
    val userId : String,
    val testId : String,
    val wpm : Float,
    val accuracy : Float,
    val passed : Boolean,
    val completedAt : com.google.firebase.Timestamp? = null
)