package com.example.writingpractice.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.core.content.ContextCompat
import com.example.writingpractice.BuildConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setNotificationEnabled(granted) }

    fun onNotificationToggle(enable: Boolean) {
        if (!enable) { viewModel.setNotificationEnabled(false); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.setNotificationEnabled(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.setNotificationEnabled(true)
        }
    }

    // Local state for text field — no DataStore round-trip while typing
    var localApiKey by rememberSaveable { mutableStateOf("") }

    // One-time direct DataStore read to bypass stateIn's empty initial value
    LaunchedEffect(Unit) {
        localApiKey = viewModel.loadApiKey()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // imePadding BEFORE verticalScroll so the scroll area shrinks when keyboard appears
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Daily goal
            SectionHeader("일일 목표")
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("하루 문제 수", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${state.dailyGoal}문제",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = state.dailyGoal.toFloat(),
                    onValueChange = { viewModel.setDailyGoal(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28
                )
            }

            HorizontalDivider()

            // Notification
            SectionHeader("알림 설정")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("푸시 알림", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.notificationEnabled,
                    onCheckedChange = ::onNotificationToggle
                )
            }

            if (state.notificationEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "알림 시각: %02d:%02d".format(state.notificationHour, state.notificationMinute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> viewModel.setNotificationTime(h, m) },
                                state.notificationHour,
                                state.notificationMinute,
                                true
                            ).show()
                        }
                    ) {
                        Text("시간 변경")
                    }
                }
                Button(
                    onClick = { viewModel.sendTestNotification() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("테스트 알림 보내기")
                }
            }

            HorizontalDivider()

            // API Key
            SectionHeader("Claude API 설정")
            Text(
                "문제 채점 및 새 문제 생성을 위해 Anthropic API 키가 필요합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = localApiKey,
                onValueChange = { localApiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // apiKeyVisible comes from ViewModel — now properly reactive via nested combine
                visualTransformation = if (state.apiKeyVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                        Icon(
                            if (state.apiKeyVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (state.apiKeyVisible) "숨기기" else "보기"
                        )
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveAndValidateApiKey(localApiKey)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKeyStatus !is ApiKeyStatus.Checking
            ) {
                if (apiKeyStatus is ApiKeyStatus.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("저장 및 검증")
                }
            }

            when (val status = apiKeyStatus) {
                is ApiKeyStatus.Valid -> Text(
                    "API 키가 유효합니다.",
                    color = Color(0xFF388E3C),
                    style = MaterialTheme.typography.bodySmall
                )
                is ApiKeyStatus.Invalid -> Text(
                    "API 키 오류: ${status.reason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                else -> Unit
            }

            HorizontalDivider()

            // App update
            SectionHeader("앱 업데이트")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "현재 버전: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { viewModel.checkForUpdate() },
                    enabled = updateState !is UpdateState.Checking &&
                            updateState !is UpdateState.Downloading
                ) {
                    Text("업데이트 확인")
                }
            }

            when (val us = updateState) {
                UpdateState.Checking -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("확인 중...", style = MaterialTheme.typography.bodySmall)
                }

                UpdateState.UpToDate -> Text(
                    "최신 버전입니다.",
                    color = Color(0xFF388E3C),
                    style = MaterialTheme.typography.bodySmall
                )

                is UpdateState.UpdateAvailable -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "새 버전 ${us.version}이(가) 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { viewModel.downloadAndInstall(us.downloadUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다운로드 및 설치")
                    }
                }

                is UpdateState.Downloading -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val p = us.progress
                    if (p < 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("다운로드 준비 중...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LinearProgressIndicator(
                            progress = { p },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "다운로드 중... ${(p * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                is UpdateState.ReadyToInstall -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "다운로드 완료. 설치 창이 열립니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF388E3C)
                    )
                    OutlinedButton(
                        onClick = { viewModel.retryInstall(us.apkPath) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("설치 창 다시 열기")
                    }
                }

                is UpdateState.NeedInstallPermission -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "설치 권한이 필요합니다.\n설정에서 '알 수 없는 앱 설치'를 허용한 후 아래 버튼을 누르세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.retryInstall(us.apkPath) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("설치하기")
                    }
                }

                is UpdateState.Error -> Text(
                    "오류: ${us.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                UpdateState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
