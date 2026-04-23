package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val STORAGE_DATE_PATTERN = "yyyy-MM-dd"
private const val DISPLAY_DATE_PATTERN = "dd/MM/yyyy"
private const val TIME_PATTERN = "HH:mm"

fun currentStorageDate(): String = SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.getDefault()).format(Date())

fun currentStorageTime(): String = SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(Date())

fun formatDateForDisplay(storageDate: String): String {
    val parsedDate = runCatching {
        SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.getDefault()).parse(storageDate)
    }.getOrNull() ?: return storageDate

    return SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.getDefault()).format(parsedDate)
}

fun storageDateToPickerMillis(storageDate: String): Long {
    val parts = storageDate.split("-")
    if (parts.size != 3) return System.currentTimeMillis()

    val year = parts.getOrNull(0)?.toIntOrNull() ?: return System.currentTimeMillis()
    val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: return System.currentTimeMillis()
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return System.currentTimeMillis()

    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun pickerMillisToStorageDate(millis: Long): String {
    return SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.getDefault()).format(Date(millis))
}

fun storageTimeToHour(storageTime: String): Int {
    return storageTime.split(":").getOrNull(0)?.toIntOrNull()
        ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}

fun storageTimeToMinute(storageTime: String): Int {
    return storageTime.split(":").getOrNull(1)?.toIntOrNull()
        ?: Calendar.getInstance().get(Calendar.MINUTE)
}

@OptIn(ExperimentalMaterial3Api::class)
fun TimePickerState.toStorageTimeString(): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    storageDate: String,
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = storageDateToPickerMillis(storageDate),
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(pickerMillisToStorageDate(millis))
                    }
                    onDismissRequest()
                },
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Annuler")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    title: String,
    storageTime: String,
    onDismissRequest: () -> Unit,
    onTimeSelected: (String) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = storageTimeToHour(storageTime),
        initialMinute = storageTimeToMinute(storageTime),
        is24Hour = true,
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = title)
                TimeInput(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismissRequest) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onTimeSelected(timePickerState.toStorageTimeString())
                            onDismissRequest()
                        },
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}
