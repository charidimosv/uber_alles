package com.team.eddie.uber_alles.utils.firebase

class UserInfo(
        var userId: String? = null,

        var email: String? = null,
        var isDriver: Boolean? = null,
        var name: String? = null,
        var password: String? = null,
        var phone: String = "",
        var username: String? = null,

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