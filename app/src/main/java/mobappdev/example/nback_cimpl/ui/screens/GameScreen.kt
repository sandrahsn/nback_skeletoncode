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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@Composable
fun GameScreen(
    vm: GameViewModel,
    navController: NavController

) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nBack = vm.nBack
    val score = vm.score.collectAsState()
    val gameState by vm.gameState.collectAsState()
    val showPopup by vm.showEndGamePopup.collectAsState()
    val endGameMessage by vm.endGameMessage.collectAsState()
    val gridSize  = vm.gridSize


    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = "n = $nBack",
                fontSize = 24.sp
            )
            Text(
                text = "Score = ${score.value}",
                fontSize = 24.sp
            )
        }

        Grid(gameState, gridSize, Modifier)

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 8.dp,
                    vertical = 8.dp)
        ) {
            // VISUAL BUTTON
            Button(
                onClick = { vm.checkMatch() },
                shape = RoundedCornerShape(20.dp),
                enabled = gameState.gameType == GameType.Visual ||
                        gameState.gameType == GameType.AudioVisual,
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
                onClick = { vm.checkMatch() },
                shape = RoundedCornerShape(20.dp),
                enabled = gameState.gameType == GameType.Audio ||
                        gameState.gameType == GameType.AudioVisual,
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
        }

        if (showPopup) {
            AlertDialog(
                onDismissRequest = { vm.startGame() },
                title = { Text(text ="Time's up!") },
                text = { Text(text = endGameMessage) },
                confirmButton = {
                    Button( onClick = {
                        vm.endGame()
                        vm.hidePopup()
                        vm.startGame() }
                    ){ Text(text = "Play again") }
                                },
                dismissButton = {
                    Button( onClick = {
                        vm.endGame()
                        vm.hidePopup()
                        // clear gamestate
                        navController.navigate(route = "HomeScreen")
                    }) { Text(text = "Change gametype") }
                }
            )
        }
    }
}

fun reactionIncorrect() {

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
                                    if (boxNumber == gameState.eventValue) {
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