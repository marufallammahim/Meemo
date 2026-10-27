package com.marufallammahim.meemo.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marufallammahim.meemo.data.database.dao.MeemoDao
import com.marufallammahim.meemo.data.database.entity.ConsentRecordEntity
import com.marufallammahim.meemo.data.database.entity.ConversationMessageEntity
import com.marufallammahim.meemo.data.database.entity.ConversationSessionEntity
import com.marufallammahim.meemo.data.database.entity.CustomCommandAliasEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeemoDatabaseTest {

    private lateinit var database: MeemoDatabase
    private lateinit var dao: MeemoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeemoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.meemoDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndResolveCustomAliasAndVerifyCascadePurge() = runBlocking {
        val alias = CustomCommandAliasEntity(
            aliasId = "alias-1",
            triggerPhrase = "work browser",
            targetIntent = "OPEN_APPLICATION",
            targetEntity = "org.mozilla.firefox",
            requiresConfirmation = false,
            isEnabled = true,
            usageCount = 0,
            createdAtMs = 1000L
        )
        dao.upsertAlias(alias)
        val found = dao.findAliasByTrigger("WORK BROWSER")
        assertNotNull(found)
        assertEquals("org.mozilla.firefox", found?.targetEntity)

        val session = ConversationSessionEntity(
            sessionId = "sess-1",
            title = "Torch Test",
            languageCode = "en-US",
            startedAtMs = 1000L,
            lastActiveAtMs = 1005L
        )
        dao.upsertSession(session)
        dao.insertMessage(
            ConversationMessageEntity(
                messageId = "msg-1",
                sessionId = "sess-1",
                senderRole = "USER",
                contentText = "Turn on flashlight",
                detectedIntent = "TORCH_ON",
                executionStatus = "SUCCESS",
                createdAtMs = 1001L
            )
        )
        dao.upsertConsent(
            ConsentRecordEntity(
                consentScope = "NOTIFICATION_READ_ALOUD",
                isGranted = true,
                disclosureVersion = "1.0.0",
                updatedAtMs = 1002L,
                notes = "Granted in test"
            )
        )

        dao.purgeAllUserData()
        assertTrue(dao.observeRecentMessages(10).first().isEmpty())
        assertTrue(dao.getEnabledAliases().isEmpty())
    }
}
