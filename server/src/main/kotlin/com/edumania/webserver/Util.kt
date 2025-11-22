package com.edumania.webserver

import com.edumania.webserver.db.collection.User
import com.edumania.webserver.web.UserSession
import com.mongodb.client.model.Filters
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.toMap
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.bson.conversions.Bson
import org.mindrot.jbcrypt.BCrypt.gensalt
import org.mindrot.jbcrypt.BCrypt.hashpw

infix fun <T> String.eq(value: T) = Filters.eq(this, value)
infix fun Bson.and(value: Bson) = Filters.and(this, value)
fun all(vararg values: Bson) = Filters.and(*values)

fun passwordHash(password: String): String = hashpw(password, gensalt(12))

suspend inline fun <reified T> RoutingCall.formReceive() =
    Json.decodeFromJsonElement<T>(
        Json.encodeToJsonElement(
            receiveParameters()
                .toMap()
                .mapValues { it.value.firstOrNull().orEmpty() }
        )
    )

suspend fun <T> RoutingContext.withSession(
    success: suspend (User) -> T,
    noUser: suspend () -> T,
    error: suspend (Throwable) -> T): T
{
    call.sessions.get<UserSession>()?.user()
        ?.onSuccess { f -> return f.firstOrNull()?.let { success(it) } ?: run { noUser() } }
        ?.onFailure { e -> return error(e) }
    return noUser()
}

suspend fun <T> RoutingContext.withSession(success: suspend (User) -> T, noUser: suspend () -> T) =
    withSession(
        success,
        noUser
    ) { call.respond(HttpStatusCode.InternalServerError, it.message ?: ""); null }

suspend fun <T> RoutingContext.withSession(success: suspend (User) -> T) =
    withSession(success) { call.respondRedirect("/login");null }

suspend fun <T> RoutingContext.withSession(kind: User.UserKind, success: suspend (User) -> T) =
    withSession({ if (it.kind == kind) success(it) else null }) {
        call.respondRedirect("/login"); null
    }