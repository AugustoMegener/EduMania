package com.edumania.webserver.db.collection

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Course(val name: String,
                  val code: String,
                  val workload: Int,
                  val description: String,
                  val entryUserAuthorId: Long,
                  val publicId: Long = Random.nextLong())