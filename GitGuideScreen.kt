package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.viewmodel.AutomationViewModel

@Composable
fun GitGuideScreen(viewModel: AutomationViewModel) {
    val context = LocalContext.current
    val settings by viewModel.workflowSettings.collectAsState()

    val repo = settings.repoName
    val user = settings.githubUsername

    val fullScript = """
# 1. ورود به پوشه پروژه اندروید
cd "$repo"

# 2. بررسی فایل‌های اصلی
test -f settings.gradle.kts || test -f settings.gradle || echo "هشدار: ریشه پروژه را بررسی کنید"

# 3. ایجاد و آماده‌سازی مخزن Git
git init
git branch -M ${settings.mainBranch}

# 4. افزودن فایل‌ها با رعایت امنیت .gitignore
git add .
git commit -m "feat: initial commit with android CI/CD apk build automation"

# 5. اتصال به GitHub
git remote add origin https://github.com/$user/$repo.git

# 6. ارسال کد به مخزن اصلی
git push -u origin ${settings.mainBranch}

# 7. ایجاد تگ نسخه‌ای برای تولید خودکار GitHub Release
git tag -a v1.0.0 -m "Release v1.0.0 with automated APK"
git push origin v1.0.0
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("git_guide_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Guide Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "دستورالعمل اجرایی انتقال به GitHub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "انتقال پروژه $repo به حساب GitHub و فعال‌سازی بیلد خودکار APK",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Complete Automation Script Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(PrimaryIndigo, SecondaryCyan)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اسکریپت کامل اتوماسیون (Bash / Terminal)",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                viewModel.copyTextToClipboard(context, fullScript, "Git Setup Script")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("copy_full_script_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("کپی اسکریپت کامل", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF050811), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullScript,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Step by Step Instructions
        item {
            Text(
                text = "مراحل گام‌به‌گام اجرایی",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            StepInstructionCard(
                stepNumber = "۱",
                title = "استخراج امن فایل ZIP پروژه",
                desc = "فایل ZIP دانلود شده از AI Studio را در یک پوشه استخراج کنید. دقت کنید فایل settings.gradle.kts دقیقاً در ریشه پوشه پروژه قرار گیرد.",
                codeSnippet = "unzip $repo.zip -d ./$repo\ncd ./$repo",
                onCopy = { viewModel.copyTextToClipboard(context, "unzip $repo.zip -d ./$repo\ncd ./$repo") }
            )
        }

        item {
            StepInstructionCard(
                stepNumber = "۲",
                title = "ایجاد و بررسی ساختار Git محلی",
                desc = "یک مخزن محلی بسازید و شاخه را به ${settings.mainBranch} تغییر دهید.",
                codeSnippet = "git init\ngit branch -M ${settings.mainBranch}",
                onCopy = { viewModel.copyTextToClipboard(context, "git init\ngit branch -M ${settings.mainBranch}") }
            )
        }

        item {
            StepInstructionCard(
                stepNumber = "۳",
                title = "ثبت کامیت اولیه همراه با Workflow",
                desc = "فایل‌های پروژه به همراه اکشن ساخته‌شده .github/workflows/android-apk.yml و فیلتر .gitignore را کامیت کنید.",
                codeSnippet = "git add .\ngit commit -m \"feat: initial commit with android-apk automation\"",
                onCopy = { viewModel.copyTextToClipboard(context, "git add .\ngit commit -m \"feat: initial commit with android-apk automation\"") }
            )
        }

        item {
            StepInstructionCard(
                stepNumber = "۴",
                title = "اتصال به مخزن GitHub و ارسال نهایی",
                desc = "مخزن را به آدرس ریموت متصل کرده و تغییرات را به گیت‌هاب پوش کنید تا بیلد خودکار بلافاصله شروع شود.",
                codeSnippet = "git remote add origin https://github.com/$user/$repo.git\ngit push -u origin ${settings.mainBranch}",
                onCopy = { viewModel.copyTextToClipboard(context, "git remote add origin https://github.com/$user/$repo.git\ngit push -u origin ${settings.mainBranch}") }
            )
        }

        item {
            StepInstructionCard(
                stepNumber = "۵",
                title = "انتشار در GitHub Releases (اختیاری)",
                desc = "با ارسال یک Tag نسخه‌ای، اکشن به‌طور خودکار یک Release رسمی در صفحه مخزن شما ایجاد می‌کند و فایل APK را داخل آن آپلود می‌کند.",
                codeSnippet = "git tag -a v1.0.0 -m \"Release v1.0.0\"\ngit push origin v1.0.0",
                onCopy = { viewModel.copyTextToClipboard(context, "git tag -a v1.0.0 -m \"Release v1.0.0\"\ngit push origin v1.0.0") }
            )
        }

        // How to download APK Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AccentEmerald.copy(alpha = 0.1f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(AccentEmerald, SecondaryCyan)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "نحوه دانلود APK خروجی از گیت‌هاب",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "۱. روش Actions Artifact:\nبه صفحه مخزن گیت‌هاب بروید ← تب Actions ← آخرین اجرای موفق (سبز رنگ) را انتخاب کنید ← در پایین صفحه بخش Artifacts، روی 'android-apk' کلیک کنید تا فایل zip دانلود شود.\n\n۲. روش GitHub Releases:\nدر صفحه اصلی مخزن ← بخش Releases در سمت راست ← روی نسخه v1.0.0 کلیک کنید و مستقیماً فایل $repo-debug.apk را دریافت کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StepInstructionCard(
    stepNumber: String,
    title: String,
    desc: String,
    codeSnippet: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = codeSnippet,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کپی", fontSize = 11.sp)
                }
            }
        }
    }
}
