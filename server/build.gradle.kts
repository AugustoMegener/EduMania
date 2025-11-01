import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.edumania.webserver"
version = "1.0.0"
application {
    mainClass.set("com.edumania.webserver.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)

    implementation("io.ktor:ktor-server-thymeleaf")
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-swagger-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-gson-jvm")
    implementation("io.ktor:ktor-server-tomcat-jvm")

    implementation("io.ktor:ktor-server-sessions")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.10.1")
    implementation("io.insert-koin:koin-ktor:3.5.3")

    implementation("org.mindrot:jbcrypt:0.4")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(
        listOf(
            "-Xannotation-default-target=param-property"
        )
    )
}