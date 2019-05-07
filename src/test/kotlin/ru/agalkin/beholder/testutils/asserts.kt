package ru.agalkin.beholder.testutils

import ru.agalkin.beholder.Message
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun assertByteArraysEqual(a: ByteArray, b: ByteArray) {
    if (a.size != b.size) {
        assertTrue(false, "Byte arrays differ in size: ${a.size}, ${b.size}")
    }
    for (i in a.indices) {
        if (a[i] != b[i]) {
            assertTrue(false, "Different bytes at position $i: '${a[i].toInt()}', '${b[i].toInt()}'")
        }
    }
}

fun assertFieldNames(message: Message?, vararg names: String) {
    assertNotNull(message)
    assertEquals(names.sorted(), message.getFieldNames().sorted())
}

fun assertFieldValues(message: Message?, values: Map<String, String>) {
    assertNotNull(message)
    assertEquals(message.getFieldNames().sorted(), values.keys.sorted())
    for ((key, value) in values) {
        assertEquals(value, message.getStringField(key))
    }
}
