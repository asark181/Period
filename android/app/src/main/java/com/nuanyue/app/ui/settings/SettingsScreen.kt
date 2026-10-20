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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.rounded.Cloud
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuanyue.app.BuildConfig
import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.update.UpdatePanel
import com.nuanyue.app.ui.theme.NyColors
import com.nuanyue.app.update.ManualCheck
import com.nuanyue.app.update.UpdateUi
import com.nuanyue.app.update.updateRowModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.nuanyue.app.ui.components.PrimaryButton
import com.nuanyue.app.ui.components.IconBadge
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.SwitchRow
import com.nuanyue.app.ui.components.pressScale
import com.nuanyue.app.ui.theme.Palette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DialogShape = RoundedCornerShape(30.dp)

internal class SettingsHooks(
    val syncing: StateFlow<Boolean> = MutableStateFlow(false),
    val updateSettings: ((AppSettings) -> AppSettings) -> Unit = {},
    val toast: (String) -> Unit = {},
    val testNotification: () -> Unit = {},
    val createInvite: () -> Unit = {},
    val syncNow: () -> Unit = {},
    val exportJson: suspend () -> String = { "{}" },
    val joinInvite: (String, Boolean, (Boolean, String?) -> Unit) -> Unit = { _, _, done -> done(false, null) },
    val logout: () -> Unit = {},
    val clearAll: () -> Unit = {},
    val checkUpdate: () -> Unit = {},
    val clearUpToDate: () -> Unit = {},
    val manualCheck: StateFlow<ManualCheck> = MutableStateFlow(ManualCheck.Idle),
    val updatePrimary: () -> Unit = {},
    val openPage: () -> Unit = {},
)

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel, update: UpdateUi? = null) {
    val ctx = LocalContext.current
    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }
    SettingsPage(
        state,
        update,
        SettingsHooks(
            syncing = vm.syncing,
            updateSettings = vm::updateSettings,
            toast = vm::toast,
            testNotification = vm::testNotification,
            createInvite = vm::createInvite,
            syncNow = vm::syncNow,
            exportJson = vm::exportJson,
            joinInvite = vm::joinInvite,
            logout = vm::syncLogout,
            clearAll = vm::clearAll,
            checkUpdate = vm::checkFromUser,
            clearUpToDate = vm::clearUpToDate,
            manualCheck = vm.manualCheck,
            updatePrimary = { vm.updatePrimary(ctx) },
            openPage = { vm.openUpdatePage(ctx) },
        ),
        notifGranted = notifGranted,
        onAskNotify = { if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    )
}

@Composable
internal fun SettingsPage(
    state: UiState,
    update: UpdateUi?,
    hooks: SettingsHooks,
    notifGranted: Boolean = true,
    onAskNotify: () -> Unit = {},
) {
    val s = state.settings
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val syncing by hooks.syncing.collectAsStateWithLifecycle()
    val manual by hooks.manualCheck.collectAsStateWithLifecycle()
    LaunchedEffect(manual) {
        if (manual is ManualCheck.UpToDate) {
            delay(3_000)
            hooks.clearUpToDate()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        ProfileCard(state) { dialog = "nickname" }
        if (update != null) {
            Spacer(Modifier.height(16.dp))
            SoftCard {
                UpdatePanel(update, onPrimary = hooks.updatePrimary, onPage = hooks.openPage)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Cycle ----
        SoftCard {
            SectionTitle("周期")
            Text("记录不到两次时用这里的数字预测，之后按实际记录计算", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            Stepper("默认周期长度", s.defaultCycle, "天", 20..45, enabled = !s.isPartner) { v -> hooks.updateSettings { it.copy(defaultCycle = v) } }
            Stepper("默认经期天数", s.defaultPeriod, "天", 2..10, enabled = !s.isPartner) { v -> hooks.updateSettings { it.copy(defaultPeriod = v) } }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Reminders ----
        SoftCard {
            SectionTitle("提醒")
            Spacer(Modifier.height(4.dp))
            if (!notifGranted) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(Palette.OrangeSoft)
                        .clickable(onClick = onAskNotify)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Notifications, null, tint = Palette.InkSoft)
                    Spacer(Modifier.width(8.dp))
                    Text("没有通知权限，收不到提醒。点这里开启。", fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
            SwitchRow("开启提醒", "每天在下面的时间检查一次", s.remindEnabled) { v -> hooks.updateSettings { it.copy(remindEnabled = v) } }
            val on = s.remindEnabled
            Row(
                Modifier.fillMaxWidth().clickable(enabled = on) { dialog = "time" }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("提醒时间", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (on) Palette.Ink else Palette.InkFaint)
                Icon(Icons.Rounded.Schedule, null, tint = Palette.Ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                com.nuanyue.app.ui.components.Numeral("%02d:%02d".format(s.remindHour, s.remindMinute), 22.sp)
            }
            SwitchRow("经期将至", "提前 ${s.remindBeforeDays} 天，当天再提醒一次", s.remindBefore, on) { v -> hooks.updateSettings { it.copy(remindBefore = v) } }
            if (s.remindBefore) {
                Stepper("提前天数", s.remindBeforeDays, "天", 1..7, enabled = on) { v -> hooks.updateSettings { it.copy(remindBeforeDays = v) } }
            }
            SwitchRow("经期推迟", "推迟第 1、3、7、14 天", s.remindLate, on) { v -> hooks.updateSettings { it.copy(remindLate = v) } }
            if (!s.isPartner) {
                SwitchRow("问一下结束没有", "超过平均经期还没点「走了」时", s.remindEnd, on) { v -> hooks.updateSettings { it.copy(remindEnd = v) } }
            }
            SwitchRow("易孕期和排卵日", "进入易孕期、排卵日当天", s.remindFertile, on) { v -> hooks.updateSettings { it.copy(remindFertile = v) } }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("发一条试试", false, {
                    if (!notifGranted) onAskNotify() else hooks.testNotification()
                })
                Pill("提醒不准时？", false, { dialog = "battery" })
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Sync ----
        SoftCard {
            SectionTitle("同步和邀请码")
            Spacer(Modifier.height(8.dp))
            if (!s.syncEnabled) {
                Text(
                    "数据先在手机上加密再上传，服务器看不到内容。换手机输入邀请码就能找回；把邀请码发给他，他也能一起看着日子、收到提醒。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("生成邀请码", { hooks.createInvite() }, Modifier.weight(1f), height = 46.dp)
                    PrimaryButton("输入邀请码", { dialog = "sync" }, Modifier.weight(1f), height = 46.dp, container = Palette.PinkSoft, content = Palette.PinkDeep)
                }
            } else {
                val invite = com.nuanyue.app.sync.Crypto.isInviteAccount(s.syncAccount)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Cloud, Palette.Mint, Palette.MintSoft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (invite) {
                            Text("邀请码", style = MaterialTheme.typography.bodySmall)
                            com.nuanyue.app.ui.components.Numeral(
                                com.nuanyue.app.sync.Crypto.displayInvite(s.syncAccount),
                                16.sp,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Palette.MintSoft)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        } else {
                            Text("账号：${s.syncAccount}", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            (if (s.isPartner) "伴侣 · 只读" else "本人") + " · " +
                                if (s.lastSyncAt > 0) "上次同步 ${SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(s.lastSyncAt))}" else "还没同步过",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (syncing) CircularProgressIndicator(Modifier.size(22.dp), color = Palette.Pink, strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("立即同步", false, { hooks.syncNow() })
                    if (invite) {
                        Pill("复制邀请码", false, {
                            val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("暖月邀请码", com.nuanyue.app.sync.Crypto.displayInvite(s.syncAccount)))
                            hooks.toast("邀请码复制好啦")
                        })
                    }
                    Pill("退出", false, { dialog = "logout" })
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Data & about ----
        SoftCard {
            SectionTitle("数据")
            Spacer(Modifier.height(4.dp))
            ActionRow(Icons.Outlined.IosShare, "导出", "JSON", Palette.Sky, Palette.SkySoft) {
                scope.launch {
                    val json = hooks.exportJson()
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, json)
                    ctx.startActivity(Intent.createChooser(send, "导出暖月数据"))
                }
            }
            UpdateCheckRow(manual, BuildConfig.VERSION_NAME, update?.versionName) { hooks.checkUpdate() }
            if (!s.isPartner) {
                ActionRow(Icons.Outlined.Delete, "清空所有记录", "无法恢复", Palette.Pink, Palette.PinkSoft, danger = true) { dialog = "clear" }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "暖月 ${BuildConfig.VERSION_NAME} · 记录保存在这台手机上\n预测仅供参考，不能用来避孕",
            style = MaterialTheme.typography.bodySmall, color = Palette.InkFaint,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }

    when (dialog) {
        "nickname" -> TextDialog("称呼", s.nickname, 10, placeholder = "首页问候时使用，可以留空", onDismiss = { dialog = null }) { v ->
            hooks.updateSettings { it.copy(nickname = v) }
        }
        "time" -> TimeDialog(s.remindHour, s.remindMinute, onDismiss = { dialog = null }) { h, m ->
            hooks.updateSettings { it.copy(remindHour = h, remindMinute = m) }
        }
        "sync" -> SyncDialog(syncing, syncError, onDismiss = { dialog = null; syncError = null }) { code, partner ->
            syncError = null
            hooks.joinInvite(code, partner) { ok, err -> if (ok) dialog = null else syncError = err }
        }
        "logout" -> ConfirmDialog("退出同步？", "本机和云端的数据都会保留。", onDismiss = { dialog = null }) { hooks.logout() }
        "clear" -> ConfirmDialog("清空所有记录？", "经期和每日记录会全部删除，无法恢复。", onDismiss = { dialog = null }) { hooks.clearAll() }
        "battery" -> BatteryDialog(onDismiss = { dialog = null }) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        }
    }
}

@Composable
private fun ProfileCard(state: UiState, onEditName: () -> Unit) {
    val s = state.settings
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onEditName).padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.nuanyue.app.ui.components.Mascot(Modifier.size(76.dp))
        Spacer(Modifier.height(8.dp))
        Text(s.nickname.ifBlank { "给自己起个昵称" }, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(
            if (s.isPartner) "伴侣 · 只能查看，不能修改" else "已记录 ${state.snapshot.periods.size} 次经期",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun Stepper(title: String, value: Int, unit: String, range: IntRange, enabled: Boolean = true, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (enabled) Palette.Ink else Palette.InkFaint)
        StepButton(Icons.Rounded.Remove, enabled && value > range.first) { onChange(value - 1) }
        Row(Modifier.width(64.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            com.nuanyue.app.ui.components.Numeral("$value", 24.sp)
            Text(" $unit", fontSize = 12.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 3.dp))
        }
        StepButton(Icons.Rounded.Add, enabled && value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.88f)
    Box(
        Modifier.size(32.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (enabled) Palette.PinkSoft else Palette.Line)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) Palette.PinkDeep else Palette.InkFaint, modifier = Modifier.size(18.dp)) }
}

@Composable
internal fun UpdateCheckRow(
    manual: ManualCheck,
    versionName: String,
    newerName: String?,
    onCheck: () -> Unit,
) {
    val row = updateRowModel(manual, versionName, newerName)
    ActionRow(
        Icons.Outlined.Download,
        "检查更新",
        row.hint.orEmpty(),
        Palette.Mint,
        Palette.MintSoft,
        hintColor = if (row.error) NyColors.RoseStrong else NyColors.InkSecondary,
        enabled = row.clickable,
        showSpinner = row.spinner,
        onClick = onCheck,
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    hint: String,
    tint: Color,
    bg: Color,
    danger: Boolean = false,
    hintColor: Color = Palette.InkSoft,
    enabled: Boolean = true,
    showSpinner: Boolean = false,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.98f)
    Row(
        Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, bg, 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) Palette.Pink else Palette.Ink,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (showSpinner) {
            CircularProgressIndicator(Modifier.size(16.dp), color = NyColors.InkSecondary, strokeWidth = 2.dp)
        } else {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = hintColor, maxLines = 1)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.InkFaint)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Pink, unfocusedBorderColor = Palette.Line,
    focusedLabelColor = Palette.Pink, cursorColor = Palette.Pink,
)

@Composable
private fun TextDialog(title: String, initial: String, max: Int, placeholder: String = "", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        shape = DialogShape,
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                text, { if (it.length <= max) text = it },
                placeholder = { Text(placeholder, color = Palette.InkFaint) },
                shape = RoundedCornerShape(16.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton({ onSave(text.trim()); onDismiss() }) { Text("保存", color = Palette.Pink, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    val st = rememberTimePickerState(hour, minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        shape = DialogShape,
        tonalElevation = 0.dp,
        title = { Text("提醒时间", style = MaterialTheme.typography.titleLarge) },
        text = {
            TimePicker(
                st,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Palette.Panel, selectorColor = Palette.Ink,
                    timeSelectorSelectedContainerColor = Palette.Panel, timeSelectorSelectedContentColor = Palette.Ink,
                    timeSelectorUnselectedContainerColor = Palette.Bg,
                ),
            )
        },
        confirmButton = { TextButton({ onSave(st.hour, st.minute); onDismiss() }) { Text("确定", color = Palette.Pink, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun SyncDialog(syncing: Boolean, error: String?, onDismiss: () -> Unit, onSubmit: (String, Boolean) -> Unit) {
    var code by remember { mutableStateOf("") }
    var partner by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        shape = DialogShape,
        tonalElevation = 0.dp,
        title = { Text("输入邀请码", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("我是本人", !partner, { partner = false })
                    Pill("我是伴侣", partner, { partner = true }, color = Palette.Sky)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (partner) "只能查看，不能修改。她的经期提醒也会发到这台手机上。"
                    else "接上之后，这台手机上的记录会和云端的合在一起。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    code, { code = it.take(20) },
                    placeholder = { Text("NY-XXXX-XXXX-XXXX", color = Palette.InkFaint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(16.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Palette.PinkDeep, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton({ onSubmit(code, partner) }, enabled = !syncing && code.isNotBlank()) {
                if (syncing) CircularProgressIndicator(Modifier.size(18.dp), color = Palette.Pink, strokeWidth = 2.dp)
                else Text("连接", color = Palette.Pink, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        shape = DialogShape,
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }) { Text("确定", color = Palette.Pink, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Palette.InkSoft) } },
    )
}

@Composable
private fun BatteryDialog(onDismiss: () -> Unit, onOpen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        shape = DialogShape,
        tonalElevation = 0.dp,
        icon = { Icon(Icons.Rounded.BatteryChargingFull, null, tint = Palette.Pink) },
        title = { Text("提醒不准时", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "小米、华为、OPPO、vivo 等手机为了省电，可能会拦下后台提醒。在应用设置里：\n\n" +
                    "1. 打开「自启动」\n2. 耗电管理选「无限制」\n3. 通知里打开「横幅」和「锁屏显示」",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton({ onOpen(); onDismiss() }) { Text("去设置", color = Palette.Pink, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onDismiss) { Text("好", color = Palette.InkSoft) } },
    )
}
