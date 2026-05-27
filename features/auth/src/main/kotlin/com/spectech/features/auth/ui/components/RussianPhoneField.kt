package com.spectech.features.auth.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.spectech.features.auth.util.RussianPhoneFormatter

/**
 * Phone input bound to [RussianPhoneFormatter]. Uses [TextFieldValue] so the
 * cursor can be pinned at the end after each keystroke — without that, the
 * Compose TextField restores its own cursor heuristic and ends up inserting
 * the next typed digit in the middle of the formatted string.
 *
 * The cursor-at-end behaviour matches iOS, where SwiftUI implicitly does the
 * same thing whenever the formatted text is rewritten on each input.
 */
@Composable
fun RussianPhoneField(
    formatter: RussianPhoneFormatter,
    modifier: Modifier = Modifier,
    placeholder: String = "+7 (999) 000-00-00",
) {
    // Single source of truth for the displayed text. We mirror it from the
    // formatter so external resets (e.g. formatter.clear()) propagate down.
    var fieldValue by remember {
        val initial = formatter.formatted
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(initial.length)))
    }

    LaunchedEffect(formatter.formatted) {
        if (fieldValue.text != formatter.formatted) {
            fieldValue = TextFieldValue(
                text = formatter.formatted,
                selection = TextRange(formatter.formatted.length),
            )
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            formatter.update(incoming.text)
            val formatted = formatter.formatted
            fieldValue = TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length),
            )
        },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = modifier,
    )
}
