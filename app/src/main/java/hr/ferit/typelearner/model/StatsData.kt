package hr.ferit.typelearner.model

import java.util.UUID

data class StatsData (
    val id : String = UUID.randomUUID().toString(),
    val userId : String,
    val wpm: Float = 0.0f,
    val accuracy : Float = 0.0f,
    val topWpm : Float = 0.0f,
    val testsFinished : Int = 0
)