package com.nuanyue.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuanyue.app.BuildConfig
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.GradientButton
import com.nuanyue.app.ui.components.IconBadge
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.SwitchRow
import com.nuanyue.app.ui.theme.Palette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) {
    val s = state.settings
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val syncing by vm.syncing.collectAsStateWithLifecycle()

    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        ProfileCard(state) { dialog = "nickname" }
        Spacer(Modifier.height(16.dp))

        // ---- Cycle ----
        SoftCard {
            SectionTitle("周期设置")
            Text("记录少于 2 次时使用这里的数值预测，之后会根据实际记录自动学习", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            Stepper("默认周期长度", s.defaultCycle, "天", 20..45, enabled = !s.isPartner) { v -> vm.updateSettings { it.copy(defaultCycle = v) } }
            Stepper("默认经期天数", s.defaultPeriod, "天", 2..10, enabled = !s.isPartner) { v -> vm.updateSettings { it.copy(defaultPeriod = v) } }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Reminders ----
        SoftCard {
            SectionTitle("贴心提醒")
            Spacer(Modifier.height(4.dp))
            if (!notifGranted) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(Palette.LemonSoft)
                        .clickable { if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Notifications, null, tint = Palette.Peach)
                    Spacer(Modifier.width(8.dp))
                    Text("还没有通知权限，点我开启，才能收到提醒哦", fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
            SwitchRow("开启提醒", "每天在固定时间检查是否需要提醒", s.remindEnabled) { v -> vm.updateSettings { it.copy(remindEnabled = v) } }
            val on = s.remindEnabled
            Row(
                Modifier.fillMaxWidth().clickable(enabled = on) { dialog = "time" }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("提醒时间", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (on) Palette.Ink else Palette.InkFaint)
                Icon(Icons.Rounded.Schedule, null, tint = Palette.Coral, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("%02d:%02d".format(s.remindHour, s.remindMinute), color = Palette.CoralDeep, fontWeight = FontWeight.Bold)
            }
            SwitchRow("经期将至", "提前 ${s.remindBeforeDays} 天提醒，当天再提醒一次", s.remindBefore, on) { v -> vm.updateSettings { it.copy(remindBefore = v) } }
            if (s.remindBefore) {
                Stepper("提前天数", s.remindBeforeDays, "天", 1..7, enabled = on) { v -> vm.updateSettings { it.copy(remindBeforeDays = v) } }
            }
            SwitchRow("经期推迟", "推迟 1、3、7、14 天时提醒", s.remindLate, on) { v -> vm.updateSettings { it.copy(remindLate = v) } }
            if (!s.isPartner) {
                SwitchRow("经期结束确认", "超过平均经期还没标记结束时提醒", s.remindEnd, on) { v -> vm.updateSettings { it.copy(remindEnd = v) } }
            }
            SwitchRow("易孕期 & 排卵日", "进入易孕期和排卵日当天提醒", s.remindFertile, on) { v -> vm.updateSettings { it.copy(remindFertile = v) } }
            if (!s.isPartner) {
                SwitchRow("每日记录", "没有其他提醒的日子，提醒记录心情", s.remindDaily, on) { v -> vm.updateSettings { it.copy(remindDaily = v) } }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("发送测试通知", false, {
                    if (!notifGranted && Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.testNotification()
                })
                Pill("防止提醒被杀", false, { dialog = "battery" }, color = Palette.Peach)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Love note ----
        SoftCard(onClick = { dialog = "note" }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Favorite, Palette.CoralDeep, Palette.RoseSoft)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("首页小纸条", style = MaterialTheme.typography.titleSmall)
                    Text(s.loveNote.ifBlank { "写一句暖心的话，每天打开都能看到" }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.InkFaint)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Sync ----
        SoftCard {
            SectionTitle("云同步 & 伴侣守护")
            Spacer(Modifier.height(8.dp))
            if (!s.syncEnabled) {
                Text(
                    "开启后数据会加密备份到云端，换手机也不怕丢。另一半也可以用同一账号以「伴侣模式」登录，只读查看你的周期并同步收到提醒。数据在手机上端到端加密，服务器无法查看内容。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                GradientButton("开启云同步", { dialog = "sync" }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Cloud, height = 46.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Cloud, Palette.Mint, Palette.MintSoft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("账号：${s.syncAccount}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            (if (s.isPartner) "伴侣模式 · 只读" else "本人记录") + " · " +
                                if (s.lastSyncAt > 0) "上次同步 ${SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(s.lastSyncAt))}" else "尚未同步",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (syncing) CircularProgressIndicator(Modifier.size(22.dp), color = Palette.Coral, strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("立即同步", false, { vm.syncNow() })
                    Pill("退出同步", false, { dialog = "logout" }, color = Palette.InkSoft)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Data & about ----
        SoftCard {
            SectionTitle("数据与关于")
            Spacer(Modifier.height(4.dp))
            ActionRow(Icons.Rounded.IosShare, "导出数据", "以 JSON 格式分享备份") {
                scope.launch {
                    val json = vm.exportJson()
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, json)
                    ctx.startActivity(Intent.createChooser(send, "导出暖月数据"))
                }
            }
            ActionRow(Icons.Rounded.Download, "检查更新", "当前版本 v${BuildConfig.VERSION_NAME}") {
                scope.launch {
                    val r = vm.checkUpdate()
                    val v = r.getOrNull()
                    when {
                        v == null -> vm.toast("检查失败：${r.exceptionOrNull()?.message ?: "网络异常"}")
                        v.versionCode > BuildConfig.VERSION_CODE -> {
                            vm.toast("发现新版本 v${v.versionName}，正在打开下载页")
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(v.url)))
                        }
                        else -> vm.toast("已经是最新版本啦")
                    }
                }
            }
            if (!s.isPartner) {
                ActionRow(Icons.Rounded.DeleteOutline, "清空所有记录", "谨慎操作，无法恢复", danger = true) { dialog = "clear" }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "暖月 v${BuildConfig.VERSION_NAME} · 用心记录每一个温柔的日子 ♡\n预测仅供参考，不能作为避孕或医疗依据",
            style = MaterialTheme.typography.bodySmall, color = Palette.InkFaint,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }

    when (dialog) {
        "nickname" -> TextDialog("怎么称呼你呢", s.nickname, 10, onDismiss = { dialog = null }) { v ->
            vm.updateSettings { it.copy(nickname = v.ifBlank { "宝贝" }) }
        }
        "note" -> TextDialog("首页小纸条", s.loveNote, 60, placeholder = "例如：今天也要开开心心的，我一直都在 ♡", onDismiss = { dialog = null }) { v ->
            vm.updateSettings { it.copy(loveNote = v) }
        }
        "time" -> TimeDialog(s.remindHour, s.remindMinute, onDismiss = { dialog = null }) { h, m ->
            vm.updateSettings { it.copy(remindHour = h, remindMinute = m) }
        }
        "sync" -> SyncDialog(syncing, syncError, onDismiss = { dialog = null; syncError = null }) { account, pwd, partner ->
            syncError = null
            vm.syncLogin(account, pwd, partner) { ok, err -> if (ok) dialog = null else syncError = err }
        }
        "logout" -> ConfirmDialog("退出云同步？", "本机数据会保留，云端数据也不会删除。", onDismiss = { dialog = null }) { vm.syncLogout() }
        "clear" -> ConfirmDialog("清空所有记录？", "所有经期和每日记录都会被删除，无法恢复哦。", onDismiss = { dialog = null }) { vm.clearAll() }
        "battery" -> BatteryDialog(onDismiss = { dialog = null }) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        }
    }
}

@Composable
private fun ProfileCard(state: UiState, onEditName: () -> Unit) {
    val s = state.settings
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Palette.heroGradient).clickable(onClick = onEditName).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            Text(if (s.isPartner) "🛡️" else "🌸", fontSize = 28.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.nickname, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                if (s.isPartner) "伴侣守护模式 · 只读查看" else "已陪伴你记录 ${state.snapshot.periods.size} 次经期",
                color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp,
            )
        }
        Text("编辑", color = Color.White, fontSize = 13.sp, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

@Composable
private fun Stepper(title: String, value: Int, unit: String, range: IntRange, enabled: Boolean = true, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (enabled) Palette.Ink else Palette.InkFaint)
        StepButton(Icons.Rounded.Remove, enabled && value > range.first) { onChange(value - 1) }
        Text("$value $unit", fontWeight = FontWeight.Bold, color = Palette.CoralDeep, modifier = Modifier.width(60.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepButton(Icons.Rounded.Add, enabled && value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(if (enabled) Palette.RoseSoft else Palette.Line).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) Palette.CoralDeep else Palette.InkFaint, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, if (danger) Palette.CoralDeep else Palette.Peach, if (danger) Palette.RoseSoft else Palette.PeachSoft, 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (danger) Palette.CoralDeep else Palette.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.InkFaint)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Coral, unfocusedBorderColor = Palette.Line,
    focusedLabelColor = Palette.Coral, cursorColor = Palette.Coral,
)

@Composable
private fun TextDialog(title: String, initial: String, max: Int, placeholder: String = "", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                text, { if (it.length <= max) text = it },
                placeholder = { Text(placeholder, color = Palette.InkFaint) },
                shape = RoundedCornerShape(16.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton({ onSave(text.trim()); onDismiss() }) { Text("保存", color = Palette.CoralDeep, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    val st = rememberTimePickerState(hour, minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("每天几点提醒你", style = MaterialTheme.typography.titleLarge) },
        text = {
            TimePicker(
                st,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Palette.RoseMist, selectorColor = Palette.Coral,
                    timeSelectorSelectedContainerColor = Palette.RoseSoft, timeSelectorSelectedContentColor = Palette.CoralDeep,
                    timeSelectorUnselectedContainerColor = Palette.Cream,
                ),
            )
        },
        confirmButton = { TextButton({ onSave(st.hour, st.minute); onDismiss() }) { Text("确定", color = Palette.CoralDeep, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun SyncDialog(syncing: Boolean, error: String?, onDismiss: () -> Unit, onSubmit: (String, String, Boolean) -> Unit) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var partner by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("开启云同步", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("🌸 我自己记录", !partner, { partner = false })
                    Pill("🛡️ 伴侣查看", partner, { partner = true }, color = Palette.Lavender)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (partner) "输入 TA 设置的同步账号和密码，即可只读查看 TA 的周期，并收到经期提醒。"
                    else "第一次使用会自动创建账号。请记住密码：它用于加密数据，忘记后无法找回云端数据。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    account, { account = it.take(32) }, label = { Text("同步账号（自定义，≥3 位）") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    password, { password = it.take(64) }, label = { Text("密码（≥6 位）") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(16.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Palette.CoralDeep, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton({ onSubmit(account, password, partner) }, enabled = !syncing) {
                if (syncing) CircularProgressIndicator(Modifier.size(18.dp), color = Palette.Coral, strokeWidth = 2.dp)
                else Text("连接", color = Palette.CoralDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }) { Text("确定", color = Palette.CoralDeep, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun BatteryDialog(onDismiss: () -> Unit, onOpen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        icon = { Icon(Icons.Rounded.BatteryChargingFull, null, tint = Palette.Peach) },
        title = { Text("让提醒更准时", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "部分手机（小米、华为、OPPO、vivo 等）会为了省电拦截后台提醒。建议在应用设置中：\n\n" +
                    "1. 允许「自启动」\n2. 电池/耗电管理设为「无限制」或「允许后台活动」\n3. 通知中开启「横幅」和「锁屏显示」",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton({ onOpen(); onDismiss() }) { Text("去设置", color = Palette.CoralDeep, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("知道了", color = Palette.InkSoft) } },
    )
}
