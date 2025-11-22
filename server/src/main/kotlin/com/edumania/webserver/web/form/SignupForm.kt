package com.edumania.webserver.web.form

import com.edumania.webserver.db.collection.User
import com.edumania.webserver.passwordHash
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class SignupForm(val firstName: String, val lastName: String, val email: String, val password: String) {

    fun createUser() =
        User(
            email,
            passwordHash(password),
            firstName,
            lastName,
            User.UserKind.DIRECTOR
        ).withNewSession()

}
