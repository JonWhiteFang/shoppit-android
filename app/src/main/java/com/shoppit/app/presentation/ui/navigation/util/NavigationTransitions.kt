package com.shoppit.app.presentation.ui.navigation.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Provides smooth navigation transition animations without frame drops.
 * Uses optimized animation curves and durations for 60fps performance.
 *
 * Requirements:
 * - 7.1: Complete transitions within 300ms
 * - 7.2: Display smooth animations without frame drops
 */
object NavigationTransitions {
    
    // Animation duration optimized for smooth 60fps performance
    private const val TRANSITION_DURATION_MS = 250
    
    /**
     * Enter transition for forward navigation (push).
     * Slides in from right with fade.
     */
    fun enterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Exit transition for forward navigation (push).
     * Fades out the previous screen.
     */
    fun exitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION_MS / 2,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Enter transition for backward navigation (pop).
     * Fades in the previous screen.
     */
    fun popEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION_MS / 2,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Exit transition for backward navigation (pop).
     * Slides out to right with fade.
     */
    fun popExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Simple fade transition for bottom navigation tab switches.
     * Faster than slide transitions for better perceived performance.
     */
    fun fadeTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Simple fade out for bottom navigation tab switches.
     */
    fun fadeOutTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing
            )
        )
    }
}
