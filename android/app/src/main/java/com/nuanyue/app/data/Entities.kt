package com.nuanyue.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One menstrual period. Days are stored as epochDay. `deleted` is a tombstone kept for sync merging. */
@Serializable
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey val id: String,
    val startDay: Long,
    val endDay: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
)

/** Everything she logged for a single day. */
@Serializable
@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val day: Long,
    /** 0 none, 1 少, 2 中, 3 多, 4 很多 */
    val flow: Int = 0,
    /** 0 none .. 4 剧痛 */
    val pain: Int = 0,
    /** comma separated mood keys */
    val moods: String = "",
    /** comma separated symptom keys */
    val symptoms: String = "",
    /** 0 none, 1 有防护, 2 无防护 */
    val love: Int = 0,
    val temperature: Float? = null,
    val weight: Float? = null,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
) {
    val moodList: List<String> get() = moods.split(',').filter { it.isNotBlank() }
    val symptomList: List<String> get() = symptoms.split(',').filter { it.isNotBlank() }

    val isEmpty: Boolean
        get() = flow == 0 && pain == 0 && moods.isBlank() && symptoms.isBlank() && love == 0 &&
            temperature == null && weight == null && note.isBlank()
}
