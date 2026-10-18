package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RepoTargetMode
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.viewmodel.AutomationViewModel
import com.example.viewmodel.RepoLookupState

@Composable
fun UploadAndRepoScreen(viewModel: AutomationViewModel) {
    val context = LocalContext.current

    val gitHubUser by viewModel.gitHubUser.collectAsState()
    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val userRepos by viewModel.userRepos.collectAsState()
    val filteredRepos by viewModel.filteredRepos.collectAsState()

    val repoQuery by viewModel.repoQuery.collectAsState()
    val repoLookupState by viewModel.repoLookupState.collectAsState()
    val repoTargetMode by viewModel.repoTargetMode.collectAsState()

    val newRepoDescription by viewModel.newRepoDescription.collectAsState()
    val newRepoPrivate by viewModel.newRepoPrivate.collectAsState()

    val selectedZip by viewModel.selectedZipResult.collectAsState()
    val isInspectingZip by viewModel.isInspectingZip.collectAsState()

    val pushProgress by viewModel.pushProgressState.collectAsState()

    var tokenInput by remember { mutableStateOf("") }
    var showTokenText by remember { mutableStateOf(false) }

    // Launcher 1: Universal GetContent
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.inspectZipFile(context, it) }
    }

    // Launcher 2: OpenDocument fallback
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.inspectZipFile(context, it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("upload_and_repo_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // --- STEP 1: GITHUB ACCOUNT LOGIN & SAVED TOKEN ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_auth_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (gitHubUser != null) AccentEmerald else PrimaryIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "۱. اتصال به حساب GitHub",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (gitHubUser != null) "متصل به: ${gitHubUser!!.login} (توکن ذخیره شده ✓)" else "ورود با توکن دسترسی (PAT) یا حالت دمو",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (gitHubUser != null) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (gitHubUser != null) {
                            OutlinedButton(
                                onClick = { viewModel.logoutGitHub() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("خروج", fontSize = 12.sp, color = AccentRose)
                            }
                        }
                    }

                    if (gitHubUser == null) {
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("توکن دسترسی شخصی (GitHub Personal Access Token)") },
                            placeholder = { Text("ghp_xxxxxxxxxxxxxx") },
                            singleLine = true,
                            visualTransformation = if (showTokenText) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showTokenText = !showTokenText }) {
                                    Icon(
                                        imageVector = if (showTokenText) Icons.Default.Key else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_token_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 توکن پس از ورود در گوشی شما ذخیره شده و در اجراهای بعدی نیازی به ورود مجدد نخواهد بود.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.loginWithGitHub(tokenInput) },
                                enabled = !isLoggingIn && tokenInput.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("login_button")
                            ) {
                                if (isLoggingIn) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بررسی...")
                                } else {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ورود و ذخیره توکن")
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.loginWithDemoAccount() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                                    .testTag("demo_login_button")
                            ) {
                                Text("حالت دمو")
                            }
                        }
                    }
                }
            }
        }

        // --- STEP 2: UPLOAD ZIP FILE ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zip_upload_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (selectedZip != null) AccentEmerald else SecondaryCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "۲. انتخاب فایل ZIP پروژه اندروید",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "شامل کدهای Kotlin، مانیفست و فایلهای Gradle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    getContentLauncher.launch("*/*")
                                } catch (_: Exception) {
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("pick_zip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انتخاب ZIP از گوشی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.loadSampleProjectZip() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                                .testTag("load_active_project_button")
                        ) {
                            Text("پروژه فعال کاتلین", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (isInspectingZip) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("در حال استخراج و تحلیل فایل‌های زیپ...", fontSize = 12.sp)
                        }
                    }

                    // ZIP Inspection Result Box
                    if (selectedZip != null) {
                        val zip = selectedZip!!
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentEmerald.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = zip.fileName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "(${zip.extractedFiles.size} فایل آماده آپلود)",
                                        fontSize = 12.sp,
                                        color = AccentEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E293B)) {
                                        Text("${zip.ktFileCount} کاتلین", color = SecondaryCyan, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E293B)) {
                                        Text("${zip.gradleFileCount} گریدل", color = Color(0xFFFBBF24), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    if (zip.hasSettingsGradle) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E293B)) {
                                            Text("settings.gradle ✓", color = AccentEmerald, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- STEP 3: REPOS SEARCH & DESTINATION ON GITHUB ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("destination_repo_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "۳. جستجو و انتخاب مخزن در GitHub (Repos یا New Repos)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نام مخزن را بنویسید تا به‌طور زنده روی گیت‌هاب جستجو شود:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Repo Search Input Field
                    OutlinedTextField(
                        value = repoQuery,
                        onValueChange = { viewModel.setRepoQuery(it) },
                        label = { Text("نام مخزن (Repository Name)") },
                        placeholder = { Text("مثلاً MyKotlinApp") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (repoLookupState is RepoLookupState.Searching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("repo_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Status Indicator Banner on GitHub
                    when (val state = repoLookupState) {
                        is RepoLookupState.Searching -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("در حال جستجو و استعلام مخزن در گیت‌هاب...", fontSize = 12.sp)
                                }
                            }
                        }
                        is RepoLookupState.Found -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AccentEmerald.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(
                                            text = "مخزن «${state.repo.name}» در گیت‌هاب شما یافت شد!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = AccentEmerald
                                        )
                                        Text(
                                            text = "کدهای پروژه مستقیماً در این مخزن موجود آپلود خواهند شد.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        is RepoLookupState.NotFound -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PrimaryIndigo.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(
                                            text = "مخزن جدید: «${state.repoName}»",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PrimaryIndigo
                                        )
                                        Text(
                                            text = "این مخزن در گیت‌هاب وجود ندارد و به‌صورت خودکار ایجاد خواهد شد.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        else -> {}
                    }

                    // Existing repos chip suggestions if user has repos
                    if (filteredRepos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "مخازن منطبق حساب شما:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredRepos.take(3).forEach { repo ->
                                FilterChip(
                                    selected = repoQuery.equals(repo.name, ignoreCase = true),
                                    onClick = { viewModel.selectExistingRepo(repo.name) },
                                    label = { Text(repo.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // New Repo Settings (if creating new)
                    if (repoLookupState is RepoLookupState.NotFound || repoTargetMode == RepoTargetMode.NEW_REPO) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newRepoDescription,
                            onValueChange = { viewModel.updateNewRepoDescription(it) },
                            label = { Text("توضیحات مخزن جدید (Description)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (newRepoPrivate) Icons.Default.Lock else Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (newRepoPrivate) "مخزن خصوصی (Private)" else "مخزن عمومی (Public)",
                                    fontSize = 13.sp
                                )
                            }
                            Switch(
                                checked = newRepoPrivate,
                                onCheckedChange = { viewModel.toggleNewRepoPrivate(it) }
                            )
                        }
                    }
                }
            }
        }

        // --- STEP 4: BIG PROMINENT UPLOAD BUTTON ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trigger_push_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryIndigo, Color(0xFF3730A3), SecondaryCyan)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "۴. آنزیپ (Unzip) و ارسال کدهای پروژه به مخزن گیت‌هاب",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "فایل زیپ استخراج شده و تمام فایلهای کاتلین و گرادل مستقیماً در مخزن قرار می‌گیرند (نه زیپ خام)",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.pushProjectAndWorkflowToGitHub() },
                            enabled = !pushProgress.isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PrimaryIndigo
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_push_button")
                        ) {
                            if (pushProgress.isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryIndigo, strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال آنزیپ و آپلود فایل‌ها...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "آنزیپ و آپلود کدهای کاتلین + ساخت build-apk.yml",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- STEP 5: PROGRESS / SUCCESS / ERROR CARD ---
        if (pushProgress.isProcessing || pushProgress.isFinished || pushProgress.errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            pushProgress.errorMessage != null -> AccentRose.copy(alpha = 0.12f)
                            pushProgress.isFinished -> AccentEmerald.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(
                                when {
                                    pushProgress.errorMessage != null -> AccentRose
                                    pushProgress.isFinished -> AccentEmerald
                                    else -> PrimaryIndigo
                                },
                                SecondaryCyan
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    pushProgress.errorMessage != null -> Icons.Default.Warning
                                    pushProgress.isFinished -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Refresh
                                },
                                contentDescription = null,
                                tint = when {
                                    pushProgress.errorMessage != null -> AccentRose
                                    pushProgress.isFinished -> AccentEmerald
                                    else -> PrimaryIndigo
                                },
                                modifier = Modifier.size(26.dp)
                            )

                            Column {
                                Text(
                                    text = pushProgress.stepTitle.ifBlank { "وضعیت آپلود" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pushProgress.errorMessage ?: pushProgress.stepDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (pushProgress.isProcessing) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { pushProgress.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SecondaryCyan
                            )
                        }

                        if (pushProgress.isFinished && pushProgress.targetRepoUrl != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pushProgress.actionsUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاهده تب Actions", fontSize = 12.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pushProgress.targetRepoUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("مشاهده مخزن", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
