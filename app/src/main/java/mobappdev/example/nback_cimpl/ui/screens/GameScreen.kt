package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.theme.Purple40
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.PopupState

@Composable
fun GameScreen(
    vm: GameViewModel,
    navController: NavController

) {
    val nBack = vm.nBack.collectAsState()
    val score = vm.score.collectAsState()
    val gameState by vm.gameState.collectAsState()
    val gameHasEnded by vm.gameHasEnded.collectAsState()
    val showPopup by vm.showPopup.collectAsState()
    val endGameMessage by vm.endGameMessage.collectAsState()
    val gridSize  = vm.gridSize.collectAsState()
    val eventInterval by vm.eventInterval.collectAsState()
    val lengthOfGame by vm.lengthOfGame.collectAsState()

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 8.dp,
                    vertical = 32.dp)
        ) {
            Text(
                text = "n-back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .padding(16.dp)
            )
            Text(
                text = "n = ${nBack.value}",
                fontSize = 24.sp
            )
            Text(
                text = "Score = ${score.value}",
                fontSize = 24.sp
            )
        }

        Grid(gameState, gridSize.value, Modifier)
        Buttons(vm)

        if (showPopup) {
            AlertDialog(
                onDismissRequest = {
                    vm.setPopupState(PopupState.None)
                    vm.runPopup() },
                title = { Text(text ="Time's up!") },
                text = { Text(text = endGameMessage) },
                confirmButton = {
                    Button( onClick = {
                        vm.setPopupState(PopupState.None)
                        vm.runPopup()
                        vm.startGame()
                    }
                    ){ Text(text = "Play again") }
                                },
                dismissButton = {
                    Button( onClick = {
                        vm.setPopupState(PopupState.None)
                        vm.runPopup()
                        navController.navigate("Homescreen")
                    }) { Text(text = "Home") }
                }
            )
        }
    }
}

@Composable
fun Buttons(
    vm: GameViewModel
) {
    val gameState by vm.gameState.collectAsState()
    var visualButtonColor by remember { mutableStateOf(Purple40) }
    var audioButtonColor by remember { mutableStateOf(Purple40) }
    var isMatch by remember { mutableStateOf(false) }
    var isAudioMatch by remember { mutableStateOf(false) }
    var visualTrigger by remember { mutableStateOf(false) }
    var audioTrigger by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            )
    ) {
        // VISUAL BUTTON
        Button(
            onClick = { visualTrigger = true
                      isMatch = vm.checkMatch()},
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = visualButtonColor),
            enabled = gameState.gameType == GameType.Visual
                    || gameState.gameType == GameType.AudioVisual,
            modifier = Modifier
                .padding(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.visual),
                contentDescription = "Visual",
                modifier = Modifier
                    .height(48.dp)
                    .aspectRatio(3f / 2f)
            )
        }

        // AUDIO BUTTON
        Button(
            onClick = { audioTrigger = true
                      isAudioMatch = vm.checkAudioMatch()},
            shape = RoundedCornerShape(20.dp),
            enabled = gameState.gameType == GameType.Audio ||
                    gameState.gameType == GameType.AudioVisual,
            colors = ButtonDefaults.buttonColors(containerColor = audioButtonColor),
            modifier = Modifier
                .padding(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.sound_on),
                contentDescription = "Sound",
                modifier = Modifier
                    .height(48.dp)
                    .aspectRatio(3f / 2f)
            )
        }

        // UI-specific logic
        LaunchedEffect(key1 = visualTrigger) {
            while (vm.isVisualGameBusy()) {
                delay(100L)
            }
            visualButtonColor = if (isMatch) {
                Color.Green
            } else {
                Color.Red
            }
            delay(200L)
            visualButtonColor = Purple40
            visualTrigger = false
            }

        LaunchedEffect(key1 = audioTrigger) {
            while (vm.isAudioGameBusy()) {
                delay(100L)
            }
            audioButtonColor = if (isAudioMatch) {
                Color.Green
            } else {
                Color.Red
            }
            delay(200L)
            audioButtonColor = Purple40
            audioTrigger = false
        }
    }
}

@Composable
fun Grid(
    gameState: GameState,
    gridSize: Int,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(0.65F),
    ) {
        var boxNumber = 1
        for (r in 0 until gridSize) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 8.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until gridSize) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color =
                                if (gameState.gameType == GameType.Visual ||
                                    gameState.gameType == GameType.AudioVisual) {
                                    if (boxNumber == gameState.visualValue) {
                                        Color.DarkGray
                                    } else {
                                        Color.LightGray
                                    }
                                } else {
                                    Color.LightGray
                                }
                            )
                   ) {
                        Text(text = "$boxNumber")
                        boxNumber += 1
                    }

                }
            }
        }
    }
}

@Preview
@Composable
fun GamecreenPreview() {
    // Since I am injecting a VM into my homescreen that depends on Application context, the preview doesn't work.
    Surface(){
        GameScreen(FakeVM(), navController = rememberNavController())
    }
}