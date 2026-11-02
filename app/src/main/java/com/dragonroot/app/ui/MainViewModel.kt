package com.dragonroot.app.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dragonroot.app.AppContainer
import com.dragonroot.app.analysis.BootImageAnalyzer
import com.dragonroot.app.analysis.BootImageInfo
import com.dragonroot.app.analysis.CompatibilityResult
import com.dragonroot.app.analysis.FirmwareLogic
import com.dragonroot.app.data.FirmwareEntity
import com.dragonroot.app.data.ResearchProjectEntity
import com.dragonroot.app.data.SettingsState
import com.dragonroot.app.model.DeviceProfile
import com.dragonroot.app.update.ImportOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompatEntry(val project: ResearchProjectEntity, val result: CompatibilityResult)

class MainViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.dao
    private fun <T> kotlinx.coroutines.flow.Flow<T>.hot(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    private val _profile = MutableStateFlow<DeviceProfile?>(null)
    val profile: StateFlow<DeviceProfile?> = _profile.asStateFlow()

    val devices = dao.observeDevices().hot(emptyList())
    val firmware = dao.observeFirmware().hot(emptyList())
    val kernelProfiles = dao.observeKernelProfiles().hot(emptyList())
    val advisories = dao.observeAdvisories().hot(emptyList())
    val projects = dao.observeProjects().hot(emptyList())
    val records = dao.observeRecords().hot(emptyList())
    val evidence = dao.observeEvidence().hot(emptyList())
    val releases = dao.observeReleases().hot(emptyList())
    val settings: StateFlow<SettingsState> = c.settings.flow.hot(SettingsState())

    /** Latest OFFICIAL firmware known for the detected model (null = not established). */
    val latestFirmware: StateFlow<FirmwareEntity?> =
        combine(profile, firmware) { p, f -> FirmwareLogic.latestFor(p?.model?.value, null, f) }.hot(null)

    val compat: StateFlow<List<CompatEntry>> =
        combine(profile, projects, records, evidence, latestFirmware) { p, projs, recs, ev, latest ->
            if (p == null) emptyList()
            else {
                val byId = ev.associateBy { it.id }
                projs.map { CompatEntry(it, c.engine.evaluate(p, it, recs, byId, latest)) }
            }
        }.hot(emptyList())

    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch {
            c.seedBuiltIns()
            _profile.value = c.collectProfile()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncing.value = true
            _message.value = c.updateService.syncNow().message
            _syncing.value = false
        }
    }

    /** Debug builds only: loads bundled, clearly fake sample data without a signature. */
    fun importSample() {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                c.appContext.assets.open("sample_release.json").bufferedReader().use { it.readText() }
            }
            _message.value = when (val o = c.importer.importUnsignedSample(json)) {
                is ImportOutcome.Imported -> "Imported SAMPLE release v${o.version}: ${o.summary}"
                is ImportOutcome.UpToDate -> "Already up to date"
                is ImportOutcome.Rejected -> o.reason
            }
        }
    }

    fun setBackgroundSync(v: Boolean) = viewModelScope.launch { c.settings.setBackgroundSync(v) }
    fun setUnmeteredOnly(v: Boolean) = viewModelScope.launch { c.settings.setUnmeteredOnly(v) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { c.settings.setNotifications(v) }
    fun wipe() = viewModelScope.launch { c.wipeLocalData(); _message.value = "All local data deleted." }
}

class BootImageViewModel(private val appContext: Context) : ViewModel() {
    data class Slot(val info: BootImageInfo? = null, val loading: Boolean = false, val error: String? = null)

    private val _a = MutableStateFlow(Slot())
    private val _b = MutableStateFlow(Slot())
    val a = _a.asStateFlow()
    val b = _b.asStateFlow()

    fun analyze(uri: Uri, slotB: Boolean) {
        val slot = if (slotB) _b else _a
        slot.value = Slot(loading = true)
        viewModelScope.launch {
            slot.value = try {
                Slot(info = BootImageAnalyzer(appContext.contentResolver).analyze(uri))
            } catch (e: Exception) {
                Slot(error = e.message ?: "Could not read the selected file")
            }
        }
    }
}
