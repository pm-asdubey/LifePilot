package com.lifepilot.features.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.GoalProposalCard
import com.lifepilot.designsystem.components.HomeBriefSkeleton
import com.lifepilot.designsystem.components.ObjectCreationCard
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.components.StatusUpdateCard
import com.lifepilot.designsystem.components.TaskCompletionCard
import com.lifepilot.designsystem.components.TaskCreationCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.AttachedDocumentContext
import com.lifepilot.domain.model.Conversation
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.AiProposal
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
    val context = LocalContext.current
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.processAttachment(it) }
        }
        pendingCameraUri = null
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { viewModel.processAttachment(it) }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        scanResult?.pdf?.uri?.let { viewModel.processAttachment(it) }
    }

    if (showAttachmentSheet) {
        AttachmentOptionSheet(
            onCamera = {
                showAttachmentSheet = false
                val captureDir = (context.getExternalFilesDir("camera_captures")
                    ?: context.filesDir.resolve("camera_captures"))
                    .also { it.mkdirs() }
                val captureFile = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    captureFile,
                )
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            },
            onScan = {
                showAttachmentSheet = false
                val options = GmsDocumentScannerOptions.Builder()
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .setGalleryImportAllowed(true)
                    .setPageLimit(10)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                    .build()
                GmsDocumentScanning.getClient(options)
                    .getStartScanIntent(context as androidx.activity.ComponentActivity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        timber.log.Timber.e(e, "Document scanner failed to start")
                    }
            },
            onPhotos = {
                filePickerLauncher.launch("image/*")
                showAttachmentSheet = false
            },
            onFiles = {
                filePickerLauncher.launch("*/*")
                showAttachmentSheet = false
            },
            onDismiss = { showAttachmentSheet = false },
        )
    }

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
                    onNewChat = viewModel::startNewConversation,
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
            AnimatedContent(
                targetState = uiState.mode,
                transitionSpec = {
                    if (targetState == HomeMode.AI_WORKSPACE) {
                        (slideInVertically { it / 3 } + fadeIn(initialAlpha = 0f)) togetherWith
                            (slideOutVertically { -it / 6 } + fadeOut())
                    } else {
                        (slideInVertically { -it / 3 } + fadeIn(initialAlpha = 0f)) togetherWith
                            (slideOutVertically { it / 6 } + fadeOut())
                    }
                },
                modifier = Modifier.weight(1f),
                label = "home_mode",
            ) { mode ->
                when (mode) {
                    HomeMode.DAILY_BRIEF -> DailyBriefContent(
                        uiState = uiState,
                        onItemClick = { item ->
                            item.objectId?.let { onNavigateToObject(it) }
                        },
                        onGoalClick = { onNavigateToPlanner() },
                        onResumeConversation = { viewModel.resumeConversation(it.conversationId) },
                        onViewAllConversations = viewModel::showConversationHistory,
                        modifier = Modifier.fillMaxSize(),
                    )
                    HomeMode.AI_WORKSPACE -> AiWorkspaceContent(
                        messages = uiState.messages,
                        isLoading = uiState.isAiLoading,
                        aiStatusMessage = uiState.aiStatusMessage,
                        isConfigured = uiState.isAiConfigured,
                        pendingAction = uiState.pendingAction,
                        pendingContextQuestion = uiState.pendingContextQuestion,
                        attachedDocumentContext = uiState.attachedDocumentContext,
                        onApproveAction = viewModel::approveAction,
                        onApproveActionPlan = viewModel::approveActionPlan,
                        onDismissAction = viewModel::dismissAction,
                        onDismissContextQuestion = viewModel::dismissContextQuestion,
                        onNavigateToSettings = onNavigateToSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // AI input bar — always visible
            AiInputBar(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                onAbort = viewModel::abortAi,
                onAttachFile = { showAttachmentSheet = true },
                onSuggestionChipClick = { suggestion ->
                    viewModel.onInputChange(suggestion)
                    viewModel.sendMessage()
                },
                isLoading = uiState.isAiLoading,
                hasMessages = uiState.messages.isNotEmpty(),
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

    if (uiState.isLoadingBrief) {
        HomeBriefSkeleton(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.md),
    ) {
        if (!hasContent) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "You're all caught up",
                        description = "Nothing needs your attention right now.\nAsk LifePilot anything using the bar below.",
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
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(3.dp, 40.dp)
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
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
    aiStatusMessage: String?,
    isConfigured: Boolean,
    pendingAction: AiProposal?,
    pendingContextQuestion: String?,
    attachedDocumentContext: AttachedDocumentContext?,
    onApproveAction: () -> Unit,
    onApproveActionPlan: (ActionPlan) -> Unit,
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
                if (attachedDocumentContext != null) {
                    item(key = "attached_document") {
                        DocumentAttachmentBubble(context = attachedDocumentContext)
                    }
                }
                if (isLoading) {
                    item(key = "thinking") {
                        ThinkingBubble(statusMessage = aiStatusMessage)
                    }
                }
            }
        }

        val cardModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        when (pendingAction) {
            is AiProposal.MetadataUpdate -> ActionProposalCard(
                action = pendingAction,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            is AiProposal.GoalProposal -> GoalProposalCard(
                title = pendingAction.title,
                description = pendingAction.description,
                deadline = pendingAction.deadline?.toString(),
                suggestedTasks = pendingAction.suggestedTasks,
                summary = pendingAction.summary,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            is AiProposal.TaskCompletion -> TaskCompletionCard(
                taskTitle = pendingAction.taskTitle,
                summary = pendingAction.summary,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            is AiProposal.TaskCreation -> TaskCreationCard(
                taskTitle = pendingAction.title,
                description = pendingAction.description,
                dueDate = pendingAction.dueDate?.toString(),
                summary = pendingAction.summary,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            is AiProposal.ObjectCreation -> ObjectCreationCard(
                objectType = pendingAction.objectType,
                domain = pendingAction.domain,
                title = pendingAction.title,
                summary = pendingAction.summary,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            is AiProposal.StatusUpdate -> StatusUpdateCard(
                recordTitle = pendingAction.objectTitle,
                newStatus = pendingAction.newStatus.name
                    .replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
                summary = pendingAction.summary,
                onApprove = onApproveAction,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            is AiProposal.ActionPlan -> ActionPlanCard(
                plan = pendingAction.plan,
                onApprove = onApproveActionPlan,
                onDismiss = onDismissAction,
                modifier = cardModifier,
            )
            null -> Unit
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
private fun DocumentAttachmentBubble(
    context: AttachedDocumentContext,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top,
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = context.title ?: context.objectType ?: "Document attached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(Spacing.xs))
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
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
private fun ThinkingBubble(
    statusMessage: String?,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Card(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.widthIn(min = 120.dp),
            onClick = { isExpanded = !isExpanded },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = "Thinking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "See what I'm doing",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Text(
                        text = statusMessage ?: "Working on it…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionProposalCard(
    action: AiProposal.MetadataUpdate,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
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
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApprove()
                }) {
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
        shape = MaterialTheme.shapes.large,
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

private val AI_SUGGESTIONS = listOf(
    "What needs attention?",
    "What's expiring soon?",
    "How's my career?",
    "Any risks I should know?",
    "What should I do today?",
    "Summarise my finances",
)

@Composable
private fun AiInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAbort: () -> Unit,
    onAttachFile: () -> Unit,
    onSuggestionChipClick: (String) -> Unit,
    isLoading: Boolean,
    hasMessages: Boolean,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider()
    Column(modifier = modifier) {
        // Suggestion chips — only shown before the first message in a fresh conversation
        if (text.isEmpty() && !isLoading && !hasMessages) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                AI_SUGGESTIONS.forEach { suggestion ->
                    FilterChip(
                        selected = false,
                        onClick = { onSuggestionChipClick(suggestion) },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Ask LifePilot anything…") },
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
            )
            if (isLoading) {
                IconButton(onClick = onAbort) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                IconButton(onClick = onAttachFile) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---- Attachment Option Sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentOptionSheet(
    onCamera: () -> Unit,
    onScan: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
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
                text = "Attach",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
            AttachmentOptionRow(
                icon = Icons.Outlined.PhotoCamera,
                label = "Camera",
                onClick = onCamera,
            )
            AttachmentOptionRow(
                icon = Icons.Outlined.DocumentScanner,
                label = "Scan document",
                onClick = onScan,
            )
            AttachmentOptionRow(
                icon = Icons.Outlined.AutoAwesome,
                label = "Photos",
                onClick = onPhotos,
            )
            AttachmentOptionRow(
                icon = Icons.Outlined.AttachFile,
                label = "Files",
                onClick = onFiles,
            )
        }
    }
}

@Composable
private fun AttachmentOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
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
