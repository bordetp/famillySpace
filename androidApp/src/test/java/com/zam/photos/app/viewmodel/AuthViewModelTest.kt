package com.zam.photos.app.viewmodel

import android.app.Activity
import android.content.Intent
import com.zam.photos.app.auth.GoogleSignInHelper
import com.zam.photos.app.auth.GoogleSignInResult
import com.zam.photos.app.data.repository.ApiResult
import com.zam.photos.app.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val authRepository: AuthRepository = mock()
    private val googleSignInHelper: GoogleSignInHelper = mock()
    private val activity: Activity = mock()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun googleSignInSuccessClearsError() = runTest {
        whenever(authRepository.signInWithGoogle("token")).thenReturn(ApiResult.Success(Unit))
        whenever(googleSignInHelper.parseSignInResult(eq(activity), any())).thenReturn(
            GoogleSignInResult.Success("token")
        )

        val viewModel = AuthViewModel(authRepository, googleSignInHelper)
        viewModel.completeGoogleSignIn(activity, Intent(), { })
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun googleSignInFailureSetsError() = runTest {
        whenever(authRepository.signInWithGoogle("token")).thenReturn(ApiResult.Error("Invalid Google token"))
        whenever(googleSignInHelper.parseSignInResult(eq(activity), any())).thenReturn(
            GoogleSignInResult.Success("token")
        )

        val viewModel = AuthViewModel(authRepository, googleSignInHelper)
        viewModel.completeGoogleSignIn(activity, Intent(), { })
        assertEquals("Invalid Google token", viewModel.state.value.error)
    }
}
