package com.shoppit.analysis.baseline

import com.shoppit.analysis.models.Finding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Implementation of BaselineManager using Gson for JSON serialization.
 */
class BaselineManagerImpl : BaselineManager {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    override fun saveBaseline(findings: List<Finding>, baselineFile: File) {
        try {
            baselineFile.parentFile?.mkdirs()
            
            val json = gson.toJson(findings)
            baselineFile.writeText(json)
            
            println("Baseline saved: ${baselineFile.absolutePath} (${findings.size} findings)")
        } catch (e: Exception) {
            println("Error saving baseline: ${e.message}")
            throw e
        }
    }
    
    override fun loadBaseline(baselineFile: File): List<Finding> {
        if (!baselineFile.exists()) {
            println("No baseline file found at ${baselineFile.absolutePath}")
            return emptyList()
        }
        
        try {
            val json = baselineFile.readText()
            val type = object : TypeToken<List<Finding>>() {}.type
            val findings = gson.fromJson<List<Finding>>(json, type)
            
            println("Baseline loaded: ${baselineFile.absolutePath} (${findings.size} findings)")
            return findings
        } catch (e: Exception) {
            println("Error loading baseline: ${e.message}")
            return emptyList()
        }
    }
    
    override fun compareWithBaseline(
        currentFindings: List<Finding>,
        baselineFindings: List<Finding>
    ): Pair<List<Finding>, List<Finding>> {
        val currentFingerprints = currentFindings.map { it.fingerprint }.toSet()
        val baselineFingerprints = baselineFindings.map { it.fingerprint }.toSet()
        
        // New findings: in current but not in baseline
        val newFindings = currentFindings.filter { it.fingerprint !in baselineFingerprints }
        
        // Fixed findings: in baseline but not in current
        val fixedFindings = baselineFindings.filter { it.fingerprint !in currentFingerprints }
        
        println("Baseline comparison:")
        println("  - New findings: ${newFindings.size}")
        println("  - Fixed findings: ${fixedFindings.size}")
        println("  - Unchanged: ${currentFindings.size - newFindings.size}")
        
        return Pair(newFindings, fixedFindings)
    }
}
