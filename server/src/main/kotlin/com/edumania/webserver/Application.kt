package com.edumania.webserver

import com.edumania.webserver.db.Database.classes
import com.edumania.webserver.db.Database.courses
import com.edumania.webserver.db.Database.initDB
import com.edumania.webserver.db.Database.users
import com.edumania.webserver.db.collection.CourseClass
import com.edumania.webserver.db.collection.User
import com.edumania.webserver.web.UserSession
import com.edumania.webserver.web.form.ActionForm
import com.edumania.webserver.web.form.ActionForm.Action.*
import com.edumania.webserver.web.form.AddUserToClassForm
import com.edumania.webserver.web.form.NewCourseForm
import com.edumania.webserver.web.form.LoginForm
import com.edumania.webserver.web.form.NewClassForm
import com.edumania.webserver.web.form.NewTaskForm
import com.edumania.webserver.web.form.NewUserForm
import com.edumania.webserver.web.form.SignupForm
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAmount
import java.util.Date
import kotlin.onFailure
import kotlin.time.ExperimentalTime


@OptIn(DelicateCoroutinesApi::class, ExperimentalTime::class)
fun main() {
    GlobalScope.launch {
        initDB()
    }
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@ExperimentalTime
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
            call.respond(ThymeleafContent("index", mapOf("nome" to "Hello World")))
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
                    User.UserKind.TEACHER -> call.respondRedirect("/teacher/panel/0/")
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
                            if (BCrypt.checkpw(form.password, user.passwordHash)) {
                                call.sessions.set(user.generateSession())
                                call.respondRedirect("/login")
                            }
                            else call.respond(ThymeleafContent("login", mapOf("error" to "invalid_password")))
                        }
                    }
                }
                .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
        }

        get("/logout") {
            val session = call.sessions.get<UserSession>()
            session?.let { users.updateOne("publicId" eq it.publicId, Updates.pull("sessionTokens", it.hash)) }
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }

        get("/student/dashboard") {
            withSession(User.UserKind.STUDENT) { user ->
                call.respond(ThymeleafContent("student-dashboard",
                    mapOf(
                        "user" to user,
                        "pendingTasks" to (classes.query(
                            ("membersPublicIds" eq user.publicId)
                        ).getOrNull()?.toList()?.flatMap { it.tasks }
                            ?.filter { LocalDate.parse(it.dueDate) >= LocalDate.now() } ?: listOf())
                    )
                ))
            }
        }


        get("/library") {
            withSession(User.UserKind.STUDENT) { user ->
                call.respond(ThymeleafContent("library",
                    mapOf(
                        "user" to user,
                        "resources" to (classes.query(
                            ("membersPublicIds" eq user.publicId)
                        ).getOrNull()?.toList()?.flatMap { a -> a.resources.map { a.publicId to it } } ?: listOf())
                    )
                ))
            }
        }


        get("/teacher/panel/{classPublicId}/") {
            withSession(User.UserKind.TEACHER) { user ->
                call.parameters["classPublicId"]
                    ?.toLongOrNull()
                    ?.let { classes.query(("publicId" eq it) and ("membersPublicIds" eq user.publicId)) }
                    ?.onSuccess { flow ->
                        flow.firstOrNull()
                            ?.let { cls ->
                                call.respond(
                                    ThymeleafContent(
                                        "teacher-panel",
                                        mapOf(
                                            "classId" to cls.publicId,
                                            "code" to cls.code,
                                            "otherClasses" to (
                                                classes.query(Filters.not("publicId" eq cls.publicId))
                                                    .getOrNull()?.toList()?.map { it.toEntry() } ?: listOf()
                                            ),
                                            "user" to user,
                                            "tasks" to cls.tasks,
                                            "resources" to cls.resources
                                        )
                                    )
                                )
                            }
                            ?: run {
                                classes.query("membersPublicIds" eq user.publicId)
                                    .onSuccess { fl ->
                                        fl.firstOrNull()?.let {
                                            call.respondRedirect("/teacher/panel/${it.publicId}/")
                                        } ?: run {
                                            call.respondRedirect("/")
                                        }
                                    }
                                    .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
                            }
                    }
                    ?.onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
            }
        }

        post("/teacher/panel/{classPublicId}/new_task") {
            withSession(User.UserKind.TEACHER) { user ->
                (call.parameters["classPublicId"]?.toLongOrNull())?.let {
                    classes.updateOne(
                        "publicId" eq it,
                        Updates.addToSet("tasks", call.formReceive<NewTaskForm>().createTask(user.publicId))
                    )
                    call.respondRedirect("/teacher/panel/${it}/")
                }
            }
        }

        post("/teacher/panel/{classPublicId}/task") {
            withSession(User.UserKind.TEACHER) { user ->
                (call.parameters["classPublicId"]?.toLongOrNull())?.let {
                    val form = call.formReceive<ActionForm>()

                    when(form.action) {
                        EDIT -> call.respondRedirect("/teacher/panel/${it}/")
                        DELETE -> {
                            classes.updateOne("publicId" eq it, Updates.pull("tasks", "id" eq form.publicId))
                            call.respondRedirect("/teacher/panel/${it}/")
                        }
                    }
                }
            }
        }

        post("/teacher/panel/{classPublicId}/new_material") {
            withSession(User.UserKind.TEACHER) { user ->
                (call.parameters["classPublicId"]?.toLongOrNull())?.let { classId ->
                    var title = ""
                    var description = ""
                    var fileName = ""
                    var fileBytes = byteArrayOf()

                    call.receiveMultipart().forEachPart {
                        when(it) {
                            is PartData.FormItem ->
                                when(it.name) {
                                    "title" -> title = it.value
                                    "description" -> description = it.value
                                }
                            is PartData.FileItem -> {
                                fileName = it.originalFileName ?: "file"
                                fileBytes = it.provider().toByteArray()
                            }
                            else -> error("")
                        }
                    }

                    classes.updateOne(
                        "publicId" eq classId,
                        Updates.addToSet("resources", CourseClass.Resource(
                            title, description, user.publicId, fileName, fileBytes
                        ))
                    )

                    call.respondRedirect("/teacher/panel/${classId}/")
                }
            }
        }

        post("/teacher/panel/{classPublicId}/resource") {
            withSession(User.UserKind.TEACHER) { user ->
                (call.parameters["classPublicId"]?.toLongOrNull())?.let {
                    val form = call.formReceive<ActionForm>()

                    when(form.action) {
                        EDIT -> call.respondRedirect("/teacher/panel/${it}/")
                        DELETE -> {
                            classes.updateOne("publicId" eq it, Updates.pull("resources", "id" eq form.publicId))
                            call.respondRedirect("/teacher/panel/${it}/")
                        }
                    }
                }
            }
        }

        get("/director/panel") {
            withSession(User.UserKind.DIRECTOR) { user ->
                val coursesList = courses.query("entryUserAuthorId" eq user.publicId).getOrNull()?.toList() ?: listOf()

                call.respond(
                    ThymeleafContent(
                        "director-panel",
                        mapOf(
                            "pwd" to call.queryParameters["pwd"],
                            "user" to user,
                            "courses" to coursesList,
                            "classes" to (
                                classes.query(Filters.`in`("coursePublicId", coursesList.map { it.publicId }))
                                    .getOrNull()?.toList()?.map { it.toEntry() } ?: listOf()
                            ),
                            "teachers" to (
                                users.query("kind" eq User.UserKind.TEACHER).getOrNull()?.toList() ?: listOf()
                            ),
                            "students" to (
                                users.query("kind" eq User.UserKind.STUDENT).getOrNull()?.toList() ?: listOf()
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
                    EDIT -> call.respondRedirect("/director/panel#courses")
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
                    EDIT -> call.respondRedirect("/director/panel#classes")
                    DELETE -> {
                        classes.deleteOne("publicId" eq form.publicId)
                        call.respondRedirect("/director/panel#classes")
                    }
                }
            }
        }

        post("/director/panel/new_teacher") {
            withSession(User.UserKind.DIRECTOR) { user ->
                val form = call.formReceive<NewUserForm>()

                users.query("email" eq form.email)
                    .onSuccess { flow ->
                        if (flow.firstOrNull() == null) {
                            val (pwd, user) = form.createPasswordAndUser(User.UserKind.TEACHER)

                            users.add(user).onSuccess {
                                call.respondRedirect("/director/panel?pwd=${pwd}")
                            }.onFailure {
                                call.respond(HttpStatusCode.InternalServerError, it.message ?: "")
                            }

                        } else call.respond(
                            ThymeleafContent("/director/panel#classes", mapOf("error" to "email_in_use"))
                        )
                    }
                    .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
            }
        }

        post("/director/panel/teacher") {
            withSession(User.UserKind.DIRECTOR)  { user ->
                val form = call.formReceive<ActionForm>()

                when (form.action) {
                    EDIT -> call.respondRedirect("/director/panel#teachers")
                    DELETE -> {
                        users.deleteOne("publicId" eq form.publicId)
                        call.respondRedirect("/director/panel#teachers")
                    }
                }
            }
        }

        post("/director/panel/new_student") {
            withSession(User.UserKind.DIRECTOR) { user ->
                val form = call.formReceive<NewUserForm>()

                users.query("email" eq form.email)
                    .onSuccess { flow ->
                        if (flow.firstOrNull() == null) {
                            val (pwd, user) = form.createPasswordAndUser(User.UserKind.STUDENT)

                            users.add(user).onSuccess {
                                call.respondRedirect("/director/panel#sudent?pwd=${pwd}")
                            }.onFailure {
                                call.respond(HttpStatusCode.InternalServerError, it.message ?: "")
                            }

                        } else call.respond(
                            ThymeleafContent("/director/panel#classes", mapOf("error" to "email_in_use"))
                        )
                    }
                    .onFailure { call.respond(HttpStatusCode.InternalServerError, it.message ?: "") }
            }
        }

        post("/director/panel/student") {
            withSession(User.UserKind.DIRECTOR)  { user ->
                val form = call.formReceive<ActionForm>()

                when (form.action) {
                    EDIT -> call.respondRedirect("/director/panel#student")
                    DELETE -> {
                        users.deleteOne("publicId" eq form.publicId)
                        call.respondRedirect("/director/panel#student")
                    }
                }
            }
        }

        post("/director/panel/add_user") {
            withSession(User.UserKind.DIRECTOR)  { user ->
                val form = call.formReceive<AddUserToClassForm>()

                classes.updateOne(
                    "publicId" eq form.classPublicId,
                    Updates.addToSet("membersPublicIds", form.userPublicId)
                )
                call.respondRedirect("/director/panel#class")
            }
        }

        get("/resource/{classId}/{queryId}") {
            val classId = call.parameters["classId"]?.toLongOrNull()
            val queryId = call.parameters["queryId"]?.toLongOrNull()

            val resource =
                classes.query("publicId" eq classId).getOrNull()?.firstOrNull()?.resources?.find { it.id == queryId }

            if (resource != null) {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        resource.fileName
                    ).toString()
                )

                call.respondBytes(resource.file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}