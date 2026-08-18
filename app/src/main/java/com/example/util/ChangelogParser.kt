package com.pesalytics.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class ReleaseFeature(
    val category: String,
    val title: String,
    val description: String
)

object ChangelogParser {
    fun getLatestReleaseFeatures(context: Context): List<ReleaseFeature> {
        val features = mutableListOf<ReleaseFeature>()
        try {
            val inputStream = context.assets.open("CHANGELOG.md")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var insideLatestRelease = false
            var currentCategory = "Added"
            
            var line = reader.readLine()
            while (line != null) {
                line = line.trim()
                
                if (line.startsWith("## ")) {
                    if (insideLatestRelease) {
                        break
                    } else {
                        insideLatestRelease = true
                    }
                } else if (line == "---") {
                    if (insideLatestRelease) {
                        break
                    }
                } else if (insideLatestRelease && line.startsWith("### ")) {
                    currentCategory = line.removePrefix("### ").trim()
                } else if (insideLatestRelease && line.startsWith("- ")) {
                    val content = line.removePrefix("- ").trim()
                    var title = "Update"
                    var description = content
                    
                    if (content.startsWith("**")) {
                        val endBold = content.indexOf("**", 2)
                        if (endBold != -1) {
                            title = content.substring(2, endBold).trim()
                            
                            var descStart = endBold + 2
                            while (descStart < content.length && (content[descStart] == ':' || content[descStart] == '-' || content[descStart] == '—' || content[descStart] == ' ' || content[descStart] == '.')) {
                                descStart++
                            }
                            if (descStart < content.length) {
                                description = content.substring(descStart).trim()
                            } else {
                                description = ""
                            }
                        }
                    }
                    
                    features.add(ReleaseFeature(currentCategory, title, description))
                }
                
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback if empty or failed
        if (features.isEmpty()) {
            features.add(ReleaseFeature("Added", "Update Successful", "Pesalytics has been updated to the latest version."))
        }
        
        return features
    }
}
