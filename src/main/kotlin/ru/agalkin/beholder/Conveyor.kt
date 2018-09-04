package ru.agalkin.beholder

class Conveyor private constructor() {
    private val steps = mutableListOf<(Message) -> StepResult>()

    fun addStep(block: (Message) -> StepResult) {
        steps.add(block)
    }

    fun dropMessages(): Conveyor {
        return Conveyor()
    }

    fun addInput(): Input {
        return InputImpl(steps.size)
    }

    fun mergeIntoInput(input: Input) {
        addStep {
            input.addMessage(it)
            return@addStep StepResult.CONTINUE
        }
    }

    fun copyToConveyor(conveyor: Conveyor) {
        val input = conveyor.addInput()
        addStep {
            input.addMessage(it.copy())
            return@addStep StepResult.CONTINUE
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
