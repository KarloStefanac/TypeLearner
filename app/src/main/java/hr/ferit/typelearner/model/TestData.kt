package hr.ferit.typelearner.model

import com.google.firebase.firestore.FieldValue
import java.util.UUID

data class TestData(
    val id: String = UUID.randomUUID().toString(),
    val userId : String,
    val text: String,
    val minAccuracy: Float,
    val time: Long,
    val timestamp: com.google.firebase.Timestamp? = null
)