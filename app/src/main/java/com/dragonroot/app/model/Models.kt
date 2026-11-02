package com.dragonroot.app.model

enum class CompatStatus(val label: String) {
    VERIFIED("Verified"),
    PARTIALLY_VERIFIED("Partially verified"),
    EXPERIMENTAL("Experimental"),
    UNSUPPORTED("Unsupported"),
    UNKNOWN("Unknown")
}

enum class EvidenceGrade(val description: String) {
    A("Official manufacturer documentation or official security advisory"),
    B("Original research from a recognized project or researcher"),
    C("Reproducible community testing with exact device/build details"),
    D("Unverified report or third-party repost");

    /** Grade D alone can never support a VERIFIED or UNSUPPORTED result. */
    val isCredible: Boolean get() = this != D
}

enum class Confidence { HIGH, MEDIUM, LOW, NONE }
enum class Manufacturer { VIVO, IQOO, OTHER }
enum class VerificationState { VERIFIED, UNVERIFIED, NEEDS_REVIEW }
enum class BootloaderRequirement { ANY, UNLOCKED_REQUIRED, NOT_ENTERED }
enum class BootloaderState { LOCKED, UNLOCKED, UNKNOWN }

enum class ProjectCategory(val label: String) {
    ROOT_MANAGER("Root-management project"),
    RESEARCH_METHOD("Documented research method"),
    CUSTOM_KERNEL("Custom kernel project")
}

enum class RootState(val label: String) {
    NO_ROOT_DETECTED("No root detected"),
    ROOT_DETECTED("Root detected"),
    UNAVAILABLE("Root status unavailable"),
    TEMPORARY_NOT_CONFIRMED("Temporary status not confirmed"),
    DEVICE_STATE_UNKNOWN("Device state unknown")
}

/** A single detected value together with where it came from. Never invented: missing = unavailable. */
data class Reading(val value: String?, val source: String, val confidence: Confidence) {
    val isAvailable: Boolean get() = !value.isNullOrBlank()
    val display: String get() = if (isAvailable) value!! else "Unavailable"

    companion object {
        fun unavailable(source: String) = Reading(null, source, Confidence.NONE)
    }
}

data class RootResult(
    val state: RootState,
    val method: String,
    val confidence: Confidence,
    val indicators: List<String>,
    val explanation: String
)

data class DeviceProfile(
    val manufacturer: Reading,
    val brand: Reading,
    val model: Reading,
    val codename: Reading,
    val product: Reading,
    val androidVersion: Reading,
    val apiLevel: Int,
    val buildId: Reading,
    val fingerprint: Reading,
    val buildNumber: Reading,
    val vendorOs: Reading,
    val securityPatch: Reading,
    val kernelRelease: Reading,
    val kernelBuild: Reading,
    val abis: List<String>,
    val soc: Reading,
    val bootloader: Reading,
    val bootloaderState: BootloaderState,
    val selinux: Reading,
    val root: RootResult
) {
    val manufacturerKind: Manufacturer
        get() = when {
            brand.value.equals("iqoo", true) || manufacturer.value.equals("iqoo", true) -> Manufacturer.IQOO
            manufacturer.value.equals("vivo", true) || brand.value.equals("vivo", true) -> Manufacturer.VIVO
            else -> Manufacturer.OTHER
        }
}

enum class CheckOutcome { MATCH, MISMATCH, UNKNOWN }
data class PrereqCheck(val name: String, val outcome: CheckOutcome, val detail: String)
data class FieldDiff(val field: String, val left: String, val right: String) {
    val changed: Boolean get() = left != right
}
