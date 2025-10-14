package com.example.musicstringstudioapp.ui.practice

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

/**
 * 练习主界面
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 录音权限
    val recordPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }
    
    // 如果需要权限但还没请求，则请求权限
    LaunchedEffect(uiState.needsPermission) {
        if (uiState.needsPermission && !recordPermissionState.status.isGranted) {
            recordPermissionState.launchPermissionRequest()
        }
    }
    
    // 显示错误消息
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // 3秒后自动清除错误消息
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音高检测练习") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：当前检测到的音高信息
            DetectedPitchDisplay(uiState)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 中部：音准指示器
            PitchIndicator(
                centsDeviation = uiState.centsDeviation,
                accuracyLevel = uiState.accuracyLevel,
                isSilent = uiState.isSilent
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部：控制按钮和状态
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 错误消息
                uiState.errorMessage?.let { errorMsg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMsg,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // 录音按钮
                FloatingActionButton(
                    onClick = { viewModel.toggleRecording() },
                    modifier = Modifier.size(72.dp),
                    containerColor = if (uiState.isRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Mic
                        },
                        contentDescription = if (uiState.isRecording) "停止" else "开始",
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (uiState.isRecording) "点击停止" else "点击开始",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 显示检测到的音高信息
 */
@Composable
fun DetectedPitchDisplay(uiState: PracticeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "检测到的音符",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 音符名称
            Text(
                text = uiState.detectedNote,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isSilent) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    when (uiState.accuracyLevel) {
                        "perfect" -> MaterialTheme.colorScheme.primary
                        "good" -> MaterialTheme.colorScheme.tertiary
                        "fair" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 频率
            Text(
                text = if (uiState.detectedFrequency > 0) {
                    "%.1f Hz".format(uiState.detectedFrequency)
                } else {
                    "--- Hz"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 偏差
            if (!uiState.isSilent && uiState.detectedFrequency > 0) {
                Text(
                    text = if (uiState.centsDeviation > 0) {
                        "+%.1f cents".format(uiState.centsDeviation)
                    } else {
                        "%.1f cents".format(uiState.centsDeviation)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (uiState.accuracyLevel) {
                        "perfect" -> MaterialTheme.colorScheme.primary
                        "good" -> MaterialTheme.colorScheme.tertiary
                        "fair" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}
