package com.dragonroot.app.analysis

import com.dragonroot.app.model.EvidenceGrade

object EvidenceGrader {
    const val STALE_AFTER_DAYS = 180L
    private const val DAY_MS = 86_400_000L

    fun best(grades: List<EvidenceGrade>): EvidenceGrade? = grades.minOrNull()

    /** VERIFIED or UNSUPPORTED claims need at least one grade A–C source. Grade D alone never qualifies. */
    fun supportsStrongClaim(grades: List<EvidenceGrade>): Boolean = grades.any { it.isCredible }

    /** A record that was never reviewed, or not reviewed within the window, needs review. */
    fun isStale(lastReviewedMillis: Long?, nowMillis: Long, maxAgeDays: Long = STALE_AFTER_DAYS): Boolean =
        lastReviewedMillis == null || nowMillis - lastReviewedMillis > maxAgeDays * DAY_MS
}
