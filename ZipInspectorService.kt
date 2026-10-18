package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.ExtractedProjectFile
import com.example.model.ZipInspectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipInspectorService {

    suspend fun inspectZipFromUri(context: Context, uri: Uri): Result<ZipInspectionResult> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext Result.failure(Exception("امکان باز کردن فایل انتخاب‌شده وجود ندارد."))
                }

                // Query display name
                var displayName = "uploaded_project.zip"
                try {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                displayName = it.getString(nameIndex) ?: displayName
                            }
                        }
                    }
                } catch (_: Exception) {}

                val result = parseZipStream(inputStream, displayName)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun parseZipStream(inputStream: InputStream, fileName: String): ZipInspectionResult {
        val zipStream = ZipInputStream(inputStream)
        var count = 0
        var totalBytes = 0L
        var ktCount = 0
        var gradleCount = 0
        var hasSettingsGradle = false
        var hasAppBuildGradle = false
        var hasGradlew = false
        var hasManifest = false
        val entries = mutableListOf<String>()
        val rawFiles = mutableListOf<ExtractedProjectFile>()

        try {
            var entry = zipStream.nextEntry
            while (entry != null) {
                val name = entry.name

                // Security & Cache Filter: Prevent Zip Slip and ignore build/cache files
                if (name.contains("..") || name.startsWith("/") || name.startsWith("\\") || shouldIgnoreEntry(name)) {
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                    continue
                }

                if (!entry.isDirectory) {
                    count++

                    // Read content bytes
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (zipStream.read(buffer).also { len = it } > 0) {
                        baos.write(buffer, 0, len)
                    }
                    val bytes = baos.toByteArray()
                    totalBytes += bytes.size

                    val lower = name.lowercase()
                    val isText = isTextFile(lower)

                    val textContent = if (isText) {
                        try {
                            String(bytes, Charsets.UTF_8)
                        } catch (_: Exception) {
                            null
                        }
                    } else null

                    rawFiles.add(
                        ExtractedProjectFile(
                            relativePath = name,
                            sizeBytes = bytes.size.toLong(),
                            isText = isText,
                            textContent = textContent,
                            rawBytes = bytes
                        )
                    )

                    entries.add(name)

                    if (lower.endsWith(".kt")) ktCount++
                    if (lower.contains("gradle") && (lower.endsWith(".gradle") || lower.endsWith(".gradle.kts") || lower.endsWith(".properties"))) gradleCount++
                    if (lower.endsWith("settings.gradle") || lower.endsWith("settings.gradle.kts")) hasSettingsGradle = true
                    if (lower.contains("app/build.gradle") || lower.contains("app/build.gradle.kts") || lower.contains("build.gradle.kts")) hasAppBuildGradle = true
                    if (lower.endsWith("gradlew") || lower.endsWith("gradlew.bat")) hasGradlew = true
                    if (lower.endsWith("androidmanifest.xml")) hasManifest = true
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        } finally {
            zipStream.close()
        }

        // Detect redundant top-level directory (e.g., "MyProject/settings.gradle.kts")
        val normalizedFiles = stripCommonPrefixIfPresent(rawFiles)

        return ZipInspectionResult(
            fileName = fileName,
            fileCount = count,
            totalSizeKb = (totalBytes / 1024).coerceAtLeast(1),
            ktFileCount = ktCount,
            gradleFileCount = gradleCount,
            hasSettingsGradle = hasSettingsGradle,
            hasAppBuildGradle = hasAppBuildGradle,
            hasGradlew = hasGradlew,
            hasManifest = hasManifest,
            fileEntries = entries.take(50),
            extractedFiles = normalizedFiles,
            isHealthy = hasSettingsGradle && hasAppBuildGradle
        )
    }

    private fun shouldIgnoreEntry(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("/build/") || lower.startsWith("build/") ||
                lower.contains("/.gradle/") || lower.startsWith(".gradle/") ||
                lower.contains("/.idea/") || lower.startsWith(".idea/") ||
                lower.contains("/.cxx/") || lower.startsWith(".cxx/") ||
                lower.contains("/bin/") || lower.startsWith("bin/") ||
                lower.endsWith(".iml") || lower.endsWith(".ds_store") ||
                lower.endsWith("thumbs.db") || lower.endsWith("local.properties") ||
                lower.endsWith(".hprof")
    }

    private fun isTextFile(lowerName: String): Boolean {
        return lowerName.endsWith(".kt") ||
                lowerName.endsWith(".java") ||
                lowerName.endsWith(".kts") ||
                lowerName.endsWith(".gradle") ||
                lowerName.endsWith(".properties") ||
                lowerName.endsWith(".xml") ||
                lowerName.endsWith(".yml") ||
                lowerName.endsWith(".yaml") ||
                lowerName.endsWith(".json") ||
                lowerName.endsWith(".txt") ||
                lowerName.endsWith(".md") ||
                lowerName.endsWith(".pro") ||
                lowerName.endsWith(".gitignore") ||
                lowerName.endsWith("gradlew")
    }

    private fun stripCommonPrefixIfPresent(files: List<ExtractedProjectFile>): List<ExtractedProjectFile> {
        if (files.isEmpty()) return files

        val firstSlashIndices = files.map { it.relativePath.indexOf('/') }
        if (firstSlashIndices.any { it <= 0 }) {
            // Some file is directly at the root, so no common folder prefix
            return files
        }

        val firstPrefix = files.first().relativePath.substring(0, firstSlashIndices.first() + 1)
        val allSharePrefix = files.all { it.relativePath.startsWith(firstPrefix) }

        return if (allSharePrefix) {
            files.map { file ->
                file.copy(relativePath = file.relativePath.removePrefix(firstPrefix))
            }
        } else {
            files
        }
    }

    fun createSampleProjectInspection(): ZipInspectionResult {
        val sampleFiles = listOf(
            ExtractedProjectFile(
                relativePath = "settings.gradle.kts",
                sizeBytes = 450,
                isText = true,
                textContent = """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MyKotlinApp"
include(":app")
                """.trimIndent()
            ),
            ExtractedProjectFile(
                relativePath = "build.gradle.kts",
                sizeBytes = 250,
                isText = true,
                textContent = """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
                """.trimIndent()
            ),
            ExtractedProjectFile(
                relativePath = "app/build.gradle.kts",
                sizeBytes = 900,
                isText = true,
                textContent = """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
}
                """.trimIndent()
            ),
            ExtractedProjectFile(
                relativePath = "app/src/main/AndroidManifest.xml",
                sizeBytes = 600,
                isText = true,
                textContent = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="MyKotlinApp"
        android:supportsRtl="true">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
                """.trimIndent()
            ),
            ExtractedProjectFile(
                relativePath = "app/src/main/java/com/example/app/MainActivity.kt",
                sizeBytes = 800,
                isText = true,
                textContent = """
package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Hello Kotlin & GitHub Actions APK!")
        }
    }
}
                """.trimIndent()
            ),
            ExtractedProjectFile(
                relativePath = ".gitignore",
                sizeBytes = 300,
                isText = true,
                textContent = """
*.apk
*.aab
.gradle/
build/
local.properties
*.keystore
*.jks
.env
                """.trimIndent()
            ),
            ExtractedProjectFile(
                relativePath = "gradlew",
                sizeBytes = 5000,
                isText = true,
                textContent = "#!/bin/sh\nexec gradle \"$@\"\n"
            )
        )

        return ZipInspectionResult(
            fileName = "Android_Kotlin_Project.zip",
            fileCount = sampleFiles.size,
            totalSizeKb = 120,
            ktFileCount = 1,
            gradleFileCount = 3,
            hasSettingsGradle = true,
            hasAppBuildGradle = true,
            hasGradlew = true,
            hasManifest = true,
            fileEntries = sampleFiles.map { it.relativePath },
            extractedFiles = sampleFiles,
            isHealthy = true
        )
    }
}
