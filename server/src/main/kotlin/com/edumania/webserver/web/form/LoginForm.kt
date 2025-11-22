package com.edumania.webserver.web.form

import kotlinx.serialization.Serializable

@Serializable
data class LoginForm(val email: String, val password: String)