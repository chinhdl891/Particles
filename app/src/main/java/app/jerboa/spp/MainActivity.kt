package app.jerboa.spp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import app.jerboa.spp.ViewModel.MUSIC
import app.jerboa.spp.ViewModel.RenderViewModel
import app.jerboa.spp.ViewModel.SOCIAL
import app.jerboa.spp.composable.renderScreen
import app.jerboa.spp.ui.theme.SPPTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.games.AuthenticationResult
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.achievement.Achievement
import com.google.android.gms.games.achievement.AchievementBuffer
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.gms.tasks.Task
import java.lang.Integer.min
import java.util.*

val DEBUG = false

data class AppInfo(
    val versionString: String,
    val firstLaunch: Boolean,
    val playGamesServices: Boolean,
    val density: Float,
    val heightDp: Float,
    val widthDp: Float
)

val achievementNameToID = mapOf(
    "GetLost" to "CggIk4Pih0AQAhAB",
    "SuperFan" to "CggIk4Pih0AQAhAC",
    "ShowMeWhatYouGot" to "CggIk4Pih0AQAhAD",
    "AverageFan" to "CggIk4Pih0AQAhAE",
    "AverageEnjoyer" to "CggIk4Pih0AQAhAF",
    "StillThere" to "CggIk4Pih0AQAhAG",
    "HipToBeSquare" to "CggIk4Pih0AQAhAH",
    "DoubleTrouble" to "CggIk4Pih0AQAhAI",
    "CirclesTheWay" to "CggIk4Pih0AQAhAJ"
)

val achievementsIDToName = achievementNameToID.entries.associate{(k,v)-> v to k}

val incrementable = listOf(
    "SuperFan",
    "AverageFan",
    "AverageEnjoyer"
)

val achievementStates = mutableMapOf(
    "GetLost" to Pair(0,1),
    "SuperFan" to Pair(0,1),
    "ShowMeWhatYouGot" to Pair(0,1),
    "AverageFan" to Pair(0,1),
    "AverageEnjoyer" to Pair(0,1),
    "StillThere" to Pair(0,1),
    "HipToBeSquare" to Pair(0,1),
    "DoubleTrouble" to Pair(0,1),
    "CirclesTheWay" to Pair(0,1)
)

class MainActivity : AppCompatActivity() {

    private val renderViewModel by viewModels<RenderViewModel>()

    private var playSuccess = false

    private var mediaPlayer = MediaPlayer()

    private val imageResources: Map<String,Int> = mapOf(
        "logo" to R.drawable.ic_logo,
        "about" to R.drawable.about_,
        "attractor" to R.drawable.attractor_,
        "repeller" to R.drawable.repeller_,
        "spinner" to R.drawable.spinner_,
        "play-controller" to R.drawable.games_controller_grey,
        "play-logo" to R.drawable.play_,
        "music" to R.drawable.ic_music,
        "burger" to R.drawable.ic_burger,
        "dismiss" to R.drawable.ic_dismiss,
        "rainbow1" to R.drawable.rainbow,
        "rainbow2" to R.drawable.c1,
        "ace" to R.drawable.ace,
        "c3" to R.drawable.c3,
        "cb1" to R.drawable.cblind1,
        "cb2" to R.drawable.cblind2,
        "trans" to R.drawable.trans,
        "pride" to R.drawable.pride,
        "music-forrest" to R.drawable.music_forrest_,
        "music-rain" to R.drawable.music_rain_,
        "music-none" to R.drawable.music_none_,
        "yt" to R.drawable.ic_yt,
        "web" to R.drawable.weblink_icon_,
        "github" to R.drawable.github_mark_white,
        "tutorial" to R.drawable.tutorial,
        "news" to R.drawable.news
    )

    private val rcAchievementUI = 9003

    private fun isGooglePlayGamesServicesInstalled(activity: Activity): Boolean {
        val v = activity.packageManager.getLaunchIntentForPackage("com.google.android.play.games") != null
        if (DEBUG) { Log.d("isGooglePlayGamesServicesInstalled", v.toString()) }
        return v
    }

    private fun showAchievements() {
        if (!isGooglePlayGamesServicesInstalled(this)){
            renderViewModel.onRequestPGSAndNotInstalled()
            return
        }
        PlayGames.getAchievementsClient(this)
            .achievementsIntent
            .addOnSuccessListener { intent -> startActivityForResult(intent, rcAchievementUI); if (DEBUG) { Log.d("showAchievements","success") } }
            .addOnFailureListener { if (DEBUG) { Log.d("showAchievements failure","${it.toString()}") } }
    }

    private fun playGamesServicesLogin() {
        if (!isGooglePlayGamesServicesInstalled(this)){
            return
        }
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)

        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
            val isAuthenticated = isAuthenticatedTask.isSuccessful && isAuthenticatedTask.result.isAuthenticated
            if (isAuthenticated) {
                // Continue with Play Games Services
                playSuccess = true
                if (DEBUG) { Log.d("playGames","success") }
            } else {
                // Disable your integration with Play Games Services or show a
                // login button to ask  players to sign-in. Clicking it should
                // call GamesSignInClient.signIn().
                // calling that will trigger sign in, and update the play store etc.
                if (DEBUG) { Log.d("playGames","failure ${isAuthenticatedTask.result.toString()}") }
                playSuccess = false
            }
        }
    }

    private fun updatePlayGamesAchievements(states: Map<String,Pair<Int,Int>>){
        if (!isGooglePlayGamesServicesInstalled(this)){return}
        if (DEBUG) { Log.d("achievements","called update play services") }
        for (s in states){
            val id: String = achievementNameToID[s.key]!!

            if (s.value.first == s.value.second) {
                // unlocked
                PlayGames.getAchievementsClient(this).unlock(id)
            }

            if (s.key in incrementable) {
                PlayGames.getAchievementsClient(this).setSteps(id,min(s.value.first,s.value.second))
            }

        }
    }

    private fun syncAchievementsState(){
        if (!isGooglePlayGamesServicesInstalled(this)){return}
        PlayGames.getAchievementsClient(this).load(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val achievements: AchievementBuffer? = task.result.get()
                if (achievements != null) {
                    for (i in 0 until achievements.count){
                        val ach: Achievement = achievements.get(i)
                        val id = ach.achievementId
                        val state = ach.state

                        val unlocked = if(state == Achievement.STATE_UNLOCKED){1}else{0}

                        if (state == Achievement.TYPE_INCREMENTAL){
                            achievementStates[achievementsIDToName[id].toString()]=Pair(
                                ach.currentSteps,
                                ach.totalSteps
                            )
                        }
                        else{
                            achievementStates[achievementsIDToName[id].toString()]=Pair(
                                unlocked,
                                1
                            )
                        }
                        if (DEBUG) { Log.d("loadAchievements", id + " " + achievementsIDToName[id].toString() + ": " + achievementStates[achievementsIDToName[id].toString()] ) }
                    }
                }
            }
        }
    }

    private fun playRate(){
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://play.google.com/store/apps/details?id=app.jerboa.spp"
                )
                setPackage("com.android.vending")
            }
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException){
            Log.e("playRate","ActivityNotFoundException, is Google Play Installed?")
        }
    }

    private fun youtube(){

        val uri = Uri.parse("https://www.youtube.com/channel/UCP3KhLhmG3Z1CMWyLkn7pbQ")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)

    }

    private fun web(){

        val uri = Uri.parse("https://jerboa.app")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)

    }

    private fun github(){

        val uri = Uri.parse("https://github.com/JerboaBurrow/Particles")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)

    }

    private fun showLicenses(){
        val intent = Intent(this.applicationContext, OssLicensesMenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //on opening OSS sometimes there is a crash..
        //https://github.com/google/play-services-plugins/issues/100
        //com.google.android.gms.internal.oss_licenses.zzf.dummy_placeholder = getResources().getIdentifier("third_party_license_metadata", "raw", getPackageName());
        startActivity(intent)
    }

    private fun installPGS(){
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://play.google.com/store/apps/details?id=com.google.android.play.games"
                )
                setPackage("com.android.vending")
            }
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException){
            Log.e("playRate","ActivityNotFoundException, is Google Play Installed?")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (DEBUG) { Log.d("playServices","$status ${status==ConnectionResult.SUCCESS}") }

        renderViewModel.requestingPlayServices.observe(
            this, androidx.lifecycle.Observer { request -> if(request){
                if (DEBUG) { Log.d("playGames","$playSuccess") }
                if (!playSuccess && isGooglePlayGamesServicesInstalled(this)){
                        if (DEBUG) { Log.d("playGames","login") }
                        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
                        gamesSignInClient.signIn()
                            .addOnFailureListener {
                                if (DEBUG) { Log.d("playGames","Login failed") }
                            }
                            .addOnSuccessListener {
                                if (DEBUG) { Log.d("playGames","Login Success") }
                            }
                    }
                showAchievements()
            }}
        )

        renderViewModel.requestingInstallPGS.observe(
            this, androidx.lifecycle.Observer {
                request -> if(request){
                   installPGS()
                    renderViewModel.onInstallPGSInitiated()
                }
            }
        )

        renderViewModel.requestingLicenses.observe(
            this, androidx.lifecycle.Observer { request -> if(request){ showLicenses() }}
        )

        renderViewModel.requestingSocial.observe(
            this, androidx.lifecycle.Observer { request ->
                when (request){
                    SOCIAL.WEB -> web()
                    SOCIAL.PLAY -> playRate()
                    SOCIAL.YOUTUBE -> youtube()
                    SOCIAL.GITHUB -> github()
                }
            }
        )

        renderViewModel.playingMusic.observe(
            this,androidx.lifecycle.Observer { playingMusic ->
                when(playingMusic) {
                    MUSIC.FORREST -> {
                        mediaPlayer.release()
                        mediaPlayer = MediaPlayer.create(this,R.raw.forrest)
                        mediaPlayer.isLooping = true
                        mediaPlayer.start()
                    }
                    MUSIC.RAIN -> {
                        mediaPlayer.release()
                        mediaPlayer = MediaPlayer.create(this,R.raw.rain)
                        mediaPlayer.isLooping = true
                        mediaPlayer.start()
                    }
                    MUSIC.NOTHING -> {
                        mediaPlayer.release()
                    }
                }
            }
        )

        val prefs = getSharedPreferences("jerboa.app.spp.prefs", MODE_PRIVATE)

//        if (BuildConfig.DEBUG){
//            prefs.edit().clear().apply()
//        }

        if (!prefs.contains("firstLaunch")){
            val prefsEdit = prefs.edit()
            prefsEdit.putBoolean("firstLaunch",true)
            prefsEdit.apply()
        }

        val firstLaunch: Boolean = prefs.getBoolean("firstLaunch",false)

        val prefsEdit = prefs.edit()
        prefsEdit.putBoolean("firstLaunch",false)
        prefsEdit.apply()

        if (DEBUG) { Log.d("launch", firstLaunch.toString()) }

        val versionString = BuildConfig.VERSION_NAME + ": " + Date(BuildConfig.TIMESTAMP)

        var showNews = false
        if (!firstLaunch) {
            if (!prefs.contains("news-28-07-23")){
                val prefsEdit = prefs.edit()
                prefsEdit.putBoolean("news-28-07-23",true)
                prefsEdit.apply()
                showNews = true
            }
        }

        // play game services

        if (isGooglePlayGamesServicesInstalled(this)) {

            PlayGamesSdk.initialize(this)
            playGamesServicesLogin()

            syncAchievementsState()

            renderViewModel.setAchievementState(achievementStates)

        }

        renderViewModel.achievementStates.observe(
            this, androidx.lifecycle.Observer {
                    states -> updatePlayGamesAchievements(states)
            }
        )

        val displayInfo = resources.displayMetrics
        val dpHeight = displayInfo.heightPixels / displayInfo.density
        val dpWidth = displayInfo.widthPixels / displayInfo.density
        val appInfo = AppInfo(
            versionString,
            firstLaunch,
            playSuccess,
            if (resources.getBoolean(R.bool.isTablet)){displayInfo.density}else{1f},
            dpHeight,
            dpWidth
        )

        if (DEBUG) { Log.d("density", appInfo.density.toString()) }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        setContent {
            SPPTheme {
                // A surface container using the 'background' color from the theme
                renderScreen(
                    renderViewModel,
                    Pair(width,height),
                    imageResources,
                    appInfo,
                    showNews
                )
            }
        }
    }
}