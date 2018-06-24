package ru.agalkin.beholder.compressors

interface Compressor {
    fun compress(bytes: ByteArray): ByteArray
    fun decompress(compressedBytes: ByteArray, originalLength: Int): ByteArray
}
