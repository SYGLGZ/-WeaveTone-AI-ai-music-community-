package com.example.myfirstapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_isDisplayed() {
        composeTestRule.setContent {
            com.example.myfirstapp.ui.screen.HomeScreen(
                onPlaySong = {},
                onNavigateToAI = {},
                onNavigateToPlaylists = {},
                onNavigateToSettings = {}
            )
        }
        composeTestRule.onNodeWithText("AI音乐").assertExists()
    }
}
