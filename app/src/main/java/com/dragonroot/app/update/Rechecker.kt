package com.dragonroot.app.update

import com.dragonroot.app.analysis.CompatibilityEngine
import com.dragonroot.app.analysis.FirmwareLogic
import com.dragonroot.app.data.AppDao
import com.dragonroot.app.data.CompatSnapshotEntity
import com.dragonroot.app.data.SettingsStore
import com.dragonroot.app.model.DeviceProfile

/**
 * Re-evaluates every catalogued project against the device after data changes.
 * Reports (a) a newer official build recorded for this model and (b) any change in compatibility status.
 * A new software update never turns a result green by itself: results come only from the engine's rules.
 */
class Rechecker(
    private val dao: AppDao,
    private val engine: CompatibilityEngine,
    private val settings: SettingsStore,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun run(profile: DeviceProfile): List<String> {
        val messages = mutableListOf<String>()
        val firmware = dao.firmwareOnce()
        val projects = dao.projectsOnce()
        val records = dao.recordsOnce()
        val evidence = dao.evidenceOnce().associateBy { it.id }
        val model = profile.model.value
        val latest = FirmwareLogic.latestFor(model, null, firmware)

        if (model != null && latest != null) {
            val seen = settings.lastSeenLatestBuild(model)
            if (seen != null && !seen.equals(latest.buildNumber, true)) {
                messages += "New official firmware recorded for your model: ${latest.buildNumber}. " +
                    "Compatibility results for older builds do not apply to it."
            }
            settings.setLastSeenLatestBuild(model, latest.buildNumber)
        }

        for (project in projects) {
            val result = engine.evaluate(profile, project, records, evidence, latest)
            val previous = dao.snapshot(project.id)
            if (previous != null && previous.status != result.status) {
                messages += "${project.name}: ${previous.status.label} → ${result.status.label}"
            }
            dao.upsertSnapshot(CompatSnapshotEntity(project.id, result.status, profile.buildNumber.value, now()))
        }
        return messages
    }
}
