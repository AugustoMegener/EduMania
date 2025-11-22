package com.edumania.webserver.web.form

import com.edumania.webserver.db.collection.User
import com.edumania.webserver.passwordHash
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.nextULong

@Serializable
data class NewUserForm(val firstName: String, val lastName: String, val email: String) {

    fun createPasswordAndUser(kind: User.UserKind) = Random.nextULong().toString().let {
        it to User(email, passwordHash(it), firstName, lastName, kind, resetPasswordOnNextLogin = true)
    }
}