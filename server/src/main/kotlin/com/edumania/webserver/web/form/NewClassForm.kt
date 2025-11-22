package com.edumania.webserver.web.form

import com.edumania.webserver.db.collection.CourseClass
import kotlinx.serialization.Serializable

@Serializable
data class NewClassForm(val code: String, val coursePublicId: Long) {

    fun createClass() = CourseClass(code, coursePublicId)
}