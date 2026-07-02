package com.lifepilot.features.objectdetail.state

import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.Relationship
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TimelineEntry

data class ObjectDetailUiState(
    val isLoading: Boolean = true,
    val lifeObject: LifeObject? = null,
    val documents: List<Document> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val relationships: List<Relationship> = emptyList(),
    val relatedObjects: Map<String, LifeObject> = emptyMap(),
    val selectedTab: ObjectDetailTab = ObjectDetailTab.OVERVIEW,
    val showLinkObjectSheet: Boolean = false,
    val pendingVerificationVersionId: String? = null,
    val error: String? = null,
)

enum class ObjectDetailTab(val label: String) {
    OVERVIEW("Overview"),
    DOCUMENTS("Documents"),
    TIMELINE("Timeline"),
    TASKS("Tasks"),
}
