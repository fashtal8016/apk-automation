package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BuildTerminalScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.UploadAndRepoScreen
import com.example.ui.screens.WorkflowScreen
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.viewmodel.AutomationViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AutomationViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AutomationViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val gitHubUser by viewModel.gitHubUser.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val pushProgress by viewModel.pushProgressState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    if (selectedTab != 0) {
        BackHandler {
            viewModel.selectTab(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "اتوماسیون اندروید و گیت‌هاب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (gitHubUser != null) AccentEmerald.copy(alpha = 0.15f) else Color(0xFF1E293B),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (gitHubUser != null) AccentEmerald else Color(0xFF94A3B8))
                            )
                            Text(
                                text = gitHubUser?.login ?: "عدم اتصال",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gitHubUser != null) AccentEmerald else Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.CloudUpload else Icons.Outlined.CloudUpload,
                            contentDescription = stringResource(R.string.tab_upload)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_upload)) },
                    modifier = Modifier.testTag("nav_tab_upload"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Code else Icons.Outlined.Code,
                            contentDescription = stringResource(R.string.tab_workflow)
                        )
                    },
                    label = { Text("ورک‌فلو") },
                    modifier = Modifier.testTag("nav_tab_workflow"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                            contentDescription = stringResource(R.string.tab_build)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_build)) },
                    modifier = Modifier.testTag("nav_tab_build"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = stringResource(R.string.tab_dashboard)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_dashboard)) },
                    modifier = Modifier.testTag("nav_tab_dashboard"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> UploadAndRepoScreen(viewModel = viewModel)
                    1 -> WorkflowScreen(viewModel = viewModel)
                    2 -> BuildTerminalScreen(viewModel = viewModel)
                    3 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { viewModel.selectTab(it) }
                    )
                    else -> UploadAndRepoScreen(viewModel = viewModel)
                }
            }
        }
    }
}
