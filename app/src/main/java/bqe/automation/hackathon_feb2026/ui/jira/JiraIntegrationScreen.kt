package bqe.automation.hackathon_feb2026.ui.jira

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import bqe.automation.hackathon_feb2026.di.AppModule

@Composable
fun JiraIntegrationScreen(
    modifier: Modifier = Modifier,
    viewModel: JiraViewModel = remember { AppModule.createJiraViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfigDialog by remember { mutableStateOf(!uiState.jiraConfigured) }
    var issueKeyInput by remember { mutableStateOf("") }
    var showTestCodeDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Jira Test Case Generator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Configuration Section
        if (!uiState.jiraConfigured) {
            Button(
                onClick = { showConfigDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configure Jira Connection")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Issue Key Input
        OutlinedTextField(
            value = issueKeyInput,
            onValueChange = { issueKeyInput = it },
            label = { Text("Jira Issue Key (e.g., AND-7709)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.jiraConfigured && !uiState.isLoading,
            placeholder = { Text("AND-7709") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { viewModel.fetchStory(issueKeyInput) },
            enabled = issueKeyInput.isNotBlank() && uiState.jiraConfigured && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Story")
        }
        
        // Loading Indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        }
        
        // Error Message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Story Details
        uiState.story?.let { story ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Story: ${story.key}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = story.fields.summary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    story.fields.getDescriptionText()?.let {
                        Text(
                            text = "Description: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    story.fields.getAcceptanceCriteriaText()?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Acceptance Criteria: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Generated Test Cases
        if (uiState.generatedTestCases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Generated Test Cases (${uiState.generatedTestCases.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            uiState.generatedTestCases.forEachIndexed { index, testCase ->
                TestCaseCard(
                    testCase = testCase,
                    index = index + 1,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.createTestCasesInJira() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Create in Jira")
                }
                
                Button(
                    onClick = { showTestCodeDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Generate Android Tests")
                }
            }
            
            if (uiState.createdTestCases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Created ${uiState.createdTestCases.size} test case(s) in Jira",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    // Configuration Dialog
    if (showConfigDialog) {
        val isConfigured by viewModel.uiState.collectAsState()
        JiraConfigDialog(
            onDismiss = { showConfigDialog = false },
            onSave = { baseUrl, email, apiToken ->
                viewModel.configureJira(baseUrl, email, apiToken)
                // Close dialog after a short delay to allow state to update
                // The error will be shown in the main UI if configuration fails
                showConfigDialog = false
            }
        )
    }
    
    // Test Code Dialog
    if (showTestCodeDialog) {
        TestCodeDialog(
            code = viewModel.generateAndroidTestFiles(),
            onDismiss = { showTestCodeDialog = false }
        )
    }
}

@Composable
fun TestCaseCard(
    testCase: bqe.automation.hackathon_feb2026.data.model.GeneratedTestCase,
    index: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$index. ${testCase.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = testCase.description,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Priority: ${testCase.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun JiraConfigDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var baseUrl by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var apiToken by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Jira") },
        text = {
            Column {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Jira Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://bqetrack.atlassian.net") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("API Token") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                Text(
                    text = "Get your API token from: https://id.atlassian.com/manage-profile/security/api-tokens",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val trimmedBaseUrl = baseUrl.trim()
                    val trimmedEmail = email.trim()
                    val trimmedToken = apiToken.trim()
                    
                    if (trimmedBaseUrl.isNotEmpty() && trimmedEmail.isNotEmpty() && trimmedToken.isNotEmpty()) {
                        onSave(trimmedBaseUrl, trimmedEmail, trimmedToken)
                    }
                },
                enabled = baseUrl.trim().isNotEmpty() && email.trim().isNotEmpty() && apiToken.trim().isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TestCodeDialog(
    code: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generated Android Test Code") },
        text = {
            Column {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
