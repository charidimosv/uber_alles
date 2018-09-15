package com.team.eddie.uber_alles.utils.firebase

class UserInfo(
        var userId: String? = "",

        var email: String? = "",
        var isDriver: Boolean? = false,
        var name: String? = "",
        var password: String? = "",
        var phone: String = "",
        var username: String? = "",

        var imageUrl: String? = null
) {

    fun toHashTable(): HashMap<String, Any?> {
        return hashMapOf(
                FirebaseHelper.EMAIL to email,
                FirebaseHelper.USERNAME to username,
                FirebaseHelper.PASSWORD to password,
                FirebaseHelper.PHONE to phone,
                FirebaseHelper.NAME to name,
                FirebaseHelper.IS_DRIVER to isDriver,
                FirebaseHelper.IMG_URL to imageUrl)
    }
}