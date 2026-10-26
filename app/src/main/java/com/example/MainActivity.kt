package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.AuthManager
import com.example.data.RyzoraRepository
import com.example.ui.components.StatusBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    OVERVIEW("Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    INBOX("Inbox", Icons.Filled.Forum, Icons.Outlined.Forum),
    ORDERS("Orders", Icons.Filled.LocalMall, Icons.Outlined.LocalMall),
    CATALOG("Catalog", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    SIMULATOR("Simulator", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
    DOCS("Setup", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Mandatory App Check configuration with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val repository = RyzoraRepository.instance

        setContent {
            RyzoraTheme {
                MainAppScaffold(
                    activity = this@MainActivity,
                    repository = repository
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    activity: ComponentActivity,
    repository: RyzoraRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableStateOf(NavigationTab.OVERVIEW) }
    val conversations by repository.conversations.collectAsState()
    val config by repository.businessConfig.collectAsState()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto-sign in on startup
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onUnauthenticated = { /* Prompt interactive */ },
            scope = coroutineScope
        )
    }

    val pendingEscalations = conversations.count { it.requiresHumanTakeover }

    // BackHandler: return to Overview if on another tab
    if (selectedTab != NavigationTab.OVERVIEW) {
        BackHandler {
            selectedTab = NavigationTab.OVERVIEW
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "7R",
                                color = RyzoraCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RYZORA",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Agent",
                                    color = RyzoraCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (currentUser != null) "Firebase: ${currentUser?.email}" else "Smart Choice. Better Life.",
                                color = if (currentUser != null) RyzoraEmerald else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        if (currentUser == null) {
                            Button(
                                onClick = {
                                    isSigningIn = true
                                    AuthManager.signInWithGoogle(
                                        activity = activity,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isSigningIn = false
                                            currentUser = Firebase.auth.currentUser
                                        },
                                        onAuthError = { errorMsg ->
                                            isSigningIn = false
                                            authError = errorMsg
                                        },
                                        onAuthCancelled = { isSigningIn = false },
                                        scope = coroutineScope
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RyzoraCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isSigningIn) "..." else "Sign in",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            IconButton(
                                onClick = {
                                    AuthManager.signOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onComplete = { currentUser = null },
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        StatusBadge(
                            text = if (config.isGlobalAiAutoReplyEnabled) "AI Live" else "AI Paused",
                            color = if (config.isGlobalAiAutoReplyEnabled) RyzoraEmerald else RyzoraRose
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextSecondary,
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 8.dp
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val hasBadge = tab == NavigationTab.INBOX && pendingEscalations > 0

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (hasBadge) {
                                        Badge(
                                            containerColor = RyzoraAmber,
                                            contentColor = Color.Black
                                        ) {
                                            Text("$pendingEscalations", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) RyzoraCyan else TextSecondary
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) RyzoraCyan else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RyzoraCyan,
                            selectedTextColor = RyzoraCyan,
                            indicatorColor = RyzoraCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.OVERVIEW -> DashboardScreen(
                    repository = repository,
                    onNavigateToConversations = { selectedTab = NavigationTab.INBOX },
                    onNavigateToOrders = { selectedTab = NavigationTab.ORDERS },
                    onNavigateToSimulator = { selectedTab = NavigationTab.SIMULATOR }
                )
                NavigationTab.INBOX -> ConversationsScreen(repository = repository)
                NavigationTab.ORDERS -> OrdersScreen(repository = repository)
                NavigationTab.CATALOG -> ProductsScreen(repository = repository)
                NavigationTab.SIMULATOR -> WebhookSimulatorScreen(repository = repository)
                NavigationTab.DOCS -> AgentSettingsScreen(repository = repository)
            }
        }
    }
}
