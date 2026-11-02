package com.nuanyue.app.ui.update

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nuanyue.app.ui.components.PrimaryButton
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.update.UpdatePhase
import com.nuanyue.app.update.UpdateUi

fun updateButtonLabel(phase: UpdatePhase): String = when (phase) {
    UpdatePhase.Available -> "下载并安装"
    UpdatePhase.Downloading -> "取消下载"
    UpdatePhase.Ready -> "安装"
    UpdatePhase.NeedsPermission -> "允许安装"
    UpdatePhase.Failed -> "重试"
}

@Composable
fun UpdateSheet(
    offer: UpdateUi,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    onPage: () -> Unit,
) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.Card,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Palette.PinkLine) },
    ) {
        UpdatePanel(
            offer,
            onPrimary = onPrimary,
            onPage = onPage,
            showNotifyHint = !granted,
            onAskNotify = { if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 12.dp),
        )
    }
}

/** The card and the sheet share this. One button, notes, and a download-page link as the last resort. */
@Composable
fun UpdatePanel(
    offer: UpdateUi,
    onPrimary: () -> Unit,
    onPage: () -> Unit,
    modifier: Modifier = Modifier,
    showNotifyHint: Boolean = false,
    onAskNotify: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Text("新版本 ${offer.versionName}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            offer.notes.ifBlank { "可以在这里下载安装，不用再打开网页。" },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (offer.message.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(offer.message, color = Palette.PinkDeep, fontSize = 13.sp)
        }
        if (offer.phase == UpdatePhase.Downloading) {
            Spacer(Modifier.height(12.dp))
            val fraction = if (offer.total > 0) (offer.received.toFloat() / offer.total.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = Palette.Pink,
                trackColor = Palette.PinkSoft,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (offer.total > 0) "已下载 ${(fraction * 100).toInt()}%" else "正在下载",
                fontSize = 12.sp,
                color = Palette.InkSoft,
            )
        }
        Spacer(Modifier.height(14.dp))
        val cancel = offer.phase == UpdatePhase.Downloading
        PrimaryButton(
            updateButtonLabel(offer.phase),
            onPrimary,
            Modifier.fillMaxWidth(),
            height = 48.dp,
            container = if (cancel) Palette.PinkSoft else Palette.Pink,
            content = if (cancel) Palette.PinkDeep else androidx.compose.ui.graphics.Color.White,
        )
        if (offer.pageUrl.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "打开下载页",
                color = Palette.InkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPage).padding(vertical = 6.dp),
            )
        }
        if (showNotifyHint) {
            Text(
                "开启通知，有新版本时提醒一次",
                color = Palette.Sky,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAskNotify).padding(vertical = 4.dp),
            )
        }
    }
}
