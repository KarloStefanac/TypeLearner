package hr.ferit.typelearner.model

data class StatsData (
    val id : String,
    val userId : String,
    val wpm: Float = 0.0f,
    val accuracy : Float = 0.0f,
    val topWpm : Float = 0.0f,
    val testFinished : Int = 0
)