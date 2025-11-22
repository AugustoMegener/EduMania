package com.edumania.webserver.web.form

import com.edumania.webserver.db.collection.CourseClass
import kotlinx.serialization.Serializable

@Serializable
data class NewTaskForm(val title: String,
                       val dueDate: String,
                       val description: String,
                       val weight: Float) {

    fun createTask(creator: Long) = CourseClass.Task(title, description, creator, dueDate, weight)
}