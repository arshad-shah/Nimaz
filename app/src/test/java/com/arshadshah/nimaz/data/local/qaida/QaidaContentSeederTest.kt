package com.arshadshah.nimaz.data.local.qaida

import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QaidaContentSeederTest {

    private val json = """
        { "contentVersion": 2,
          "lessons": [
            { "id":1,"lesson_number":1,"title_english":"The Letters",
              "title_arabic":"الحروف","title_transliteration":"Al-Huroof",
              "description":"Learn the letters","concept_tags":["letters","isolated"],
              "icon":"🔤","display_order":1 }
          ],
          "letters": [
            { "id":1,"letter_arabic":"ا","name_arabic":"ألف","name_transliteration":"alif",
              "isolated_form":"ا","initial_form":null,"medial_form":null,"final_form":"ـا",
              "is_connecting":false,"makhraj_area":"JAWF","makhraj_detail":"Jawf",
              "phonetic_hint":"like 'a'","audio_key":"letter_alif","display_order":1 }
          ],
          "lines": [
            { "id":101,"lesson_id":1,"line_number":1,"line_type":"PRACTICE",
              "instruction_english":null,"instruction_arabic":null,"display_order":1 }
          ],
          "cells": [
            { "id":1001,"line_id":101,"lesson_id":1,"position":1,"text_arabic":"ا",
              "transliteration":"alif","token_type":"LETTER","audio_key":"letter_alif",
              "highlight_group":null,"letter_id":1,"notes":null }
          ] }
    """.trimIndent()

    private fun seeder(dao: QaidaDao, storedVersion: Int): QaidaContentSeeder {
        val store = object : QaidaContentVersionStore {
            var v = storedVersion
            override suspend fun get() = v
            override suspend fun set(version: Int) { v = version }
        }
        val reader = object : QaidaAssetReader {
            override fun read(path: String): String = json
        }
        return QaidaContentSeeder(dao = dao, versionStore = store, assetReader = reader)
    }

    @Test
    fun seedsWhenEmpty_mapsAllContentTables() = runTest {
        val dao = mockk<QaidaDao>(relaxed = true)
        coEvery { dao.lessonCount() } returns 0
        val lessons = slot<List<QaidaLessonEntity>>()
        val letters = slot<List<QaidaLetterEntity>>()
        val lines = slot<List<QaidaLineEntity>>()
        val cells = slot<List<QaidaCellEntity>>()
        coEvery {
            dao.replaceAllContent(capture(lessons), capture(letters), capture(lines), capture(cells))
        } returns Unit

        seeder(dao, storedVersion = 0).seedIfNeeded()

        assertThat(lessons.captured.map { it.id }).containsExactly(1)
        assertThat(letters.captured.map { it.id }).containsExactly(1)
        assertThat(lines.captured.map { it.id }).containsExactly(101)
        assertThat(cells.captured.map { it.id }).containsExactly(1001)
        // concept_tags are stored JSON-encoded for the repository to parse back.
        assertThat(lessons.captured.first().conceptTags).isEqualTo("[\"letters\",\"isolated\"]")
        // Optional fields map through correctly.
        val letter = letters.captured.first()
        assertThat(letter.initialForm).isNull()
        assertThat(letter.isConnecting).isFalse()
        coVerify(exactly = 1) { dao.replaceAllContent(any(), any(), any(), any()) }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<QaidaDao>(relaxed = true)
        coEvery { dao.lessonCount() } returns 17
        seeder(dao, storedVersion = 2).seedIfNeeded()
        coVerify(exactly = 0) { dao.replaceAllContent(any(), any(), any(), any()) }
    }

    @Test
    fun reseedsWhenContentVersionIncremented() = runTest {
        val dao = mockk<QaidaDao>(relaxed = true)
        coEvery { dao.lessonCount() } returns 17
        seeder(dao, storedVersion = 1).seedIfNeeded() // bundled is 2 > stored 1
        coVerify(exactly = 1) { dao.replaceAllContent(any(), any(), any(), any()) }
    }

    @Test
    fun seedsWhenTablesEmptyEvenIfVersionCurrent() = runTest {
        val dao = mockk<QaidaDao>(relaxed = true)
        coEvery { dao.lessonCount() } returns 0
        seeder(dao, storedVersion = 2).seedIfNeeded() // empty tables force a seed
        coVerify(exactly = 1) { dao.replaceAllContent(any(), any(), any(), any()) }
    }
}
