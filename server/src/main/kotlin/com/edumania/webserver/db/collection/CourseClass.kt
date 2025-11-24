package com.edumania.webserver.db.collection

import com.edumania.webserver.db.Database.courses
import com.edumania.webserver.eq
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.Random.Default.nextLong

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
        val creator: Long,
        val dueDate: String,
        val weight: Float,
        val notes: List<Pair<Long, Float>> = listOf(),
        val id: Long = nextLong()
    )

    @Serializable
    data class Resource(
        val title: String,
        val description: String,
        val creator: Long,
        val fileName: String,
        val file: ByteArray,
        val id: Long = nextLong()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Resource) return false

            return id == other.id
        }

        override fun hashCode(): Int {
            var result = creator.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + file.contentHashCode()
            return result
        }
    }

    suspend fun toEntry() =
        ClassEntry(
            code,
            courses.query("publicId" eq coursePublicId).getOrNull()?.firstOrNull()?.name ?: "",
            membersPublicIds.size,
            publicId
        )

    @Serializable
    data class ClassEntry(val code: String, val courseName: String, val studentAmount: Int, val publicId: Long)
}