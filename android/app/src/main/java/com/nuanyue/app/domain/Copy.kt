package com.nuanyue.app.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * All user-facing wording. House style: gentle and warm, like someone who cares about her,
 * but always concrete — every line should be something she can actually use. No emoji.
 */
object Copy {
    fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
        in 5..10 -> "早上好呀"
        in 11..13 -> "中午好呀"
        in 14..17 -> "下午好呀"
        in 18..22 -> "晚上好呀"
        else -> "这么晚还没睡呀"
    }

    /** One short line under the greeting, depending on the phase. */
    fun mood(phase: Phase): String = when (phase) {
        Phase.MENSTRUAL -> "这几天辛苦啦，多休息，别碰凉的"
        Phase.FOLLICULAR -> "身体在慢慢恢复，最近状态会越来越好"
        Phase.FERTILE -> "最近皮肤和精神都不错呢"
        Phase.OVULATION -> "今天是排卵日，身体可能有点小变化"
        Phase.LUTEAL -> "快到经期了，情绪起伏一点也没关系"
        Phase.LATE -> "晚几天很常见，先别太担心"
        Phase.NONE -> "来的那天点一下「月经开始」就好"
    }

    private fun md(d: LocalDate) = "${d.monthValue}月${d.dayOfMonth}日"

    // ---- Notifications. `partner` = the boyfriend's phone. ----

    fun beforePeriod(days: Int, date: LocalDate, partner: Boolean): Pair<String, String> =
        if (partner) "她的大姨妈大概 $days 天后来" to "${md(date)}左右。这几天少让她碰凉的，多陪陪她、多让着她一点。"
        else "大姨妈大概 $days 天后来" to "${md(date)}左右。包里先放两片卫生巾，这几天别吃太冰的哦。"

    fun periodToday(partner: Boolean): Pair<String, String> =
        if (partner) "她的大姨妈可能今天来" to "看看家里的热水袋和止痛药还在不在，晚上早点回家。"
        else "大姨妈可能今天来" to "来了的话打开暖月点一下「月经开始」，之后会算得更准。"

    fun late(days: Int, partner: Boolean): Pair<String, String> =
        if (partner) "她的经期推迟了 $days 天" to "先别追问，她自己也会在意的，陪她放松一点。"
        else "大姨妈推迟了 $days 天" to "如果已经来了，记得补记一下。没来也别太焦虑，压力会让它更晚。"

    fun endCheck(day: Int): Pair<String, String> =
        "大姨妈走了吗" to "已经是第 $day 天啦。走了的话点一下「月经结束」。"

    fun fertileStart(partner: Boolean): Pair<String, String> =
        (if (partner) "她今天进入易孕期" else "今天进入易孕期") to "接下来大约 10 天受孕几率比较高。"

    fun ovulation(partner: Boolean): Pair<String, String> =
        (if (partner) "今天是她的排卵日" else "今天是排卵日") to "一个周期里受孕几率最高的一天。"
}
