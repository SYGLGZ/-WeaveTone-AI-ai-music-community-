package com.example.myfirstapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myfirstapp.R
import com.example.myfirstapp.ui.theme.BlackBase
import com.example.myfirstapp.ui.theme.Divider
import com.example.myfirstapp.ui.theme.TextPrimary
import com.example.myfirstapp.ui.theme.TextSecondary
import com.example.myfirstapp.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, stringResource(R.string.cd_back), tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BlackBase)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.settings_about_app), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_app_name), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("AI音乐", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Divider))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Divider))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_tech_stack), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Kotlin + Jetpack Compose", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Divider))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_audio_engine), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Media3 ExoPlayer", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Divider))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_ai_model), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("ACE-Step / Magenta RT", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.settings_features), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                FeatureItem(stringResource(R.string.settings_feature_scan))
                FeatureItem(stringResource(R.string.settings_feature_playlist))
                FeatureItem(stringResource(R.string.settings_feature_ai))
                FeatureItem(stringResource(R.string.settings_feature_background))
                FeatureItem(stringResource(R.string.settings_feature_modes))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, stringResource(R.string.settings_features), modifier = Modifier.size(20.dp), tint = Color(0xFF4ADE80))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
