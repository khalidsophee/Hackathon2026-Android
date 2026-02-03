package bqe.automation.hackathon_feb2026.ui.jira

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import bqe.automation.hackathon_feb2026.di.AppModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JiraIntegrationScreen(
    modifier: Modifier = Modifier,
    viewModel: JiraViewModel = remember { AppModule.createJiraViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfigDialog by remember { mutableStateOf(false) }
    var issueKeyInput by remember { mutableStateOf("") }
    var showTestCodeDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jira Test Case Generator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { showConfigDialog = true }
            ) {
                Text("Use Other User")
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
        
        // AI Status Indicator - Groq is always available (free service)
        val isAIConfigured = bqe.automation.hackathon_feb2026.data.network.NetworkModule.openAIApiService != null
        if (!isAIConfigured) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI (Groq) - Free LLM Available",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI test case generation is enabled using Groq's free service. Optional: Add API key for better performance (get free key at console.groq.com)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "AI (Groq) is configured - Test cases will be generated using free AI",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (error.contains("API Key", ignoreCase = true) || error.contains("401", ignoreCase = true)) {
                        Text(
                            text = "💡 Get your FREE Groq API key (30 seconds, no credit card):\nhttps://console.groq.com\n\nThen click 'Use Other User' to configure it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Generated Test Cases (${uiState.generatedTestCases.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.usingAI) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "🤖 AI Generated",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "📋 Rule-Based",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Display test cases in a scrollable list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp), // Limit height for better scrolling
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.generatedTestCases,
                    key = { it.title }
                ) { testCase ->
                    TestCaseCard(
                        testCase = testCase,
                        index = uiState.generatedTestCases.indexOf(testCase) + 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Project selection for test cases
            val storyProjectKey = uiState.story?.fields?.project?.key ?: "AND"
            val availableProjects = uiState.availableProjects
            var testCaseProjectKey by remember { mutableStateOf("TCM-XRAY (TC)") }
            var expanded by remember { mutableStateOf(false) }
            
            // Validate issue type
            val issueType = uiState.story?.fields?.issuetype?.name?.lowercase() ?: ""
            val isStory = issueType == "story"
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create Test Cases in Jira",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.fetchAllProjects() },
                            enabled = !uiState.isLoading
                        ) {
                            Text("📋 Refresh")
                        }
                    }
                    
                    // Show warning if not a Story
                    val currentStory = uiState.story
                    if (currentStory != null && !isStory) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "⚠️ Test cases can only be added to Stories. Current issue type: '${currentStory.fields.issuetype.name}'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Project Dropdown
                    if (availableProjects.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = testCaseProjectKey,
                                onValueChange = { testCaseProjectKey = it },
                                readOnly = true,
                                label = { Text("Select Project") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                availableProjects.forEach { project ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(
                                                    text = project.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Key: ${project.key}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            testCaseProjectKey = project.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback to text field if projects not loaded
                        OutlinedTextField(
                            value = testCaseProjectKey,
                            onValueChange = { testCaseProjectKey = it },
                            label = { Text("Project Name or Key (e.g., TCM-XRAY (TC) or TC)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("TCM-XRAY (TC)") },
                            singleLine = true
                        )
                    }
                    Text(
                        text = "Test cases will be created with steps, linked to the story, and set to 'Pending' resolution (Xray automation)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Note: Project key will be auto-cleaned (e.g., 'TCM-XRAY (TC)' → 'TCM'). Use the actual project key from Jira.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "💡 Click 'List Projects' to see all available projects in your Jira instance (check Logcat for output)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (uiState.story != null) {
                        Text(
                            text = "💡 Tip: If project doesn't exist, try using '$storyProjectKey' (from the story) or verify the project key in Jira",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.createTestCasesInJira(testCaseProjectKey.uppercase().trim()) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && testCaseProjectKey.isNotBlank()
                ) {
                    Text("Create in Jira (${testCaseProjectKey.uppercase()})")
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
            },
            onSaveGemini = { apiKey ->
                viewModel.configureOpenAI(apiKey)
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TC-$index",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = when (testCase.priority.lowercase()) {
                        "high" -> MaterialTheme.colorScheme.errorContainer
                        "medium" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = testCase.priority,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (testCase.priority.lowercase()) {
                            "high" -> MaterialTheme.colorScheme.onErrorContainer
                            "medium" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = testCase.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = testCase.description,
                style = MaterialTheme.typography.bodyMedium
            )
            if (testCase.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Steps:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                testCase.steps.forEachIndexed { stepIndex, step ->
                    Text(
                        text = "${stepIndex + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Expected: ${testCase.expectedResult}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun JiraConfigDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onSaveGemini: ((String) -> Unit)? = null
) {
    var baseUrl by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var apiToken by remember { mutableStateOf("") }
    var openAIApiKey by remember { mutableStateOf("") }
    var showGeminiSection by remember { mutableStateOf(false) }
    
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
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "AI-Powered Test Generation (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enable AI (Groq - Free) for intelligent test case generation. Get free API key at https://console.groq.com (optional but recommended)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = openAIApiKey,
                    onValueChange = { openAIApiKey = it },
                    label = { Text("Groq API Key (Optional - Free)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("Leave empty to use rule-based generation") }
                )
                Text(
                    text = "Get free API key: https://console.groq.com\n(Completely free, no credit card needed)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val trimmedBaseUrl = baseUrl.trim()
                    val trimmedEmail = email.trim()
                    val trimmedToken = apiToken.trim()
                    val trimmedOpenAIKey = openAIApiKey.trim()
                    
                    if (trimmedBaseUrl.isNotEmpty() && trimmedEmail.isNotEmpty() && trimmedToken.isNotEmpty()) {
                        onSave(trimmedBaseUrl, trimmedEmail, trimmedToken)
                        if (trimmedOpenAIKey.isNotEmpty() && onSaveGemini != null) {
                            onSaveGemini(trimmedOpenAIKey)
                        }
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
