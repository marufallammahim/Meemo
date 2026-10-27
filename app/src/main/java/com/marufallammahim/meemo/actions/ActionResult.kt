package com.marufallammahim.meemo.actions

import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.domain.model.InstalledAppInfo

/**
 * Explicit outcome of an action execution or capability check.
 * UI and TTS responses are derived strictly from this verified outcome.
 */
sealed interface ActionResult {
    val outcomeCode: String

    data class Success(
        val summaryEn: String,
        val summaryBn: String,
        val verifiedBySystem: Boolean = true,
        val details: Map<String, String> = emptyMap()
    ) : ActionResult {
        override val outcomeCode: String = "SUCCESS"
    }

    data class PartialSuccess(
        val summaryEn: String,
        val summaryBn: String,
        val reasonEn: String,
        val reasonBn: String
    ) : ActionResult {
        override val outcomeCode: String = "PARTIAL_SUCCESS"
    }

    data class PermissionRequired(
        val error: MeemoError.PermissionDenied
    ) : ActionResult {
        override val outcomeCode: String = "PERMISSION_REQUIRED"
    }

    data class Ambiguous(
        val query: String,
        val candidateApps: List<InstalledAppInfo>
    ) : ActionResult {
        override val outcomeCode: String = "AMBIGUOUS"
    }

    data class RequiresConfirmation(
        val pendingAction: MeemoAction,
        val promptEn: String,
        val promptBn: String
    ) : ActionResult {
        override val outcomeCode: String = "REQUIRES_CONFIRMATION"
    }

    data class Unavailable(
        val error: MeemoError
    ) : ActionResult {
        override val outcomeCode: String = "UNAVAILABLE"
    }

    data class Cancelled(
        val messageEn: String = "Command cancelled.",
        val messageBn: String = "কমান্ড বাতিল করা হয়েছে।"
    ) : ActionResult {
        override val outcomeCode: String = "CANCELLED"
    }

    data class Failed(
        val error: MeemoError
    ) : ActionResult {
        override val outcomeCode: String = "FAILED"
    }
}
