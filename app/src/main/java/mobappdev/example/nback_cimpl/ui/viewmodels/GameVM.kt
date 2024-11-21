package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import android.speech.tts.TextToSpeech


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
    val endGameMessage: StateFlow<String>
    val showEndGamePopup: StateFlow<Boolean>
    val gridSize: Int
    val lengthOfGame: Int
    val eventInterval: Long
    val busy: StateFlow<Boolean>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch(): Boolean
    fun endGame()
    fun hidePopup()
    fun isGameBusy(): Boolean
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    application: GameApplication
): GameViewModel, ViewModel() {

    // State Flows for handling states in a reactive way
    private val _gameState = MutableStateFlow(GameState()) // remote control to change GameState
    override val gameState: StateFlow<GameState> // sign everyone sees, read-only
        get() = _gameState.asStateFlow()
    //encapsulation - keeps things safe. converting MutableStateFlow into read-only (StateFlow)

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    private val _navigateToDetail = MutableStateFlow(false)
    val navigateToDetail: StateFlow<Boolean>
        get() = _navigateToDetail

    private val _showEndGamePopup = MutableStateFlow(false)
    override val showEndGamePopup: StateFlow<Boolean>
        get() = _showEndGamePopup

    private val _endGameMessage = MutableStateFlow("Game finished")
    override val endGameMessage: StateFlow<String>
        get() = _endGameMessage

    private val _busy = MutableStateFlow(false)
    override val busy: StateFlow<Boolean>
        get() = _busy

    private val checkedIndices = mutableSetOf<Int>() // Keep track of indices already checked


    // variables
    override val nBack: Int = 2
    override val gridSize: Int = 3
    override val lengthOfGame: Int = 10
    override val eventInterval: Long = 1500L  // 2000 ms (2s)
    private var job: Job? = null  // coroutine job for the game event
    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events


    private var textToSpeech: TextToSpeech? = null
    private var context = application.applicationContext
    private var n = 0
    private var letters = mutableListOf("A", "B", "C", "D", "E", "G", "H", "X", "Y", "Z")

    // trigger navigation
    fun onDetailButtonClicked() {
        _navigateToDetail.value = true
    }

    // Reset navigation state
    fun doneNavigating() {
        _navigateToDetail.value = false
    }

    // trigger end game popup
    private fun onGameEnd() {
        _endGameMessage.value = "Game finished. Your final score is ${score.value}"
        _showEndGamePopup.value = true
    }

    // Reset pop up
    private fun resetPopup() {
        _showEndGamePopup.value = false
    }

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()
        n = 0

        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        events = nBackHelper.generateNBackString(lengthOfGame, gridSize*gridSize, 30, nBack).toList().toTypedArray()
        // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

        // start a new coroutine vmscope within the lifecycle of ViewModel
        // run game logic here, tasks that vm is responsible for, such as:
        // starting the game, processing game logic, or saving scores.
        // Anything you want to persist across configuration changes (e.g., device rotation).

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame(events)
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }

            if (score.value > highscore.value) {
                _highscore.value = score.value
                userPreferencesRepository.saveHighScore(_highscore.value)
            }
        }
    }

    private fun initializeTextToSpeech() {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d("GameVM", "TextToSpeech initialized successfully.")
                } else {
                    // Initialization failed
                    Log.e("GameVM", "TextToSpeech initialization failed.")
                }
            }
        }
    }

    private fun speakText(letter: String) {
        // ?. prevents NullPointerException
        textToSpeech?.speak(letter,TextToSpeech.QUEUE_FLUSH,null,null)
    }


    private suspend fun runAudioGame(events: Array<Int>) {
        // inherits parent coroutine context from startGame()
        _score.value = 0
        initializeTextToSpeech()
            for (value in events) {
                _busy.value = true
                _gameState.value = _gameState.value.copy(
                    eventValue = value,
                    indexValue = n)
                val letter = letters[value]
                speakText(letter)
                Log.d("GameVM", "sent $letter")
                n += 1
                _busy.value = false
                delay(eventInterval)
            }
        endGame()
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        // inherits parent coroutine context from startGame()
    _score.value = 0
        for (value in events) {
            _busy.value = true
            _gameState.value = _gameState.value.copy(
                eventValue = value,
                indexValue = n
            )
            n += 1
            _busy.value = false
            delay(eventInterval)
        }
        endGame()
    }

    override fun isGameBusy(): Boolean {
        if (_busy.value) {
            return true
        }
        else {
            return false
        }
    }

    override fun endGame() {
        job?.cancel()
        onGameEnd()
    }

    override fun hidePopup() {
        resetPopup()
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown() // Release TTS engine
        textToSpeech = null
    }

    override fun checkMatch(): Boolean {
        val indexNow = _gameState.value.indexValue
        val valueNow = _gameState.value.eventValue

        if (indexNow >= nBack) {

            if (checkedIndices.contains(indexNow)) {
                return false // Already checked, so no match registered again
            }
            else {
                val indexNbackAgo = indexNow - nBack
                val valueNbackAgo = events[indexNbackAgo]

                if (valueNow == valueNbackAgo) {
                    _score.value = score.value + 1
                    return true
                } else {
                    // can't have negative score
                    if (_score.value > 0) {
                        _score.value = score.value - 1
                    }
                    return false
                }
            }
        }
        return false
    }

    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(
                    application.userPreferencesRespository,
                    application = application
                )
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
    val eventValue: Int = -1,  // The value of the array string
    val indexValue: Int = 0
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
    override val showEndGamePopup: StateFlow<Boolean>
        get() = MutableStateFlow(false).asStateFlow()
    override val endGameMessage: StateFlow<String>
        get() = MutableStateFlow("Test").asStateFlow()
    override val gridSize: Int
        get() = 3
    override val lengthOfGame: Int
        get() = 10
    override val eventInterval: Long
        get() = 1000L
    override var busy: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun isGameBusy(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch(): Boolean {
    return false}

    override fun endGame() {
    }

    override fun hidePopup() {
    }
}