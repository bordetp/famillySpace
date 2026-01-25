package com.zam.photos.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zam.photos.app.ui.screens.CommentsScreen
import com.zam.photos.app.ui.screens.CreatePostScreen
import com.zam.photos.app.ui.screens.ExploreScreen
import com.zam.photos.app.ui.screens.FeedScreen
import com.zam.photos.app.ui.screens.LoginScreen
import com.zam.photos.app.ui.screens.ProfileScreen
import com.zam.photos.app.ui.screens.RegisterScreen

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("auth/login")
    data object Register : AppDestination("auth/register")
    data object Feed : AppDestination("home")
    data object Explore : AppDestination("explore")
    data object CreatePost : AppDestination("create")
    data object Comments : AppDestination("comments")
    data object Profile : AppDestination("profile")
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAuthScreen = currentRoute in listOf(AppDestination.Login.route, AppDestination.Register.route)

    Scaffold(
        bottomBar = {
            if (!isAuthScreen && currentRoute != null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == AppDestination.Feed.route,
                        onClick = {
                            navController.navigate(AppDestination.Feed.route) {
                                popUpTo(AppDestination.Feed.route) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == AppDestination.Explore.route,
                        onClick = {
                            navController.navigate(AppDestination.Explore.route) {
                                popUpTo(AppDestination.Explore.route) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Explore") },
                        label = { Text("Explore") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == AppDestination.CreatePost.route,
                        onClick = { navController.navigate(AppDestination.CreatePost.route) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = "Create") },
                        label = { Text("Create") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == AppDestination.Profile.route,
                        onClick = {
                            navController.navigate(AppDestination.Profile.route) {
                                popUpTo(AppDestination.Profile.route) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AppDestination.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(AppDestination.Feed.route) },
                    onNavigateToRegister = { navController.navigate(AppDestination.Register.route) }
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
                    onOpenComments = { navController.navigate(AppDestination.Comments.route) }
                )
            }
            composable(AppDestination.Explore.route) {
                ExploreScreen()
            }
            composable(AppDestination.CreatePost.route) {
                CreatePostScreen(
                    onPostCreated = { navController.navigate(AppDestination.Feed.route) }
                )
            }
            composable(AppDestination.Comments.route) {
                CommentsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
