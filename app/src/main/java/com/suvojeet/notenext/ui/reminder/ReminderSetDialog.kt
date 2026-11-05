@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.RepeatOption
import com.suvojeet.notenext.ui.components.springPress

import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ReminderSheetContent(
    initialDate: LocalDate? = null,
    initialTime: LocalTime? = null,
    initialRepeatOption: RepeatOption? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate, LocalTime, RepeatOption) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(initialTime ?: LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedRepeatMenu by remember { mutableStateOf(false) }
    var selectedRepeatOption by remember { mutableStateOf(initialRepeatOption ?: RepeatOption.NEVER) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = MaterialTheme.shapes.extraLarge,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = com.suvojeet.notenext.core.R.string.set_reminder),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f).springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(id = R.string.rem_date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM dd")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedCard(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f).springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(id = R.string.rem_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        ExposedDropdownMenuBox(
            expanded = expandedRepeatMenu,
            onExpandedChange = { expandedRepeatMenu = !expandedRepeatMenu },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRepeatOption.label,
                onValueChange = { /* Read Only */ },
                readOnly = true,
                label = { Text(stringResource(id = R.string.rem_repeat_interval)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRepeatMenu) },
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedRepeatMenu,
                onDismissRequest = { expandedRepeatMenu = false },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                RepeatOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            selectedRepeatOption = option
                            expandedRepeatMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismissRequest, modifier = Modifier.springPress()) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { onConfirm(selectedDate, selectedTime, selectedRepeatOption) },
                modifier = Modifier.springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(id = R.string.rem_set_reminder_button), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ReminderSetDialog(
    initialDate: LocalDate? = null,
    initialTime: LocalTime? = null,
    initialRepeatOption: RepeatOption? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate, LocalTime, RepeatOption) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            ReminderSheetContent(
                initialDate = initialDate,
                initialTime = initialTime,
                initialRepeatOption = initialRepeatOption,
                onDismissRequest = onDismissRequest,
                onConfirm = onConfirm
            )
        }
    }
}
