package com.marufallammahim.meemo.plugins

import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginRegistryManager @Inject constructor(
    private val repository: MeemoRepository
) {
    fun observeRegisteredPlugins(): Flow<List<PluginRegistryEntity>> = repository.observePlugins()
}
