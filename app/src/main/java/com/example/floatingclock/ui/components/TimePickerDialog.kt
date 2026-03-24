package com.example.floatingclock.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = initialTime
        }
    }

    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var second by remember { mutableIntStateOf(calendar.get(Calendar.SECOND)) }
    var millisecond by remember { mutableIntStateOf(calendar.get(Calendar.MILLISECOND)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "设置目标时间",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 日期选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("日期:", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberPicker(
                            value = month,
                            onValueChange = { month = it },
                            range = 1..12,
                            label = "月"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        NumberPicker(
                            value = day,
                            onValueChange = { day = it },
                            range = 1..31,
                            label = "日"
                        )
                    }
                }

                // 时间选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("时间:", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberPicker(
                            value = hour,
                            onValueChange = { hour = it },
                            range = 0..23,
                            label = "时"
                        )
                        Text(":", modifier = Modifier.padding(horizontal = 2.dp))
                        NumberPicker(
                            value = minute,
                            onValueChange = { minute = it },
                            range = 0..59,
                            label = "分"
                        )
                        Text(":", modifier = Modifier.padding(horizontal = 2.dp))
                        NumberPicker(
                            value = second,
                            onValueChange = { second = it },
                            range = 0..59,
                            label = "秒"
                        )
                    }
                }

                // 毫秒选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("毫秒:", fontWeight = FontWeight.Medium)
                    Slider(
                        value = millisecond.toFloat(),
                        onValueChange = { millisecond = it.toInt() },
                        valueRange = 0f..999f,
                        steps = 998,
                        modifier = Modifier.width(200.dp)
                    )
                    Text(
                        String.format("%03d", millisecond),
                        modifier = Modifier.width(40.dp)
                    )
                }

                // 快速预设
                Text("快速预设:", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(1, 5, 10, 30)
                    presets.forEach { minutes ->
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.MINUTE, minutes)
                                year = cal.get(Calendar.YEAR)
                                month = cal.get(Calendar.MONTH) + 1
                                day = cal.get(Calendar.DAY_OF_MONTH)
                                hour = cal.get(Calendar.HOUR_OF_DAY)
                                minute = cal.get(Calendar.MINUTE)
                                second = cal.get(Calendar.SECOND)
                                millisecond = cal.get(Calendar.MILLISECOND)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+${minutes}分")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(year, month - 1, day, hour, minute, second)
                    cal.set(Calendar.MILLISECOND, millisecond)
                    onConfirm(cal.timeInMillis)
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format("%02d", value),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    val newValue = if (value <= range.first) range.last else value - 1
                    onValueChange(newValue)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", fontSize = 16.sp)
            }
            Text(label, fontSize = 10.sp)
            TextButton(
                onClick = {
                    val newValue = if (value >= range.last) range.first else value + 1
                    onValueChange(newValue)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 16.sp)
            }
        }
    }
}
