package com.spectech.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spectech.features.auth.R
import com.spectech.features.auth.ui.components.RussianPhoneField
import com.spectech.features.auth.util.RussianPhoneFormatter
import com.spectech.features.auth.viewmodel.AuthFlowViewModel

@Composable
fun StartAuthScreen(
    viewModel: AuthFlowViewModel,
    onCreateAccount: () -> Unit,
    onBackToHome: (() -> Unit)? = null,
) {
    val formatter = remember { RussianPhoneFormatter() }
    val scroll = rememberScrollState()
    val brandBlue = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(brandBlue, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.auth_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.auth_phone_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            RussianPhoneField(formatter = formatter, modifier = Modifier.fillMaxWidth())
            viewModel.startError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = err.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { formatter.e164?.let(viewModel::submitPhone) },
            enabled = formatter.isComplete && !viewModel.isSubmittingPhone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = brandBlue,
                disabledContainerColor = brandBlue.copy(alpha = 0.4f),
            ),
        ) {
            if (viewModel.isSubmittingPhone) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.auth_send_otp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.auth_no_account_question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCreateAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
        ) {
            Text(stringResource(R.string.auth_create_account), color = brandBlue)
        }

        if (onBackToHome != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                modifier = Modifier.padding(8.dp),
                text = stringResource(R.string.auth_back_to_home),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
