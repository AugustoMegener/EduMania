package com.edumania.webserver.db

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

object Database {

    suspend fun Application.installMongoDB() {
        install(Koin) {
            modules(
                module {
                    single {
                        MongoClient.create(
                            environment.config.propertyOrNull("ktor.mongo.uri")?.getString() ?:
                            throw RuntimeException("Failed to access MongoDB URI.")
                        )
                    }
                    single {
                        get<MongoClient>().getDatabase(environment.config.property("ktor.mongo.database").getString())
                    }
                },
                module {
                    single<Repository<User>> { Repository.create(get(), "users") }
                }
            )
        }


    }
}