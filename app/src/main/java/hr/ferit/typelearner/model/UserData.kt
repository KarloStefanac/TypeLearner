package hr.ferit.typelearner.model

data class UserData (
    val id : String,
    val username: String,
    val password: String,
    val email: String,
    val timestamp: com.google.firebase.Timestamp? = null
)