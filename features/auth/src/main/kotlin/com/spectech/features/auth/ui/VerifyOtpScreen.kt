package com.spectech.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spectech.features.auth.R
import com.spectech.features.auth.ui.components.OtpBoxes
import com.spectech.features.auth.viewmodel.AuthFlowViewModel
import com.spectech.uikit.strings.localizedMessage
import kotlinx.coroutines.delay

private const val CODE_LENGTH = 6
private const val RESEND_SECONDS = 60

@Composable
fun VerifyOtpScreen(
    viewModel: AuthFlowViewModel,
    phone: String,
) {
    var code by remember { mutableStateOf("") }
    var secondsRemaining by remember { mutableIntStateOf(RESEND_SECONDS) }
    val brandBlue = MaterialTheme.colorScheme.primary
    val scroll = rememberScrollState()

    LaunchedEffect(phone) {
        secondsRemaining = RESEND_SECONDS
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(brandBlue, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Inventory2, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.auth_enter_otp), style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.auth_otp_sent_to), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatPhoneForDisplay(phone), style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(32.dp))

        OtpBoxes(code = code, onCodeChange = { code = it })

        viewModel.verifyError?.let { err ->
            Spacer(Modifier.height(12.dp))
            Text(err.localizedMessage(), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))

        if (secondsRemaining > 0) {
            Row {
                Text(stringResource(R.string.auth_resend_in),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                Text(" ${secondsRemaining}s", color = brandBlue, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            TextButton(onClick = {
                viewModel.submitPhone(phone)
                secondsRemaining = RESEND_SECONDS
            }) {
                Text(stringResource(R.string.auth_resend_code), color = brandBlue,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.submitCode(phone, code) },
            enabled = code.length == CODE_LENGTH && !viewModel.isVerifyingCode,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = brandBlue,
                disabledContainerColor = brandBlue.copy(alpha = 0.4f),
            ),
        ) {
            if (viewModel.isVerifyingCode) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp))
            } else {
                Text(stringResource(R.string.auth_verify_continue), color = Color.White,
                    style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.auth_otp_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
    }
}

private fun formatPhoneForDisplay(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length != 11 || !digits.startsWith("7")) return phone
    val rest = digits.drop(1)
    return "+7 (${rest.substring(0, 3)}) ${rest.substring(3, 6)}-${rest.substring(6, 8)}-${rest.substring(8, 10)}"
}
