package com.edumania.webserver.db.collection

import com.edumania.webserver.db.Database.users
import com.edumania.webserver.db.Repository
import com.edumania.webserver.eq
import com.edumania.webserver.web.UserSession
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextULong

@Serializable
data class User(
    var email: String,
    var passwordHash: String,
    var firstName: String,
    var lastName: String,
    val kind: UserKind,
    val publicId: Long = Random.nextLong(),
    var sessionTokens: List<String> = listOf(),
    val resetPasswordOnNextLogin: Boolean = false
) {
    enum class UserKind {
        DIRECTOR, TEACHER, STUDENT
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return publicId == other.publicId
    }

    suspend fun generateSessionToken() = UUID.randomUUID().toString().also {
        sessionTokens += it
        users.updateOne("publicId" eq publicId, Updates.addToSet("sessionTokens", it))
    }
    suspend fun generateSession() = UserSession(publicId, generateSessionToken())

    suspend fun withNewSession() = this to generateSession()

    override fun hashCode() = email.hashCode()

    class Repo(collection: MongoCollection<User>) : Repository<User>(collection)
}