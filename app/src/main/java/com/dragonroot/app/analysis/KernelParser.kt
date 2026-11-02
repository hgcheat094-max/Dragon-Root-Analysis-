package com.dragonroot.app.analysis

import com.dragonroot.app.model.FieldDiff

data class KernelVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<KernelVersion> {
    override fun compareTo(other: KernelVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val re = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?""")
        fun parse(s: String?): KernelVersion? {
            val m = re.find(s?.trim().orEmpty()) ?: return null
            return KernelVersion(
                m.groupValues[1].toInt(), m.groupValues[2].toInt(),
                m.groupValues[3].ifEmpty { "0" }.toInt()
            )
        }
    }
}

data class KernelInfo(
    val raw: String,
    val version: KernelVersion,
    val localVersion: String,
    /** e.g. "android12-5.10", only when the release string itself carries an -androidNN- tag. */
    val androidKmi: String?
)

object KernelParser {
    private val head = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?(.*)$""")
    private val kmiTag = Regex("""android(\d+)""")

    fun parse(release: String?): KernelInfo? {
        val r = release?.trim().orEmpty()
        if (r.isEmpty()) return null
        val m = head.find(r) ?: return null
        val v = KernelVersion(
            m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].ifEmpty { "0" }.toInt()
        )
        val suffix = m.groupValues[4]
        val kmi = kmiTag.find(suffix)?.let { "android${it.groupValues[1]}-${v.major}.${v.minor}" }
        return KernelInfo(r, v, suffix, kmi)
    }

    fun diff(a: KernelInfo?, b: KernelInfo?): List<FieldDiff> = listOf(
        FieldDiff("Kernel version", a?.version?.toString() ?: "Unavailable", b?.version?.toString() ?: "Unavailable"),
        FieldDiff("Android KMI tag", a?.androidKmi ?: "Not in string", b?.androidKmi ?: "Not in string"),
        FieldDiff("Local version suffix", a?.localVersion?.ifBlank { "(none)" } ?: "Unavailable", b?.localVersion?.ifBlank { "(none)" } ?: "Unavailable"),
        FieldDiff("Full release string", a?.raw ?: "Unavailable", b?.raw ?: "Unavailable")
    )
}
