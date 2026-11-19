package com.symphine.gamespace.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class OperationStatus(val label: String, val isTerminal: Boolean = true) {
    AVAILABLE("Available", false),
    VERIFIED("Verified", true),
    UNSUPPORTED("Unsupported", true),
    REQUIRES_PERMISSION("Requires Permission", false),
    REQUIRES_SHIZUKU("Requires Shizuku", false),
    UNKNOWN("Unknown", false),
    PENDING("Pending", false),
    APPLYING("Applying", false),
    APPLIED("Applied", true),
    FAILED("Failed", true),
    RESTORING("Restoring", false),
    RESTORED("Restored", true),
    ROLLBACK_FAILED("Rollback Failed", true)
}

@Serializable
enum class RiskLevel(val label: String) {
    SAFE("Safe / Read-Only"),
    LOW("Low Risk (Cosmetic / Local)"),
    MODERATE("Moderate (System Setting)"),
    HIGH("Elevated (Display / Resolution)")
}

@Serializable
data class OperationRecord(
    val id: String,
    val operationName: String,
    val targetPackage: String? = null,
    val settingKey: String? = null,
    val originalValue: String? = null,
    val requestedValue: String? = null,
    val verifiedValue: String? = null,
    val status: OperationStatus,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Serializable
data class OperationPreview(
    val operationId: String,
    val title: String,
    val description: String,
    val targetPackage: String? = null,
    val changes: List<SettingChangePreview>,
    val compatibilityStatus: OperationStatus,
    val riskLevel: RiskLevel,
    val warning: String? = null,
    val restoreInstructions: String
)

@Serializable
data class SettingChangePreview(
    val key: String,
    val namespace: String,
    val currentValue: String?,
    val targetValue: String
)
