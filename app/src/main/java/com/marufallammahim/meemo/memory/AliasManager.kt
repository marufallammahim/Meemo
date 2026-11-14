package com.marufallammahim.meemo.memory

import com.marufallammahim.meemo.domain.model.CustomAliasModel
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AliasManager @Inject constructor(
    private val repository: MeemoRepository
) {
    fun observeAliases(): Flow<List<CustomAliasModel>> = repository.observeAliases()

    suspend fun createOrUpdateAlias(
        triggerPhrase: String,
        targetIntent: String,
        targetEntity: String,
        requiresConfirmation: Boolean = false
    ) {
        if (triggerPhrase.isBlank() || targetEntity.isBlank()) return
        repository.saveAlias(
            triggerPhrase = triggerPhrase,
            targetIntent = targetIntent,
            targetEntity = targetEntity,
            requiresConfirmation = requiresConfirmation
        )
    }

    suspend fun removeAlias(aliasId: String) {
        repository.deleteAlias(aliasId)
    }
}
