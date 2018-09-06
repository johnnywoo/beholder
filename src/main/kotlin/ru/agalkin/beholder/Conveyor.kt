package ru.agalkin.beholder

/// import ru.agalkin.beholder.formatters.JsonFormatter
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val fakeStep: (Message) -> Any? = {}

class Conveyor private constructor(
    private val steps: MutableList<(Message) -> Any?> = CopyOnWriteArrayList(),
    private val instructions: MutableList<Long> = CopyOnWriteArrayList()
) {
    private val isReachable = AtomicBoolean(false)
    private val lastInstructionId = AtomicInteger(0)

    private val forkStepId = 1

    init {
        synchronized(steps) {
            if (instructions.size == 0) {
                // Номера инструкций должны начинаться с 1, чтобы проще было определять отсутствие инструкции
                instructions.add(-1) // id 0
            }

            if (steps.size == 0) {
                // Номера шагов должны начинаться с 1, чтобы проще было определять отсутствие шага
                steps.add(fakeStep) // id 0

                // Добавляем специальные шаги
                steps.add(fakeStep) // id 1
            }
        }
    }

    fun addStep(block: (Message) -> Any?) {
        if (!isReachable.get()) {
            return
        }

        synchronized(steps) {
            // Добавляем новый шаг
            steps.add(block)
            val addedStepId = steps.size - 1

            // Добавляем инструкцию с этим шагом
            addChainedInstruction(addedStepId, 0)
        }
    }

    private fun addInstruction(stepId: Int, nextInstructionId: Int): Int {
        val prevInstructionId = lastInstructionId.get()
        /// println("addInstruction($stepId, $nextInstructionId) prev $prevInstructionId")

        if (prevInstructionId > 0 && instructions[prevInstructionId] == 0L) {
            // В предыдущей инструкции нет ни шага, ни goto. Можно туда и прописаться.
            instructions[prevInstructionId] = createInstructionValue(stepId, nextInstructionId)
            return prevInstructionId
        }

        // Добавляем новую инструкцию
        instructions.add(createInstructionValue(stepId, nextInstructionId))
        val addedInstructionId = instructions.size - 1

        lastInstructionId.set(addedInstructionId)

        return addedInstructionId
    }

    private fun addChainedInstruction(stepId: Int, nextInstructionId: Int): Int {
        val prevInstructionId = lastInstructionId.get()
        /// println("addChainedInstruction($stepId, $nextInstructionId) prev $prevInstructionId")

        if (prevInstructionId > 0 && instructions[prevInstructionId] == 0L) {
            // В предыдущей инструкции нет ни шага, ни goto. Можно туда и прописаться.
            instructions[prevInstructionId] = createInstructionValue(stepId, nextInstructionId)
            return prevInstructionId
        }

        val addedInstructionId = addInstruction(stepId, nextInstructionId)

        // В предыдущую инструкцию этого конвейера теперь можно прописать нашу новую инструкцию в качестве следующей
        if (prevInstructionId != 0) {
            instructions[prevInstructionId] = createInstructionValue(instructions[prevInstructionId].toInt(), addedInstructionId)
        }

        return addedInstructionId
    }

    private fun createInstructionValue(stepId: Int, gotoInstructionId: Int): Long {
        val maxStepId = (1L shl 31) - 1 // 1 бит там занят знаком
        if (stepId > maxStepId) {
            throw BeholderException("Conveyor step id $stepId is more than $maxStepId")
        }
        val maxInstructionId = (1L shl 31) - 1 // 1 бит там занят знаком
        if (gotoInstructionId > maxInstructionId) {
            throw BeholderException("Conveyor instruction id $gotoInstructionId is more than $maxInstructionId")
        }
        return (gotoInstructionId.toLong() shl 32) + stepId
    }

    fun addInput(): Input {
        synchronized(steps) {
            return insertInput()
        }
    }

    private fun insertInput(): InputImpl {
        isReachable.set(true)

        // Добавляем инструкцию без шага, которая будет служить точкой входа
        return InputImpl(addChainedInstruction(0, 0))
    }

    fun terminateByMergingIntoInput(input: Input): Conveyor {
        if (!isReachable.get()) {
            return this
        }

        synchronized(steps) {
            if (input is InputImpl) {
                // Надо добавить инструкцию, у которой goto будет на требуемое место
                addChainedInstruction(0, input.instructionId)
            } else {
                // Снаружи зачем-то понадобилось сделать свой Input.
                // Придётся им воспользоваться.
                addStep(input::addMessage)
            }
        }

        return createRelatedConveyor()
    }

    fun copyToConveyor(conveyor: Conveyor) {
        if (!isReachable.get()) {
            return
        }

        synchronized(steps) {
            val input = conveyor.insertInput()

            // Сначала добавляем инструкцию на форк.
            // Она содержит номер инструкции, в которую поедет копия сообщения.
            addChainedInstruction(forkStepId, input.instructionId)

            // Теперь следующим id добавляем инструкцию, которая продолжает текущий конвейер.
            addInstruction(0, 0)
        }
    }

    fun createRelatedConveyor()
        = Conveyor(steps, instructions)



    interface Input {
        fun addMessage(message: Message)
    }

    private inner class InputImpl(val instructionId: Int) : Input {
        override fun addMessage(message: Message) {
            /// nest++

            val forkInstructionIds = LinkedList<Int>()
            val forkMessages = LinkedList<Message>()

            var gotoInstructionId = instructionId
            var currentMessage = message

            /// val debug: (String) -> Unit = {
            ///     println("  ".repeat(nest) + it)
            /// }

            /// debug("================ MESSAGE START ===============")
            /// debug("instructions: ${instructions.size}")

            while (true) {
                /// debug("=== FORK START ===")

                while (gotoInstructionId != 0) {
                    val instruction = instructions[gotoInstructionId]

                    val currentInstructionId = gotoInstructionId

                    gotoInstructionId = (instruction shr 32).toInt()
                    val stepId = (instruction and ((1L shl 32) - 1)).toInt()

                    /// debug("Instruction $currentInstructionId" + if (forkInstructionIds.size > 0) "  forks: ${forkInstructionIds.size}" else "")
                    /// debug("~ step $stepId")
                    /// debug("~ message " + JsonFormatter(null).formatMessage(currentMessage).toString())

                    when (stepId) {
                        0 -> {
                            // debug("~ no step")
                            // Шага нет, просто едем на следующую инструкцию
                        }
                        forkStepId -> {
                            // debug("~ fork --------------------------------------")
                            // Нужно сделать форк и поехать на следующую инструкцию
                            forkInstructionIds.add(gotoInstructionId)
                            forkMessages.add(currentMessage.copy())
                            gotoInstructionId = currentInstructionId + 1
                        }
                        else -> {
                            // debug("~ lambda")
                            val step = steps[stepId]
                            if (step(currentMessage) == StepResult.DROP) {
                                // Кончился текущий форк
                                gotoInstructionId = 0
                                // debug("+ dropped by step")
                            }
                        }
                    }

                    /// debug("~ next instruction $gotoInstructionId")
                }

                if (forkInstructionIds.isEmpty()) {
                    break
                }

                gotoInstructionId = forkInstructionIds.remove()
                currentMessage = forkMessages.remove()
            }

            /// debug("++++++++++++++++ MESSAGE END ++++++++++++++")
            /// nest--
        }
    }

    enum class StepResult {
        CONTINUE, DROP
    }

    companion object {
        /// var nest = 0

        fun createInitialConveyor()
            = Conveyor()
    }
}
