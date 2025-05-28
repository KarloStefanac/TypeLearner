package hr.ferit.typelearner.model

data class TestResult (
    val id : String,
    val userId : String,
    val testId : String,
    val wpm : Float,
    val accuracy : Float,
    val passed : Boolean,
    val completedAt : com.google.firebase.Timestamp? = null
)