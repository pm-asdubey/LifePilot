package com.lifepilot.features.object.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifepilot.features.document.upload.DocumentUploadSheet
import com.lifepilot.features.object.ui.ObjectDetailScreen

fun NavGraphBuilder.objectDetailScreen(navController: NavController) {
    composable(
        route = "object/{objectId}",
        arguments = listOf(navArgument("objectId") { type = NavType.StringType }),
    ) {
        var uploadTargetObjectId by remember { mutableStateOf<String?>(null) }

        ObjectDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDocument = { documentId ->
                navController.navigate("document/$documentId")
            },
            onUploadDocument = { objectId ->
                uploadTargetObjectId = objectId
            },
        )

        val targetId = uploadTargetObjectId
        if (targetId != null) {
            DocumentUploadSheet(
                objectId = targetId,
                onDismiss = { uploadTargetObjectId = null },
                onDocumentUploaded = { _ ->
                    uploadTargetObjectId = null
                },
            )
        }
    }
}
