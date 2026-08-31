package com.zam.photos.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zam.photos.app.R
import com.zam.photos.app.ui.theme.BorderLight
import com.zam.photos.app.ui.theme.Terracotta

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isFab: Boolean = false
)

@Composable
fun FamilyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    profileName: String? = null,
    profileImageUrl: String? = null,
    unreadMessages: Int = 0
) {
    val items = listOf(
        BottomNavItem("home", stringResource(R.string.nav_home), Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("messages", stringResource(R.string.nav_messages), Icons.Outlined.ChatBubbleOutline, Icons.Outlined.ChatBubbleOutline),
        BottomNavItem("create", stringResource(R.string.nav_create), Icons.Filled.Add, Icons.Filled.Add, isFab = true),
        BottomNavItem("explore", stringResource(R.string.nav_grid), Icons.Outlined.GridView, Icons.Outlined.GridView),
        BottomNavItem("profile", stringResource(R.string.nav_profile), Icons.Filled.Person, Icons.Outlined.Person)
    )

    Column {
        HorizontalDivider(color = BorderLight, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                if (item.isFab) {
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            FloatingActionButton(
                                onClick = { onNavigate(item.route) },
                                shape = CircleShape,
                                containerColor = Terracotta,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = item.label, modifier = Modifier.size(18.dp))
                            }
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.surface)
                    )
                } else {
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            if (item.route == "profile" && !profileName.isNullOrBlank()) {
                                Avatar(
                                    name = profileName,
                                    imageUrl = profileImageUrl,
                                    size = 24.dp
                                )
                            } else if (item.route == "messages" && unreadMessages > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = Terracotta) {
                                        Text(
                                            if (unreadMessages > 9) "9+" else unreadMessages.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }) {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            } else {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Terracotta,
                            selectedTextColor = Terracotta,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FamilySpaceFeedHeader(
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    userName: String?,
    userImageUrl: String?,
    unreadNotificationCount: Int = 0
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(containerColor = Terracotta) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onNotificationsClick),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (userName != null) {
                    Avatar(
                        name = userName,
                        imageUrl = userImageUrl,
                        size = 30.dp,
                        modifier = Modifier.clickable(onClick = onProfileClick)
                    )
                }
            }
        }
        HorizontalDivider(color = BorderLight)
    }
}
