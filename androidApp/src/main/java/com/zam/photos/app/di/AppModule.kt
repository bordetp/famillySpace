package com.zam.photos.app.di

import com.zam.photos.app.auth.GoogleSignInHelper
import com.zam.photos.app.data.api.createHttpClient
import com.zam.photos.app.data.local.TokenStore
import com.zam.photos.app.data.local.ThemeStore
import com.zam.photos.app.data.repository.AuthRepository
import com.zam.photos.app.data.repository.ChatRepository
import com.zam.photos.app.data.repository.DeviceRepository
import com.zam.photos.app.data.repository.FamilyRepository
import com.zam.photos.app.data.repository.NotificationRepository
import com.zam.photos.app.data.repository.PostRepository
import com.zam.photos.app.viewmodel.AuthViewModel
import com.zam.photos.app.viewmodel.ChatThreadViewModel
import com.zam.photos.app.viewmodel.CommentsViewModel
import com.zam.photos.app.viewmodel.CreatePostViewModel
import com.zam.photos.app.viewmodel.FeedViewModel
import com.zam.photos.app.viewmodel.InboxViewModel
import com.zam.photos.app.viewmodel.NewConversationViewModel
import com.zam.photos.app.viewmodel.NotificationsViewModel
import com.zam.photos.app.viewmodel.PostDetailViewModel
import com.zam.photos.app.viewmodel.ProfileViewModel
import com.zam.photos.app.viewmodel.SessionViewModel
import com.zam.photos.app.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { TokenStore(androidContext()) }
    single { ThemeStore(androidContext()) }
    single { GoogleSignInHelper() }
    single { createHttpClient(get()) }
    single { AuthRepository(get(), get(), get()) }
    single { PostRepository(get()) }
    single { NotificationRepository(get()) }
    single { ChatRepository(get()) }
    single { FamilyRepository(get()) }
    single { DeviceRepository(get()) }
    single { com.zam.photos.app.data.repository.AdminRepository(get()) }
    single { com.zam.photos.app.push.PushTokenManager(androidContext(), get()) }
    viewModel { SessionViewModel(get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { CreatePostViewModel(get()) }
    viewModel { (postId: String) -> CommentsViewModel(get(), postId) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { NotificationsViewModel(get()) }
    viewModel { InboxViewModel(get()) }
    viewModel { (conversationId: String) -> ChatThreadViewModel(get(), get(), get(), conversationId) }
    viewModel { NewConversationViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { (postId: String) -> PostDetailViewModel(get(), postId) }
    viewModel { com.zam.photos.app.viewmodel.ModerationViewModel(get()) }
}
