package com.dragonroot.app

import android.app.Application
import com.dragonroot.app.update.UpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DragonRootApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.seedBuiltIns() }
        appScope.launch {
            container.settings.flow
                .map { it.backgroundSync to it.unmeteredOnly }
                .distinctUntilChanged()
                .collect { (enabled, unmetered) -> UpdateScheduler.apply(this@DragonRootApp, enabled, unmetered) }
        }
    }
}
