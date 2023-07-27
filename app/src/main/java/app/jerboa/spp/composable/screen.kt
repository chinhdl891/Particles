package app.jerboa.spp.composable

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import app.jerboa.spp.AppInfo
import app.jerboa.spp.ViewModel.COLOUR_MAP
import app.jerboa.spp.ViewModel.MUSIC
import app.jerboa.spp.ViewModel.SOCIAL
import app.jerboa.spp.ViewModel.TOY
import app.jerboa.spp.data.ColourMap
import app.jerboa.spp.ui.SPPView
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun screen(
    displayingMenu: Boolean,
    displayingSound: Boolean,
    displayingAbout: Boolean,
    toy: TOY,
    particleNumber: Float,
    allowAdapt: Boolean,
    colourMap: COLOUR_MAP,
    playingMusic: MUSIC,
    adaptMsg: Boolean,
    promptPGS: Boolean,
    resolution: Pair<Int,Int>,
    images: Map<String,Int>,
    info: AppInfo,
    onDisplayingMenuChanged: (Boolean) -> Unit,
    onDisplayingMusicChanged: () -> Unit,
    onDisplayingAboutChanged: (Boolean) -> Unit,
    onAttractorChanged: (TOY) -> Unit,
    onRequestPlayServices: () -> Unit,
    onAchievementStateChanged: (Pair<String,Int>) -> Unit,
    onAdapt: (Float) -> Unit,
    onAllowAdaptChanged: () -> Unit,
    onAdaptMessageShown: () -> Unit,
    onRequestingLicenses: () -> Unit,
    onParticleNumberChanged: (Float) -> Unit,
    onSelectColourMap: (COLOUR_MAP) -> Unit,
    selectDefaultColourMap: () -> Unit,
    onMusicSelected: (MUSIC) -> Unit,
    onRequestingSocial: (SOCIAL) -> Unit,
    onResetTutorial: () -> Unit,
    onPromptPGS: (Boolean) -> Unit
) {

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val seenHelp = remember { mutableStateOf(!info.firstLaunch) }

    val width75Percent = info.widthDp * 0.75
    val height25Percent = info.heightDp * 0.25
    val height10Percent = info.heightDp * 0.1
    val menuItemHeight = height10Percent * 0.66

    selectDefaultColourMap()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {

            },
            bottomBar = {
                menu(
                    displayingMenu,
                    particleNumber,
                    width75Percent,
                    height10Percent,
                    menuItemHeight,
                    images,
                    info,
                    onDisplayingAboutChanged,
                    onAttractorChanged,
                    onRequestPlayServices,
                    onParticleNumberChanged,
                    onSelectColourMap
                )
            }
        ) {
            if (adaptMsg) {
                onAdaptMessageShown()
                coroutineScope.launch {
                    val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                        message = "FPS lower than 30 adapting...",
                        actionLabel = "STOP!",
                        duration = SnackbarDuration.Short
                    )
                    when (snackbarResult) {
                        SnackbarResult.Dismissed -> Log.d("screen", "Dismissed")
                        SnackbarResult.ActionPerformed -> {
                            onAllowAdaptChanged()
                        }
                    }
                }
            }
            if (promptPGS) {
                coroutineScope.launch {
                    val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                        message = "Achievements require Play Games Services",
                        actionLabel = "Install",
                        duration = SnackbarDuration.Short
                    )
                    when (snackbarResult) {
                        SnackbarResult.Dismissed -> Log.d("screen", "Dismissed")
                        SnackbarResult.ActionPerformed -> {
                            onPromptPGS(true)
                        }
                    }
                }
                onPromptPGS(false)
            }
            AndroidView(
                factory = {
                    SPPView(
                        it, null,
                        resolution,
                        onDisplayingMenuChanged,
                        onAchievementStateChanged,
                        onAdapt,
                        toy,
                        particleNumber,
                        allowAdapt,
                        colourMap
                    )
                },
                update = { view ->
                    view.placingToy = toy
                    view.particleNumber = particleNumber
                    view.setParticleNumber(particleNumber)
                    view.setAllowAdapt(allowAdapt)
                    view.setColourMap(colourMap)
                }
            )
            about(
                displayingAbout,
                width75Percent,
                images,
                info,
                onRequestingLicenses,
                onRequestingSocial,
                onResetTutorial
            )
            menuPrompt(images,displayingMenu,displayingSound,menuItemHeight,onDisplayingMenuChanged,onDisplayingMusicChanged,onMusicSelected)
        }
    }
}