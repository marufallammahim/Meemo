package com.meemo.core.model

sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class NeedsPermission(val permission: String, val message: String) : ActionResult
    data class NeedsUserAction(val message: String, val actionKey: String? = null) : ActionResult
    data class NeedsConfirmation(val message: String, val confirm: MeemoIntent) : ActionResult
    data class Unsupported(val reason: String) : ActionResult
    data class Failed(val message: String, val cause: Throwable? = null) : ActionResult
}
