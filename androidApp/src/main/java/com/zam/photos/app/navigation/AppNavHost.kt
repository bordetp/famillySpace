package com.zam.photos.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zam.photos.app.ui.screens.CommentsScreen
import com.zam.photos.app.ui.screens.CreatePostScreen
import com.zam.photos.app.ui.screens.FeedScreen
import com.zam.photos.app.ui.screens.LoginScreen
import com.zam.photos.app.ui.screens.ProfileScreen
import com.zam.photos.app.ui.screens.RegisterScreen

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("auth/login")
    data object Register : AppDestination("auth/register")
    data object Feed : AppDestination("feed")
    data object CreatePost : AppDestination("posts/create")
    data object Comments : AppDestination("posts/comments")
    data object Profile : AppDestination("profile")
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Login.route
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(AppDestination.Feed.route) }
            )
        }
        composable(AppDestination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(AppDestination.Feed.route) },
                onNavigateToLogin = { navController.popBackStack(AppDestination.Login.route, inclusive = false) }
            )
        }
        composable(AppDestination.Feed.route) {
            FeedScreen(
                onCreatePost = { navController.navigate(AppDestination.CreatePost.route) },
                onOpenComments = { navController.navigate(AppDestination.Comments.route) },
                onOpenProfile = { navController.navigate(AppDestination.Profile.route) }
            )
        }
        composable(AppDestination.CreatePost.route) {
            CreatePostScreen(
                onPostCreated = { navController.popBackStack(AppDestination.Feed.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(AppDestination.Comments.route) {
            CommentsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppDestination.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
