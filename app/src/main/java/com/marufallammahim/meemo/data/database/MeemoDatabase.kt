package com.marufallammahim.meemo.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.marufallammahim.meemo.data.database.dao.MeemoDao
import com.marufallammahim.meemo.data.database.entity.AssistantPreferenceEntity
import com.marufallammahim.meemo.data.database.entity.AutomationExecutionEntity
import com.marufallammahim.meemo.data.database.entity.AutomationRuleEntity
import com.marufallammahim.meemo.data.database.entity.CapabilitySnapshotEntity
import com.marufallammahim.meemo.data.database.entity.CommandHistoryEntity
import com.marufallammahim.meemo.data.database.entity.ConsentRecordEntity
import com.marufallammahim.meemo.data.database.entity.ConversationMessageEntity
import com.marufallammahim.meemo.data.database.entity.ConversationSessionEntity
import com.marufallammahim.meemo.data.database.entity.CustomCommandAliasEntity
import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity

@Database(
    entities = [
        AssistantPreferenceEntity::class,
        ConversationSessionEntity::class,
        ConversationMessageEntity::class,
        CommandHistoryEntity::class,
        CustomCommandAliasEntity::class,
        AutomationRuleEntity::class,
        AutomationExecutionEntity::class,
        CapabilitySnapshotEntity::class,
        PluginRegistryEntity::class,
        ConsentRecordEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class MeemoDatabase : RoomDatabase() {
    abstract fun meemoDao(): MeemoDao

    companion object {
        const val DATABASE_NAME = "meemo_assistant.db"
    }
}
