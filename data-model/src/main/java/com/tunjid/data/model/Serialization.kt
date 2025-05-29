package com.tunjid.data.model

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/**
 * A declaration for type that can be serialized as bytes.
 * Currently does this using CBOR; Protobufs were evaluated but they're too terse an don't work
 * very well for preserving information for lists or maps
 */
interface ByteSerializable

/**
 * Serializes a [ByteSerializable] to a [ByteArray] and deserializes a [ByteArray] back into
 * its original type
 */
interface ByteSerializer {
    val format: BinaryFormat
}

inline fun <reified T : ByteSerializable> ByteSerializer.fromBytes(bytes: ByteArray): T =
    format.decodeFromByteArray(bytes)

inline fun <reified T : ByteSerializable> ByteSerializer.toBytes(item: T): ByteArray =
    format.encodeToByteArray(value = item)

class DelegatingByteSerializer(
    override val format: BinaryFormat
) : ByteSerializer