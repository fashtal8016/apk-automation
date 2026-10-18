package com.example.service

import android.util.Base64
import com.example.model.GitHubRepo
import com.example.model.GitHubUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitHubApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun verifyToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Android-APK-Automation-App")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val user = GitHubUser(
                    login = json.optString("login", "unknown"),
                    name = json.optString("name", json.optString("login")),
                    avatarUrl = json.optString("avatar_url", null),
                    publicRepos = json.optInt("public_repos", 0)
                )
                Result.success(user)
            } else {
                val errorMsg = if (response.code == 401) {
                    "توکن نامعتبر است یا منقضی شده است (HTTP 401)"
                } else {
                    "خطا در ارتباط با گیت‌هاب: کد ${response.code}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطای شبکه: ${e.localizedMessage ?: "عدم دسترسی به اینترنت"}"))
        }
    }

    suspend fun fetchRepositories(token: String): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user/repos?sort=updated&per_page=50")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Android-APK-Automation-App")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val array = JSONArray(body)
                val repos = mutableListOf<GitHubRepo>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    repos.add(
                        GitHubRepo(
                            name = obj.optString("name"),
                            fullName = obj.optString("full_name"),
                            isPrivate = obj.optBoolean("private", false),
                            htmlUrl = obj.optString("html_url")
                        )
                    )
                }
                Result.success(repos)
            } else {
                Result.failure(Exception("دریافت مخازن ناموفق بود (کد ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkRepoExists(token: String, owner: String, repoName: String): Result<GitHubRepo?> = withContext(Dispatchers.IO) {
        try {
            val clean = repoName.trim().replace(" ", "-")
            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$clean")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Android-APK-Automation-App")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val obj = JSONObject(body)
                val repo = GitHubRepo(
                    name = obj.optString("name", clean),
                    fullName = obj.optString("full_name", "$owner/$clean"),
                    isPrivate = obj.optBoolean("private", false),
                    htmlUrl = obj.optString("html_url", "https://github.com/$owner/$clean")
                )
                Result.success(repo)
            } else if (response.code == 404) {
                Result.success(null)
            } else {
                Result.failure(Exception("خطا در استعلام مخزن (${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepository(
        token: String,
        repoName: String,
        description: String,
        isPrivate: Boolean,
        owner: String
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val cleanName = repoName.trim().replace(" ", "-")
            val payload = JSONObject().apply {
                put("name", cleanName)
                put("description", description)
                put("private", isPrivate)
                put("auto_init", true)
            }

            val request = Request.Builder()
                .url("https://api.github.com/user/repos")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Android-APK-Automation-App")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || response.code == 201) {
                val obj = JSONObject(body)
                val created = GitHubRepo(
                    name = obj.optString("name", cleanName),
                    fullName = obj.optString("full_name", "$owner/$cleanName"),
                    isPrivate = obj.optBoolean("private", isPrivate),
                    htmlUrl = obj.optString("html_url", "https://github.com/$owner/$cleanName")
                )
                Result.success(created)
            } else if (response.code == 422 && body.contains("already exists")) {
                // Repository already exists on GitHub, gracefully use it
                val existing = GitHubRepo(
                    name = cleanName,
                    fullName = "$owner/$cleanName",
                    isPrivate = isPrivate,
                    htmlUrl = "https://github.com/$owner/$cleanName"
                )
                Result.success(existing)
            } else {
                val errorJson = try { JSONObject(body) } catch (_: Exception) { null }
                val message = errorJson?.optString("message") ?: "خطای ناشناخته (${response.code})"
                Result.failure(Exception("ایجاد مخزن با خطا مواجه شد: $message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitWorkflowFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        fileContent: String,
        commitMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedBase64 = Base64.encodeToString(fileContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            commitBase64File(token, owner, repo, path, encodedBase64, commitMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitRawBytesFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        bytes: ByteArray,
        commitMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            commitBase64File(token, owner, repo, path, encodedBase64, commitMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun commitBase64File(
        token: String,
        owner: String,
        repo: String,
        path: String,
        encodedBase64: String,
        commitMessage: String
    ): Result<String> {
        return try {
            // Check if file already exists to get SHA
            var existingSha: String? = null
            try {
                val checkRequest = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repo/contents/$path")
                    .header("Authorization", "Bearer ${token.trim()}")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Android-APK-Automation-App")
                    .build()
                val checkResponse = client.newCall(checkRequest).execute()
                if (checkResponse.isSuccessful) {
                    val checkBody = checkResponse.body?.string() ?: ""
                    val json = JSONObject(checkBody)
                    existingSha = json.optString("sha")
                }
            } catch (_: Exception) {}

            val payload = JSONObject().apply {
                put("message", commitMessage)
                put("content", encodedBase64)
                if (!existingSha.isNullOrEmpty()) {
                    put("sha", existingSha)
                }
            }

            val putRequest = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/contents/$path")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Android-APK-Automation-App")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(putRequest).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || response.code == 200 || response.code == 201) {
                val obj = JSONObject(body)
                val commitUrl = obj.optJSONObject("commit")?.optString("html_url") ?: ""
                Result.success(commitUrl)
            } else {
                val errorJson = try { JSONObject(body) } catch (_: Exception) { null }
                val msg = errorJson?.optString("message") ?: "کد خطای ${response.code}"
                Result.failure(Exception("ثبت فایل $path ناموفق بود: $msg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
