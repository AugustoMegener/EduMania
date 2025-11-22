package com.edumania.webserver.web

import com.edumania.webserver.and
import com.edumania.webserver.db.Database.users
import com.edumania.webserver.eq
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserSession(val publicId: Long, val hash: String = UUID.randomUUID().toString()) {

    fun user() = users.query(("publicId" eq publicId) and ("sessionTokens" eq hash))

}