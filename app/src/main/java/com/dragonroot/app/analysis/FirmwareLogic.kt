package com.dragonroot.app.analysis

import com.dragonroot.app.data.FirmwareEntity
import com.dragonroot.app.model.DeviceProfile
import com.dragonroot.app.model.FieldDiff

object FirmwareLogic {

    fun matchesModel(fw: FirmwareEntity, model: String?): Boolean {
        if (model.isNullOrBlank()) return false
        return fw.deviceModel.equals(model, true) || fw.modelNumber.equals(model, true)
    }

    /**
     * Latest OFFICIAL firmware for a model (optionally a region). Third-party records are ignored on
     * purpose: they must never be presented as the latest official build.
     */
    fun latestFor(model: String?, region: String?, all: List<FirmwareEntity>): FirmwareEntity? =
        all.filter { it.official && matchesModel(it, model) && (region == null || it.region.equals(region, true)) }
            .maxWithOrNull(compareBy<FirmwareEntity>({ it.releaseDate ?: Long.MIN_VALUE }, { it.securityPatch ?: "" }))

    /** Warnings when comparing a firmware record with the running device. */
    fun warningsAgainstDevice(profile: DeviceProfile, fw: FirmwareEntity): List<String> {
        val w = mutableListOf<String>()
        val model = profile.model.value
        if (!matchesModel(fw, model)) w += "Record is for ${fw.deviceModel}, not your model (${model ?: "unknown"})."
        val build = profile.buildNumber.value
        if (build != null && !build.equals(fw.buildNumber, true)) w += "Record is for build ${fw.buildNumber}; your device reports $build."
        if (fw.region != null) w += "Region ${fw.region}: Android cannot reliably report your sales region, so confirm it matches before using any file."
        return w
    }

    data class Comparison(val warnings: List<String>, val diffs: List<FieldDiff>)

    /** Compares two builds. Only meaningful for the same exact model and region. */
    fun compare(a: FirmwareEntity, b: FirmwareEntity): Comparison {
        val warnings = mutableListOf<String>()
        if (!a.deviceModel.equals(b.deviceModel, true)) warnings += "Different models (${a.deviceModel} vs ${b.deviceModel}); comparison is not meaningful."
        if (!a.region.equals(b.region, true)) warnings += "Different regions (${a.region ?: "n/a"} vs ${b.region ?: "n/a"}); builds are not interchangeable."
        fun s(v: String?) = v ?: "Not published"
        val diffs = listOf(
            FieldDiff("Build number", a.buildNumber, b.buildNumber),
            FieldDiff("Android version", s(a.androidVersion), s(b.androidVersion)),
            FieldDiff("Vendor OS", s(a.vendorOs), s(b.vendorOs)),
            FieldDiff("Security patch", s(a.securityPatch), s(b.securityPatch)),
            FieldDiff("Kernel version", s(a.kernelVersion), s(b.kernelVersion)),
            FieldDiff("Release notes", a.releaseNotes?.take(120) ?: "Not published", b.releaseNotes?.take(120) ?: "Not published")
        )
        return Comparison(warnings, diffs)
    }
}
