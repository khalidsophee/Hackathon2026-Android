package bqe.automation.hackathon_feb2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import bqe.automation.hackathon_feb2026.ui.jira.JiraIntegrationScreen
import bqe.automation.hackathon_feb2026.ui.theme.Hackathon_Feb2026Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hackathon_Feb2026Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JiraIntegrationScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

