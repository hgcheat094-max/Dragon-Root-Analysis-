package com.dragonroot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dragonroot.app.model.CompatStatus
import com.dragonroot.app.ui.DragonRootTheme
import com.dragonroot.app.ui.EmptyState
import com.dragonroot.app.ui.StatusChip
import org.junit.Rule
import org.junit.Test

class StatusChipTest {
    @get:Rule val rule = createComposeRule()

    @Test fun unknownStatusIsLabelledWithText_notColorOnly() {
        rule.setContent { DragonRootTheme { StatusChip(CompatStatus.UNKNOWN) } }
        rule.onNodeWithText("UNKNOWN").assertIsDisplayed()
    }

    @Test fun emptyStateShowsExplanation() {
        rule.setContent { DragonRootTheme { EmptyState("No firmware records yet", "Records come from signed releases.") } }
        rule.onNodeWithText("No firmware records yet").assertIsDisplayed()
    }
}
