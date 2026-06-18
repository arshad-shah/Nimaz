package com.arshadshah.nimaz.data.local.help

import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HelpContentSeederTest {

    private val json = """
        { "contentVersion": 2, "topics": [
          { "id":"t1","order":1,"icon":"schedule","color":"indigo",
            "title":{"en":"Prayer Times","fr":"Horaires"},"subtitle":{"en":"Sub"},
            "items":[
              {"id":"i1","type":"question","order":1,"question":{"en":"Q?"},"answer":{"en":"A."}},
              {"id":"i2","type":"guide","order":2,"icon":"tune","estimatedMinutes":1,
               "title":{"en":"Guide"},
               "steps":[{"id":"s1","order":1,"deeplink":"prayer_settings",
                         "pathLabels":["More","Prayer Settings"],
                         "title":{"en":"Step"},"body":{"en":"Body"}}]}
            ] }
        ] }
    """.trimIndent()

    private fun seeder(dao: HelpDao, storedVersion: Int): HelpContentSeeder {
        val store = object : HelpContentVersionStore {
            var v = storedVersion
            override suspend fun get() = v
            override suspend fun set(version: Int) { v = version }
        }
        val reader = object : HelpAssetReader {
            override fun read(path: String): String = json
        }
        return HelpContentSeeder(
            dao = dao,
            versionStore = store,
            assetReader = reader
        )
    }

    @Test
    fun seedsWhenEmpty_flattensLocaleMapsAndPathLabels() = runTest {
        val dao = mockk<HelpDao>(relaxed = true)
        coEvery { dao.topicCount() } returns 0
        val topics = slot<List<HelpTopicEntity>>()
        val strings = slot<List<HelpStringEntity>>()
        coEvery { dao.insertTopics(capture(topics)) } returns Unit
        coEvery { dao.insertStrings(capture(strings)) } returns Unit

        seeder(dao, storedVersion = 0).seedIfNeeded()

        assertThat(topics.captured.map { it.id }).containsExactly("t1")
        assertThat(
            strings.captured.filter { it.ownerId == "t1" && it.fieldKey == "title" }.map { it.langCode }
        ).containsExactly("en", "fr")
        coVerify { dao.clearStrings(); dao.clearTopics() }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<HelpDao>(relaxed = true)
        coEvery { dao.topicCount() } returns 6
        seeder(dao, storedVersion = 2).seedIfNeeded()
        coVerify(exactly = 0) { dao.insertTopics(any()) }
    }

    @Test
    fun reseedsWhenContentVersionIncremented() = runTest {
        val dao = mockk<HelpDao>(relaxed = true)
        coEvery { dao.topicCount() } returns 6
        seeder(dao, storedVersion = 1).seedIfNeeded() // bundled is 2 > stored 1
        coVerify(exactly = 1) { dao.insertTopics(any()) }
    }
}
