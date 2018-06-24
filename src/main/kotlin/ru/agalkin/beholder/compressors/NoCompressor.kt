package ru.agalkin.beholder.compressors

import ru.agalkin.beholder.BeholderException

class NoCompressor : Compressor {
    override fun compress(bytes: ByteArray): ByteArray {
        return bytes
    }

    override fun decompress(compressedBytes: ByteArray, originalLength: Int): ByteArray {
        if (originalLength != compressedBytes.size) {
            throw BeholderException("NoCompressor cannot decompress ${compressedBytes.size} bytes into $originalLength")
        }
        return compressedBytes
    }
}
