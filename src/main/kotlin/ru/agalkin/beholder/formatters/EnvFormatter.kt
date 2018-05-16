package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

class EnvFormatter(envVarName: String) : Formatter {
    private val envVarValue = FieldValue.fromString(System.getenv(envVarName))

    override fun formatMessage(message: Message)
        = envVarValue
}
