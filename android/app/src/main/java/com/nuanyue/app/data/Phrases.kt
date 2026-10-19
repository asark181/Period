package com.nuanyue.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.nuanyue.app.domain.Phase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * A short line shown on the home screen. Categories:
 * P 经期, F 卵泡期, O 易孕/排卵, L 经前/推迟, A 日常, B 给伴侣看的.
 */
@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val text: String,
)

@Dao
interface PhraseDao {
    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(phrases: List<PhraseEntity>)

    @Query("DELETE FROM phrases")
    suspend fun clear()

    @Query("SELECT * FROM phrases WHERE category IN (:categories) AND id != :exclude ORDER BY RANDOM() LIMIT 1")
    suspend fun random(categories: List<String>, exclude: Long): PhraseEntity?
}

class PhraseRepository(private val context: Context, private val dao: PhraseDao) {
    private val seedLock = Mutex()

    /** Imports assets/phrases.txt the first time, or again when the bundled file grows. */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        seedLock.withLock {
            val lines = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).useLines { seq ->
                seq.mapNotNull { line ->
                    val i = line.indexOf('|')
                    if (i <= 0) null else PhraseEntity(category = line.substring(0, i), text = line.substring(i + 1).trim())
                }.filter { it.text.isNotEmpty() }.toList()
            }
            if (dao.count() != lines.size) {
                dao.clear()
                dao.insertAll(lines)
            }
        }
    }

    /** Mostly phase-specific lines, sometimes an everyday one. */
    suspend fun next(phase: Phase, partner: Boolean, exclude: Long): PhraseEntity? = withContext(Dispatchers.IO) {
        ensureSeeded()
        val own = when {
            partner -> "B"
            phase == Phase.MENSTRUAL -> "P"
            phase == Phase.FOLLICULAR -> "F"
            phase == Phase.FERTILE || phase == Phase.OVULATION -> "O"
            phase == Phase.LUTEAL || phase == Phase.LATE -> "L"
            else -> "A"
        }
        val categories = if (Random.nextFloat() < 0.7f) listOf(own) else listOf("A")
        dao.random(categories, exclude) ?: dao.random(listOf("A"), exclude)
    }

    companion object {
        const val ASSET = "phrases.txt"
    }
}
