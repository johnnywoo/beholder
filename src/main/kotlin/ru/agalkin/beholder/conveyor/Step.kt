package ru.agalkin.beholder.conveyor

import ru.agalkin.beholder.Message

interface Step {
    fun execute(message: Message): StepResult
    fun getDescription(): String
        = "step"
}
