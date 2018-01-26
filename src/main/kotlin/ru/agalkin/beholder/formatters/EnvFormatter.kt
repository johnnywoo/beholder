package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class EnvFormatter(envVarName: String) : Formatter {
    private val envVarValue = System.getenv(envVarName)

    override fun formatMessage(message: Message): String {
        return envVarValue
    }
}
