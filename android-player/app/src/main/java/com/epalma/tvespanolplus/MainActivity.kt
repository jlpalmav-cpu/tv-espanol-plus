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
            val filter by vm.filter.collectAsState()
            BackHandler(enabled = screen != Screen.HOME) {
                when (screen) {
                    Screen.CHANNELS -> when {
                        filter.startsWith("CountryCat:") -> {
                            val country = filter.removePrefix("CountryCat:").substringBefore('|')
                            vm.openCategory("País:$country")
                        }
                        filter.startsWith("País:") -> vm.openCategory("Países")
                        filter.startsWith("Subcat:") -> vm.openCategory(filter.removePrefix("Subcat:").substringBefore('|'))
                        else -> vm.go(Screen.HOME)
                    }
                    Screen.PLAYER, Screen.DUAL, Screen.SEARCH, Screen.PLAYLISTS, Screen.SETTINGS, Screen.ABOUT -> vm.go(Screen.HOME)
                    Screen.HOME -> Unit
                }
            }
            TVEspanolPlusRootV6(vm = vm, onExit = { finishAffinity() })
        }
    }
}
