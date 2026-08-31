package com.zam.photos.app.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.zam.photos.app.ModerationAccess
import com.zam.photos.app.debug.AuthDebugLog
import com.zam.photos.app.push.PushTokenManager
import com.zam.photos.app.ui.components.FamilyBottomBar
import com.zam.photos.app.ui.screens.ChatThreadScreen
import com.zam.photos.app.ui.screens.CommentsScreen
import com.zam.photos.app.ui.screens.CreatePostScreen
import com.zam.photos.app.ui.screens.ExploreScreen
import com.zam.photos.app.ui.screens.FeedScreen
import com.zam.photos.app.ui.screens.LoginScreen
import com.zam.photos.app.ui.screens.MessagesInboxScreen
import com.zam.photos.app.ui.screens.ModerationScreen
import com.zam.photos.app.ui.screens.NewConversationScreen
import com.zam.photos.app.ui.screens.NotificationsScreen
import com.zam.photos.app.ui.screens.PostDetailScreen
import com.zam.photos.app.ui.screens.PendingApprovalScreen
import com.zam.photos.app.ui.screens.PrivacyPolicyScreen
import com.zam.photos.app.ui.screens.ProfileScreen
import com.zam.photos.app.ui.screens.SettingsScreen
import com.zam.photos.app.viewmodel.InboxViewModel
import com.zam.photos.app.viewmodel.ProfileViewModel
import com.zam.photos.app.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.runtime.rememberCoroutineScope

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("auth/login")
    data object Feed : AppDestination("home")
    data object Messages : AppDestination("messages")
    data object Explore : AppDestination("explore")
    data object CreatePost : AppDestination("create")
    data object Comments : AppDestination("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    data object PostDetail : AppDestination("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    data object Profile : AppDestination("profile")
    data object PrivacyPolicy : AppDestination("privacy")
    data object Notifications : AppDestination("notifications")
    data object Settings : AppDestination("settings")
    data object Moderation : AppDestination("moderation")
    data object Chat : AppDestination("chat/{conversationId}") {
        fun createRoute(id: String) = "chat/$id"
    }
    data object NewConversation : AppDestination("messages/new")
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    inboxViewModel: InboxViewModel = koinViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    val inboxState by inboxViewModel.state.collectAsState()
    val profileUser = profileState.user
    val context = LocalContext.current
    val pushTokenManager: PushTokenManager = koinInject()
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { pushTokenManager.registerToken() }
        }
    }

    LaunchedEffect(isLoggedIn, profileUser?.isApproved) {
        if (isLoggedIn == true && profileUser?.isApproved == true) {
            when {
                pushTokenManager.canPostNotifications() -> pushTokenManager.registerToken()
                Build.VERSION.SDK_INT >= 33 -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> pushTokenManager.registerToken()
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) {
            profileViewModel.loadProfile()
            if (currentRoute in listOf(AppDestination.Login.route, null)) {
                navController.navigate(AppDestination.Feed.route) {
                    popUpTo(AppDestination.Login.route) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(isLoggedIn, profileUser?.approvalStatus) {
        if (isLoggedIn == true && profileUser?.isApproved == true) {
            inboxViewModel.refresh()
            val data = (context as? android.app.Activity)?.intent?.data
            val postId = when {
                data?.scheme == "familyspace" && data.host == "post" -> data.pathSegments.firstOrNull()
                data?.pathSegments?.let { it.size >= 2 && it[0] == "post" } == true -> data.pathSegments[1]
                else -> null
            }
            if (postId != null) {
                navController.navigate(AppDestination.PostDetail.createRoute(postId))
            }
        }
    }

    if (isLoggedIn == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (isLoggedIn == true) {
        if (profileState.isLoading && profileUser == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        val gatedUser = profileUser
        if (gatedUser != null && !gatedUser.isApproved && !ModerationAccess.isModerator(gatedUser.email)) {
            PendingApprovalScreen(
                isRejected = gatedUser.isRejected,
                onRefresh = { profileViewModel.loadProfile() },
                onLogout = {
                    profileViewModel.logout(context) {
                        sessionViewModel.refresh()
                    }
                }
            )
            return
        }
    }

    val isAuthScreen = currentRoute == AppDestination.Login.route
    val homeRoutes = listOf(
        AppDestination.Feed.route,
        AppDestination.Messages.route,
        AppDestination.Explore.route,
        AppDestination.CreatePost.route,
        AppDestination.Profile.route
    )
    val hideBottomBar = isAuthScreen ||
        currentRoute == AppDestination.PrivacyPolicy.route ||
        currentRoute == AppDestination.Notifications.route ||
        currentRoute == AppDestination.Settings.route ||
        currentRoute == AppDestination.Moderation.route ||
        currentRoute == AppDestination.NewConversation.route ||
        currentRoute?.startsWith("comments/") == true ||
        currentRoute?.startsWith("post/") == true ||
        currentRoute?.startsWith("chat/") == true

    val isModerator = ModerationAccess.isModerator(profileUser?.email)

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppDestination.Feed.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!hideBottomBar && currentRoute in homeRoutes) {
                FamilyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { navigateToTab(it) },
                    profileName = profileUser?.name,
                    profileImageUrl = profileUser?.profileImageUrl?.takeIf { it.isNotBlank() },
                    unreadMessages = inboxState.conversations.sumOf { it.unreadCount }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn == true) AppDestination.Feed.route else AppDestination.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AppDestination.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        AuthDebugLog.log("Nav: onLoginSuccess — markLoggedIn + navigate feed")
                        sessionViewModel.markLoggedIn()
                        navController.navigate(AppDestination.Feed.route) {
                            popUpTo(AppDestination.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestination.Feed.route) {
                FeedScreen(
                    onOpenComments = { postId -> navController.navigate(AppDestination.Comments.createRoute(postId)) },
                    onOpenPost = { postId -> navController.navigate(AppDestination.PostDetail.createRoute(postId)) },
                    onOpenProfile = { navigateToTab(AppDestination.Profile.route) },
                    onOpenNotifications = { navController.navigate(AppDestination.Notifications.route) }
                )
            }
            composable(AppDestination.Messages.route) {
                MessagesInboxScreen(
                    onOpenConversation = { id -> navController.navigate(AppDestination.Chat.createRoute(id)) },
                    onNewConversation = { navController.navigate(AppDestination.NewConversation.route) }
                )
            }
            composable(AppDestination.Explore.route) {
                ExploreScreen(onOpenPost = { postId -> navController.navigate(AppDestination.PostDetail.createRoute(postId)) })
            }
            composable(AppDestination.CreatePost.route) {
                CreatePostScreen(
                    onPostCreated = { navController.navigate(AppDestination.Feed.route) },
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(
                route = AppDestination.Comments.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStack ->
                val postId = backStack.arguments?.getString("postId") ?: return@composable
                CommentsScreen(postId = postId, onBack = { navController.popBackStack() })
            }
            composable(
                route = AppDestination.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "familyspace://post/{postId}" },
                    navDeepLink { uriPattern = "https://famillyspace.duckdns.org/post/{postId}" }
                )
            ) { backStack ->
                val postId = backStack.arguments?.getString("postId") ?: return@composable
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onOpenComments = { id -> navController.navigate(AppDestination.Comments.createRoute(id)) }
                )
            }
            composable(AppDestination.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        sessionViewModel.refresh()
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenSettings = { navController.navigate(AppDestination.Settings.route) }
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPrivacyPolicy = { navController.navigate(AppDestination.PrivacyPolicy.route) },
                    showModeration = isModerator,
                    onOpenModeration = { navController.navigate(AppDestination.Moderation.route) }
                )
            }
            composable(AppDestination.Moderation.route) {
                if (!isModerator) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    ModerationScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPost = { postId ->
                            navController.navigate(AppDestination.PostDetail.createRoute(postId))
                        }
                    )
                }
            }
            composable(AppDestination.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { postId ->
                        navController.popBackStack()
                        navController.navigate(AppDestination.PostDetail.createRoute(postId))
                    },
                    onOpenConversation = { id ->
                        navController.popBackStack()
                        navController.navigate(AppDestination.Chat.createRoute(id))
                    }
                )
            }
            composable(
                route = AppDestination.Chat.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStack ->
                val id = backStack.arguments?.getString("conversationId") ?: return@composable
                ChatThreadScreen(conversationId = id, onBack = { navController.popBackStack() })
            }
            composable(AppDestination.NewConversation.route) {
                NewConversationScreen(
                    onBack = { navController.popBackStack() },
                    onOpened = { id ->
                        navController.popBackStack()
                        navController.navigate(AppDestination.Chat.createRoute(id))
                    }
                )
            }
        }
    }
}
