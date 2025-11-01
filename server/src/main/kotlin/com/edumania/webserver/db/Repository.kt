package com.edumania.webserver.db

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.conversions.Bson

class Repository<T : Any>(private val collection: MongoCollection<T>) {

    suspend fun add(value: T) = collection.runCatching { insertOne(value) }

    suspend fun add(value: List<T>) = collection.runCatching { insertMany(value.toList()) }

    suspend inline fun <reified C : T> add(vararg value: C) = add(value.toList())

    suspend inline operator fun <reified C : T> plusAssign(value: C) { add(value).getOrThrow() }
    suspend inline operator fun <reified C : T> plusAssign(value: List<C>) { add(value).getOrThrow() }

    suspend fun deleteOne(filter: Bson) = collection.runCatching { deleteOne(filter) }

    suspend fun deleteMany(filter: Bson) = collection.runCatching { deleteMany(filter) }

    fun query(filter: Bson) = collection.runCatching { find(filter) }

    companion object {
        inline fun <reified T : Any> create(db: MongoDatabase, name: String) =
            Repository(db.getCollection<T>(name))
    }
}