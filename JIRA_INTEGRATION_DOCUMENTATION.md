# Jira Test Case Generator - Integration Documentation

## Overview

This Android application provides seamless integration with Jira to automatically generate test cases from user stories. The app fetches story descriptions and acceptance criteria from Jira, generates comprehensive test cases, and can create them directly in your Jira project.

## Features

### 1. **Jira Story Fetching**
- Connect to your Jira instance using API token authentication
- Fetch user stories by issue key (e.g., AND-7709)
- Parse and display story details including:
  - Summary
  - Description (supports Atlassian Document Format)
  - Acceptance Criteria

### 2. **Intelligent Test Case Generation**
- Automatically generates test cases from:
  - Story descriptions
  - Acceptance criteria
- Supports multiple formats:
  - Bullet points
  - Numbered lists
  - Given-When-Then format
- Extracts test steps and expected results
- Assigns priority levels (High, Medium, Low) based on keywords

### 3. **Jira Test Case Creation**
- Creates test cases directly in Jira
- Formats descriptions in Atlassian Document Format (ADF)
- Links test cases to parent stories
- Includes comprehensive test case details:
  - Test steps
  - Expected results
  - Priority levels
  - Related story references

### 4. **Android Test Code Generation**
- Generates Android test code templates
- Creates Compose UI test structures
- Includes test method stubs with TODOs
- Ready for implementation

## Architecture

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: Retrofit 2.9.0
- **HTTP Client**: OkHttp 4.12.0
- **JSON Parsing**: Gson 2.10.1
- **Async Operations**: Kotlin Coroutines
- **State Management**: StateFlow

### Project Structure

```
app/src/main/java/bqe/automation/hackathon_feb2026/
├── data/
│   ├── api/
│   │   └── JiraApiService.kt          # Retrofit API interface
│   ├── model/
│   │   ├── JiraModels.kt              # Jira data models
│   │   ├── AtlassianDocument.kt       # ADF format models
│   │   └── JiraErrorResponse.kt       # Error response model
│   ├── network/
│   │   └── NetworkModule.kt           # Network configuration
│   └── repository/
│       └── JiraRepository.kt         # Data repository
├── domain/
│   └── TestCaseGenerator.kt           # Test case generation logic
├── ui/
│   └── jira/
│       ├── JiraViewModel.kt          # ViewModel
│       └── JiraIntegrationScreen.kt  # UI components
└── di/
    └── AppModule.kt                  # Dependency injection
```

## Key Components

### 1. Network Module (`NetworkModule.kt`)

Handles all network configuration:
- Base URL management
- Authentication (Basic Auth with API token)
- HTTP client setup with logging
- Retrofit instance creation

**Features:**
- Dynamic initialization with Jira credentials
- Automatic credential injection in requests
- Request/response logging for debugging

### 2. Jira API Service (`JiraApiService.kt`)

Defines REST API endpoints:
- `GET /rest/api/3/issue/{issueKey}` - Fetch issue details
- `POST /rest/api/3/issue` - Create new test case

### 3. Data Models

**JiraModels.kt:**
- `JiraIssue` - Complete issue structure
- `JiraFields` - Issue fields with ADF parsing
- `TestCaseFields` - Test case creation payload
- `GeneratedTestCase` - Generated test case model

**AtlassianDocument.kt:**
- `AtlassianDocument` - ADF document structure
- `ADFNode` - ADF node structure
- `ADFConverter` - Text to ADF conversion utility

### 4. Test Case Generator (`TestCaseGenerator.kt`)

Intelligent test case generation engine:

**Parsing Capabilities:**
- Bullet point lists (-, *, •)
- Numbered lists (1., 2.), (1), 2))
- Given-When-Then format
- Sentence-based parsing (fallback)

**Generation Logic:**
- Extracts test steps from acceptance criteria
- Identifies expected results
- Determines priority from keywords
- Generates positive and negative test cases

### 5. Repository (`JiraRepository.kt`)

Data access layer:
- Fetches stories from Jira
- Generates test cases
- Creates test cases in Jira
- Formats descriptions to ADF
- Error handling and parsing

### 6. ViewModel (`JiraViewModel.kt`)

Business logic and state management:
- Jira configuration
- Story fetching
- Test case generation
- Test case creation in Jira
- Android test code generation

### 7. UI Components (`JiraIntegrationScreen.kt`)

User interface:
- Configuration dialog
- Story input and display
- Generated test cases list
- Test case creation controls
- Android test code viewer

## How It Works

### Step 1: Configuration

1. Launch the app
2. Click "Configure Jira Connection"
3. Enter:
   - **Base URL**: `https://your-domain.atlassian.net`
   - **Email**: Your Jira email address
   - **API Token**: Your Jira API token

### Step 2: Fetch Story

1. Enter Jira issue key (e.g., `AND-7709`)
2. Click "Fetch Story"
3. App displays:
   - Story summary
   - Description
   - Acceptance criteria

### Step 3: Generate Test Cases

Test cases are automatically generated from:
- Acceptance criteria (primary source)
- Story description (secondary source)

The generator:
- Parses acceptance criteria
- Extracts test steps
- Identifies expected results
- Assigns priorities

### Step 4: Create in Jira

1. Review generated test cases
2. Click "Create in Jira"
3. Test cases are created with:
   - Proper ADF formatting
   - All test steps
   - Expected results
   - Priority levels
   - Related story reference

### Step 5: Generate Android Tests

1. Click "Generate Android Tests"
2. View generated test code
3. Copy and implement in your test files

## Technical Details

### Authentication

Uses Basic Authentication:
```
Authorization: Basic base64(email:apiToken)
```

### API Endpoints

**Fetch Issue:**
```
GET /rest/api/3/issue/{issueKey}?fields=summary,description,issuetype,project,customfield_10026
```

**Create Test Case:**
```
POST /rest/api/3/issue
Content-Type: application/json

{
  "fields": {
    "project": {"key": "AND"},
    "summary": "Test Case Title",
    "description": { /* ADF format */ },
    "issuetype": {"name": "Test"}
  }
}
```

### Atlassian Document Format (ADF)

The app converts plain text to ADF format:

```json
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Test case description"
        }
      ]
    }
  ]
}
```

### Error Handling

Comprehensive error handling:
- Network errors
- Authentication failures
- Jira API errors
- Parsing errors
- User-friendly error messages

## Configuration

### Custom Field IDs

You may need to adjust custom field IDs in `JiraModels.kt`:

```kotlin
@SerializedName("customfield_10026") // Acceptance Criteria
val acceptanceCriteria: JsonElement?
```

To find your custom field IDs:
1. Go to Jira Administration
2. Navigate to Issues → Custom Fields
3. Find your field and note the ID

### Test Issue Type

Ensure your Jira project has a "Test" issue type, or update in `JiraRepository.kt`:

```kotlin
issuetype = JiraIssueTypeKey("Test") // Change if different
```

## Dependencies

```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("com.google.code.gson:gson:2.10.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
```

## Permissions

Required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Usage Example

### Fetching a Story

```kotlin
viewModel.fetchStory("AND-7709")
```

### Creating Test Cases

```kotlin
viewModel.createTestCasesInJira()
```

### Generating Android Test Code

```kotlin
val testCode = viewModel.generateAndroidTestFiles()
```

## Generated Test Case Structure

Each generated test case includes:

1. **Title**: Descriptive test case name
2. **Description**: What is being tested
3. **Steps**: Sequential test steps
4. **Expected Result**: Expected outcome
5. **Priority**: High, Medium, or Low
6. **Related Story**: Link to parent story

## Generated Android Test Code Structure

```kotlin
@RunWith(AndroidJUnit4::class)
class JiraAND7709TestCases {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testTC1VerifyBasicFunctionality() {
        // Test implementation
    }
}
```

## Best Practices

1. **API Token Security**
   - Never commit API tokens to version control
   - Use secure storage for production apps
   - Rotate tokens regularly

2. **Error Handling**
   - Always check error messages
   - Verify Jira configuration before use
   - Handle network failures gracefully

3. **Test Case Quality**
   - Review generated test cases
   - Customize as needed
   - Ensure coverage of acceptance criteria

4. **Performance**
   - Test cases are generated synchronously
   - Network calls are async (Coroutines)
   - UI remains responsive

## Troubleshooting

### Common Issues

1. **"Jira not configured"**
   - Ensure you've entered credentials
   - Check base URL format
   - Verify API token is valid

2. **"Failed to fetch issue"**
   - Verify issue key exists
   - Check permissions
   - Verify network connection

3. **"Failed to create test case"**
   - Check project key is correct
   - Verify "Test" issue type exists
   - Check field permissions

4. **"Operation value must be an Atlassian Document"**
   - This is handled automatically
   - Description is converted to ADF format

## Future Enhancements

Potential improvements:

1. **Test Case Templates**
   - Customizable templates
   - Project-specific templates

2. **Bulk Operations**
   - Create multiple test cases at once
   - Batch updates

3. **Test Execution Integration**
   - Link to test execution tools
   - Track test results

4. **Advanced Parsing**
   - Markdown support
   - Rich text formatting
   - Image support

5. **Offline Support**
   - Cache stories locally
   - Queue test case creation

## Support

For issues or questions:
- Check error messages in the app
- Review Jira API documentation
- Verify custom field configurations

## Conclusion

This Jira integration provides a powerful solution for automating test case generation from user stories. It streamlines the testing workflow by:

- Reducing manual test case creation time
- Ensuring test cases cover acceptance criteria
- Maintaining consistency across test cases
- Integrating seamlessly with Jira workflows

The modular architecture makes it easy to extend and customize for specific project needs.

---

**Version**: 1.0  
**Last Updated**: February 2026  
**Platform**: Android (API 24+)  
**Framework**: Jetpack Compose
