package com.team.eddie.uber_alles.utils.firebase

class UserInfo(
        var userId: String = "",

        var email: String = "",
        var username: String = "",
        var password: String = "",

        var name: String = "",
        var phone: String = "",

        var driver: String = "",

        var imageUrl: String? = null
) {

    fun toHashTable(): HashMap<String, Any?> {
        return hashMapOf(
                FirebaseHelper.EMAIL to email,
                FirebaseHelper.USERNAME to username,
                FirebaseHelper.PASSWORD to password,
                FirebaseHelper.NAME to name,
                FirebaseHelper.PHONE to phone,
                FirebaseHelper.IMG_URL to imageUrl,
                FirebaseHelper.IS_DRIVER to driver)
    }
}