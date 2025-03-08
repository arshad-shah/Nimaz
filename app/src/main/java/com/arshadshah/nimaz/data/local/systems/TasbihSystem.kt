package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.TasbihTrackerDao
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import java.time.LocalDate
import javax.inject.Inject

class TasbihSystem @Inject constructor(
    private val tasbihTrackerDao: TasbihTrackerDao
) {
    fun updateTasbih(tasbih: LocalTasbih) = tasbihTrackerDao.updateTasbih(tasbih.id, tasbih.count)
    fun updateTasbihGoal(tasbih: LocalTasbih) =
        tasbihTrackerDao.updateTasbihGoal(tasbih.id, tasbih.goal)

    fun saveTasbih(tasbih: LocalTasbih) = tasbihTrackerDao.saveTasbih(tasbih)
    fun getTasbihById(id: Int) = tasbihTrackerDao.getTasbihById(id)
    fun getAllTasbih() = tasbihTrackerDao.getAll()
    fun getTasbihForDate(date: LocalDate) = tasbihTrackerDao.getForDate(date)
    fun deleteTasbih(tasbih: LocalTasbih) = tasbihTrackerDao.deleteTasbih(tasbih)
}
