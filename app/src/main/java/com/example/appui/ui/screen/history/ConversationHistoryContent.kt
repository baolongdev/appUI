package com.example.appui.ui.screen.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.data.model.Speaker
import com.example.appui.data.model.VoiceConversation
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.util.*

enum class SortOrder {
    DATE_DESC, DATE_ASC, TITLE, MESSAGES_COUNT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationHistoryContent(
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    var selectedConversation by remember { mutableStateOf<VoiceConversation?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<VoiceConversation?>(null) }
    var showShareDialog by remember { mutableStateOf<VoiceConversation?>(null) }
    var showTagDialog by remember { mutableStateOf<VoiceConversation?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val useGrid = screenWidth >= 600

    val filteredConversations = remember(conversations, searchQuery, sortOrder, selectedTag) {
        conversations
            .filter { conversation ->
                val matchesSearch = searchQuery.isBlank() ||
                        (conversation.title?.contains(searchQuery, ignoreCase = true) == true) ||
                        conversation.messages.any { it.text.contains(searchQuery, ignoreCase = true) }

                val matchesTag = selectedTag == null || conversation.tags.contains(selectedTag)

                matchesSearch && matchesTag
            }
            .let { list ->
                when (sortOrder) {
                    SortOrder.DATE_DESC -> list.sortedByDescending { it.timestamp }
                    SortOrder.DATE_ASC -> list.sortedBy { it.timestamp }
                    SortOrder.TITLE -> list.sortedBy { it.title ?: "" }
                    SortOrder.MESSAGES_COUNT -> list.sortedByDescending { it.messages.size }
                }
            }
    }

    val allTags = remember(conversations) {
        conversations.flatMap { it.tags }.distinct().sorted()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Header với gradient
        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.History,
                                    null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column {
                            Text(
                                "Lịch sử hội thoại",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.extendedColors.success)
                                    )
                                    Text(
                                        "${conversations.size} cuộc hội thoại",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Sort Button - FIXED ALIGNMENT
                    Box {
                        FilledTonalIconButton(
                            onClick = { showSortMenu = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.FilterList, "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            offset = DpOffset(0.dp, 8.dp) // ✅ Add offset để menu xuống dưới button
                        ) {
                            SortMenuItem(
                                "Mới nhất",
                                Icons.Filled.Schedule,
                                sortOrder == SortOrder.DATE_DESC
                            ) {
                                sortOrder = SortOrder.DATE_DESC
                                showSortMenu = false
                            }
                            SortMenuItem(
                                "Cũ nhất",
                                Icons.Filled.History,
                                sortOrder == SortOrder.DATE_ASC
                            ) {
                                sortOrder = SortOrder.DATE_ASC
                                showSortMenu = false
                            }
                            SortMenuItem(
                                "Tên (A-Z)",
                                Icons.Filled.SortByAlpha,
                                sortOrder == SortOrder.TITLE
                            ) {
                                sortOrder = SortOrder.TITLE
                                showSortMenu = false
                            }
                            SortMenuItem(
                                "Số tin nhắn",
                                Icons.Filled.ChatBubble,
                                sortOrder == SortOrder.MESSAGES_COUNT
                            ) {
                                sortOrder = SortOrder.MESSAGES_COUNT
                                showSortMenu = false
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Modern Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Tìm kiếm cuộc hội thoại...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Tags Filter
                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { selectedTag = null },
                            label = { Text("Tất cả") },
                            leadingIcon = if (selectedTag == null) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = if (selectedTag == tag) null else tag },
                                label = { Text(tag) },
                                leadingIcon = { Icon(Icons.Filled.Tag, null, Modifier.size(18.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Content (keep grid/list logic unchanged but use updated card design below)
        if (filteredConversations.isEmpty()) {
            ModernEmptyState(conversations.isEmpty())
        } else {
            if (useGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conversation ->
                        ModernConversationCard(
                            conversation = conversation,
                            onClick = { selectedConversation = conversation },
                            onShare = { showShareDialog = conversation },
                            onDelete = {
                                conversationToDelete = conversation
                                showDeleteDialog = true
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(conversation.id) },
                            onEditTags = { showTagDialog = conversation }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredConversations.size, key = { filteredConversations[it].id }) { index ->
                        ModernConversationCard(
                            conversation = filteredConversations[index],
                            onClick = { selectedConversation = filteredConversations[index] },
                            onShare = { showShareDialog = filteredConversations[index] },
                            onDelete = {
                                conversationToDelete = filteredConversations[index]
                                showDeleteDialog = true
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(filteredConversations[index].id) },
                            onEditTags = { showTagDialog = filteredConversations[index] }
                        )
                    }
                }
            }
        }
    }

    // Dialogs (will be in next message)
    selectedConversation?.let { DetailDialog(it, { selectedConversation = null }, { showShareDialog = it }) }
    showShareDialog?.let { ShareDialog(it, { showShareDialog = null }, viewModel) }
    showTagDialog?.let { ModernTagDialog(it, { showTagDialog = null }, viewModel) }
    if (showDeleteDialog) {
        ModernDeleteDialog(
            onConfirm = {
                conversationToDelete?.let { viewModel.deleteConversation(it.id) }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ============= MODERN CONVERSATION CARD =============
@Composable
private fun ModernConversationCard(
    conversation: VoiceConversation,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditTags: () -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (conversation.isFavorite)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top // ✅ Changed to Top
                ) {
                    // Agent Avatar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SmartToy,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f), // ✅ Removed fill = false
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = conversation.title ?: "Cuộc hội thoại",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        conversation.agentName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onToggleFavorite, Modifier.size(32.dp)) {
                        Icon(
                            if (conversation.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            null,
                            Modifier.size(18.dp),
                            tint = if (conversation.isFavorite) extendedColors.warning
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShare, Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Share,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge(Icons.Filled.CalendarToday, formatDateShort(conversation.timestamp))
                StatBadge(Icons.Filled.ChatBubbleOutline, "${conversation.messages.size}")
                StatBadge(Icons.Filled.Timer, formatDuration(conversation.durationMs))
            }

            // Tags Section
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (conversation.tags.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LabelOff,
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Chưa có tag",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        conversation.tags.take(3).forEach { tag ->
                            TagChip(tag)
                        }
                        if (conversation.tags.size > 3) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    "+${conversation.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = onEditTags,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}


@Composable
private fun StatBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Tag,
                null,
                Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                tag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ============= MODERN TAG DIALOG =============
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ModernTagDialog(
    conversation: VoiceConversation,
    onDismiss: () -> Unit,
    viewModel: ConversationHistoryViewModel
) {
    var currentTags by remember { mutableStateOf(conversation.tags.toSet()) }
    var tagsToAdd by remember { mutableStateOf<Set<String>>(emptySet()) }
    var tagsToRemove by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newTagText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val suggestedTags = listOf(
        "Quan trọng", "Khẩn cấp", "Tư vấn",
        "Hỗ trợ", "Đơn hàng", "Thanh toán"
    )

    val finalTags = (currentTags + tagsToAdd) - tagsToRemove

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Label,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Quản lý Tags",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Tag,
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${finalTags.size} tags đã chọn",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ✅ INPUT SECTION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Add,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                "Thêm tag mới",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = {
                                if (it.length <= 20) {
                                    newTagText = it
                                    showError = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Nhập tên tag và nhấn Enter") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Edit,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = if (newTagText.isNotBlank()) {
                                {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val trimmed = newTagText.trim()
                                            when {
                                                trimmed.isEmpty() -> {
                                                    showError = true
                                                    errorMessage = "Tag không được trống"
                                                }
                                                finalTags.contains(trimmed) -> {
                                                    showError = true
                                                    errorMessage = "Tag đã tồn tại"
                                                }
                                                suggestedTags.contains(trimmed) -> {
                                                    showError = true
                                                    errorMessage = "Dùng tag gợi ý bên dưới"
                                                }
                                                else -> {
                                                    tagsToAdd = tagsToAdd + trimmed
                                                    newTagText = ""
                                                    showError = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            "Add",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else null,
                            isError = showError,
                            supportingText = if (showError) {
                                { Text(errorMessage) }
                            } else {
                                {
                                    Text(
                                        "${newTagText.length}/20",
                                        color = if (newTagText.length >= 15)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val trimmed = newTagText.trim()
                                    when {
                                        trimmed.isEmpty() -> {
                                            showError = true
                                            errorMessage = "Tag không được trống"
                                        }
                                        finalTags.contains(trimmed) -> {
                                            showError = true
                                            errorMessage = "Tag đã tồn tại"
                                        }
                                        suggestedTags.contains(trimmed) -> {
                                            showError = true
                                            errorMessage = "Dùng tag gợi ý bên dưới"
                                        }
                                        else -> {
                                            tagsToAdd = tagsToAdd + trimmed
                                            newTagText = ""
                                            showError = false
                                        }
                                    }
                                }
                            )
                        )
                    }
                }

                // ✅ SUGGESTED TAGS SECTION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Lightbulb,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Text(
                                "Gợi ý",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestedTags.forEach { tag ->
                                val isActive = finalTags.contains(tag)
                                FilterChip(
                                    selected = isActive,
                                    onClick = {
                                        if (isActive) {
                                            if (currentTags.contains(tag)) {
                                                tagsToRemove = tagsToRemove + tag
                                            } else {
                                                tagsToAdd = tagsToAdd - tag
                                            }
                                        } else {
                                            if (tagsToRemove.contains(tag)) {
                                                tagsToRemove = tagsToRemove - tag
                                            } else {
                                                tagsToAdd = tagsToAdd + tag
                                            }
                                        }
                                    },
                                    label = { Text(tag) },
                                    leadingIcon = if (isActive) {
                                        { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // ✅ CURRENT TAGS SECTION
                if (finalTags.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Tag,
                                            null,
                                            Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                Text(
                                    "Tags hiện tại",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                finalTags.forEach { tag ->
                                    val willBeRemoved = tagsToRemove.contains(tag)
                                    InputChip(
                                        selected = !willBeRemoved,
                                        onClick = {
                                            if (currentTags.contains(tag)) {
                                                if (willBeRemoved) {
                                                    tagsToRemove = tagsToRemove - tag
                                                } else {
                                                    tagsToRemove = tagsToRemove + tag
                                                }
                                            } else {
                                                tagsToAdd = tagsToAdd - tag
                                            }
                                        },
                                        label = {
                                            Text(
                                                tag,
                                                style = if (willBeRemoved) {
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                    )
                                                } else {
                                                    MaterialTheme.typography.bodyMedium
                                                }
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                if (willBeRemoved) Icons.Filled.Undo else Icons.Filled.Close,
                                                null,
                                                Modifier.size(18.dp)
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            containerColor = if (willBeRemoved) {
                                                MaterialTheme.colorScheme.errorContainer
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                            labelColor = if (willBeRemoved) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // ✅ REMOVED TAGS PREVIEW
                if (tagsToRemove.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Sẽ xóa ${tagsToRemove.size} tag: ${tagsToRemove.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateTags(conversation.id, finalTags.toList())
                    onDismiss()
                },
                enabled = tagsToAdd.isNotEmpty() || tagsToRemove.isNotEmpty()
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Lưu (${finalTags.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}


// ============= MODERN SHARE DIALOG =============
@Composable
private fun ShareDialog(
    conversation: VoiceConversation,
    onDismiss: () -> Unit,
    viewModel: ConversationHistoryViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Share,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    "Chia sẻ cuộc hội thoại",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    conversation.title ?: "Untitled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Chọn định dạng file:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // HTML Option
                ElevatedCard(
                    onClick = {
                        viewModel.shareAsHtml(conversation)
                        onDismiss()
                    },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Article,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                "HTML File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Dễ đọc trên web browser",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Icon(
                            Icons.Filled.ArrowForward,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // JSON Option
                OutlinedCard(
                    onClick = {
                        viewModel.shareAsJson(conversation)
                        onDismiss()
                    }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Code,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                "JSON File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Import vào app khác",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.Filled.ArrowForward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}

// ============= MODERN DETAIL DIALOG =============
@Composable
private fun DetailDialog(
    conversation: VoiceConversation,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.ChatBubble,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        conversation.title ?: "Cuộc hội thoại",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatDate(conversation.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalIconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, "Share")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversation.messages.size) { index ->
                    val message = conversation.messages[index]
                    val isUser = message.speaker == Speaker.USER

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isUser)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                Modifier
                                    .widthIn(max = 280.dp)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isUser)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (isUser) Icons.Filled.Person else Icons.Filled.SmartToy,
                                                null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isUser)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        if (isUser) "Bạn" else "Agent",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    message.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Đóng") }
        }
    )
}

// ============= MODERN DELETE DIALOG =============
@Composable
private fun ModernDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.DeleteForever,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        title = {
            Text(
                "Xóa cuộc hội thoại?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Hành động này không thể hoàn tác.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Tất cả tin nhắn sẽ bị xóa vĩnh viễn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Xóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

// ============= MODERN EMPTY STATES =============
@Composable
private fun ModernEmptyState(isEmpty: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isEmpty) Icons.Filled.HistoryToggleOff else Icons.Filled.SearchOff,
                        null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                if (isEmpty) "Chưa có cuộc hội thoại nào" else "Không tìm thấy kết quả",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                if (isEmpty) "Bắt đầu trò chuyện với AI để xem lịch sử"
                else "Thử tìm với từ khóa khác",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============= SORT MENU ITEM =============
@Composable
private fun SortMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                icon,
                null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null
    )
}

// ============= HELPER FUNCTIONS =============
private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDateShort(timestamp: Long): String {
    return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
