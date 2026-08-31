package com.zam.photos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zam.photos.app.R
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.TextMuted

@Composable
fun PendingApprovalScreen(
    isRejected: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    RefreshOnResume { onRefresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isRejected) Icons.Outlined.Block else Icons.Outlined.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Terracotta
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(
                if (isRejected) R.string.approval_rejected_title
                else R.string.approval_pending_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (isRejected) R.string.approval_rejected_message
                else R.string.approval_pending_message
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (!isRejected) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                Text(stringResource(R.string.approval_check_again))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.logout))
        }
    }
}
