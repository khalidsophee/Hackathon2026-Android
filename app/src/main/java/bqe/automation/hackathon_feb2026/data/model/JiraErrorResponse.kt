package bqe.automation.hackathon_feb2026.data.model

import com.google.gson.annotations.SerializedName

data class JiraErrorResponse(
    @SerializedName("errorMessages")
    val errorMessages: List<String>?,
    val errors: Map<String, String>?
)
