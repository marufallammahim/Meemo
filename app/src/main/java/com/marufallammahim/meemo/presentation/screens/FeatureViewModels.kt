package com.marufallammahim.meemo.presentation.screens

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marufallammahim.meemo.apps.AppManager
import com.marufallammahim.meemo.automation.AutomationEngine
import com.marufallammahim.meemo.capabilities.CapabilityManager
import com.marufallammahim.meemo.core.logging.DiagnosticLogEntry
import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.core.security.EncryptedCredentialStore
import com.marufallammahim.meemo.data.database.entity.CapabilitySnapshotEntity
import com.marufallammahim.meemo.data.database.entity.CommandHistoryEntity
import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.model.AppThemeMode
import com.marufallammahim.meemo.domain.model.AutomationRuleModel
import com.marufallammahim.meemo.domain.model.CustomAliasModel
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.memory.AliasManager
import com.marufallammahim.meemo.permissions.PermissionCoordinator
import com.marufallammahim.meemo.permissions.PermissionStatusSummary
import com.marufallammahim.meemo.root.RootCommandSpec
import com.marufallammahim.meemo.root.RootManager
import com.marufallammahim.meemo.root.RootStatus
import com.marufallammahim.meemo.settings.SettingsCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: MeemoRepository
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun selectLanguage(mode: AppLanguageMode) {
        viewModelScope.launch { repository.setLanguageMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingCompleted(true) }
    }
}

@Immutable
data class InstalledAppsUiState(
    val searchQuery: String = "",
    val launchableApps: List<InstalledAppInfo> = emptyList(),
    val browsers: List<InstalledAppInfo> = emptyList(),
    val preferredBrowserPackage: String? = null
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appManager: AppManager,
    private val repository: MeemoRepository
) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")
    private val allAppsFlow = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val browsersFlow = MutableStateFlow<List<InstalledAppInfo>>(emptyList())

    val uiState: StateFlow<InstalledAppsUiState> = combine(
        searchQueryFlow,
        allAppsFlow,
        browsersFlow,
        repository.userPreferencesFlow
    ) { query, apps, browsers, prefs ->
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appLabel.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
        InstalledAppsUiState(
            searchQuery = query,
            launchableApps = filtered,
            browsers = browsers,
            preferredBrowserPackage = prefs.preferredBrowserPackage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstalledAppsUiState())

    init {
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        allAppsFlow.value = appManager.listLaunchableApps()
        browsersFlow.value = appManager.listInstalledBrowsers()
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun launchApp(packageName: String) {
        appManager.launchPackage(packageName)
    }

    fun selectPreferredBrowser(packageName: String?) {
        viewModelScope.launch {
            repository.setPreferredBrowserPackage(packageName)
        }
    }
}

@Immutable
data class MemoryAliasesUiState(
    val aliases: List<CustomAliasModel> = emptyList(),
    val routines: List<AutomationRuleModel> = emptyList(),
    val historyEntries: List<CommandHistoryEntity> = emptyList()
)

@HiltViewModel
class MemoryAliasesViewModel @Inject constructor(
    private val aliasManager: AliasManager,
    private val automationEngine: AutomationEngine,
    private val repository: MeemoRepository
) : ViewModel() {

    val uiState: StateFlow<MemoryAliasesUiState> = combine(
        aliasManager.observeAliases(),
        automationEngine.observeRules(),
        repository.observeCommandHistory(limit = 50)
    ) { aliases, routines, history ->
        MemoryAliasesUiState(
            aliases = aliases,
            routines = routines,
            historyEntries = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryAliasesUiState())

    fun saveCustomAlias(triggerPhrase: String, targetPackageOrApp: String) {
        viewModelScope.launch {
            aliasManager.createOrUpdateAlias(
                triggerPhrase = triggerPhrase,
                targetIntent = "OPEN_APPLICATION",
                targetEntity = targetPackageOrApp
            )
        }
    }

    fun deleteAlias(aliasId: String) {
        viewModelScope.launch { aliasManager.removeAlias(aliasId) }
    }

    fun createSampleRoutine(name: String, triggerPhrase: String) {
        viewModelScope.launch { automationEngine.createDefaultRoutine(name, triggerPhrase) }
    }

    fun deleteRoutine(ruleId: String) {
        viewModelScope.launch { automationEngine.deleteRule(ruleId) }
    }
}

@Immutable
data class CapabilitiesDiagnosticsUiState(
    val capabilitySnapshots: List<CapabilitySnapshotEntity> = emptyList(),
    val plugins: List<PluginRegistryEntity> = emptyList(),
    val permissions: PermissionStatusSummary = PermissionStatusSummary(false, false, false, false),
    val rootStatus: RootStatus = RootStatus.NOT_CHECKED,
    val approvedRootSpecs: List<RootCommandSpec> = emptyList(),
    val diagnosticLogs: List<DiagnosticLogEntry> = emptyList()
)

@HiltViewModel
class CapabilitiesViewModel @Inject constructor(
    private val capabilityManager: CapabilityManager,
    private val permissionCoordinator: PermissionCoordinator,
    private val rootManager: RootManager,
    private val repository: MeemoRepository,
    private val safeLogger: SafeLogger,
    val settingsCoordinator: SettingsCoordinator
) : ViewModel() {

    val uiState: StateFlow<CapabilitiesDiagnosticsUiState> = combine(
        repository.observeCapabilitySnapshots(),
        repository.observePlugins(),
        repository.userPreferencesFlow
    ) { snapshots, plugins, prefs ->
        CapabilitiesDiagnosticsUiState(
            capabilitySnapshots = snapshots,
            plugins = plugins,
            permissions = permissionCoordinator.currentStatus(),
            rootStatus = rootManager.getCurrentRootStatus(prefs.rootFeaturesEnabled),
            approvedRootSpecs = rootManager.getApprovedCommandSpecs(),
            diagnosticLogs = safeLogger.getRecentLogs()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CapabilitiesDiagnosticsUiState())

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val fresh = capabilityManager.generateDiagnosticSnapshots()
            repository.saveCapabilitySnapshots(fresh)
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MeemoRepository,
    private val credentialStore: EncryptedCredentialStore,
    val settingsCoordinator: SettingsCoordinator
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setLanguageMode(mode: AppLanguageMode) {
        viewModelScope.launch { repository.setLanguageMode(mode) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTtsEnabled(enabled) }
    }

    fun setTtsRate(rate: Float) {
        viewModelScope.launch { repository.setTtsSpeechRate(rate) }
    }

    fun setSaveHistory(enabled: Boolean) {
        viewModelScope.launch { repository.setSaveConversationHistory(enabled) }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setWakeWordEnabled(enabled) }
    }

    fun setNotificationReadAloud(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationReadAloud(enabled) }
    }

    fun setNotificationSenderOnly(senderOnly: Boolean) {
        viewModelScope.launch { repository.setNotificationAnnounceSenderOnly(senderOnly) }
    }

    fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        viewModelScope.launch { repository.setQuietHours(enabled, startHour, endHour) }
    }

    fun setRootFeaturesEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setRootFeaturesEnabled(enabled) }
    }

    fun configureAiProvider(enabled: Boolean, endpoint: String, model: String, apiKeyInput: String) {
        viewModelScope.launch {
            if (apiKeyInput.isNotBlank()) {
                credentialStore.saveAiApiKey(apiKeyInput)
            }
            repository.setAiConfiguration(enabled, endpoint, model)
        }
    }

    fun hasSavedAiKey(): Boolean = credentialStore.hasConfiguredAiKey()

    fun clearAiKey() {
        credentialStore.clearAiApiKey()
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            credentialStore.clearAiApiKey()
            repository.purgeAllUserData()
        }
    }
}
