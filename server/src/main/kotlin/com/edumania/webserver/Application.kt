@file:OptIn(ExperimentalUuidApi::class)

package com.edumania.webserver

import com.edumania.webserver.db.Database.installMongoDB
import com.edumania.webserver.db.Repository
import com.edumania.webserver.db.SessionToken
import com.edumania.webserver.db.User
import com.mongodb.client.model.Filters
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import io.ktor.util.*
import kotlinx.coroutines.flow.firstOrNull
import org.koin.ktor.ext.inject
import org.mindrot.jbcrypt.BCrypt
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlin.uuid.ExperimentalUuidApi

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

suspend fun Application.module() {

    install(Sessions) {
        cookie<String>("USER_TOKEN") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(ContentNegotiation) {
        gson {}
    }
    installMongoDB()

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }

    routing {
        val users by inject<Repository<User>>()

        get("/") {
            call.respond(ThymeleafContent("index", mapOf("nome" to "Hello World")))
        }

        get("/new_user") {
            call.respond(ThymeleafContent("new_user", call.request.queryParameters.toMap()))
        }

        post("/user/new") {
            val userRequest = call.receive<User>()

            users.query(Filters.eq("email", userRequest.email)).onSuccess { mails ->
                if (mails.firstOrNull() == null) {
                    userRequest.passwordHash = BCrypt.hashpw(userRequest.passwordHash, BCrypt.gensalt(12))

                    SessionToken()

                    val newUser = users.add(userRequest)

                    newUser.onSuccess {
                        
                    }
                } else {
                    call.respondRedirect("/new_user?error=email_in_use")
                }
            }.onFailure {
                call.respondText(it.message ?: "", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}