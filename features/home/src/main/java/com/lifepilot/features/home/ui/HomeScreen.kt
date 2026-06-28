package com.lifepilot.features.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.Conversation
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.ProposedAction
import com.lifepilot.domain.model.StoredMessage
import com.lifepilot.features.home.state.AttentionItem
import com.lifepilot.features.home.state.AttentionUrgency
import com.lifepilot.features.home.state.HomeMode
import com.lifepilot.features.home.viewmodel.HomeViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToObject: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToVerification: (objectId: String, versionId: String) -> Unit = { _, _ -> },
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Conversation history sheet
    if (uiState.showConversationHistory) {
        ConversationHistorySheet(
            conversations = uiState.allConversations,
            onResume = { viewModel.resumeConversation(it) },
            onDelete = { viewModel.deleteConversation(it) },
            onDismiss = viewModel::hideConversationHistory,
        )
    }

    Scaffold(
        topBar = {
            when (uiState.mode) {
                HomeMode.DAILY_BRIEF -> DailyBriefTopBar(
                    greeting = uiState.greeting,
                    profileName = uiState.profileName,
                    onSearch = onNavigateToSearch,
                    onHistory = viewModel::showConversationHistory,
                )
                HomeMode.AI_WORKSPACE -> AiWorkspaceTopBar(
                    title = uiState.conversationTitle,
                    onBack = viewModel::returnToBrief,
                    onNewChat = viewModel::startNewChat,
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            when (uiState.mode) {
                HomeMode.DAILY_BRIEF -> DailyBriefContent(
                    uiState = uiState,
                    onItemClick = { item ->
                        item.objectId?.let { onNavigateToObject(it) }
                    },
                    onGoalClick = { onNavigateToPlanner() },
                    onResumeConversation = { viewModel.resumeConversation(it.conversationId) },
                    onViewAllConversations = viewModel::showConversationHistory,
                    modifier = Modifier.weight(1f),
                )
                HomeMode.AI_WORKSPACE -> AiWorkspaceContent(
                    messages = uiState.messages,
                    isLoading = uiState.isAiLoading,
                    isConfigured = uiState.isAiConfigured,
                    pendingAction = uiState.pendingAction,
                    pendingContextQuestion = uiState.pendingContextQuestion,
                    onApproveAction = viewModel::approveAction,
                    onDismissAction = viewModel::dismissAction,
                    onDismissContextQuestion = viewModel::dismissContextQuestion,
                    onNavigateToSettings = onNavigateToSettings,
                    modifier = Modifier.weight(1f),
                )
            }

            // AI input bar — always visible
            AiInputBar(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                isLoading = uiState.isAiLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---- Top Bars ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyBriefTopBar(
    greeting: String,
    profileName: String,
    onSearch: () -> Unit,
    onHistory: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = profileName.ifBlank { "LifePilot" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = onHistory) {
                Icon(imageVector = Icons.Outlined.History, contentDescription = "Conversation history")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiWorkspaceTopBar(
    title: String,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back to brief")
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = "New chat")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

// ---- Daily Brief ----

@Composable
private fun DailyBriefContent(
    uiState: com.lifepilot.features.home.state.HomeUiState,
    onItemClick: (AttentionItem) -> Unit,
    onGoalClick: (Goal) -> Unit,
    onResumeConversation: (Conversation) -> Unit,
    onViewAllConversations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = uiState.attentionItems.isNotEmpty() ||
        uiState.activeGoals.isNotEmpty() ||
        uiState.recentConversations.isNotEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.md),
    ) {
        if (!hasContent && !uiState.isLoadingBrief) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "You're all caught up.",
                        description = "Ask LifePilot anything using the bar below.",
                    )
                }
            }
        }

        if (uiState.attentionItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "NEEDS ATTENTION",
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            items(uiState.attentionItems, key = { "attn_${it.id}" }) { item ->
                AttentionCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
        }

        if (uiState.activeGoals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "ACTIVE GOALS",
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            items(uiState.activeGoals.take(3), key = { "goal_${it.goalId}" }) { goal ->
                CompactGoalCard(
                    goal = goal,
                    onClick = { onGoalClick(goal) },
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
        }

        if (uiState.recentConversations.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "RECENT CONVERSATIONS",
                    actionLabel = "View all",
                    onAction = onViewAllConversations,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            items(uiState.recentConversations, key = { "conv_${it.conversationId}" }) { conv ->
                ConversationRow(
                    conversation = conv,
                    onClick = { onResumeConversation(conv) },
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun AttentionCard(
    item: AttentionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (item.urgency) {
        AttentionUrgency.CRITICAL -> MaterialTheme.colorScheme.error
        AttentionUrgency.HIGH -> com.lifepilot.designsystem.theme.Warning
        AttentionUrgency.MEDIUM -> MaterialTheme.colorScheme.primary
        AttentionUrgency.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 40.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactGoalCard(
    goal: Goal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            LinearProgressIndicator(
                progress = { goal.progress / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp),
            )
            Text(
                text = "${goal.progress}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = conversation.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = conversation.updatedAt
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d")),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- AI Workspace ----

@Composable
private fun AiWorkspaceContent(
    messages: List<StoredMessage>,
    isLoading: Boolean,
    isConfigured: Boolean,
    pendingAction: ProposedAction?,
    pendingContextQuestion: String?,
    onApproveAction: () -> Unit,
    onDismissAction: () -> Unit,
    onDismissContextQuestion: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!isConfigured) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Connect an AI provider in Settings to get answers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Set up")
                    }
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Ask about your life",
                    description = "Ask about your passport, insurance, deadlines or anything you've stored.",
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    MessageBubble(
                        role = msg.role,
                        content = msg.content,
                    )
                }
                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (pendingAction != null) {
            ActionProposalCard(
                action = pendingAction,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }

        if (pendingContextQuestion != null && pendingAction == null) {
            ContextQuestionCard(
                question = pendingContextQuestion,
                onDismiss = onDismissContextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }
    }
}

@Composable
private fun MessageBubble(
    role: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    val isUser = role == "USER"
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
        }
        Card(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
        if (isUser) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ActionProposalCard(
    action: ProposedAction,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "Update \"${action.objectTitle}\"?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            action.fields.forEach { field ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${field.displayName}: ${field.value.take(80)}${if (field.value.length > 80) "…" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("Skip", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                TextButton(onClick = onApprove) {
                    Text("Save to record", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun ContextQuestionCard(
    question: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = question,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ---- AI Input Bar ----

@Composable
private fun AiInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider()
    Row(
        modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Ask LifePilot anything…") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
            shape = RoundedCornerShape(24.dp),
        )
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isLoading,
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank() && !isLoading)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Conversation History Sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistorySheet(
    conversations: List<Conversation>,
    onResume: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                text = "Conversations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No conversations yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn {
                    items(conversations, key = { it.conversationId }) { conv ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDelete(conv.conversationId)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(end = Spacing.lg),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onResume(conv.conversationId) }
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = conv.updatedAt
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("MMM d")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    }
                }
            }
        }
    }
}
