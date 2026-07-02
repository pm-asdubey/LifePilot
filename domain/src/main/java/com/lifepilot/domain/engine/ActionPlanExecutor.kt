package com.lifepilot.domain.engine

import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan

/**
 * Executes an approved [ActionPlan] sequentially.
 *
 * The executor is responsible for dependency ordering, mapping each [ActionItem]
 * to the appropriate single-step executor, handling partial failures, and reporting
 * progress. It keeps [com.lifepilot.features.home.viewmodel.HomeViewModel] focused
 * on UI orchestration.
 */
interface ActionPlanExecutor {

    /**
     * @param plan The approved action plan.
     * @param onProgress Called after each item with the item and its result.
     * @return Overall result. Success means all enabled items completed; failure
     *         means at least one enabled item failed, but execution continued.
     */
    suspend fun execute(
        plan: ActionPlan,
        onProgress: suspend (item: ActionItem, result: Result<Unit>) -> Unit = { _, _ -> },
    ): Result<Unit>
}
