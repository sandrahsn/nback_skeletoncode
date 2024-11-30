package mobappdev.example.nback_cimpl.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * This is the Home screen composable
 *
 * Currently this screen shows the saved highscore
 * It also contains a button which can be used to show that the C-integration works
 * Furthermore it contains two buttons that you can use to start a game
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun HomeScreen(
    vm: GameViewModel,
    navController: NavController
) {
    val highscore by vm.highscore.collectAsState()
    val nValue by vm.nBack.collectAsState()
    val gridSize by vm.gridSize.collectAsState()
    val eventInterval by vm.eventInterval.collectAsState()
    val lengthOfGame by vm.lengthOfGame.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(32.dp),
                text = "High Score = $highscore",
                style = MaterialTheme.typography.headlineLarge
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7F),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "n: $nValue",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                    Text(
                        text = "Grid: $gridSize*$gridSize",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                    Text(
                        text = "Time between events: ${eventInterval/1000} s",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                    Text(
                        text = "Round length: $lengthOfGame",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier
                        .padding(20.dp)
                    )

                    // SETTINGS BUTTON
                    Button (
                        onClick = { navController.navigate(route = "SettingsScreen") },
                        enabled = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        elevation = ButtonDefaults.buttonElevation(40.dp),
                        modifier = Modifier
                            .size(width = 160.dp, height = 80.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    {
                        Text(
                            text = "Settings",
                            fontSize = 24.sp,
                            modifier = Modifier
                        )
                    }
                }

            }
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Start Game by choosing game type",
                style = MaterialTheme.typography.displaySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // VISUAL BUTTON
                Button(onClick = {
                        navController.navigate(route = "GameScreen")
                        scope.launch {
                            vm.setGameType(gameType = GameType.Visual)
                            vm.startGame()
                        }
                    })
                {
                    Icon(
                        painter = painterResource(id = R.drawable.visual),
                        contentDescription = "Visual",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }

                // AUDIOVISUAL BUTTON
                Button(onClick = {
                    navController.navigate(route = "GameScreen")
                    scope.launch {
                        vm.setGameType(gameType = GameType.AudioVisual)
                        vm.startGame()
                    }
                })
                {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f/2f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sound_on),
                            contentDescription = "Visual",
                            modifier = Modifier.fillMaxSize(0.6F)
                                .align(Alignment.CenterVertically)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.visual),
                            contentDescription = "Sound",
                            modifier = Modifier.fillMaxSize()
                                .align(Alignment.CenterVertically)
                        )
                    }
                }

                // AUDIO BUTTON
                Button(onClick = {
                    navController.navigate(route = "GameScreen")
                    scope.launch {
                        vm.setGameType(gameType = GameType.Audio)
                        vm.startGame()
                    }
                })
                {
                    Icon(
                        painter = painterResource(id = R.drawable.sound_on),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }

            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    // Since I am injecting a VM into my homescreen that depends on Application context, the preview doesn't work.
    Surface(){
        HomeScreen(FakeVM(), navController = rememberNavController())
    }
}