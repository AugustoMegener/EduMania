package com.edumania.webserver.web.form

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActionForm(val publicId: Long, val action: Action) {

    @Serializable
    enum class Action { @SerialName("EDIT") EDIT, @SerialName("DELETE") DELETE }
}
