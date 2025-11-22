package com.edumania.webserver.db.collection

import com.edumania.webserver.db.Database.courses
import com.edumania.webserver.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class CourseClass(
    var code: String,
    val coursePublicId: Long,
    var membersPublicIds: List<Long> = listOf(),
    var tasks: List<Task> = listOf(),
    var resources: List<Resource> = listOf(),
    val publicId: Long = Random.nextLong()
) {

    @Serializable
    data class Task(
        val title: String,
        val description: String,
        val creator: ULong,
        val dueDate: String,
        val weight: Float,
        val notes: Pair<ULong, Float>
    )

    @Serializable
    data class Resource(
        val title: String,
        val description: String,
        val creator: ULong,
        val files: List<ByteArray>,
    )

    suspend fun toEntry() =
        ClassEntry(
            code,
            courses.query("publicId" eq coursePublicId).getOrNull()?.firstOrNull()?.name ?: "",
            membersPublicIds.size
        )

    data class ClassEntry(val code: String, val courseName: String, val studentAmount: Int)
}