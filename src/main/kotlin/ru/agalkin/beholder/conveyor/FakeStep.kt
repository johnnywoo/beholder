package ru.agalkin.beholder.conveyor

import ru.agalkin.beholder.Message

object FakeStep : Step {
    override fun execute(message: Message)
        = StepResult.CONTINUE

    override fun getDescription()
        = "Fake step"
}
