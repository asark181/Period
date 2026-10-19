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

    data class Tip(val title: String, val body: String)

    fun tips(phase: Phase): List<Tip> = when (phase) {
        Phase.MENSTRUAL -> listOf(
            Tip("肚子疼的话", "热水袋敷一敷小肚子，比红糖水管用。实在疼就吃一片布洛芬，记得饭后吃。"),
            Tip("晚上睡得安心", "量多的头两天换夜用款或安睡裤，就不用半夜爬起来啦。"),
            Tip("吃点补血的", "红肉、鸭血、猪肝都不错，再配点橙子，铁更好吸收。"),
            Tip("别勉强运动", "散散步、拉拉伸就够了，这几天好好偷个懒。"),
        )
        Phase.FOLLICULAR -> listOf(
            Tip("状态在变好", "这一周通常是精力最好、皮肤最稳的时候。"),
            Tip("想运动就趁现在", "跑步、跳操、力量训练，这几天恢复快，效果也好。"),
            Tip("试试新护肤品", "皮肤这周最不容易过敏，想换新品可以放在这几天。"),
        )
        Phase.FERTILE, Phase.OVULATION -> listOf(
            Tip("易孕期要小心", "前后大概 10 天。暂时不想要宝宝的话，每次都要做好措施哦。"),
            Tip("一点小信号", "一侧小肚子隐隐作痛、分泌物变多变透明，都是排卵前后的正常现象。"),
            Tip("可以量量体温", "排卵后基础体温会升高 0.3–0.5℃，早上醒来先别起床，量一下最准。"),
        )
        Phase.LUTEAL -> listOf(
            Tip("经前小情绪", "容易水肿、长痘、心情低落，都是激素在作怪，不是你的错。"),
            Tip("想吃甜的", "很正常呀。黑巧克力和坚果比奶茶、蛋糕更不容易长痘。"),
            Tip("提前准备好", "包里放两片卫生巾，到时候就不会手忙脚乱啦。"),
        )
        Phase.LATE -> listOf(
            Tip("晚几天很常见", "熬夜、压力大、换季、体重变化，都可能让大姨妈迟到几天。"),
            Tip("什么时候要留意", "推迟超过 7 天、最近又有同房的话，可以先用验孕棒测一下。"),
        )
        Phase.NONE -> listOf(
            Tip("从下一次开始", "来的那天点一下「月经开始」，走的那天点「月经结束」。记满两次，预测就会按你自己的节奏来算。"),
        )
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

    fun daily(): Pair<String, String> =
        "今天过得怎么样" to "花几秒记一下心情和身体，小月帮你记着。"

    // ---- Record vocabulary ----

    val moods = listOf(
        "happy" to "开心", "calm" to "平静", "energetic" to "元气",
        "sensitive" to "敏感", "irritable" to "烦躁", "sad" to "低落",
        "anxious" to "焦虑", "tired" to "累累的",
    )
    val symptoms = listOf(
        "cramps" to "肚子疼", "backache" to "腰酸", "headache" to "头痛", "breast" to "胸胀",
        "acne" to "长痘", "bloating" to "胀气", "insomnia" to "失眠", "appetite" to "嘴馋",
        "nausea" to "恶心", "diarrhea" to "拉肚子", "constipation" to "便秘", "discharge" to "分泌物多",
        "edema" to "水肿", "cold" to "手脚冰凉", "dizzy" to "头晕",
    )
    val flows = listOf("无", "少", "中", "多", "很多")
    val pains = listOf("不痛", "一点点", "有点痛", "很痛", "疼哭了")
    val loves = listOf("无", "有措施", "无措施")

    fun moodLabel(key: String) = moods.firstOrNull { it.first == key }?.second ?: key
    fun symptomLabel(key: String) = symptoms.firstOrNull { it.first == key }?.second ?: key
}
