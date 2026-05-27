package com.spectech.features.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Six-box OTP code entry. Auto-advances on input, back-advances on delete,
 * and distributes a pasted 6-digit code across all boxes. Mirrors iOS
 * `OTPDigitBox` + the orchestrating handler in `VerifyOTPView`.
 *
 * SMS-OTP autofill hint can be layered on once the team picks a stable Compose
 * autofill API revision; for Phase 2 we keep the box self-contained.
 */
@Composable
fun OtpBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    codeLength: Int = 6,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember { List(codeLength) { FocusRequester() } }

    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    val digits = remember(code) {
        val chars = code.filter(Char::isDigit).take(codeLength)
        (0 until codeLength).map { chars.getOrNull(it)?.toString() ?: "" }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        digits.forEachIndexed { index, digit ->
            OtpBox(
                digit = digit,
                onValueChange = { rawInput ->
                    handleDigitChange(
                        index = index,
                        rawInput = rawInput,
                        current = digits,
                        codeLength = codeLength,
                        focusRequesters = focusRequesters,
                        emit = onCodeChange,
                    )
                },
                modifier = Modifier.focusRequester(focusRequesters[index]),
            )
        }
    }
}

@Composable
private fun OtpBox(
    digit: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandBlue = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    val baseModifier = modifier
        .size(48.dp, 56.dp)
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        .border(1.dp, outline, RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp)
        .wrapContentSize(Alignment.Center)

    BasicTextField(
        value = TextFieldValue(text = digit, selection = TextRange(digit.length)),
        onValueChange = { onValueChange(it.text) },
        modifier = baseModifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(brandBlue),
    )
}

private fun handleDigitChange(
    index: Int,
    rawInput: String,
    current: List<String>,
    codeLength: Int,
    focusRequesters: List<FocusRequester>,
    emit: (String) -> Unit,
) {
    val filtered = rawInput.filter(Char::isDigit)

    // Paste case: full code distributed across all boxes.
    if (filtered.length >= codeLength) {
        emit(filtered.take(codeLength))
        focusRequesters[codeLength - 1].requestFocus()
        return
    }

    val newDigit = filtered.lastOrNull()?.toString() ?: ""
    val mutated = current.toMutableList().also { it[index] = newDigit }
    emit(mutated.joinToString(""))

    when {
        newDigit.isNotEmpty() && index < codeLength - 1 -> focusRequesters[index + 1].requestFocus()
        newDigit.isEmpty() && index > 0 -> focusRequesters[index - 1].requestFocus()
    }
}
