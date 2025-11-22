package com.edumania.webserver.db

import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.conversions.Bson

open class Repository<T : Any>(private val collection: MongoCollection<T>) {

    suspend fun add(value: T) = collection.runCatching { insertOne(value) }

    suspend fun add(value: List<T>) = collection.runCatching { insertMany(value.toList()) }

    suspend inline fun <reified C : T> add(vararg value: C) = add(value.toList())

    suspend fun updateOne(filter: Bson, updates: Bson, options: UpdateOptions.() -> UpdateOptions = { this }) {
        collection.updateOne(filter, updates, UpdateOptions().options())
    }

    suspend inline operator fun <reified C : T> plusAssign(value: C) { add(value).getOrThrow() }
    suspend inline operator fun <reified C : T> plusAssign(value: List<C>) { add(value).getOrThrow() }

    suspend fun deleteOne(filter: Bson) = collection.runCatching { deleteOne(filter) }

    suspend fun deleteMany(filter: Bson) = collection.runCatching { deleteMany(filter) }

    fun query(filter: Bson) = collection.runCatching { find(filter) }
}