package com.edumania.webserver.web.form

import com.edumania.webserver.db.collection.Course
import kotlinx.serialization.Serializable

@Serializable
data class NewCourseForm(val name: String, val code: String, val workload: Int, val description: String) {

    fun createCourse(authorId: Long) = Course(name, code, workload, description, authorId)
}