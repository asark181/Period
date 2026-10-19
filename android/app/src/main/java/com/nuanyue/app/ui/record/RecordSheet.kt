package com.nuanyue.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.ui.components.PrimaryButton
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.theme.Palette
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordSheet(
    date: LocalDate,
    initial: DayRecordEntity,
    readOnly: Boolean,
    onDismiss: () -> Unit,
    onSave: (DayRecordEntity) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var flow by remember { mutableStateOf(initial.flow) }
    var pain by remember { mutableStateOf(initial.pain) }
    var moods by remember { mutableStateOf(initial.moodList.toSet()) }
    var symptoms by remember { mutableStateOf(initial.symptomList.toSet()) }
    var love by remember { mutableStateOf(initial.love) }
    var temp by remember { mutableStateOf(initial.temperature?.toString() ?: "") }
    var weight by remember { mutableStateOf(initial.weight?.toString() ?: "") }
    var note by remember { mutableStateOf(initial.note) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Palette.Bg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text("${date.monthValue} 月 ${date.dayOfMonth} 日", style = MaterialTheme.typography.titleLarge)
            if (readOnly) Text("只读", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(18.dp))

            SectionTitle("流量")
            Spacer(Modifier.height(10.dp))
            // A level bar: tapping a step fills everything up to it; tapping the current step clears it.
            Row(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)),
            ) {
                (1..4).forEach { level ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .background(if (flow >= level) Palette.Pink.copy(alpha = 0.25f + 0.25f * level) else Palette.Card)
                            .clickable(enabled = !readOnly) { flow = if (flow == level) 0 else level },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(Copy.flows[level], fontSize = 13.sp, color = if (flow >= level && level >= 2) Color.White else Palette.Ink)
                    }
                    if (level < 4) Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Line))
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("痛经程度")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Copy.pains.forEachIndexed { i, label ->
                    Pill(label, pain == i, { if (!readOnly) pain = i })
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("心情")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Copy.moods.forEach { (key, label) ->
                    Pill(label, key in moods, { if (!readOnly) moods = moods.toggle(key) })
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("身体")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Copy.symptoms.forEach { (key, label) ->
                    Pill(label, key in symptoms, { if (!readOnly) symptoms = symptoms.toggle(key) })
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("同房")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Copy.loves.forEachIndexed { i, label -> Pill(label, love == i, { if (!readOnly) love = i }) }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("体温 / 体重")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(temp, { temp = it }, "体温", "℃", readOnly, Modifier.weight(1f))
                NumberField(weight, { weight = it }, "体重", "kg", readOnly, Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("备注")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note, onValueChange = { if (it.length <= 300) note = it },
                readOnly = readOnly,
                placeholder = { Text("选填", color = Palette.InkFaint) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
            )

            Spacer(Modifier.height(24.dp))
            if (!readOnly) {
                PrimaryButton(
                    "保存",
                    onClick = {
                        onSave(
                            initial.copy(
                                flow = flow, pain = pain,
                                moods = moods.joinToString(","), symptoms = symptoms.joinToString(","),
                                love = love,
                                temperature = temp.toFloatOrNull()?.takeIf { it in 34f..43f },
                                weight = weight.toFloatOrNull()?.takeIf { it in 20f..300f },
                                note = note.trim(),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, unit: String, readOnly: Boolean, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() || it == '.' }) onChange(v) },
        readOnly = readOnly,
        label = { Text(label) },
        suffix = { Text(unit, color = Palette.InkSoft) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
        modifier = modifier,
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Pink,
    unfocusedBorderColor = Palette.Line,
    focusedContainerColor = Palette.Card,
    unfocusedContainerColor = Palette.Card,
    focusedLabelColor = Palette.Pink,
    cursorColor = Palette.Pink,
)

private fun Set<String>.toggle(k: String) = if (k in this) this - k else this + k
