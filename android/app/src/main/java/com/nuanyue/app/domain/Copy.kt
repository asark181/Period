package com.nuanyue.app.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * All user-facing wording. House style: short, concrete, no emoji, no cutesy particles.
 * Say something she can act on, or say nothing.
 */
object Copy {
    fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }

    data class Tip(val title: String, val body: String)

    fun tips(phase: Phase): List<Tip> = when (phase) {
        Phase.MENSTRUAL -> listOf(
            Tip("痛经", "热水袋敷小腹比红糖水管用。疼得厉害就吃布洛芬，饭后吃。"),
            Tip("量多的头两天", "晚上换长一点的夜用款或安睡裤，睡得踏实些。"),
            Tip("补铁", "红肉、鸭血、猪肝都可以，配点维生素 C 吸收更好。"),
            Tip("运动", "散步和拉伸就够了，这几天不用勉强自己。"),
        )
        Phase.FOLLICULAR -> listOf(
            Tip("状态回升", "雌激素在升高，这一周通常精力最好、皮肤也最稳定。"),
            Tip("适合运动", "力量训练和跑步放在这几天，恢复快，效果也好。"),
            Tip("护肤", "想试新的护肤品，这周最不容易过敏。"),
        )
        Phase.FERTILE, Phase.OVULATION -> listOf(
            Tip("易孕期", "前后大约 10 天。没有备孕计划的话，这段时间每次都要做措施。"),
            Tip("排卵的信号", "一侧小腹隐痛、分泌物变多变清，都是排卵前后的正常现象。"),
            Tip("量体温", "排卵后基础体温会升高 0.3–0.5℃。早上醒来、起床前量最准。"),
        )
        Phase.LUTEAL -> listOf(
            Tip("经前一周", "容易水肿、长痘、情绪低落，少吃太咸太甜的会好受一些。"),
            Tip("想吃甜的", "很正常。黑巧克力和坚果比奶茶、蛋糕更合适。"),
            Tip("提前准备", "包里放两片卫生巾，到时候不慌。"),
        )
        Phase.LATE -> listOf(
            Tip("晚几天很常见", "熬夜、压力、换季、体重变化，都可能让经期推迟。"),
            Tip("什么时候要留意", "推迟超过 7 天、最近有过同房，可以先用验孕棒测一下。"),
        )
        Phase.NONE -> listOf(
            Tip("从下一次开始", "来的那天点一下「来了」。记满两次，预测就会按你自己的周期来算。"),
        )
    }

    private fun md(d: LocalDate) = "${d.monthValue} 月 ${d.dayOfMonth} 日"

    // ---- Notifications. `partner` = the boyfriend's phone. ----

    fun beforePeriod(days: Int, date: LocalDate, partner: Boolean): Pair<String, String> =
        if (partner) "她的经期预计 $days 天后开始" to "${md(date)}左右。这几天少让她碰凉的，多担待一点。"
        else "经期预计 $days 天后开始" to "${md(date)}左右。包里可以先放两片卫生巾。"

    fun periodToday(partner: Boolean): Pair<String, String> =
        if (partner) "她的经期可能今天开始" to "看看家里的热水袋和止痛药还在不在。"
        else "经期可能今天开始" to "来了的话打开暖月点一下，之后的预测会更准。"

    fun late(days: Int, partner: Boolean): Pair<String, String> =
        if (partner) "她的经期推迟了 $days 天" to "先别追问，她自己也会在意。"
        else "经期推迟了 $days 天" to "如果已经来了，记得补记一下。"

    fun endCheck(day: Int): Pair<String, String> =
        "经期结束了吗" to "已经是第 $day 天了。结束了就点一下「走了」。"

    fun fertileStart(partner: Boolean): Pair<String, String> =
        (if (partner) "她今天进入易孕期" else "今天进入易孕期") to "接下来大约 10 天受孕几率较高。"

    fun ovulation(partner: Boolean): Pair<String, String> =
        (if (partner) "今天是她的排卵日" else "今天是排卵日") to "一个周期里受孕几率最高的一天。"

    fun daily(): Pair<String, String> =
        "记一下今天" to "心情、身体有什么变化，花几秒就好。"

    // ---- Record vocabulary ----

    val moods = listOf(
        "happy" to "开心", "calm" to "平静", "energetic" to "有精神",
        "sensitive" to "敏感", "irritable" to "烦躁", "sad" to "低落",
        "anxious" to "焦虑", "tired" to "疲惫",
    )
    val symptoms = listOf(
        "cramps" to "腹痛", "backache" to "腰酸", "headache" to "头痛", "breast" to "乳房胀痛",
        "acne" to "长痘", "bloating" to "腹胀", "insomnia" to "失眠", "appetite" to "食欲大",
        "nausea" to "恶心", "diarrhea" to "腹泻", "constipation" to "便秘", "discharge" to "分泌物多",
        "edema" to "水肿", "cold" to "手脚冰凉", "dizzy" to "头晕",
    )
    val flows = listOf("无", "少", "中", "多", "很多")
    val pains = listOf("不痛", "轻微", "有点痛", "很痛", "疼得厉害")
    val loves = listOf("无", "有措施", "无措施")

    fun moodLabel(key: String) = moods.firstOrNull { it.first == key }?.second ?: key
    fun symptomLabel(key: String) = symptoms.firstOrNull { it.first == key }?.second ?: key
}
