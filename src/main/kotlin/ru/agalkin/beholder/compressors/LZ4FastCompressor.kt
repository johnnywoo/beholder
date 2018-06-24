package ru.agalkin.beholder.compressors

import net.jpountz.lz4.LZ4Factory

class LZ4FastCompressor : Compressor {
    private val compressor = LZ4Factory.fastestInstance().fastCompressor()
    private val decompressor = LZ4Factory.fastestInstance().fastDecompressor()

    override fun compress(bytes: ByteArray): ByteArray {
        val compressedBytes = ByteArray(compressor.maxCompressedLength(bytes.size))
        val compressedLength = compressor.compress(bytes, compressedBytes)
        if (compressedLength != compressedBytes.size) {
            return compressedBytes.copyOfRange(0, compressedLength)
        }
        return compressedBytes
    }

    override fun decompress(compressedBytes: ByteArray, originalLength: Int): ByteArray {
        return decompressor.decompress(compressedBytes, originalLength)
    }
}
