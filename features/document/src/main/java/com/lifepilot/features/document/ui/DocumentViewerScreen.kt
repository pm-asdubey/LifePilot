package com.lifepilot.features.document.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lifepilot.domain.model.DocumentVersion
import com.lifepilot.features.document.viewmodel.DocumentViewerState
import com.lifepilot.features.document.viewmodel.DocumentViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    onNavigateBack: () -> Unit,
    onExtractMetadata: ((objectId: String, versionId: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: DocumentViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.document?.versions?.find {
                            it.versionId == state.document?.currentVersionId
                        }?.originalName ?: "Document",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Download / share the stored document (a normalised PDF) via the system chooser.
                    state.currentVersion?.let { ver ->
                        IconButton(onClick = {
                            runCatching {
                                val file = java.io.File(ver.filePath)
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = ver.mimeType.ifBlank { "application/pdf" }
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(send, "Share or save document"),
                                )
                            }.onFailure { timber.log.Timber.e(it, "Failed to share document") }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Download or share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.ocrText != null) {
                        if (onExtractMetadata != null) {
                            val doc = state.document
                            val ver = state.currentVersion
                            if (doc != null && ver != null) {
                                IconButton(onClick = {
                                    onExtractMetadata(doc.objectId, ver.versionId)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = "Extract metadata with AI",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        IconButton(onClick = viewModel::toggleOcrText) {
                            Icon(
                                imageVector = Icons.Filled.TextFields,
                                contentDescription = "Toggle extracted text",
                                tint = if (state.showOcrText)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }
                else -> {
                    DocumentContent(
                        state = DocumentViewerState(
                            document = state.document,
                            currentVersion = state.currentVersion,
                            ocrText = state.ocrText,
                            showOcrText = state.showOcrText,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentContent(
    state: DocumentViewerState,
    modifier: Modifier = Modifier,
) {
    val version = state.currentVersion ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        when {
            version.mimeType.startsWith("image/") -> {
                AsyncImage(
                    model = version.filePath,
                    contentDescription = "Document image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            version.mimeType == "application/pdf" -> {
                PdfViewer(
                    filePath = version.filePath,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = version.originalName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.showOcrText && state.ocrText != null,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Extracted Text",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.ocrText ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        DocumentMetadataSection(version = version)
    }
}

@Composable
private fun PdfViewer(
    filePath: String,
    modifier: Modifier = Modifier,
) {
    var bitmaps by remember(filePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isRendering by remember(filePath) { mutableStateOf(true) }
    var renderError by remember(filePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        isRendering = true
        renderError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = java.io.File(filePath)
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                val targetWidth = 1080
                val pages = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val scale = targetWidth.toFloat() / page.width
                    val pageHeight = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(targetWidth, pageHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pages.add(bitmap)
                }
                renderer.close()
                pfd.close()
                pages.toList()
            }
        }
        result.onSuccess { pages ->
            bitmaps = pages
            isRendering = false
        }.onFailure { e ->
            renderError = e.message ?: "Failed to render PDF"
            isRendering = false
        }
    }

    when {
        isRendering -> Box(
            modifier = modifier.height(400.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Rendering PDF…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        renderError != null -> Box(
            modifier = modifier
                .height(400.dp)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = renderError ?: "Render error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(24.dp),
            )
        }
        bitmaps.isEmpty() -> Box(
            modifier = modifier
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Empty PDF",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Column(modifier = modifier) {
            bitmaps.forEachIndexed { index, bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < bitmaps.lastIndex) 2.dp else 0.dp),
                )
            }
        }
    }
}

@Composable
private fun DocumentMetadataSection(
    version: DocumentVersion,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "File Details",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        MetadataRow(label = "File name", value = version.originalName)
        MetadataRow(label = "Type", value = version.mimeType)
        if (version.sizeBytes > 0) {
            MetadataRow(
                label = "Size",
                value = formatFileSize(version.sizeBytes),
            )
        }
        MetadataRow(
            label = "Scanned text",
            value = if (version.ocrText != null) "Extracted" else "Pending",
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
