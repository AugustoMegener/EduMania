package com.edumania.webserver.db

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
data class SessionToken(val time: Long = Clock.System.now().epochSeconds, val hash: Uuid = Uuid.random())