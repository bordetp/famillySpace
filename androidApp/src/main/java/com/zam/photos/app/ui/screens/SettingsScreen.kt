package com.zam.photos.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zam.photos.app.R
import com.zam.photos.app.ui.theme.ThemeMode
import com.zam.photos.app.ui.theme.appBorder
import com.zam.photos.app.ui.theme.appMuted
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    showModeration: Boolean = false,
    onOpenModeration: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    RefreshOnResume { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(stringResource(R.string.settings_appearance), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            val themeOptions = listOf(
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark,
                ThemeMode.SYSTEM to R.string.theme_system
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

            Text(stringResource(R.string.settings_notifications), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.push_notifications), modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                Switch(checked = state.pushEnabled, onCheckedChange = viewModel::setPushEnabled)
            }
            Text(stringResource(R.string.push_notifications_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.appMuted)

            HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

            if (showModeration) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenModeration).padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.moderation_entry), modifier = Modifier.padding(start = 12.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPrivacyPolicy).padding(vertical = 8.dp)
            ) {
                Icon(Icons.Outlined.Policy, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.privacy_policy), modifier = Modifier.padding(start = 12.dp))
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun sharePostLink(context: Context, postId: String, authorName: String, content: String) {
    val link = "https://famillyspace.duckdns.org/post/$postId"
    val text = buildString {
        append(authorName)
        if (content.isNotBlank()) append(" : ").append(content.take(140))
        append("\n")
        append(context.getString(R.string.share_post_cta))
        append(" ")
        append(link)
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            context.getString(R.string.action_share)
        )
    )
}
