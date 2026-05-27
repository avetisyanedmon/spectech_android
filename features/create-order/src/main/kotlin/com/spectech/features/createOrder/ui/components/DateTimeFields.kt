package com.spectech.features.createOrder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spectech.features.createOrder.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone as JTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
    ) {
        OutlinedTextField(
            value = formatDate(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        // Transparent overlay so the field receives the click instead of trying
        // to focus the underlying read-only TextField.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true },
        )
    }

    if (showPicker) {
        val initial = remember(value) {
            value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onValueChange(date)
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.create_order_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.create_order_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
    ) {
        OutlinedTextField(
            value = formatTime(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true },
        )
    }

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(LocalTime(state.hour, state.minute))
                    showPicker = false
                }) { Text(stringResource(R.string.create_order_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.create_order_cancel))
                }
            },
            title = { Text(label) },
            text = {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    TimePicker(state = state)
                }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
fun DateTimeField(
    label: String,
    value: LocalDateTime,
    onValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        DateField(
            label = stringResource(R.string.create_order_date),
            value = value.date,
            onValueChange = { onValueChange(LocalDateTime(it, value.time)) },
        )
        TimeField(
            label = stringResource(R.string.create_order_time),
            value = value.time,
            onValueChange = { onValueChange(LocalDateTime(value.date, it)) },
        )
    }
}

// Formatting helpers ----------------------------------------------------------

private fun formatDate(date: LocalDate): String {
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).apply {
        timeZone = JTimeZone.getTimeZone("UTC")
    }
    val instant = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    return sdf.format(java.util.Date(instant))
}

private fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
