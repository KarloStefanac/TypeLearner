package hr.ferit.typelearner.model

import java.util.UUID

data class TestData(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val minAccuracy: Any,
    val time: Long
)