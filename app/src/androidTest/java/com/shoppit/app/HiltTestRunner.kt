package com.shoppit.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for Hilt instrumented tests.
 * 
 * This runner replaces the default application with HiltTestApplication,
 * which enables Hilt dependency injection in instrumented tests.
 * 
 * Configure in build.gradle.kts:
 * testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
