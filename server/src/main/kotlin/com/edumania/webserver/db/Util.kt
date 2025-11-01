package com.edumania.webserver.db

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.types.ObjectId
import java.util.*

object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor = PrimitiveSerialDescriptor("_id", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeLong(value.date.time)
    }

    override fun deserialize(decoder: Decoder): ObjectId =
        ObjectId.getSmallestWithDate(Date(decoder.decodeLong()))
}