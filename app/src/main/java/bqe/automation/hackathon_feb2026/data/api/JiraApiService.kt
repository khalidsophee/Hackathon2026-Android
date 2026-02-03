package bqe.automation.hackathon_feb2026.data.api

import bqe.automation.hackathon_feb2026.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface JiraApiService {
    
    @GET("rest/api/3/issue/{issueKey}")
    suspend fun getIssue(
        @Path("issueKey") issueKey: String,
        @Query("fields") fields: String = "summary,description,issuetype,project,customfield_10026"
    ): Response<JiraIssue>
    
    @POST("rest/api/3/issue")
    suspend fun createTestCase(
        @Body request: CreateTestCaseRequest
    ): Response<CreatedTestCase>
    
    @GET("rest/api/3/issue/{issueKey}")
    suspend fun getTestCase(
        @Path("issueKey") testCaseKey: String
    ): Response<JiraIssue>
}
