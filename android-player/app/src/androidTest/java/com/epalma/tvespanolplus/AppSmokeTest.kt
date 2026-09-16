package com.epalma.tvespanolplus

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun appShowsBrandWithoutLogin() {
        rule.onNode(hasContentDescription("TV Español+"), useUnmergedTree = true).fetchSemanticsNode()
    }
}
