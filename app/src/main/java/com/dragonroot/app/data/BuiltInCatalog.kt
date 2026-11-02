package com.dragonroot.app.data

import com.dragonroot.app.model.BootloaderRequirement
import com.dragonroot.app.model.EvidenceGrade
import com.dragonroot.app.model.ProjectCategory

/**
 * Catalog entries for well-known Android root-management / kernel projects.
 *
 * These rows carry ONLY the project identity and a pointer to its official source. Supported Android
 * versions, kernel ranges, architectures and install conditions are deliberately left "not entered":
 * they must come from a reviewed, signed database release (or be read from the project's own docs).
 * Dragon Root never installs or runs any of these tools. URLs should be re-verified before use.
 */
object BuiltInCatalog {
    private const val NOTE =
        "Catalog entry only. Supported Android/kernel/architecture ranges and installation conditions " +
            "have not been entered yet — consult the project's official documentation. " +
            "Dragon Root never installs or runs this tool, and support for a project on one device says " +
            "nothing about any other device or firmware build."

    private fun entry(id: String, name: String, researcher: String, url: String) = ResearchProjectEntity(
        id = id, name = name, category = ProjectCategory.ROOT_MANAGER, researcher = researcher,
        sourceUrl = url, cve = null, affectedComponent = null,
        minApi = null, maxApi = null, kernelMin = null, kernelMax = null, architectures = null,
        bootloaderRequirement = BootloaderRequirement.NOT_ENTERED,
        mitigations = null, limitations = null, notes = NOTE,
        sourceGrade = EvidenceGrade.B, lastReviewed = null, builtIn = true, isSample = false
    )

    val projects: List<ResearchProjectEntity> = listOf(
        entry("magisk", "Magisk", "Magisk project (topjohnwu)", "https://github.com/topjohnwu/Magisk"),
        entry("kernelsu", "KernelSU", "KernelSU project (tiann)", "https://github.com/tiann/KernelSU"),
        entry("kernelsu-next", "KernelSU Next", "KernelSU Next project", "https://github.com/KernelSU-Next/KernelSU-Next"),
        entry("apatch", "APatch", "APatch project (bmax121)", "https://github.com/bmax121/APatch")
    )
}
