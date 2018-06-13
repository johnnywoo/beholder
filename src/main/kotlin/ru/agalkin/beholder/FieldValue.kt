package ru.agalkin.beholder

abstract class FieldValue {
    companion object {
        val empty: FieldValue = StringFieldValue("")

        fun fromString(string: String): FieldValue {
            if (string.isEmpty()) {
                return empty
            }
            return StringFieldValue(string)
        }

        fun fromByteArray(byteArray: ByteArray, length: Int): FieldValue {
            if (length == 0) {
                return empty
            }
            return ByteArrayFieldValue(byteArray, length)
        }
    }

    abstract fun getByteLength(): Int

    // todo remove this, make some kind of write() method
    abstract fun toByteArray(): ByteArray

    abstract override fun toString(): String

    fun prepend(prefix: String): FieldValue
        = ModifiedFieldValue(prefix, this)

    fun withNewlineAtEnd()
        = FieldValue.fromString(addNewlineIfNeeded(toString()))


    private class StringFieldValue(private val string: String) : FieldValue() {
        override fun getByteLength()
            = string.toByteArray().size

        override fun toByteArray()
            = string.toByteArray()

        override fun toString()
            = string
    }

    private class ByteArrayFieldValue(
        private val byteArray: ByteArray,
        private val byteArrayLength: Int = 0
    ) : FieldValue() {

        override fun getByteLength()
            = byteArrayLength

        override fun toByteArray()
            = byteArray

        override fun toString()
            = String(byteArray, 0, byteArrayLength)
    }

    private class ModifiedFieldValue(
        private val prefix: String,
        private val fieldValue: FieldValue
    ) : FieldValue() {

        override fun toString()
            = prefix + fieldValue.toString()

        override fun toByteArray()
            = toString().toByteArray()

        override fun getByteLength()
            = prefix.toByteArray().size + fieldValue.getByteLength()
    }
}
