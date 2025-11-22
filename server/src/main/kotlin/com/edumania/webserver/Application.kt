package com.edumania.webserver

import com.edumania.webserver.db.Database.classes
import com.edumania.webserver.db.Database.courses
import com.edumania.webserver.db.Database.initDB
import com.edumania.webserver.db.Database.users
import com.edumania.webserver.db.collection.User
import com.edumania.webserver.web.UserSession
import com.edumania.webserver.web.form.ActionForm
import com.edumania.webserver.web.form.ActionForm.Action.*
import com.edumania.webserver.web.form.NewCourseForm
import com.edumania.webserver.web.form.LoginForm
import com.edumania.webserver.web.form.NewClassForm
import com.edumania.webserver.web.form.SignupForm
import com.mongodb.client.model.Filters
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlin.onFailure


@OptIn(DelicateCoroutinesApi::class)
fun main() {
    GlobalScope.launch {
        initDB()
    }
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

suspend fun Application.module() {

    install(Sessions) {
        cookie<UserSession>("SESSION") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(ContentNegotiation) {
        gson {}
        Json {
            ignoreUnknownKeys = true
        }
    }

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }



    routing {
        staticResources("/static", "static")

        get("/") {
            call.respond(ThymeleafContent("edu", mapOf("nome" to "Hello World")))
        }

        get("/signup") {
            withSession(
                { call.respondRedirect("/student/dashboard") },
                { call.respond(ThymeleafContent("signup", mapOf())) }
            )
        }

        post("/signup") {
            val form = call.formReceive<SignupForm>()

            users.query("email" eq form.email)
                .onSuccess { flow ->
                    if (flow.firstOrNull() == null) {
                        val (user, session) = form.createUser()

                        users.add(user).onSuccess {
                            call.sessions.set(session)
                            call.respondRedirect("/student/dashboard")
                        }.onFailure {
                            call.respond(HttpStatusCode.InternalServerError, it.message ?: "")
                        }

                    } else call.respond(ThymeleafContent("signup", mapOf("error" to "email_in_use")))
                }
                .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
        }

        get("/login") {
            withSession(
                { when(it.kind) {
                    User.UserKind.DIRECTOR -> call.respondRedirect("/director/panel")
                    User.UserKind.TEACHER -> call.respondRedirect("/teacher/teacher")
                    User.UserKind.STUDENT -> call.respondRedirect("/student/dashboard")
                } },
                { call.respond(ThymeleafContent("login", mapOf())) }
            )
        }

        post("/login") {
            val form = call.formReceive<LoginForm>()

            users.query("email" eq form.email)
                .onSuccess {
                    when (val user = it.firstOrNull()) {
                        null -> call.respond(ThymeleafContent("login", mapOf("error" to "invalid_login")))
                        else -> {
                            if (passwordHash(form.password) == user.passwordHash) {
                                call.sessions.set(user.generateSession())
                                call.respondRedirect("/student/dashboard")
                            }
                            else call.respond(ThymeleafContent("login", mapOf("error" to "invalid_password")))
                        }
                    }
                }
                .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }

        get("/student/dashboard") {
            withSession(User.UserKind.STUDENT) {
                call.respond(ThymeleafContent("student-dashboard", mapOf("user" to it)))
            }
        }

        get("/teacher/panel") {
            withSession(User.UserKind.TEACHER) {
                call.respond(ThymeleafContent("teacher-panel", mapOf("user" to it)))
            }
        }

        get("/director/panel") {
            withSession(User.UserKind.DIRECTOR) { user ->
                val coursesList = courses.query("entryUserAuthorId" eq user.publicId).getOrNull()?.toList() ?: listOf()

                call.respond(
                    ThymeleafContent(
                        "director-panel",
                        mapOf(
                            "user" to user,
                            "courses" to coursesList,
                            "classes" to (
                                classes.query(Filters.`in`("coursePublicId", coursesList.map { it.publicId }))
                                    .getOrNull()?.toList() ?: listOf()
                            )
                        )
                    )
                )
            }
        }

        post("/director/panel/new_course") {
            withSession(User.UserKind.DIRECTOR) { user ->
                val form = call.formReceive<NewCourseForm>()

                courses.query("code" eq form.code)
                    .onSuccess { flow ->
                        if (flow.firstOrNull() == null) {
                            val course = form.createCourse(user.publicId)

                            courses.add(course).onSuccess {
                                call.respondRedirect("/director/panel#courses")
                            }.onFailure {
                                call.respond(HttpStatusCode.InternalServerError, it.message ?: "")
                            }

                        } else call.respond(
                            ThymeleafContent("/director/panel#courses", mapOf("error" to "code_in_use"))
                        )
                    }
                    .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
            }
        }

        post("/director/panel/course") {
            withSession(User.UserKind.DIRECTOR)  { user ->
                val form = call.formReceive<ActionForm>()

                when (form.action) {
                    EDIT -> call.respondRedirect("/director/panel")
                    DELETE -> {
                        courses.deleteOne("publicId" eq form.publicId)
                        classes.deleteMany("coursePublicId" eq form.publicId)
                        call.respondRedirect("/director/panel")
                    }
                }
            }
        }

        post("/director/panel/new_class") {
            withSession(User.UserKind.DIRECTOR) { user ->
                val form = call.formReceive<NewClassForm>()

                classes.query("code" eq form.code)
                    .onSuccess { flow ->
                        if (flow.firstOrNull() == null) {
                            val course = form.createClass()

                            classes.add(course).onSuccess {
                                call.respondRedirect("/director/panel#classes")
                            }.onFailure {
                                call.respond(HttpStatusCode.InternalServerError, it.message ?: "")
                            }

                        } else call.respond(
                            ThymeleafContent("/director/panel#classes", mapOf("error" to "code_in_use"))
                        )
                    }
                    .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
            }
        }

        post("/director/panel/class") {
            withSession(User.UserKind.DIRECTOR)  { user ->
                val form = call.formReceive<ActionForm>()

                when (form.action) {
                    EDIT -> call.respondRedirect("/director/panel")
                    DELETE -> {
                        courses.deleteOne("publicId" eq form.publicId)
                        classes.deleteMany("coursePublicId" eq form.publicId)
                        call.respondRedirect("/director/panel")
                    }
                }
            }
        }
    }
}