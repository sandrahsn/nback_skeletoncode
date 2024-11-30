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
    val nBack: StateFlow<Int>
    val endGameMessage: StateFlow<String>
    val settingsSavedMessage: StateFlow<String>
    val showPopup: StateFlow<Boolean>
    val gridSize: StateFlow<Int>
    val lengthOfGame: StateFlow<Int>
    val eventInterval: StateFlow<Long>
    val busyVisual: StateFlow<Boolean>
    val busyAudio: StateFlow<Boolean>
    val popupState: StateFlow<PopupState>
    val gameHasEnded: StateFlow<Boolean>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch(): Boolean
    fun checkAudioMatch(): Boolean
    fun isVisualGameBusy(): Boolean
    fun isAudioGameBusy(): Boolean
    fun setGridSize(newGridSize: Int)
    fun setNvalue(newNvalue: Int)
    fun setEventInterval(newEventInterval: Long)
    fun setLengthOfGame(newLengthOfGame: Int)
    fun runPopup()
    fun setPopupState(popupState: PopupState)
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

    private val _endGameMessage = MutableStateFlow("Game finished")
    override val endGameMessage: StateFlow<String>
        get() = _endGameMessage

    private val _settingsSavedMessage = MutableStateFlow("Settings saved")
    override val settingsSavedMessage: StateFlow<String>
        get() = _settingsSavedMessage

    private val _showPopup = MutableStateFlow(false)
    override val showPopup: StateFlow<Boolean>
        get() = _showPopup

    private val _busyVisual = MutableStateFlow(false)
    override val busyVisual: StateFlow<Boolean>
        get() = _busyVisual

    private val _busyAudio = MutableStateFlow(false)
    override val busyAudio: StateFlow<Boolean>
        get() = _busyAudio

    private val checkedIndices = mutableSetOf<Int>() // Keep track of indices already checked

    private val _nBack = MutableStateFlow(2)
    override val nBack: StateFlow<Int>
        get() = _nBack

    private val _gridSize = MutableStateFlow(3)
    override val gridSize: StateFlow<Int>
        get() = _gridSize

    private val _eventInterval = MutableStateFlow(3000L) // ms
    override val eventInterval: StateFlow<Long>
        get() = _eventInterval

    private val _lengthOfGame = MutableStateFlow(10)
    override val lengthOfGame: StateFlow<Int>
        get() = _lengthOfGame

    // variables
    private var job: Job? = null  // coroutine job for the game event
    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events
    private var secondEvents = emptyArray<Int>()

    private var textToSpeech: TextToSpeech? = null
    private var context = application.applicationContext
    private var n = 0
    private var letters = mutableListOf("A", "B", "C", "D", "E", "G", "H", "X", "Y", "Z")

    private val _popupState = MutableStateFlow<PopupState>(PopupState.None)
    override val popupState: StateFlow<PopupState>
        get () =  _popupState.asStateFlow()

    private val _gameHasEnded = MutableStateFlow<Boolean>(false)
    override val gameHasEnded: StateFlow<Boolean>
        get () = _gameHasEnded.asStateFlow()

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun setPopupState(popupState: PopupState) {
        _popupState.value = popupState
    }

    override fun setNvalue(newNvalue: Int) {
        _nBack.value = newNvalue

    }

    override fun setGridSize(newGridSize: Int) {
        _gridSize.value =  newGridSize
    }

    override fun setEventInterval(newEventInterval: Long) {
        _eventInterval.value = newEventInterval
    }

    override fun setLengthOfGame(newLengthOfGame: Int) {
        _lengthOfGame.value = newLengthOfGame
    }

    override fun startGame() {
        job?.cancel()
        n = 0
        _score.value = 0

        setPopupState(PopupState.None)
        runPopup()
        events = createEvents()

        // start new coroutine within the lifecycle of ViewModel, run game logic here,
        // tasks that vm is responsible for: start game, process game logic, saving scores
        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame(events)
                GameType.AudioVisual -> runAudioVisualGame(events)
                GameType.Visual -> runVisualGame(events)
            }
            if (score.value > highscore.value) {
                _highscore.value = score.value
                userPreferencesRepository.saveHighScore(_highscore.value)
            }
        }
    }

    private fun createEvents(): Array<Int> {
        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        return nBackHelper.generateNBackString(
            lengthOfGame.value,
            gridSize.value*gridSize.value,
            30,
            nBack.value
        )
            .toList().toTypedArray()
    }

    private suspend fun runVisualGame(visualEvents: Array<Int>) {
        // inherits parent coroutine context from startGame()
        for (value in visualEvents) {
            _busyVisual.value = true
            _gameState.value = _gameState.value.copy(
                visualValue = value,
                indexValue = n
            )
            n += 1
            _busyVisual.value = false
            delay(eventInterval.value)
        }
        setPopupState(PopupState.EndGame)
        runPopup()
    }

    private suspend fun runAudioGame(audioEvents: Array<Int>) {
        // inherits parent coroutine context from startGame()
        initializeTextToSpeech()
        for (value in audioEvents) {
            _busyAudio.value = true
            _gameState.value = _gameState.value.copy(
                audioValue = value,
                indexValue = n)
            val letter = letters[value]
            speakText(letter)
            n += 1
            _busyAudio.value = false
            delay(eventInterval.value)
        }
        setPopupState(PopupState.EndGame)
        runPopup()
    }

    private suspend fun runAudioVisualGame(events: Array<Int>){
        secondEvents = createEvents()
        initializeTextToSpeech()
        Log.d("GameVM", "events: $events")
        Log.d("GameVM", "secondEvents: $secondEvents")

        // parallell iteration (both arrays are of the same size)
        for ((vValue, aValue) in events.zip(secondEvents)) {
            _busyVisual.value = true
            _busyAudio.value = true
            _gameState.value = _gameState.value.copy(
                audioValue = aValue,
                visualValue = vValue,
                indexValue = n
            )
            Log.d("GameVM","aValue: $aValue")
            Log.d("GameVM","vValue: $vValue")
            val letter = letters[aValue]
            speakText(letter)
            n += 1
            _busyAudio.value = false
            _busyVisual.value = false
            delay(eventInterval.value)
        }
        setPopupState(PopupState.EndGame)
        runPopup()
    }

    override fun runPopup() {
        job = viewModelScope.launch {
            when (popupState.value) {
                PopupState.None -> endPopup()
                PopupState.EndGame -> endGame()
                PopupState.SettingsSaved -> settingsPopup()
            }
        }
    }

    // STATE: END POPUP
    private fun endPopup() {
        job?.cancel()
        _showPopup.value = false
    }

    // STATE: END GAME
    private fun endGame() {
        _endGameMessage.value = "Game finished. Your final score is ${score.value}"
        _showPopup.value = true
        _gameHasEnded.value = true
    }

    // STATE: SETTINGS SAVED
    private fun settingsPopup() {
        _settingsSavedMessage.value = """
            n: ${nBack.value}
            Grid size: ${gridSize.value}*${gridSize.value}
            Time interval: ${eventInterval.value/1000}
            Round length: ${lengthOfGame.value}
            """
        _showPopup.value = true
    }

    override fun isVisualGameBusy(): Boolean {
        return if (_busyVisual.value) {
            true
        } else {
            false
        }
    }

    override fun isAudioGameBusy(): Boolean {
        return if (_busyAudio.value) {
            true
        } else {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown() // Release TTS engine
        textToSpeech = null
    }

    override fun checkMatch(): Boolean {
        val indexNow = _gameState.value.indexValue
        val visualValueNow = _gameState.value.visualValue

        if (indexNow >= nBack.value) {

            if (checkedIndices.contains(indexNow)) {
                return false // Already checked, so no match registered again
            }
            else {
                val indexNbackAgo = indexNow - nBack.value
                val visualValueNbackAgo = events[indexNbackAgo]

                if (visualValueNow == visualValueNbackAgo) {
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

    override fun checkAudioMatch(): Boolean {
        val indexNow = _gameState.value.indexValue
        val audioValueNow = _gameState.value.audioValue

        if (indexNow >= nBack.value) {
            if (checkedIndices.contains(indexNow)) {
                return false
            }
            else {
                val indexNbackAgo = indexNow - nBack.value
                val audioValueNbackAgo = events[indexNbackAgo]

                if (audioValueNow == audioValueNbackAgo) {
                    _score.value = score.value + 1
                    return true
                } else {
                    if (_score.value > 0) {
                        _score.value = score.value - 1
                    }
                    return false
                }
            }
        }
        return false
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
        textToSpeech?.speak(letter,TextToSpeech.QUEUE_FLUSH,null,null)
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

enum class PopupState {
    None, EndGame, SettingsSaved
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
    val visualValue: Int = -1,  // The value of the array string
    val audioValue: Int = -1,
    val indexValue: Int = 0
)

enum class Screen {
    Home, Game
}

class FakeVM(
) : GameViewModel{
    override val settingsSavedMessage: StateFlow<String>
        get() = MutableStateFlow("hej").asStateFlow()
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val endGameMessage: StateFlow<String>
        get() = MutableStateFlow("Test").asStateFlow()
    override val gridSize: StateFlow<Int>
        get() = MutableStateFlow(3).asStateFlow()
    override val lengthOfGame: StateFlow<Int>
        get() = MutableStateFlow(10).asStateFlow()
    override val eventInterval: StateFlow<Long>
        get() = MutableStateFlow(1000L).asStateFlow()
    override var busyVisual: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
        set(value) {}
    override var busyAudio: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
        set(value) {}
    override val showPopup: StateFlow<Boolean>
        get() = TODO("not yet impl")
    override val popupState: StateFlow<PopupState>
        get() = TODO("Not yet implemented")
    override val gameHasEnded: StateFlow<Boolean>
        get() = TODO("Not yet implemented")

    override fun runPopup() {
        TODO("Not yet implemented")
    }
    override fun setPopupState(popupState: PopupState) {
        TODO("Not yet implemented")
    }
    override fun isVisualGameBusy(): Boolean {
        TODO("Not yet implemented")
    }
    override fun isAudioGameBusy(): Boolean {
        TODO("Not yet implemented")
    }
    override fun setGameType(gameType: GameType) {}
    override fun setNvalue(newNvalue: Int) {
        TODO("Not yet implemented")
    }
    override fun setGridSize(newGridSize: Int) {
        TODO("Not yet implemented")
    }
    override fun setEventInterval(newEventInterval: Long) {
        TODO("Not yet implemented")
    }
    override fun setLengthOfGame(newLengthOfGame: Int) {
        TODO("Not yet implemented")
    }
    override fun startGame() {
    }
    override fun checkMatch(): Boolean {
    return false}
    override fun checkAudioMatch(): Boolean {
        TODO("Not yet implemented")
    }
}