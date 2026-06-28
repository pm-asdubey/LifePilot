package com.lifepilot.domain.model

data class ProposedAction(
    val id: String,
    val objectId: String,
    val objectTitle: String,
    val objectType: String,
    val summary: String,
    val fields: List<ProposedField>,
)

data class ProposedField(
    val fieldId: String,
    val displayName: String,
    val value: String,
    val mode: UpdateMode = UpdateMode.SET,
)

enum class UpdateMode { SET, APPEND }
