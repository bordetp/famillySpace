package com.zam.photos.app

object ModerationAccess {
    const val ADMIN_EMAIL = "deceirem@gmail.com"

    fun isModerator(email: String?): Boolean =
        email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true
}
