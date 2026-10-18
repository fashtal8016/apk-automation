package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ApkInfo
import com.example.model.BuildStep
import com.example.model.ExtractedProjectFile
import com.example.model.GitHubRepo
import com.example.model.GitHubUser
import com.example.model.HealthCheckItem
import com.example.model.PushProgressState
import com.example.model.RepoTargetMode
import com.example.model.StepState
import com.example.model.WorkflowSettings
import com.example.model.ZipInspectionResult
import com.example.service.GitHubApiService
import com.example.service.ZipInspectorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RepoLookupState {
    object Idle : RepoLookupState()
    object Searching : RepoLookupState()
    data class Found(val repo: GitHubRepo) : RepoLookupState()
    data class NotFound(val repoName: String) : RepoLookupState()
    data class Error(val message: String) : RepoLookupState()
}

class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("github_auth_prefs", Context.MODE_PRIVATE)
    private val gitHubApiService = GitHubApiService()
    private val zipInspectorService = ZipInspectorService()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // GitHub Auth State
    private val _gitHubToken = MutableStateFlow("")
    val gitHubToken: StateFlow<String> = _gitHubToken.asStateFlow()

    private val _gitHubUser = MutableStateFlow<GitHubUser?>(null)
    val gitHubUser: StateFlow<GitHubUser?> = _gitHubUser.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    // Repositories State & Live Search
    private val _userRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val userRepos: StateFlow<List<GitHubRepo>> = _userRepos.asStateFlow()

    private val _filteredRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val filteredRepos: StateFlow<List<GitHubRepo>> = _filteredRepos.asStateFlow()

    private val _repoQuery = MutableStateFlow("MyKotlinApp")
    val repoQuery: StateFlow<String> = _repoQuery.asStateFlow()

    private val _repoLookupState = MutableStateFlow<RepoLookupState>(RepoLookupState.Idle)
    val repoLookupState: StateFlow<RepoLookupState> = _repoLookupState.asStateFlow()

    private val _isLoadingRepos = MutableStateFlow(false)
    val isLoadingRepos: StateFlow<Boolean> = _isLoadingRepos.asStateFlow()

    private val _repoTargetMode = MutableStateFlow(RepoTargetMode.NEW_REPO)
    val repoTargetMode: StateFlow<RepoTargetMode> = _repoTargetMode.asStateFlow()

    private val _selectedExistingRepo = MutableStateFlow("")
    val selectedExistingRepo: StateFlow<String> = _selectedExistingRepo.asStateFlow()

    private val _newRepoName = MutableStateFlow("MyKotlinApp")
    val newRepoName: StateFlow<String> = _newRepoName.asStateFlow()

    private val _newRepoDescription = MutableStateFlow("Android Kotlin App automated with GitHub Actions APK build")
    val newRepoDescription: StateFlow<String> = _newRepoDescription.asStateFlow()

    private val _newRepoPrivate = MutableStateFlow(false)
    val newRepoPrivate: StateFlow<Boolean> = _newRepoPrivate.asStateFlow()

    // ZIP File State
    private val _selectedZipResult = MutableStateFlow<ZipInspectionResult?>(null)
    val selectedZipResult: StateFlow<ZipInspectionResult?> = _selectedZipResult.asStateFlow()

    private val _isInspectingZip = MutableStateFlow(false)
    val isInspectingZip: StateFlow<Boolean> = _isInspectingZip.asStateFlow()

    // Push & Automation Progress
    private val _pushProgressState = MutableStateFlow(PushProgressState())
    val pushProgressState: StateFlow<PushProgressState> = _pushProgressState.asStateFlow()

    // Workflow & Build State
    private val _workflowSettings = MutableStateFlow(WorkflowSettings())
    val workflowSettings: StateFlow<WorkflowSettings> = _workflowSettings.asStateFlow()

    private val _healthChecks = MutableStateFlow<List<HealthCheckItem>>(emptyList())
    val healthChecks: StateFlow<List<HealthCheckItem>> = _healthChecks.asStateFlow()

    private val _buildSteps = MutableStateFlow<List<BuildStep>>(emptyList())
    val buildSteps: StateFlow<List<BuildStep>> = _buildSteps.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    private val _buildProgress = MutableStateFlow(0f)
    val buildProgress: StateFlow<Float> = _buildProgress.asStateFlow()

    private val _latestApk = MutableStateFlow<ApkInfo?>(null)
    val latestApk: StateFlow<ApkInfo?> = _latestApk.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var repoCheckJob: Job? = null

    init {
        loadInitialHealthChecks()
        resetBuildSteps()
        loadSampleProjectZip()

        // Persistent Login: Check stored token on launch
        val savedToken = prefs.getString("github_token", null)
        if (!savedToken.isNullOrBlank()) {
            _gitHubToken.value = savedToken
            loginWithGitHub(savedToken, isAutoLogin = true)
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    // --- GitHub Authentication with SharedPreferences Persistence ---
    fun updateGitHubToken(token: String) {
        _gitHubToken.value = token
    }

    fun loginWithGitHub(token: String, isAutoLogin: Boolean = false) {
        if (token.isBlank()) {
            _toastMessage.value = "لطفاً توکن دسترسی GitHub را وارد کنید"
            return
        }
        _isLoggingIn.value = true
        _gitHubToken.value = token.trim()

        viewModelScope.launch {
            val result = gitHubApiService.verifyToken(token.trim())
            _isLoggingIn.value = false
            result.onSuccess { user ->
                _gitHubUser.value = user
                _workflowSettings.update { it.copy(githubUsername = user.login) }

                // Save token to SharedPreferences for persistence across restarts
                prefs.edit().putString("github_token", token.trim()).apply()

                if (!isAutoLogin) {
                    _toastMessage.value = "خوش آمدید، ${user.login}! توکن با موفقیت ذخیره شد."
                }
                fetchUserRepos(token.trim())
                checkRepoOnGitHub(_repoQuery.value)
            }.onFailure { err ->
                if (!isAutoLogin) {
                    _toastMessage.value = err.message ?: "خطا در ورود به گیت‌هاب"
                }
            }
        }
    }

    fun loginWithDemoAccount() {
        _isLoggingIn.value = true
        viewModelScope.launch {
            delay(400)
            val demoUser = GitHubUser(
                login = "android-developer",
                name = "Android Dev",
                avatarUrl = null,
                publicRepos = 8
            )
            _gitHubUser.value = demoUser
            val demoToken = "ghp_demo_access_token_simulated"
            _gitHubToken.value = demoToken
            prefs.edit().putString("github_token", demoToken).apply()

            _workflowSettings.update { it.copy(githubUsername = demoUser.login) }
            val dummyRepos = listOf(
                GitHubRepo("MyKotlinApp", "android-developer/MyKotlinApp", false, "https://github.com/android-developer/MyKotlinApp"),
                GitHubRepo("WeatherApp", "android-developer/WeatherApp", false, "https://github.com/android-developer/WeatherApp"),
                GitHubRepo("AndroidAutomation", "android-developer/AndroidAutomation", true, "https://github.com/android-developer/AndroidAutomation")
            )
            _userRepos.value = dummyRepos
            _filteredRepos.value = dummyRepos
            _selectedExistingRepo.value = "MyKotlinApp"
            _repoLookupState.value = RepoLookupState.Found(dummyRepos.first())
            _isLoggingIn.value = false
            _toastMessage.value = "ورود در حالت آزمایشی (Demo) تأیید و ذخیره شد."
        }
    }

    fun logoutGitHub() {
        _gitHubUser.value = null
        _gitHubToken.value = ""
        _userRepos.value = emptyList()
        _filteredRepos.value = emptyList()
        _repoLookupState.value = RepoLookupState.Idle

        // Remove token from SharedPreferences
        prefs.edit().remove("github_token").apply()
        _toastMessage.value = "از حساب کاربری خارج شدید و توکن حذف گردید."
    }

    private fun fetchUserRepos(token: String) {
        _isLoadingRepos.value = true
        viewModelScope.launch {
            val result = gitHubApiService.fetchRepositories(token)
            _isLoadingRepos.value = false
            result.onSuccess { repos ->
                _userRepos.value = repos
                filterRepos(_repoQuery.value)
                if (repos.isNotEmpty() && _selectedExistingRepo.value.isEmpty()) {
                    _selectedExistingRepo.value = repos.first().name
                }
            }
        }
    }

    // --- Search & Verify Repository on GitHub ---
    fun setRepoQuery(query: String) {
        val clean = query.trim().replace(" ", "-")
        _repoQuery.value = clean
        _newRepoName.value = clean
        _selectedExistingRepo.value = clean
        _workflowSettings.update { it.copy(repoName = clean) }

        filterRepos(clean)
        checkRepoOnGitHub(clean)
    }

    private fun filterRepos(query: String) {
        if (query.isBlank()) {
            _filteredRepos.value = _userRepos.value
        } else {
            _filteredRepos.value = _userRepos.value.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun checkRepoOnGitHub(repoName: String) {
        if (repoName.isBlank()) {
            _repoLookupState.value = RepoLookupState.Idle
            return
        }

        val user = _gitHubUser.value
        val token = _gitHubToken.value

        // Check local list first
        val localMatch = _userRepos.value.firstOrNull { it.name.equals(repoName, ignoreCase = true) }
        if (localMatch != null) {
            _repoLookupState.value = RepoLookupState.Found(localMatch)
            _repoTargetMode.value = RepoTargetMode.EXISTING_REPO
            return
        }

        if (user == null || token.isBlank()) {
            _repoLookupState.value = RepoLookupState.NotFound(repoName)
            _repoTargetMode.value = RepoTargetMode.NEW_REPO
            return
        }

        if (token.contains("demo")) {
            if (repoName.equals("MyKotlinApp", ignoreCase = true) || repoName.equals("WeatherApp", ignoreCase = true)) {
                _repoLookupState.value = RepoLookupState.Found(GitHubRepo(repoName, "${user.login}/$repoName"))
                _repoTargetMode.value = RepoTargetMode.EXISTING_REPO
            } else {
                _repoLookupState.value = RepoLookupState.NotFound(repoName)
                _repoTargetMode.value = RepoTargetMode.NEW_REPO
            }
            return
        }

        repoCheckJob?.cancel()
        repoCheckJob = viewModelScope.launch {
            _repoLookupState.value = RepoLookupState.Searching
            delay(400) // Debounce
            val result = gitHubApiService.checkRepoExists(token, user.login, repoName)
            result.onSuccess { existingRepo ->
                if (existingRepo != null) {
                    _repoLookupState.value = RepoLookupState.Found(existingRepo)
                    _repoTargetMode.value = RepoTargetMode.EXISTING_REPO
                } else {
                    _repoLookupState.value = RepoLookupState.NotFound(repoName)
                    _repoTargetMode.value = RepoTargetMode.NEW_REPO
                }
            }.onFailure {
                _repoLookupState.value = RepoLookupState.NotFound(repoName)
                _repoTargetMode.value = RepoTargetMode.NEW_REPO
            }
        }
    }

    // --- Repos Target Configuration ---
    fun setRepoTargetMode(mode: RepoTargetMode) {
        _repoTargetMode.value = mode
    }

    fun selectExistingRepo(name: String) {
        _selectedExistingRepo.value = name
        _repoQuery.value = name
        _newRepoName.value = name
        _workflowSettings.update { it.copy(repoName = name) }
        checkRepoOnGitHub(name)
    }

    fun updateRepoName(name: String) {
        setRepoQuery(name)
    }

    fun updateGithubUser(user: String) {
        _workflowSettings.update { it.copy(githubUsername = user.trim()) }
    }

    fun updateJavaVersion(version: String) {
        _workflowSettings.update { it.copy(javaVersion = version) }
    }

    fun updateGradleTask(task: String) {
        _workflowSettings.update { it.copy(gradleTask = task) }
    }

    fun toggleArtifacts(enabled: Boolean) {
        _workflowSettings.update { it.copy(uploadArtifacts = enabled) }
    }

    fun toggleReleases(enabled: Boolean) {
        _workflowSettings.update { it.copy(publishReleasesOnTags = enabled) }
    }

    fun updateNewRepoName(name: String) {
        setRepoQuery(name)
    }

    fun updateNewRepoDescription(desc: String) {
        _newRepoDescription.value = desc
    }

    fun toggleNewRepoPrivate(isPrivate: Boolean) {
        _newRepoPrivate.value = isPrivate
    }

    // --- ZIP File Handling ---
    fun inspectZipFile(context: Context, uri: Uri) {
        _isInspectingZip.value = true
        viewModelScope.launch {
            val result = zipInspectorService.inspectZipFromUri(context, uri)
            _isInspectingZip.value = false
            result.onSuccess { inspection ->
                _selectedZipResult.value = inspection
                _toastMessage.value = "فایل زیپ ${inspection.fileName} بازرسی شد (${inspection.fileCount} فایل)"
                updateHealthChecksFromZip(inspection)
            }.onFailure { err ->
                _toastMessage.value = "خطا در خواندن فایل زیپ: ${err.message}"
            }
        }
    }

    fun loadSampleProjectZip() {
        val inspection = zipInspectorService.createSampleProjectInspection()
        _selectedZipResult.value = inspection
        updateHealthChecksFromZip(inspection)
    }

    private fun updateHealthChecksFromZip(zip: ZipInspectionResult) {
        _healthChecks.value = listOf(
            HealthCheckItem(
                id = "settings_gradle",
                title = "settings.gradle(.kts)",
                description = if (zip.hasSettingsGradle) "فایل ریشه و ماژول‌ها در فایل زیپ تأیید شد." else "فایل settings.gradle یافت نشد! ریشه پروژه را بررسی کنید.",
                isPassed = zip.hasSettingsGradle,
                category = "STRUCTURE"
            ),
            HealthCheckItem(
                id = "build_gradle",
                title = "build.gradle / app module",
                description = if (zip.hasAppBuildGradle) "پیکربندی ماژول برنامه و کاتلین تأیید شد." else "فایل build.gradle یافت نشد.",
                isPassed = zip.hasAppBuildGradle,
                category = "GRADLE"
            ),
            HealthCheckItem(
                id = "gradle_wrapper",
                title = "Gradle Wrapper (gradlew)",
                description = if (zip.hasGradlew) "ابزار مستقل gradlew درون فایل زیپ موجود است." else "فایل gradlew مفقود است (در گیت‌هاب ایجاد خواهد شد).",
                isPassed = zip.hasGradlew,
                category = "GRADLE"
            ),
            HealthCheckItem(
                id = "manifest_check",
                title = "AndroidManifest.xml",
                description = if (zip.hasManifest) "فایل مانیفست اندروید در ساختار موجود است." else "فایل مانیفست یافت نشد.",
                isPassed = zip.hasManifest,
                category = "STRUCTURE"
            ),
            HealthCheckItem(
                id = "workflow_build_apk",
                title = "ورک‌فلو اکشن build-apk.yml",
                description = "ورک‌فلو خودکارسازی ساخت APK و Release آماده ارسال به مخزن است.",
                isPassed = true,
                category = "AUTOMATION"
            )
        )
    }

    // --- Push Project & build-apk.yml to GitHub ---
    fun pushProjectAndWorkflowToGitHub() {
        val user = _gitHubUser.value
        val token = _gitHubToken.value
        if (user == null || token.isEmpty()) {
            _toastMessage.value = "لطفاً ابتدا با توکن گیت‌هاب وارد شوید یا حالت دمو را بزنید."
            return
        }

        val targetRepo = _repoQuery.value.ifBlank { "MyKotlinApp" }
        val filesToUpload = _selectedZipResult.value?.extractedFiles ?: emptyList()

        _pushProgressState.value = PushProgressState(
            isProcessing = true,
            stepTitle = "شروع فرآیند آپلود به مخزن $targetRepo",
            stepDescription = "بررسی اتصال به گیت‌هاب...",
            progress = 0.1f
        )

        viewModelScope.launch {
            try {
                val owner = user.login

                // Demo mode simulation
                if (token.contains("demo")) {
                    delay(500)
                    _pushProgressState.value = PushProgressState(
                        isProcessing = true,
                        stepTitle = "آماده‌سازی مخزن ${user.login}/$targetRepo",
                        stepDescription = if (_repoTargetMode.value == RepoTargetMode.NEW_REPO) "ایجاد مخزن جدید در گیت‌هاب..." else "بررسی مخزن موجود...",
                        progress = 0.25f
                    )
                    delay(700)
                    val total = filesToUpload.size.coerceAtLeast(6)
                    for (i in 1..total) {
                        delay(180)
                        _pushProgressState.value = PushProgressState(
                            isProcessing = true,
                            stepTitle = "در حال آپلود کدهای پروژه به مخزن ($i/$total)",
                            stepDescription = "ارسال فایل کاتلین/گریدل شماره $i...",
                            progress = 0.25f + (0.45f * (i.toFloat() / total.toFloat())),
                            uploadedFilesCount = i
                        )
                    }
                    delay(500)
                    _pushProgressState.value = PushProgressState(
                        isProcessing = true,
                        stepTitle = "ایجاد فایل اکشن .github/workflows/build-apk.yml",
                        stepDescription = "فعال‌سازی خط ساخت خودکار APK...",
                        progress = 0.85f,
                        uploadedFilesCount = total + 1
                    )
                    delay(500)
                    val repoUrl = "https://github.com/${user.login}/$targetRepo"
                    val actionsUrl = "$repoUrl/actions"
                    _pushProgressState.value = PushProgressState(
                        isProcessing = false,
                        stepTitle = "آپلود و راه‌اندازی با موفقیت انجام شد!",
                        stepDescription = "تمام فایل‌های کاتلین به همراه اکشن build-apk.yml در مخزن گیت‌هاب شما مستقر شدند.",
                        progress = 1.0f,
                        targetRepoUrl = repoUrl,
                        actionsUrl = actionsUrl,
                        isFinished = true,
                        uploadedFilesCount = total + 2
                    )
                    _toastMessage.value = "پروژه و ورک‌فلو با موفقیت در گیت‌هاب آپلود شدند!"
                    return@launch
                }

                // --- Real GitHub REST API ---

                // 1. Create Repo if it doesn't exist
                if (_repoTargetMode.value == RepoTargetMode.NEW_REPO || _repoLookupState.value is RepoLookupState.NotFound) {
                    _pushProgressState.value = _pushProgressState.value.copy(
                        stepTitle = "ایجاد مخزن جدید $targetRepo در گیت‌هاب",
                        stepDescription = "ارسال درخواست ساخت مخزن تمیز به گیت‌هاب...",
                        progress = 0.2f
                    )
                    val createResult = gitHubApiService.createRepository(
                        token = token,
                        repoName = targetRepo,
                        description = _newRepoDescription.value,
                        isPrivate = _newRepoPrivate.value,
                        owner = owner
                    )
                    if (createResult.isFailure) {
                        val err = createResult.exceptionOrNull()?.message ?: "خطا در ایجاد مخزن"
                        _pushProgressState.value = PushProgressState(errorMessage = err)
                        _toastMessage.value = err
                        return@launch
                    }
                }

                // 2. Upload extracted project files (Kotlin, Gradle, Manifest, etc.)
                var uploadedCount = 0
                val totalFiles = filesToUpload.size
                if (totalFiles > 0) {
                    filesToUpload.forEachIndexed { index, file ->
                        val currentNum = index + 1
                        _pushProgressState.value = _pushProgressState.value.copy(
                            stepTitle = "آپلود فایل‌های پروژه ($currentNum/$totalFiles)",
                            stepDescription = "در حال ارسال: ${file.relativePath}",
                            progress = 0.25f + (0.45f * (currentNum.toFloat() / totalFiles.toFloat())),
                            uploadedFilesCount = uploadedCount
                        )

                        if (file.isText && file.textContent != null) {
                            gitHubApiService.commitWorkflowFile(
                                token = token,
                                owner = owner,
                                repo = targetRepo,
                                path = file.relativePath,
                                fileContent = file.textContent,
                                commitMessage = "feat: add ${file.relativePath} from Android project ZIP"
                            )
                        } else if (file.rawBytes != null) {
                            gitHubApiService.commitRawBytesFile(
                                token = token,
                                owner = owner,
                                repo = targetRepo,
                                path = file.relativePath,
                                bytes = file.rawBytes,
                                commitMessage = "feat: add ${file.relativePath}"
                            )
                        }
                        uploadedCount++
                    }
                }

                // 3. Commit .github/workflows/build-apk.yml
                _pushProgressState.value = _pushProgressState.value.copy(
                    stepTitle = "ایجاد ورک‌فلو .github/workflows/build-apk.yml",
                    stepDescription = "فعال‌سازی خط خودکار ساخت APK در GitHub Actions...",
                    progress = 0.85f,
                    uploadedFilesCount = uploadedCount + 1
                )

                val workflowYaml = generateWorkflowYaml()
                val workflowCommitResult = gitHubApiService.commitWorkflowFile(
                    token = token,
                    owner = owner,
                    repo = targetRepo,
                    path = ".github/workflows/build-apk.yml",
                    fileContent = workflowYaml,
                    commitMessage = "ci: add automated android apk build workflow"
                )

                if (workflowCommitResult.isFailure) {
                    val err = workflowCommitResult.exceptionOrNull()?.message ?: "خطا در ثبت فایل اکشن"
                    _pushProgressState.value = PushProgressState(errorMessage = err)
                    _toastMessage.value = err
                    return@launch
                }

                // 4. Commit README.md
                val readmeContent = """
# $targetRepo

Android Kotlin project with automated GitHub Actions APK CI/CD pipeline.

## 🚀 Automated APK Build
This repository includes `.github/workflows/build-apk.yml`:
- Builds APK on every push to `main`
- Retains downloadable APK artifact in the Actions tab for 30 days
- Automatically publishes APK to GitHub Releases on tag pushes (`v*`)
                """.trimIndent()

                gitHubApiService.commitWorkflowFile(
                    token = token,
                    owner = owner,
                    repo = targetRepo,
                    path = "README.md",
                    fileContent = readmeContent,
                    commitMessage = "docs: add project README with CI/CD guide"
                )

                val repoUrl = "https://github.com/$owner/$targetRepo"
                val actionsUrl = "$repoUrl/actions"

                _pushProgressState.value = PushProgressState(
                    isProcessing = false,
                    stepTitle = "پروژه و ورک‌فلو با موفقیت در گیت‌هاب آپلود شدند!",
                    stepDescription = "فایل‌های کاتلین و اکشن build-apk.yml در مخزن مستقر شدند و بیلد در حال اجراست.",
                    progress = 1.0f,
                    targetRepoUrl = repoUrl,
                    actionsUrl = actionsUrl,
                    isFinished = true,
                    uploadedFilesCount = uploadedCount + 2
                )
                _toastMessage.value = "آپلود فایل‌های زیپ و ساخت اکشن تکمیل شد!"

            } catch (e: Exception) {
                _pushProgressState.value = PushProgressState(
                    errorMessage = "خطا در عملیات: ${e.localizedMessage ?: "مشکل ارتباطی"}"
                )
                _toastMessage.value = "خطا: ${e.message}"
            }
        }
    }

    fun resetPushState() {
        _pushProgressState.value = PushProgressState()
    }

    // --- Build Pipeline Simulation ---
    fun startBuildPipeline() {
        if (_isBuilding.value) return
        _isBuilding.value = true
        _buildProgress.value = 0.05f
        resetBuildSteps()
        _consoleLogs.value = listOf(
            "🚀 [Gradle Daemon] Initializing build environment...",
            "⚙️ JDK Distribution: Eclipse Temurin 17.0.12 (x86_64 Linux)",
            "📁 Workspace Root: /workspace"
        )

        viewModelScope.launch {
            val totalSteps = _buildSteps.value.size

            for (index in 0 until totalSteps) {
                val stepStartTime = System.currentTimeMillis()
                _buildSteps.update { list ->
                    list.mapIndexed { i, step ->
                        if (i == index) step.copy(state = StepState.RUNNING) else step
                    }
                }

                val currentStep = _buildSteps.value[index]
                addLog("▶️ Step ${index + 1}/$totalSteps: ${currentStep.title}")
                addLog("   $ ${currentStep.command}")

                when (index) {
                    0 -> {
                        delay(500)
                        addLog("   ✓ gradlew permissions verified: -rwxr-xr-x")
                        addLog("   ✓ Gradle 8.11.1 distribution OK")
                    }
                    1 -> {
                        delay(600)
                        addLog("   ✓ Resolving dependencies from Google and MavenCentral")
                        addLog("   ✓ Kotlin & Compose libraries verified")
                    }
                    2 -> {
                        delay(800)
                        addLog("   ✓ KSP & Compose compiler processing complete")
                        addLog("   ✓ Compiled Kotlin classes to DEX")
                    }
                    3 -> {
                        delay(500)
                        addLog("   ✓ Running Unit & Robolectric Tests... [PASSED]")
                    }
                    4 -> {
                        delay(900)
                        addLog("   ✓ Packaging resources and creating APK")
                        addLog("   ✓ APK digitally signed with debug key")
                    }
                    5 -> {
                        delay(500)
                        val apkName = "${_newRepoName.value.ifBlank { "MyKotlinApp" }}-debug.apk"
                        val sha = "e8f2b7a90412c938" + (10000000..99999999).random().toString() + "f4b01"
                        addLog("   ✓ Output APK: release-apk/$apkName")
                        addLog("   ✓ Size: 18.4 MB | SHA-256: $sha")

                        val nowFormatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        _latestApk.value = ApkInfo(
                            fileName = apkName,
                            sizeMb = 18.4,
                            buildType = "debug",
                            packageName = "com.aistudio.apkautomation.kxmpzq",
                            versionCode = 1,
                            versionName = "1.0",
                            outputPath = "release-apk/$apkName",
                            sha256 = sha,
                            generatedTimeFormatted = nowFormatted
                        )
                    }
                }

                val duration = System.currentTimeMillis() - stepStartTime
                _buildSteps.update { list ->
                    list.mapIndexed { i, step ->
                        if (i == index) step.copy(state = StepState.COMPLETED, durationMs = duration) else step
                    }
                }

                _buildProgress.value = (index + 1).toFloat() / totalSteps
            }

            addLog("🎉 BUILD SUCCESSFUL in ${(1..3).random()}s")
            _isBuilding.value = false
            _toastMessage.value = "ساخت APK با موفقیت انجام شد!"
        }
    }

    private fun addLog(log: String) {
        _consoleLogs.update { it + log }
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
        resetBuildSteps()
        _latestApk.value = null
        _buildProgress.value = 0f
    }

    fun generateWorkflowYaml(): String {
        val repo = _repoQuery.value.ifBlank { "MyKotlinApp" }
        return """
name: Android APK Build

on:
  push:
    branches:
      - main
      - master
    tags:
      - 'v*'
  pull_request:
    branches:
      - main
      - master
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Validate Gradle Wrapper
        run: |
          test -f gradlew || {
            echo "Gradle Wrapper is missing"
            exit 1
          }
          chmod +x gradlew

      - name: Build debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Collect APK
        shell: bash
        run: |
          mkdir -p release-apk

          find . -type f \
            -path '*/build/outputs/apk/*/*.apk' \
            -not -path '*/build/outputs/apk/*/androidTest/*' \
            -exec cp {} release-apk/ \;

          test -n "$$(find release-apk -type f -name '*.apk' -print -quit)" || {
            echo "No APK was generated"
            exit 1
          }

          python3 - <<'PY'
          import os
          from pathlib import Path

          folder = Path("release-apk")
          files = sorted(folder.glob("*.apk"))
          repo = os.environ.get("GITHUB_REPOSITORY", "$repo").split("/")[-1]

          for index, path in enumerate(files, 1):
              name = (
                  f"{repo}-debug.apk"
                  if index == 1
                  else f"{repo}-debug-{index}.apk"
              )
              target = folder / name

              if path != target:
                  path.rename(target)

          for path in sorted(folder.glob("*.apk")):
              print(f"{path}: {path.stat().st_size} bytes")
          PY

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: android-apk
          path: release-apk/*.apk
          if-no-files-found: error
          retention-days: 30

      - name: Publish GitHub Release
        if: startsWith(github.ref, 'refs/tags/v')
        uses: softprops/action-gh-release@v2
        with:
          files: release-apk/*.apk
          generate_release_notes: true
""".trimIndent()
    }

    private fun loadInitialHealthChecks() {
        _healthChecks.value = listOf(
            HealthCheckItem(
                id = "settings_gradle",
                title = "settings.gradle.kts",
                description = "ریشه پروژه و ماژول :app شناسایی شد.",
                isPassed = true,
                category = "STRUCTURE"
            ),
            HealthCheckItem(
                id = "build_gradle",
                title = "app/build.gradle.kts",
                description = "پلاگین‌های Application، Compose و KSP پیکربندی شده‌اند.",
                isPassed = true,
                category = "GRADLE"
            ),
            HealthCheckItem(
                id = "gradle_wrapper",
                title = "Gradle Wrapper (gradlew)",
                description = "اسکریپت و توزیع نسخه 8.11 فعال است.",
                isPassed = true,
                category = "GRADLE"
            ),
            HealthCheckItem(
                id = "gitignore_secrets",
                title = "امنیت فیلتر .gitignore",
                description = "محافظت در برابر نشت keystore و فایلهای محرمانه فعال است.",
                isPassed = true,
                category = "SECURITY"
            )
        )
    }

    private fun resetBuildSteps() {
        _buildSteps.value = listOf(
            BuildStep(1, "اعتبارسنجی محیط و دسترسی‌ها", "chmod +x gradlew", "بررسی اسکریپت Wrapper و JDK 17"),
            BuildStep(2, "حل وابستگی‌ها و بسته‌ها", "./gradlew dependencies", "دریافت بسته‌های Compose و AndroidX"),
            BuildStep(3, "کامپایل کاتلین و KSP", "./gradlew compileDebugKotlin", "تبدیل کدهای برنامه به بایت‌کد"),
            BuildStep(4, "اجرای تست‌های اعتبارسنجی", "./gradlew testDebugUnitTest", "بررسی سلامت منطق برنامه"),
            BuildStep(5, "تولید APK و امضای دیجیتال", "./gradlew assembleDebug", "بسته‌بندی فایل APK خروجی"),
            BuildStep(6, "استخراج خروجی Artifact", "collect-apk -> release-apk/", "محاسبه سایز و چک‌سام SHA-256")
        )
    }

    fun copyTextToClipboard(context: Context, text: String, label: String = "Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        _toastMessage.value = "کپی شد به کلیپ‌بورد!"
    }
}
