package com.example.myfirstapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerScreen_isDisplayed() {
        composeTestRule.setContent {
            com.example.myfirstapp.ui.screen.PlayerScreen(
                songId = 1L,
                onBack = {}
            )
        }
        composeTestRule.onNodeWithText("正在播放").assertExists()
    }
}
