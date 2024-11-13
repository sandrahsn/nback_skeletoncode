package mobappdev.example.nback_cimpl.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.Navigation.findNavController
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GameScreen(
    vm: GameViewModel
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val nBack = vm.nBack
    val score = vm.score.collectAsState()
    val gameState by vm.gameState.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "N-back",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(16.dp)
                )
                Text(
                    text = "N = $nBack",
                    fontSize = 24.sp
                )
                Text(
                    text = "Score = $score"
                )
            }
            Grid(vm, gameState, Modifier)

        }
    }
}

@Composable
fun Grid(
    vm: GameViewModel,
    gameState: GameState,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(0.7F),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        var n = 0
        for (r in 1 until 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (c in 1 until 4) {
                    val position = n
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                    ) {
                        Button(
                            onClick = { /*TO DO*/
                                // send position to VM,
                                // return position
                                vm.checkMatch(position)
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = (if (n == gameState.eventValue - 1) {
                                ButtonDefaults.buttonColors(Color.DarkGray)
                            } else {
                                ButtonDefaults.buttonColors(Color.LightGray)
                            }),
                            modifier = Modifier
                                .fillMaxSize()
                                .size(80.dp)
                                .background(
                                    color = if (n == gameState.eventValue - 1) {
                                        Color.DarkGray
                                    } else {
                                        Color.LightGray
                                    }
                                ),
                        ) {
                            Text(text = "$position")
                        }
                    }
                    n += 1
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
        GameScreen(FakeVM())
    }
}