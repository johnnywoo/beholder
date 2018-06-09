package ru.agalkin.beholder

open class FieldValue(
    private val stringValue: String? = null,
    private val byteArrayValue: ByteArray? = null,
    private val byteArrayLength: Int = 0
) {
    companion object {
        fun fromString(string: String)
            = FieldValue(stringValue = string)

        fun fromByteArray(byteArray: ByteArray, length: Int)
            = FieldValue(byteArrayValue = byteArray, byteArrayLength = length)

        val empty = fromString("")
    }

    open fun getByteLength(): Int {
        if (stringValue != null) {
            return stringValue.toByteArray().size
        }
        if (byteArrayValue != null) {
            return byteArrayLength
        }
        return 0
    }

    fun toByteArray(): ByteArray {
        if (stringValue != null) {
            return stringValue.toByteArray()
        }
        if (byteArrayValue != null) {
            return byteArrayValue
        }
        return byteArrayOf()
    }

    override fun toString(): String {
        if (stringValue != null) {
            return stringValue
        }
        if (byteArrayValue != null) {
            return String(byteArrayValue, 0, byteArrayLength)
        }
        return ""
    }

    fun prepend(prefix: String): FieldValue
        = ModifiedFieldValue(prefix, this)

    fun withNewlineAtEnd()
        = FieldValue.fromString(addNewlineIfNeeded(toString()))


    private class ModifiedFieldValue(
        private val prefix: String,
        private val fieldValue: FieldValue
    ) : FieldValue() {

        override fun toString()
            = prefix + fieldValue.toString()

        override fun getByteLength()
            = prefix.toByteArray().size + fieldValue.getByteLength()
    }
}
