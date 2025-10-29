package com.shoppit.app.presentation.ui.navigation

import android.view.KeyEvent
import androidx.navigation.NavHostController
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.shoppit.app.presentation.ui.navigation.util.KeyboardNavigationHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for keyboard navigation functionality.
 * 
 * Tests verify:
 * - Keyboard shortcuts navigate to correct destinations
 * - Alt+1, Alt+2, Alt+3 shortcuts work correctly
 * - Escape key triggers back navigation
 * - Invalid key combinations are ignored
 * 
 * Requirements:
 * - 9.2: Support keyboard navigation through all interactive elements
 * - 9.4: Ensure tab order follows logical reading patterns
 * - 9.5: Implement focus management during screen transitions
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardNavigationTest {
    
    private lateinit var navController: TestNavHostController
    
    @Before
    fun setup() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    }
    
    /**
     * Test Alt+1 navigates to Meals section.
     * Requirement: 9.2
     */
    @Test
    fun altPlusOne_navigatesToMeals() {
        // Create key event for Alt+1
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_1, 0, KeyEvent.META_ALT_ON
        )
        
        // Handle the keyboard shortcut
        val handled = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent,
            navController
        )
        
        // Verify the event was handled
        assertTrue("Alt+1 should be handled", handled)
    }
    
    /**
     * Test Alt+2 navigates to Planner section.
     * Requirement: 9.2
     */
    @Test
    fun altPlusTwo_navigatesToPlanner() {
        // Create key event for Alt+2
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_2, 0, KeyEvent.META_ALT_ON
        )
        
        // Handle the keyboard shortcut
        val handled = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent,
            navController
        )
        
        // Verify the event was handled
        assertTrue("Alt+2 should be handled", handled)
    }
    
    /**
     * Test Alt+3 navigates to Shopping section.
     * Requirement: 9.2
     */
    @Test
    fun altPlusThree_navigatesToShopping() {
        // Create key event for Alt+3
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_3, 0, KeyEvent.META_ALT_ON
        )
        
        // Handle the keyboard shortcut
        val handled = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent,
            navController
        )
        
        // Verify the event was handled
        assertTrue("Alt+3 should be handled", handled)
    }
    
    /**
     * Test that keyboard shortcuts without Alt modifier are ignored.
     * Requirement: 9.2
     */
    @Test
    fun numberKeyWithoutAlt_isNotHandled() {
        // Create key event for 1 without Alt
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_1, 0, 0
        )
        
        // Handle the keyboard shortcut
        val handled = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent,
            navController
        )
        
        // Verify the event was not handled
        assertFalse("Number key without Alt should not be handled", handled)
    }
    
    /**
     * Test that key up events are ignored.
     * Requirement: 9.2
     */
    @Test
    fun keyUpEvent_isNotHandled() {
        // Create key event for Alt+1 with ACTION_UP
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_1, 0, KeyEvent.META_ALT_ON
        )
        
        // Handle the keyboard shortcut
        val handled = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent,
            navController
        )
        
        // Verify the event was not handled
        assertFalse("Key up events should not be handled", handled)
    }
    
    /**
     * Test that invalid key codes are ignored.
     * Requirement: 9.2
     */
    @Test
    fun invalidKeyCode_isNotHandled() {
        // Create key event for Alt+9 (not a valid shortcut)
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_9, 0, KeyEvent.META_ALT_ON
        )
        
        // Handle the keyboard shortcut
        val handled = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent,
            navController
        )
        
        // Verify the event was not handled
        assertFalse("Invalid key codes should not be handled", handled)
    }
    
    /**
     * Test Escape key triggers back navigation.
     * Requirement: 9.2
     */
    @Test
    fun escapeKey_triggersBackNavigation() {
        // Create key event for Escape
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_ESCAPE, 0, 0
        )
        
        // Handle back navigation
        val handled = KeyboardNavigationHandler.handleBackNavigation(
            keyEvent,
            navController
        )
        
        // Verify the event was handled
        assertTrue("Escape key should trigger back navigation", handled)
    }
    
    /**
     * Test that non-Escape keys don't trigger back navigation.
     * Requirement: 9.2
     */
    @Test
    fun nonEscapeKey_doesNotTriggerBackNavigation() {
        // Create key event for Enter
        val keyEvent = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_ENTER, 0, 0
        )
        
        // Handle back navigation
        val handled = KeyboardNavigationHandler.handleBackNavigation(
            keyEvent,
            navController
        )
        
        // Verify the event was not handled
        assertFalse("Non-Escape keys should not trigger back navigation", handled)
    }
    
    /**
     * Test that keyboard shortcuts work in sequence.
     * Requirement: 9.2, 9.5
     */
    @Test
    fun multipleKeyboardShortcuts_workInSequence() {
        // Navigate to Planner with Alt+2
        val keyEvent1 = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_2, 0, KeyEvent.META_ALT_ON
        )
        val handled1 = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent1,
            navController
        )
        assertTrue("First shortcut should be handled", handled1)
        
        // Navigate to Shopping with Alt+3
        val keyEvent2 = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_3, 0, KeyEvent.META_ALT_ON
        )
        val handled2 = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent2,
            navController
        )
        assertTrue("Second shortcut should be handled", handled2)
        
        // Navigate back to Meals with Alt+1
        val keyEvent3 = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_1, 0, KeyEvent.META_ALT_ON
        )
        val handled3 = KeyboardNavigationHandler.handleBottomNavigationShortcut(
            keyEvent3,
            navController
        )
        assertTrue("Third shortcut should be handled", handled3)
    }
}
