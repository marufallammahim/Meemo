package com.marufallammahim.meemo.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Compose Navigation destinations for Meemo.
 */
@Serializable
sealed interface MeemoDestination {
    @Serializable
    data object Onboarding : MeemoDestination

    @Serializable
    data object AssistantHome : MeemoDestination

    @Serializable
    data object InstalledApps : MeemoDestination

    @Serializable
    data object MemoryAliases : MeemoDestination

    @Serializable
    data object CapabilitiesDiagnostics : MeemoDestination

    @Serializable
    data object PrivacySettings : MeemoDestination

    @Serializable
    data object History : MeemoDestination
}
