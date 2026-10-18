package com.example.model

data class GitHubUser(
    val login: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val publicRepos: Int = 0
)

data class GitHubRepo(
    val name: String,
    val fullName: String,
    val isPrivate: Boolean = false,
    val htmlUrl: String = ""
)

data class ExtractedProjectFile(
    val relativePath: String,
    val sizeBytes: Long,
    val isText: Boolean,
    val textContent: String? = null,
    val rawBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExtractedProjectFile
        return relativePath == other.relativePath
    }

    override fun hashCode(): Int {
        return relativePath.hashCode()
    }
}

data class ZipInspectionResult(
    val fileName: String,
    val fileCount: Int,
    val totalSizeKb: Long,
    val ktFileCount: Int,
    val gradleFileCount: Int,
    val hasSettingsGradle: Boolean,
    val hasAppBuildGradle: Boolean,
    val hasGradlew: Boolean,
    val hasManifest: Boolean,
    val fileEntries: List<String> = emptyList(),
    val extractedFiles: List<ExtractedProjectFile> = emptyList(),
    val isHealthy: Boolean = true
)

enum class RepoTargetMode {
    NEW_REPO,
    EXISTING_REPO
}

data class PushProgressState(
    val isProcessing: Boolean = false,
    val stepTitle: String = "",
    val stepDescription: String = "",
    val progress: Float = 0f,
    val targetRepoUrl: String? = null,
    val actionsUrl: String? = null,
    val errorMessage: String? = null,
    val isFinished: Boolean = false,
    val uploadedFilesCount: Int = 0
)
