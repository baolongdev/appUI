package com.example.appui.ui.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel
import com.example.appui.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    agents: List<AgentSummaryResponseModel> = emptyList(),
    onVoiceClick: (String?) -> Unit,
    onAgentsClick: () -> Unit
) {
    var selectedAgentId by remember { mutableStateOf<String?>(null) }
    var customAgentId by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var useCustomId by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // ✅ BUILD OPTIONS: Custom ID first, no "Select an agent"
    val dropdownOptions = buildList {
        add("Custom ID..." to "custom")
        agents.forEach { agent ->
            add(agent.name to agent.agentId)
        }
    }

    var selectedOption by remember { mutableStateOf(dropdownOptions[0]) }

    val finalAgentId = when {
        useCustomId -> customAgentId
        else -> selectedAgentId
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(Spacing.Large)
                .widthIn(max = 400.dp)
                .align(Alignment.TopCenter)
        ) {
            // ==================== HEADER ====================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Home,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    "Welcome",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Choose an option to get started",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // ==================== AGENT DROPDOWN ====================
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedOption.first,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("Select Agent") },
                    leadingIcon = {
                        Icon(Icons.Default.SmartToy, null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = MaterialTheme.shapes.large
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    dropdownOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.first) },
                            onClick = {
                                selectedOption = option
                                expanded = false

                                if (option.second == "custom") {
                                    useCustomId = true
                                    selectedAgentId = null
                                } else {
                                    selectedAgentId = option.second
                                    useCustomId = false
                                    customAgentId = ""
                                    showError = false
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    if (option.second == "custom")
                                        Icons.Default.Edit
                                    else
                                        Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }

            // ==================== CUSTOM ID INPUT ====================
            if (useCustomId) {
                OutlinedTextField(
                    value = customAgentId,
                    onValueChange = {
                        customAgentId = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom Agent ID") },
                    placeholder = { Text("Nhập Agent ID...") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, null)
                    },
                    trailingIcon = if (customAgentId.isNotEmpty()) {
                        {
                            IconButton(onClick = { customAgentId = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    } else null,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Vui lòng nhập Agent ID hợp lệ") }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            // ==================== 3 ACTION BUTTONS ====================
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Voice Chat Button
                ElevatedButton(
                    onClick = {
                        if (finalAgentId?.isNotBlank() == true) {
                            onVoiceClick(finalAgentId)
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Voice Chat",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Bắt đầu cuộc trò chuyện",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // AI Face Button
                ElevatedButton(
                    onClick = {
                        if (finalAgentId?.isNotBlank() == true) {
                            // TODO: Navigate to AI Face
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Face,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "AI Face",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Trải nghiệm AI với khuôn mặt",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // My Agents Button
                ElevatedButton(
                    onClick = onAgentsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "My Agents",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Quản lý danh sách Agent",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Bottom spacing
            Spacer(Modifier.height(24.dp))
        }
    }
}
