package ru.agalkin.beholder

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class Conveyor private constructor() {
    private val steps = CopyOnWriteArrayList<(Message) -> Any>()

    private val isReachable = AtomicBoolean(false)

    fun addStep(block: (Message) -> Any) {
        if (isReachable.get()) {
            steps.add(block)
        }
    }

    fun dropMessages(): Conveyor {
        if (!isReachable.get()) {
            return this
        }
        return createRelatedConveyor()
    }

    fun addInput(): Input {
        isReachable.set(true)
        return InputImpl(steps.size)
    }

    fun mergeIntoInput(input: Input) {
        if (!isReachable.get()) {
            return
        }
        addStep(input::addMessage)
    }

    fun copyToConveyor(conveyor: Conveyor) {
        if (!isReachable.get()) {
            return
        }
        val input = conveyor.addInput()
        addStep { message ->
            input.addMessage(message.copy())
        }
    }

    fun createRelatedConveyor()
        = Conveyor()



    interface Input {
        fun addMessage(message: Message)
    }

    private inner class InputImpl(private val inputAtStepIndex: Int) : Input {
        override fun addMessage(message: Message) {
            for (i in inputAtStepIndex until steps.size) {
                val step = steps[i]
                if (step(message) == StepResult.DROP) {
                    break
                }
            }
        }
    }

    enum class StepResult {
        CONTINUE, DROP
    }

    companion object {
        fun createInitialConveyor()
            = Conveyor()
    }
}
