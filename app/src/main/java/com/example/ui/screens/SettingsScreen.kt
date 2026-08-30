package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ReleaseNote(
    val version: String,
    val date: String,
    val summary: String,
    val details: List<String>
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    val releases = listOf(
        ReleaseNote(
            version = "1.00.00",
            date = "August 2026",
            summary = "Initial Release of AI Meal Generator",
            details = listOf(
                "Integrated Gemini 3.5 Flash AI with Google Search grounding for real-time culinary ideas.",
                "Added ingredient inventory management with quick-add chips.",
                "Added portion selector to scale ingredient amounts and recipes for any number of people eating.",
                "Added Room database support to save favorite generated recipes locally.",
                "Added interactive What's New release history accordion and FourgeAI LABS About section."
            )
        )
    )

    var expandedVersion by remember { mutableStateOf<String?>(null) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Info",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // What's New Menu Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWhatsNewDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "What's New in v1.00.00", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "View update notes and release history", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // About Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "About AI Meal Generator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "App Version: 1.00.00 (Build 1)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Created by: ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "FourgeAI LABS",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fourgeailabs"))
                                context.startActivity(intent)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "App GitHub Repository:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "https://github.com/fourgeailabs/ai-meal-generator",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fourgeailabs/ai-meal-generator"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = { Text("What's New & Release History") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tap an update version below to view details:")
                    Spacer(modifier = Modifier.height(4.dp))
                    releases.forEach { release ->
                        val isExpanded = (expandedVersion == release.version)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedVersion = if (isExpanded) null else release.version
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Version ${release.version} (${release.date})",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = release.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        release.details.forEach { detail ->
                                            Text(
                                                text = "• $detail",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhatsNewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
