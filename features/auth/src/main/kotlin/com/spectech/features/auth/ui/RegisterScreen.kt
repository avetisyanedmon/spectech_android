package com.spectech.features.auth.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.spectech.domain.enums.UserRole
import com.spectech.domain.model.RegistrationProfile
import com.spectech.features.auth.R
import com.spectech.features.auth.ui.components.RussianPhoneField
import com.spectech.features.auth.util.RussianPhoneFormatter
import com.spectech.features.auth.viewmodel.AuthFlowViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthFlowViewModel,
    onSignIn: () -> Unit,
) {
    val formatter = remember { RussianPhoneFormatter() }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var accountType by remember {
        mutableStateOf(RegistrationProfile.AccountType.INDIVIDUAL)
    }
    var role by remember { mutableStateOf(UserRole.CUSTOMER) }
    val scroll = rememberScrollState()
    val brandBlue = MaterialTheme.colorScheme.primary

    val isFormValid = name.isNotBlank() && city.isNotBlank() && formatter.isComplete

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_register_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))

        // Account type
        Text(stringResource(R.string.auth_account_type), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = accountType == RegistrationProfile.AccountType.INDIVIDUAL,
                onClick = { accountType = RegistrationProfile.AccountType.INDIVIDUAL },
                label = { Text(stringResource(R.string.auth_account_individual)) },
            )
            FilterChip(
                selected = accountType == RegistrationProfile.AccountType.LEGAL_ENTITY,
                onClick = { accountType = RegistrationProfile.AccountType.LEGAL_ENTITY },
                label = { Text(stringResource(R.string.auth_account_legal_entity)) },
            )
        }

        Spacer(Modifier.height(16.dp))

        // Role
        Text(stringResource(R.string.auth_role_label), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = role == UserRole.CUSTOMER,
                onClick = { role = UserRole.CUSTOMER },
                label = { Text(stringResource(R.string.role_customer)) },
            )
            FilterChip(
                selected = role == UserRole.CONTRACTOR,
                onClick = { role = UserRole.CONTRACTOR },
                label = { Text(stringResource(R.string.role_contractor)) },
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.auth_full_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_email_optional)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text(stringResource(R.string.auth_city)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.auth_phone_label), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        RussianPhoneField(formatter = formatter, modifier = Modifier.fillMaxWidth())

        viewModel.startError?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val e164 = formatter.e164 ?: return@Button
                viewModel.registrationProfile = RegistrationProfile(
                    name = name.trim(),
                    email = email.trim().takeIf { it.isNotEmpty() },
                    city = city.trim(),
                    role = role,
                    accountType = accountType,
                )
                viewModel.submitPhone(e164)
            },
            enabled = isFormValid && !viewModel.isSubmittingPhone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
        ) {
            if (viewModel.isSubmittingPhone) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp))
            } else {
                Text(stringResource(R.string.auth_send_otp), color = Color.White)
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSignIn) {
            Text(stringResource(R.string.auth_already_have_account))
        }
        Spacer(Modifier.height(32.dp))
    }
}
