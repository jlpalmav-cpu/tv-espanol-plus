package com.epalma.tvespanolplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val screen by vm.screen.collectAsState()
            BackHandler(enabled = screen != Screen.HOME) {
                when (screen) {
                    Screen.PLAYER, Screen.DUAL, Screen.CHANNELS, Screen.SEARCH, Screen.PLAYLISTS, Screen.SETTINGS, Screen.ABOUT -> vm.go(Screen.HOME)
                    Screen.HOME -> Unit
                }
            }
            TVEspanolPlusRoot(vm = vm, onExit = { finishAffinity() })
        }
    }
}
