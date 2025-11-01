package com.edumania.webserver.db

import kotlinx.serialization.Serializable

@Serializable
data class User(
    /*@param:BsonId @Serializable(ObjectIdSerializer::class)
    val id: ObjectId? = null,*/
    val email: String,
    var passwordHash: String,
    var sessionTokens: Array<SessionToken>,
    var text: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return email == other.email
    }

    override fun hashCode() = email.hashCode()
}