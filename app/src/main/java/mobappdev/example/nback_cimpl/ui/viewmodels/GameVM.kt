package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.ui.screens.GameScreen

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch(position: Int)
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
): GameViewModel, ViewModel() {

    // val carColor = MutabelStateFlow("Red")
    //  remote control to change the car color to red

    // val currentCarColor: StateFlow<String> = carColor
    // Everyone sees this sign (StateFlow), which is read-only

    //carColor.value = "Blue"
    // Changing the color with the remote control

    // get() = carColor.asStateFlow()
    // encapsulation - keeping things safe
    // converting remote controle (MutableStateFlow) into a read-only sign (StateFlow).
    // Others can see it but not change it

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    // nBack is currently hardcoded
    override val nBack: Int = 2

    private var job: Job? = null  // coroutine job for the game event
    private val eventInterval: Long = 2000L  // 2000 ms (2s)

    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events

    private var indexInEvents = 0

    // Navigation state using StateFlow, handles state in a reactive way
    private val _navigateToDetail = MutableStateFlow(false)
    val navigateToDetail: StateFlow<Boolean> get() = _navigateToDetail

    // Function to trigger navigation
    fun onDetailButtonClicked() {
        _navigateToDetail.value = true
    }

    // Reset the navigation state
    fun doneNavigating() {
        _navigateToDetail.value = false
    }

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()  // Cancel any existing game loop

        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        events = nBackHelper.generateNBackString(10, 9, 30, nBack).toList().toTypedArray()
        // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            if (score.value > highscore.value ) {
                _highscore.value = score.value
            }
            _score.value = 0
        }
    }

    private fun runAudioGame() {
        // Todo: Make work for Basic grade
    }

    private suspend fun runVisualGame(events: Array<Int>){

        for (value in events) {

            // copy: create a new instance of GameState(Gametype, eventValue) with updated eventValue
            _gameState.value = _gameState.value.copy(eventValue = value)
            // update _gameState to a new instance of _gameState that is exactly similar except with the eventvalue value

            indexInEvents += 1

            // count scores

            delay(eventInterval)
        }

    }
    override fun checkMatch(clickedPosition: Int) {
        // check for match between clicked position (value) and value nBack times ago

        if (indexInEvents >= nBack) {
            if (clickedPosition  == events[indexInEvents - nBack]) {
                // increase score +1
                _score.value = score.value + 1
            }
            else {
                // decrease score -1
                if (_score.value > 0) {
                    _score.value = score.value - 1
                }
            }
        }

        /**
         * Todo: This function should check if there is a match when the user presses a match button
         * Make sure the user can only register a match once for each event.
         */
    }

    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

// Data Class (immutable: properties cannot be changed directly after being created -
// instead create nre instance of original object through copy() and new values)
// for ex. carColor.value = carColor.value.copy(doorColor = "Blue")
data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1  // The value of the array string
)

enum class Screen {
    Home, Game
}

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: Int
        get() = 2

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch(clickedPosition: Int) {
    }
}