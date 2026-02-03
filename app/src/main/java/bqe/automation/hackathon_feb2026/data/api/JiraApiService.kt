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
    
    // Link test cases to parent issue (for Xray "Tests" relationship)
    @POST("rest/api/3/issueLink")
    suspend fun linkIssue(
        @Body request: IssueLinkRequest
    ): Response<Unit>
    
    // Update issue fields (e.g., resolution, test steps)
    @PUT("rest/api/3/issue/{issueKey}")
    suspend fun updateIssue(
        @Path("issueKey") issueKey: String,
        @Body request: UpdateIssueRequest
    ): Response<Unit>
    
    // Get all projects
    @GET("rest/api/3/project")
    suspend fun getAllProjects(): Response<List<JiraProjectInfo>>
    
    // Search for issues using JQL (migrated to new API endpoint)
    @GET("rest/api/3/search/jql")
    suspend fun searchIssues(
        @Query("jql") jql: String,
        @Query("fields") fields: String = "summary,key,issuetype,project,status",
        @Query("maxResults") maxResults: Int = 20
    ): Response<JiraSearchResponse>
}
