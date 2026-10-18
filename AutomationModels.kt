package com.example.model

enum class StepState {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class HealthCheckItem(
    val id: String,
    val title: String,
    val description: String,
    val isPassed: Boolean,
    val category: String, // "STRUCTURE", "SECURITY", "GRADLE"
    val fixHint: String? = null
)

data class BuildStep(
    val id: Int,
    val title: String,
    val command: String,
    val description: String,
    val state: StepState = StepState.PENDING,
    val logs: List<String> = emptyList(),
    val durationMs: Long = 0
)

data class ApkInfo(
    val fileName: String,
    val sizeMb: Double,
    val buildType: String,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val outputPath: String,
    val sha256: String,
    val generatedTimeFormatted: String
)

data class WorkflowSettings(
    val repoName: String = "MyKotlinApp",
    val githubUsername: String = "username",
    val mainBranch: String = "main",
    val javaVersion: String = "17",
    val gradleTask: String = "assembleDebug",
    val uploadArtifacts: Boolean = true,
    val retentionDays: Int = 30,
    val publishReleasesOnTags: Boolean = true
)
