package app.jerboa.spp.ViewModel

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import app.jerboa.spp.R
import app.jerboa.spp.incrementable
import java.util.*

const val MAX_PARTICLES = 500000f
const val PARTICLES_SLIDER_DEFAULT = 100000f/ MAX_PARTICLES

enum class COLOUR_MAP {
    R1,
    R2,
    ACE,
    C3,
    CB1,
    CB2,
    TRANS,
    PRIDE
}

enum class MUSIC {FORREST, RAIN, NOTHING}

enum class TOY {ATTRACTOR,REPELLOR,SPINNER}

enum class SOCIAL {NOTHING, WEB, PLAY, YOUTUBE, GITHUB}

class RenderViewModel : ViewModel() {

    private val musicPlayer: MediaPlayer = MediaPlayer()

    private val _achievementStates = MutableLiveData(
        mapOf(
            "GetLost" to Pair(0,0),
            "SuperFan" to Pair(0,0),
            "ShowMeWhatYouGot" to Pair(0,0),
            "AverageFan" to Pair(0,0),
            "AverageEnjoyer" to Pair(0,0),
            "StillThere" to Pair(0,0),
            "HipToBeSquare" to Pair(0,0),
            "CirclesTheWay" to Pair(0,0),
            "DoubleTrouble" to Pair(0,0)
        )
    )

    val achievementStates: MutableLiveData<Map<String, Pair<Int, Int>>> = _achievementStates

    // TUTORIAL

    private val _dismissedTutorial = MutableLiveData(false)
    private val _resetTutorial = MutableLiveData(false)

    val dismissedTutorial: MutableLiveData<Boolean> = _dismissedTutorial
    val resetTutorial: MutableLiveData<Boolean> = _resetTutorial

    fun onResetTutorial(){
        _resetTutorial.value = true; _dismissedTutorial.value = false
        onDisplayingMenuChanged(false)
    }
    fun onDismissTutorial(){_dismissedTutorial.value = true; _resetTutorial.value = false}

    // SOCIAL
    private val _requestingSocial = MutableLiveData(SOCIAL.NOTHING)
    val requestingSocial: MutableLiveData<SOCIAL> = _requestingSocial

    fun onRequestingSocial(v: SOCIAL){
        _requestingSocial.value = v
    }

    // menu displays

    private val _displayingMenu = MutableLiveData(false)
    private val _displayingSound = MutableLiveData(false)
    private val _displayingAbout = MutableLiveData(false)

    val displayingSound: MutableLiveData<Boolean> = _displayingSound
    val displayingMenu : MutableLiveData<Boolean> = _displayingMenu
    val displayingAbout: MutableLiveData<Boolean> = _displayingAbout

    private val _lastClick = MutableLiveData(0L)

    fun onDisplayingMenuChanged(newVal: Boolean){
        _displayingMenu.value = !_displayingMenu.value!!
        if (_displayingMenu.value == false){
            _displayingAbout.value = false
        }

        if (_displayingMenu.value == true){
            _displayingSound.value = false
        }

        _lastClick.value = System.currentTimeMillis()

    }

    fun onDisplayingMusicChanged(){
        _displayingSound.value = !_displayingSound.value!!
        if (_displayingSound.value == true){
            _displayingMenu.value = false
            _displayingAbout.value = false
        }
    }

    fun onDisplayingAboutChanged(newVal: Boolean){
        _displayingAbout.value = !_displayingAbout.value!!
    }

    // MUSIC

    private val _playingMusic = MutableLiveData(MUSIC.NOTHING)
    val playingMusic: MutableLiveData<MUSIC> = _playingMusic

    fun onMusicSelected(v: MUSIC){
        _playingMusic.value = v
    }
    // COLOUR

    private val _colourMap = MutableLiveData(COLOUR_MAP.R1)
    val colourMap: MutableLiveData<COLOUR_MAP> = _colourMap

    fun onSelectColourMap(v: COLOUR_MAP){
        _colourMap.value = v
    }

    private val _selectedDefaultColourMap = MutableLiveData(false)

    fun selectDefaultColourMap(){

        if (_selectedDefaultColourMap.value == false){return}

        val date = Calendar.getInstance()
        val month = date.get(Calendar.MONTH)
        val day = date.get(Calendar.DAY_OF_MONTH)

        if (month == Calendar.MARCH && day == 31){
            _colourMap.value = COLOUR_MAP.TRANS
        }
        else if (month == Calendar.JUNE && day == 28){
            _colourMap.value = COLOUR_MAP.PRIDE
        }
        else if (month == Calendar.APRIL && day == 6){
            _colourMap.value = COLOUR_MAP.ACE
        }

        _selectedDefaultColourMap.value = true
    }

    private val _toy = MutableLiveData(TOY.ATTRACTOR) // true attractor, false repeller
    val toy: MutableLiveData<TOY> = _toy
    fun onAttractorChanged(newVal: TOY){
        //Log.d("on attractor changed", newVal.toString())
        _toy.value = newVal
    }

    // PGS

    private val _requestingPlayServices = MutableLiveData(false)
    val requestingPlayServices: MutableLiveData<Boolean> = _requestingPlayServices

    fun onRequestPlayServices(){
        _requestingPlayServices.value = true
    }

    fun onRequestPGSAndNotInstalled(){
        _promptInstallPGS.value = true
    }

    fun onPromptInstallPGS(v: Boolean = false){
        _promptInstallPGS.value = false
        if (v) {
            _requestingInstallPGS.value = true
        }
    }

    fun onInstallPGSInitiated(){
        _requestingInstallPGS.value = false
    }

    private val _promptInstallPGS = MutableLiveData(false)
    val promptInstallPGS: MutableLiveData<Boolean> = _promptInstallPGS

    private val _requestingInstallPGS = MutableLiveData(false)
    val requestingInstallPGS: MutableLiveData<Boolean> = _requestingInstallPGS

    private val _requestingLicenses = MutableLiveData(false)
    val requestingLicenses: MutableLiveData<Boolean> = _requestingLicenses

    fun onRequestingLicenses(){
        _requestingLicenses.value = true
    }

    private val _particleNumber = MutableLiveData(PARTICLES_SLIDER_DEFAULT)
    val particleNumber: MutableLiveData<Float> = _particleNumber

    fun onParticleNumberChanged(v: Float){
        _particleNumber.value = v
    }

    // ADAPT

    private val _allowAdapt = MutableLiveData(true)
    private val _autoAdaptMessage = MutableLiveData(false)
    val allowAdapt: MutableLiveData<Boolean> = _allowAdapt
    val autoAdaptMessage: MutableLiveData<Boolean> = _autoAdaptMessage

    fun onAdapt(v: Float) {
        _particleNumber.postValue(v)
        _autoAdaptMessage.postValue(true)
    }

    fun onAllowAdaptChanged(){
        _allowAdapt.value = !_allowAdapt.value!!
    }

    fun onAdaptMessageShown(){
        _autoAdaptMessage.value = false
    }

    fun onAchievementStateChanged(data: Pair<String,Int>){
        //Log.d("achievements", data.toString())
        val ach = data.first
        val value = data.second

        val state = _achievementStates.value!!.toMutableMap()

        if (!state.containsKey(ach)){
            Log.e("achievement state does not contain key",ach)
            return
        }

        if (state[ach]!!.first == state[ach]!!.second){
            return
        }

        if (ach in incrementable) {
            state[ach] = Pair(
                achievementStates.value!![ach]!!.first + value,
                achievementStates.value!![ach]!!.second
            )
        }
        else{
            state[ach] = Pair(1,1)
        }

        _achievementStates.postValue(state.toMap())
    }

    fun setAchievementState(states: MutableMap<String, Pair<Int, Int>>){
        _achievementStates.value = states
        achievementStates.value = states
    }

}