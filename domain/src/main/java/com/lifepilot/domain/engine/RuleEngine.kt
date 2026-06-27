package com.lifepilot.domain.engine

interface RuleEngine {
    suspend fun evaluateRemindersForObject(objectId: String)
}
