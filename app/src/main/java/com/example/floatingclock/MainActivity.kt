package com.example.floatingclock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.floatingclock.service.FloatingClockService
import com.example.floatingclock.ui.theme.FloatingClockTheme
import com.example.floatingclock.ui.components.TimePickerDialog
import com.example.floatingclock.ui.components.AlertSettingsDialog
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestOverlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndStartService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FloatingClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartFloating = { checkAndStartService() }
                    )
                }
            }
        }
    }

    private fun checkAndStartService() {
        if (Settings.canDrawOverlays(this)) {
            startFloatingService()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            requestOverlayPermission.launch(intent)
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingClockService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onStartFloating: () -> Unit) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showAlertSettings by remember { mutableStateOf(false) }
    var isCountdownMode by remember { mutableStateOf(false) }
    var targetTimeMillis by remember { mutableStateOf(System.currentTimeMillis() + 60000) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "悬浮时钟",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 模式选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "显示模式",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row {
                        FilterChip(
                            selected = !isCountdownMode,
                            onClick = { isCountdownMode = false },
                            label = { Text("当前时间") },
                            leadingIcon = if (!isCountdownMode) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        FilterChip(
                            selected = isCountdownMode,
                            onClick = { isCountdownMode = true },
                            label = { Text("倒计时") },
                            leadingIcon = if (isCountdownMode) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    if (isCountdownMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sdf = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())
                            Text("设置目标时间: ${sdf.format(java.util.Date(targetTimeMillis))}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提醒设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "提醒设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "提示音、震动、提前提醒",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    OutlinedButton(onClick = { showAlertSettings = true }) {
                        Text("设置")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 启动按钮
            Button(
                onClick = onStartFloating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "启动悬浮时钟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "首次使用需要授予悬浮窗权限",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }

    // 时间选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = targetTimeMillis,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                targetTimeMillis = time
                showTimePicker = false
            }
        )
    }

    // 提醒设置对话框
    if (showAlertSettings) {
        AlertSettingsDialog(
            onDismiss = { showAlertSettings = false },
            onConfirm = { /* 保存设置 */ }
        )
    }
}
