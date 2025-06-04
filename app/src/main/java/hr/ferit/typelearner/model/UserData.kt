package hr.ferit.typelearner.model

import java.util.UUID

data class UserData (
    val id : String = UUID.randomUUID().toString(),
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)