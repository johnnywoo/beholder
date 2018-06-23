package ru.agalkin.beholder.queue

enum class Received(val waitMillis: Long = 0) {
    OK,
    RETRY
}
