package beholder.backend.config

import beholder.backend.BadStartException

class AppConfiguration {
    // this is filled by GSON from app.json
    val port: Int = 3822
    val logFileMasks = Array(0, {""})
}
