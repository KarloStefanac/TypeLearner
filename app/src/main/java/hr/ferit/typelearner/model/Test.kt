package hr.ferit.typelearner.model

data class Test(
    val id : String,
    val text : String,
    val minAccuracy : Float,
    val time : Long
)