package com.edumania.webserver.web.form

import kotlinx.serialization.Serializable

@Serializable
data class ActionForm(val publicId: Long, val action: Action) {

    @Serializable
    enum class Action { EDIT, DELETE }
}
