package mobappdev.example.nback_cimpl.ui.screens

import android.text.TextUtils.isDigitsOnly
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.PopupState

/**
 * This is the Settings screen composable
 *
 * Currently this screen enables the user to choose settings
 * It also contains a button to go back to the Homepage
 *
 * Date: 21-11-2024
 * Version: Version 1.0
 * Author: sandrahsn
 *
 */

@Composable
fun SettingsScreen(
    vm: GameViewModel,
    navController: NavController
) {
    var newNvalue by rememberSaveable { mutableStateOf(" ") }
    var newGridSize by rememberSaveable { mutableStateOf(" ") }
    var newEventInterval by rememberSaveable { mutableStateOf(" ") }
    var newGameLength by rememberSaveable { mutableStateOf(" ") }

    var n: Int
    var g: Int
    var l: Int
    var e: Long

    val showPopup by vm.showPopup.collectAsState()
    val settingsSavedMessage by vm.settingsSavedMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            modifier = Modifier.padding(32.dp),
            style = MaterialTheme.typography.headlineLarge
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.85F),
            contentAlignment = Alignment.Center,
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = newNvalue,
                    onValueChange = { newNvalue = it },
                    label = { Text("N") },
                    modifier = Modifier
                        .padding(8.dp)
                )
                OutlinedTextField(
                    value = newGridSize,
                    onValueChange = { newGridSize = it },
                    label = { Text( text = "Grid size") },
                    modifier = Modifier
                        .padding(8.dp)
                )
                OutlinedTextField(
                    value = newEventInterval,
                    onValueChange = { newEventInterval = it },
                    label = { Text( text = "Time interval") },
                    modifier = Modifier
                        .padding(8.dp)
                )
                OutlinedTextField(
                    value = newGameLength,
                    onValueChange = { newGameLength = it },
                    label = { Text( text = "Round length") },
                    modifier = Modifier
                        .padding(8.dp)
                )

                Spacer(
                    modifier = Modifier
                        .padding(32.dp)
                )

                // SAVE BUTTON
                Button (
                    onClick = {
                        if (isDigitsOnly(newNvalue) &&
                            isDigitsOnly(newGridSize) &&
                            isDigitsOnly(newGameLength) &&
                            isDigitsOnly(newEventInterval)
                        ) {
                            try {
                                n = newNvalue.toInt()
                                g = newGridSize.toInt()
                                l = newGameLength.toInt()
                                e = newEventInterval.toLong() * 1000 // [ms]

                                vm.setNvalue(newNvalue = n)
                                vm.setGridSize(newGridSize = g)
                                vm.setLengthOfGame(newLengthOfGame = l)
                                vm.setEventInterval(newEventInterval = e)

                                vm.setPopupState(popupState = PopupState.SettingsSaved)
                                vm.runPopup()
                            } catch (e: NumberFormatException) {
                                  // Show error message or handle invalid input
                            }
                        } // else { /*felmeddelande*/ }
                    },
                    shape = RoundedCornerShape(20.dp),
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 100.dp, height = 50.dp)
                ) {
                    Text(
                        text = "Save",
                        fontSize = 20.sp,
                        modifier = Modifier
                    )

                }
            }
        }

        // HOMESCREEN BUTTON
        Button(
            onClick = {
                vm.setPopupState(PopupState.None)
                navController.navigate(route = "HomeScreen")
            },
            shape = RoundedCornerShape(20.dp),
            enabled = true,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 160.dp, height = 80.dp)
        )
        {
            Text(
                text = "Home",
                fontSize = 32.sp,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        }

        // SETTINGS SAVED POP UP
        if (showPopup) {
            AlertDialog(
                onDismissRequest = { /*inget*/ },
                title = { Text(text ="Settings to save:") },
                text = { Text(text = settingsSavedMessage) },
                confirmButton = {
                    Button( onClick = {
                        vm.setPopupState(PopupState.None)
                        vm.runPopup()
                    }) {
                        Text(text = "OK") }
                }
            )
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    // Since I am injecting a VM into my homescreen that depends on Application context, the preview doesn't work.
    Surface{
        SettingsScreen(FakeVM(), navController = rememberNavController())
    }
}