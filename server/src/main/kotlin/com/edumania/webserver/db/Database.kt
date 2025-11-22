package com.edumania.webserver.db

import com.edumania.webserver.db.collection.CourseClass
import com.edumania.webserver.db.collection.Course
import com.edumania.webserver.db.collection.User
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase

object Database {

    lateinit var database: MongoDatabase

    val users by lazy { Repository(database.getCollection<User>("users")) }
    val courses by lazy { Repository(database.getCollection<Course>("courses"))  }
    val classes by lazy { Repository(database.getCollection<CourseClass>("classes"))  }

    fun initDB() {
        database = MongoClient.create(System.getenv("MONGODB_LOGIN")!!).getDatabase("edumania")


    }
}