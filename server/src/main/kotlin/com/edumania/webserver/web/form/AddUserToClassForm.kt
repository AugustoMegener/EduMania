package com.edumania.webserver.web.form

import kotlinx.serialization.Serializable

@Serializable
data class AddUserToClassForm(val userPublicId: Long, val classPublicId: Long)
