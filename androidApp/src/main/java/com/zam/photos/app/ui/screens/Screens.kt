package com.zam.photos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onLoginSuccess) {
            Text("Login")
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRegisterSuccess) {
            Text("Register")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToLogin) { Text("Back to login") }
    }
}

@Composable
fun FeedScreen(onCreatePost: () -> Unit, onOpenComments: () -> Unit, onOpenProfile: () -> Unit) {
    val posts = listOf("Family picnic", "First steps", "Graduation day")

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onCreatePost, modifier = Modifier.fillMaxWidth()) {
            Text("Create post")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(posts) { post ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = post, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onOpenComments) { Text("Comments") }
                }
            }
        }
        Button(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Profile")
        }
    }
}

@Composable
fun CreatePostScreen(onPostCreated: () -> Unit, onCancel: () -> Unit) {
    var content by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Share an update") },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onPostCreated) { Text("Publish") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
fun CommentsScreen(onBack: () -> Unit) {
    val comments = listOf("Nice!", "Congrats", "Love this")

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(comments) { comment ->
                Text(text = comment, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "User profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}
