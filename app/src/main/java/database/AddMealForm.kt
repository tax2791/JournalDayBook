package com.kushal.mealapp.database

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

//currently this code is not used
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun AddMealForm(viewModel: MealViewModel) {
    val nameFocusRequester = remember { FocusRequester() }
    val dateFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val members by viewModel.allMembers.collectAsState(emptyList())
    var selectedName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var nameExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name Dropdown
        ExposedDropdownMenuBox(
            expanded = nameExpanded,
            onExpandedChange = { nameExpanded = !nameExpanded }
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                label = { Text("Select Name") },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        modifier = Modifier.clickable { nameExpanded = true }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester)
                    .clickable { nameExpanded = true }
            )

            ExposedDropdownMenu(
                expanded = nameExpanded,
                onDismissRequest = { nameExpanded = false }
            ) {
                members.forEach { member ->
                    DropdownMenuItem(
                        onClick = {
                            selectedName = member.name
                            nameExpanded = false
                        }
                    ) {
                        Text(member.name)
                    }
                }
            }
        }

        // Date Input
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(dateFocusRequester)
                .onKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        amountFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { amountFocusRequester.requestFocus() })
        )

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
                showError = amount.isNotEmpty() && amount.toDoubleOrNull() == null
            },
            label = { Text("Amount") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester)
                .onKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        focusManager.clearFocus()
                        true
                    } else {
                        false
                    }
                },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            isError = showError
        )

        if (showError) {
            Text(
                text = "Please enter a valid number for the amount",
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
        }

        // Submit Button
        Button(
            onClick = {
                if (selectedName.isNotEmpty() && date.isNotEmpty() && amount.toDoubleOrNull() != null) {
                    val meal = Meal(
                        id = 0,
                        name = selectedName,
                        date = date,
                        amount = amount.toDouble()
                    )
                    viewModel.addMeal(meal)

                    // Reset the fields after submission
                    selectedName = ""
                    date = ""
                    amount = ""
                    showError = false
                    nameFocusRequester.requestFocus()
                }
            },
            enabled = selectedName.isNotEmpty() && date.isNotEmpty() && amount.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Meal")
        }
    }
}
