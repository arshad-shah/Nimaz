package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A line together with its ordered tappable cells. Cells are ordered by
 * `position` by the DAO query that builds the parent relation.
 */
data class QaidaLineWithCells(
    @Embedded val line: QaidaLineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "line_id"
    )
    val cells: List<QaidaCellEntity>
)

/**
 * A full lesson page: the lesson plus its lines, each line with its cells.
 * Returned by `QaidaDao.getLessonWithLinesAndCells`.
 */
data class QaidaLessonWithContent(
    @Embedded val lesson: QaidaLessonEntity,
    @Relation(
        entity = QaidaLineEntity::class,
        parentColumn = "id",
        entityColumn = "lesson_id"
    )
    val lines: List<QaidaLineWithCells>
)
