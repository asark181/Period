package com.nuanyue.app.domain

import java.time.LocalTime

/** Warm, caring wording used across the app. */
object Copy {
    fun greeting(nickname: String, now: LocalTime = LocalTime.now()): String {
        val part = when (now.hour) {
            in 5..10 -> "早安"
            in 11..13 -> "午安"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深啦"
        }
        return "$part，$nickname"
    }

    fun subGreeting(phase: Phase): String = when (phase) {
        Phase.MENSTRUAL -> "这几天要好好爱自己，暖暖的最舒服"
        Phase.FOLLICULAR -> "身体在慢慢充满能量，适合做喜欢的事"
        Phase.FERTILE -> "状态正好，皮肤也在发光呢"
        Phase.OVULATION -> "今天是排卵日，元气满满的一天"
        Phase.LUTEAL -> "情绪起伏是正常的，对自己温柔一点"
        Phase.LATE -> "别着急，放松心情，它可能就快来啦"
        Phase.NONE -> "记录第一次经期，开启温柔的陪伴吧"
    }

    data class Tip(val emoji: String, val title: String, val body: String)

    fun tips(phase: Phase): List<Tip> = when (phase) {
        Phase.MENSTRUAL -> listOf(
            Tip("☕", "多喝温热水", "红糖姜茶、桂圆红枣茶都不错，少喝冰饮哦。"),
            Tip("🛌", "早点休息", "保证 8 小时睡眠，熬夜会让经期更难受。"),
            Tip("🔥", "注意保暖", "小肚子贴个暖宝宝，腰腹不要着凉。"),
            Tip("🥗", "补充铁元素", "多吃红肉、菠菜、动物肝脏，补回流失的铁。"),
            Tip("🧘", "温和运动", "散步、拉伸就好，避免剧烈运动。"),
        )
        Phase.FOLLICULAR -> listOf(
            Tip("🏃", "适合运动", "新陈代谢变快，这时运动燃脂效果最好。"),
            Tip("✨", "护肤黄金期", "雌激素上升，皮肤状态好，适合做深层护理。"),
            Tip("📚", "专注力在线", "思维活跃，适合学习和完成重要的事情。"),
        )
        Phase.FERTILE, Phase.OVULATION -> listOf(
            Tip("💗", "易孕期注意", "如果暂时没有宝宝计划，一定要做好防护。"),
            Tip("💧", "多补水", "排卵期可能有轻微腹痛或分泌物增多，属正常现象。"),
            Tip("🍓", "多吃新鲜果蔬", "维生素 C 和 E 让状态更好。"),
        )
        Phase.LUTEAL -> listOf(
            Tip("🍫", "嘴馋很正常", "想吃甜食可以选黑巧克力、坚果，适量就好。"),
            Tip("🧂", "少盐防水肿", "经前容易浮肿，清淡饮食会舒服很多。"),
            Tip("🌙", "照顾好情绪", "经前容易烦躁，听听音乐、和喜欢的人聊聊天。"),
            Tip("🎒", "提前准备", "包里备好卫生巾，有备无患。"),
        )
        Phase.LATE -> listOf(
            Tip("🌿", "放松心情", "压力、作息、饮食变化都会让经期推迟几天。"),
            Tip("🩺", "必要时检查", "推迟超过一周且有同房，可以先用验孕棒看看。"),
            Tip("🎒", "随时准备", "包里放好卫生巾，随时迎接它。"),
        )
        Phase.NONE -> listOf(
            Tip("📅", "开始记录", "点击「大姨妈来了」记录本次经期，越记越准。"),
        )
    }

    // ---- Notifications ----
    fun beforePeriod(days: Int, nickname: String, partner: Boolean): Pair<String, String> =
        if (partner) "TA 的大姨妈 ${days} 天后到访" to "提前准备好红糖水和暖宝宝，这几天对 TA 多一点耐心和拥抱吧 🤍"
        else "${nickname}，大姨妈预计 ${days} 天后到访" to "记得备好卫生巾，少吃冰的、早点睡觉哦 🌸"

    fun periodToday(nickname: String, partner: Boolean): Pair<String, String> =
        if (partner) "TA 的大姨妈可能今天到访" to "今天多关心一下 TA，热水和拥抱都安排上 ☕"
        else "${nickname}，大姨妈可能今天到访" to "来了的话记得在暖月里点一下「大姨妈来了」哦"

    fun late(days: Int, nickname: String, partner: Boolean): Pair<String, String> =
        if (partner) "TA 的经期已推迟 ${days} 天" to "别给 TA 压力，陪 TA 放松一下吧"
        else "大姨妈推迟了 ${days} 天" to "${nickname}别担心，如果已经来了，记得补记一下哦"

    fun endCheck(nickname: String): Pair<String, String> =
        "${nickname}，大姨妈走了吗？" to "如果结束了，记得点一下「大姨妈走了」，预测会更准哦"

    fun fertileStart(partner: Boolean): Pair<String, String> =
        (if (partner) "TA 进入易孕期了" else "今天进入易孕期") to "易孕期约 10 天，如果暂时没有宝宝计划，要做好防护哦"

    fun ovulation(partner: Boolean): Pair<String, String> =
        (if (partner) "今天是 TA 的排卵日" else "今天是排卵日") to "受孕几率最高的一天，请注意哦"

    fun daily(nickname: String): Pair<String, String> =
        "${nickname}，今天感觉怎么样？" to "花 10 秒记录一下心情和身体状态吧 📝"

    val moods = listOf(
        "happy" to "😊 开心", "calm" to "😌 平静", "energetic" to "💪 元气",
        "sensitive" to "🥺 敏感", "irritable" to "😤 烦躁", "sad" to "😢 低落",
        "anxious" to "😰 焦虑", "tired" to "😴 疲惫",
    )
    val symptoms = listOf(
        "cramps" to "腹痛", "backache" to "腰酸", "headache" to "头痛", "breast" to "乳房胀痛",
        "acne" to "长痘痘", "bloating" to "腹胀", "insomnia" to "失眠", "appetite" to "食欲大增",
        "nausea" to "恶心", "diarrhea" to "腹泻", "constipation" to "便秘", "discharge" to "白带增多",
        "edema" to "水肿", "cold" to "手脚冰凉", "dizzy" to "头晕",
    )
    val flows = listOf("无", "少", "中", "多", "很多")
    val pains = listOf("不痛", "微痛", "有点痛", "很痛", "剧痛")
    val loves = listOf("无", "有防护", "无防护")

    fun moodLabel(key: String) = moods.firstOrNull { it.first == key }?.second ?: key
    fun symptomLabel(key: String) = symptoms.firstOrNull { it.first == key }?.second ?: key
}
