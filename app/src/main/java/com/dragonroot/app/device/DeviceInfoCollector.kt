package com.dragonroot.app.device

import android.os.Build
import com.dragonroot.app.model.BootloaderState
import com.dragonroot.app.model.Confidence
import com.dragonroot.app.model.DeviceProfile
import com.dragonroot.app.model.Reading
import com.dragonroot.app.model.RootResult
import com.dragonroot.app.model.RootState
import java.io.File
import java.util.concurrent.TimeUnit

/** Values from android.os.Build, captured so the collector can be unit-tested with mock profiles. */
data class BuildSnapshot(
    val manufacturer: String?, val brand: String?, val model: String?, val device: String?,
    val product: String?, val release: String?, val sdkInt: Int, val id: String?,
    val fingerprint: String?, val display: String?, val securityPatch: String?, val tags: String?,
    val supportedAbis: List<String>, val socManufacturer: String?, val socModel: String?,
    val kernelOsVersion: String?
) {
    companion object {
        fun fromSystem() = BuildSnapshot(
            manufacturer = Build.MANUFACTURER, brand = Build.BRAND, model = Build.MODEL, device = Build.DEVICE,
            product = Build.PRODUCT, release = Build.VERSION.RELEASE, sdkInt = Build.VERSION.SDK_INT,
            id = Build.ID, fingerprint = Build.FINGERPRINT, display = Build.DISPLAY,
            securityPatch = Build.VERSION.SECURITY_PATCH, tags = Build.TAGS,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            socManufacturer = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER else null,
            socModel = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else null,
            kernelOsVersion = System.getProperty("os.version")
        )
    }
}

interface PropReader { fun get(key: String): String? }

interface FileProbe {
    fun exists(path: String): Boolean
    fun readText(path: String, maxBytes: Int = 4096): String?
}

/** Reads non-privileged system properties through the standard `getprop` tool. */
class GetpropReader : PropReader {
    override fun get(key: String): String? = try {
        val p = ProcessBuilder("getprop", key).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().use { it.readText() }.trim()
        if (!p.waitFor(2, TimeUnit.SECONDS)) { p.destroy(); null } else out.ifBlank { null }
    } catch (e: Exception) {
        null
    }
}

class AndroidFileProbe : FileProbe {
    override fun exists(path: String): Boolean = try { File(path).exists() } catch (e: SecurityException) { false }

    override fun readText(path: String, maxBytes: Int): String? = try {
        File(path).inputStream().use { s ->
            val buf = ByteArray(maxBytes)
            val n = s.read(buf)
            if (n <= 0) null else String(buf, 0, n, Charsets.UTF_8).trim().ifBlank { null }
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Collects device facts using only normal Android APIs and unprivileged system properties.
 * Missing values are reported as unavailable — never guessed. No privileged command is executed.
 */
class DeviceInfoCollector(
    private val build: BuildSnapshot,
    private val props: PropReader,
    private val files: FileProbe
) {
    fun collect(): DeviceProfile {
        val (bootloaderReading, bootloaderState) = bootloader()
        return DeviceProfile(
            manufacturer = fromBuild(build.manufacturer),
            brand = fromBuild(build.brand),
            model = fromBuild(build.model),
            codename = fromBuild(build.device),
            product = fromBuild(build.product),
            androidVersion = fromBuild(build.release),
            apiLevel = build.sdkInt,
            buildId = fromBuild(build.id),
            fingerprint = fromBuild(build.fingerprint),
            buildNumber = fromBuild(build.display),
            vendorOs = vendorOs(),
            securityPatch = fromBuild(build.securityPatch),
            kernelRelease = kernelRelease(),
            kernelBuild = kernelBuild(),
            abis = build.supportedAbis,
            soc = soc(),
            bootloader = bootloaderReading,
            bootloaderState = bootloaderState,
            selinux = selinux(),
            root = rootCheck()
        )
    }

    private fun fromBuild(v: String?): Reading {
        val c = v?.trim()
        return if (c.isNullOrBlank() || c.equals("unknown", true)) Reading.unavailable("android.os.Build")
        else Reading(c, "android.os.Build", Confidence.HIGH)
    }

    /** Property names are community-observed and vendor-dependent, so confidence is MEDIUM at best. */
    private fun vendorOs(): Reading {
        val name = props.get("ro.vivo.os.name")
        val version = props.get("ro.vivo.os.version")
        if (!version.isNullOrBlank()) {
            val text = listOfNotNull(name?.takeIf { it.isNotBlank() }, version).joinToString(" ")
            return Reading(text, "system properties ro.vivo.os.name / ro.vivo.os.version", Confidence.MEDIUM)
        }
        val display = props.get("ro.vivo.os.build.display.id")
        if (!display.isNullOrBlank()) {
            return Reading(display, "system property ro.vivo.os.build.display.id", Confidence.MEDIUM)
        }
        return Reading.unavailable("system properties ro.vivo.os.*")
    }

    private fun kernelRelease(): Reading {
        build.kernelOsVersion?.trim()?.takeIf { it.isNotBlank() }?.let {
            return Reading(it, "Java system property os.version", Confidence.HIGH)
        }
        parseProcVersionRelease(files.readText("/proc/version"))?.let {
            return Reading(it, "/proc/version", Confidence.HIGH)
        }
        return Reading.unavailable("os.version and /proc/version")
    }

    private fun kernelBuild(): Reading {
        val text = files.readText("/proc/version")
        return if (text.isNullOrBlank()) Reading.unavailable("/proc/version (not readable by apps on many builds)")
        else Reading(text, "/proc/version", Confidence.HIGH)
    }

    private fun soc(): Reading {
        val m = build.socModel?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
        if (build.sdkInt >= 31 && m != null) {
            val mfr = build.socManufacturer?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
            return Reading(listOfNotNull(mfr, m).joinToString(" "), "android.os.Build.SOC_*", Confidence.HIGH)
        }
        val platform = props.get("ro.board.platform")
        if (!platform.isNullOrBlank()) {
            return Reading("Board platform codename: $platform", "system property ro.board.platform", Confidence.LOW)
        }
        return Reading.unavailable("android.os.Build.SOC_* / ro.board.platform")
    }

    private fun bootloader(): Pair<Reading, BootloaderState> {
        val locked = props.get("ro.boot.flash.locked")
        val vb = props.get("ro.boot.verifiedbootstate")
        return when {
            locked == "1" -> Reading("Locked (ro.boot.flash.locked=1)", "system property ro.boot.flash.locked", Confidence.MEDIUM) to BootloaderState.LOCKED
            locked == "0" -> Reading("Unlocked (ro.boot.flash.locked=0)", "system property ro.boot.flash.locked", Confidence.MEDIUM) to BootloaderState.UNLOCKED
            vb.equals("orange", true) -> Reading("Unlocked (verified boot state: orange)", "system property ro.boot.verifiedbootstate", Confidence.MEDIUM) to BootloaderState.UNLOCKED
            vb.equals("green", true) -> Reading("Verified boot state is green — suggests a locked bootloader, not conclusive", "system property ro.boot.verifiedbootstate", Confidence.LOW) to BootloaderState.UNKNOWN
            else -> Reading.unavailable("ro.boot.flash.locked / ro.boot.verifiedbootstate") to BootloaderState.UNKNOWN
        }
    }

    private fun selinux(): Reading = when (files.readText("/sys/fs/selinux/enforce")?.trim()) {
        "1" -> Reading("Enforcing", "/sys/fs/selinux/enforce", Confidence.HIGH)
        "0" -> Reading("Permissive", "/sys/fs/selinux/enforce", Confidence.HIGH)
        else -> Reading.unavailable("/sys/fs/selinux/enforce (not accessible to apps on most builds)")
    }

    /**
     * Passive heuristic only: looks for su binaries on disk. It never executes su, never inspects
     * installed apps (a root-manager APK is not proof of root), and never requests privileges.
     */
    private fun rootCheck(): RootResult {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sbin/su",
            "/vendor/bin/su", "/su/bin/su", "/system/bin/.ext/.su"
        )
        val found = suPaths.filter { files.exists(it) }
        val notes = mutableListOf<String>()
        if (build.tags?.contains("test-keys") == true) notes += "Build is signed with test-keys (custom/development build; not proof of root)"
        return if (found.isNotEmpty()) {
            RootResult(
                RootState.ROOT_DETECTED, "File-existence check for su binaries", Confidence.MEDIUM,
                found.map { "su binary present: $it" } + notes,
                "A su binary exists in a standard location. This indicates root tooling is installed."
            )
        } else {
            RootResult(
                RootState.NO_ROOT_DETECTED, "File-existence check for su binaries", Confidence.LOW, notes,
                "No su binary was found in standard locations. This is a weak signal: modern systemless " +
                    "root can hide from apps, so absence is not proof that the device is unrooted."
            )
        }
    }

    companion object {
        private val procRegex = Regex("""Linux version\s+(\S+)""")
        fun parseProcVersionRelease(text: String?): String? = text?.let { procRegex.find(it)?.groupValues?.get(1) }
    }
}
