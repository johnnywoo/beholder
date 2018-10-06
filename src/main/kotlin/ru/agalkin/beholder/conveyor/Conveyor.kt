package ru.agalkin.beholder.conveyor

/// import ru.agalkin.beholder.formatters.JsonFormatter
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.Message
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class Conveyor private constructor(baseConveyor: Conveyor? = null) {
    private val steps: StepList = baseConveyor?.steps ?: StepList()
    private val instructions: InstructionList = baseConveyor?.instructions ?: InstructionList()
    private val inputs: MutableList<InputImpl> = baseConveyor?.inputs ?: CopyOnWriteArrayList()

    private val lastInstructionId = AtomicInteger(0)

    private val forkStepId = 1
    private val conditionalStepId = 2

    fun addStep(step: Step) {
        synchronized(steps) {
            // Добавляем новый шаг
            val addedStepId = steps.add(step)

            // Добавляем инструкцию с этим шагом
            addChainedInstruction(addedStepId, 0)
        }
    }

    private fun addInstruction(stepId: Int, nextInstructionId: Int): Int {
        /// println("addInstruction($stepId, $nextInstructionId) prev $prevInstructionId")

        // Добавляем новую инструкцию
        val addedInstructionId = instructions.add(createInstructionValue(stepId, nextInstructionId))

        lastInstructionId.set(addedInstructionId)

        return addedInstructionId
    }

    private fun addChainedInstruction(stepId: Int, nextInstructionId: Int): Int {
        val prevInstructionId = lastInstructionId.get()
        /// println("addChainedInstruction($stepId, $nextInstructionId) prev $prevInstructionId")

        val addedInstructionId = addInstruction(stepId, nextInstructionId)

        // В предыдущую инструкцию этого конвейера теперь можно прописать нашу новую инструкцию в качестве следующей
        if (prevInstructionId != 0) {
            instructions[prevInstructionId] = createInstructionValue(instructions[prevInstructionId].toInt(), addedInstructionId)
        }

        return addedInstructionId
    }

    fun addInput(description: String): ConveyorInput {
        synchronized(steps) {
            val input = insertInput(description)
            inputs.add(input)
            return input
        }
    }

    private fun insertInput(description: String): InputImpl {
        // Добавляем инструкцию без шага, которая будет служить точкой входа
        return InputImpl(addChainedInstruction(0, 0), description)
    }

    fun terminateByMergingIntoInput(input: ConveyorInput): Conveyor {
        synchronized(steps) {
            if (input is InputImpl) {
                // Надо добавить инструкцию, у которой goto будет на требуемое место
                addChainedInstruction(0, input.instructionId)
            } else {
                // Снаружи зачем-то понадобилось сделать свой ConveyorInput.
                // Придётся им воспользоваться.
                addStep(InputStep(input))
            }
        }

        return createRelatedConveyor()
    }

    fun addConditions(conditions: List<Pair<Step, (Conveyor) -> Conveyor>>): Conveyor {
        // conditional step = execute +1, if it dropped goto conditional
        //
        // 0 conditional goto 3
        // 1 (case 1 condition) goto +1
        // 2 (case 1 subcommands) goto Z
        // 3 conditional goto 6
        // 4 (case 2 condition) goto +1
        // 5 (case 2 subcommands) goto Z
        // 6 (last case condition) goto +1
        // 7 (last case subcommands) goto +1
        // Z
        synchronized(steps) {
            if (conditions.isEmpty()) {
                throw BeholderException("Cannot add 0 conditions to a Conveyor")
            }

            /// println("conditions: ${conditions.size}")

            if (conditions.size == 1) {
                addStep(conditions[0].first)
                return conditions[0].second(this)
            }

            val branchEndingInstructionIds = mutableListOf<Int>()

            var conveyor = this
            var prevConditionalInstructionId = 0
            for (i in conditions.indices) {
                val conditionBlock = conditions[i].first
                val builderBlock = conditions[i].second

                val isLast = i == (conditions.size - 1)

                if (!isLast) {
                    val conditionalInstructionId = conveyor.addChainedInstruction(conditionalStepId, 0)
                    if (prevConditionalInstructionId != 0) {
                        instructions[prevConditionalInstructionId] = createInstructionValue(conditionalStepId, conditionalInstructionId)
                    }
                    prevConditionalInstructionId = conditionalInstructionId
                    conveyor = conveyor.createRelatedConveyor()

                    val addedStepId = conveyor.steps.add(conditionBlock)
                    conveyor.addChainedInstruction(addedStepId, 0)
                } else {
                    conveyor = conveyor.createRelatedConveyor()

                    val addedStepId = conveyor.steps.add(conditionBlock)
                    val conditionalInstructionId = conveyor.addChainedInstruction(addedStepId, 0)

                    if (prevConditionalInstructionId != 0) {
                        instructions[prevConditionalInstructionId] = createInstructionValue(conditionalStepId, conditionalInstructionId)
                    }
                }
                conveyor = builderBlock(conveyor)
                branchEndingInstructionIds.add(conveyor.lastInstructionId.get())
            }

            val afterSwitchInstructionId = conveyor.addChainedInstruction(0, 0)
            for (lastId in branchEndingInstructionIds) {
                setGoto(lastId, afterSwitchInstructionId)
            }

            return conveyor
        }
    }

    private fun setGoto(fromInstructionId: Int, toInstructionId: Int) {
        val stepId = (instructions[fromInstructionId] and ((1L shl 32) - 1)).toInt()
        instructions[fromInstructionId] = createInstructionValue(stepId, toInstructionId)
    }

    fun copyToConveyor(conveyor: Conveyor, description: String) {
        synchronized(steps) {
            val input = conveyor.insertInput("copy for $description")

            // Сначала добавляем инструкцию на форк.
            // Она содержит номер инструкции, в которую поедет копия сообщения.
            addChainedInstruction(forkStepId, input.instructionId)

            // Теперь следующим id добавляем инструкцию, которая продолжает текущий конвейер.
            addInstruction(0, 0)
        }
    }

    fun createRelatedConveyor()
        = Conveyor(this)

    fun dumpInstructions() {
        if (instructions.getSize() <= 1) {
            return
        }
        println("=== built ===")
        dumpInstructionCollection(instructions.toList())
        println("=== optimized ===")
        dumpInstructionCollection(instructions.array.toList())
    }

    private fun dumpInstructionCollection(instructions: List<Long>) {
        for (id in 1 until instructions.size) {
            val gotoId = (instructions[id] shr 32).toInt()
            val stepId = (instructions[id] and ((1L shl 32) - 1)).toInt()

            for (input in inputs) {
                if (input.instructionId == id) {
                    println("[input ${input.description}]")
                }
            }

            when (stepId) {
                0 -> println("i$id")
                forkStepId -> println("i$id fork")
                conditionalStepId -> println("i$id conditional")
                else -> println("i$id step $stepId ${steps[stepId].getDescription()}")
            }
            if (gotoId == 0) {
                println("terminate")
            } else if (gotoId != id + 1) {
                println("goto i$gotoId")
            }
        }
    }

    private inner class InputImpl(val instructionId: Int, val description: String) : ConveyorInput {
        override fun addMessage(message: Message) {
            /// if (nest == 0) dumpInstructions()

            val stepsArray = steps.array
            val instructionsArray = instructions.array

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
                    val instruction = instructionsArray[gotoInstructionId]

                    val currentInstructionId = gotoInstructionId

                    gotoInstructionId = (instruction shr 32).toInt()
                    val stepId = (instruction and ((1L shl 32) - 1)).toInt()

                    /// debug("Instruction $currentInstructionId" + if (forkInstructionIds.size > 0) "  forks: ${forkInstructionIds.size}" else "")
                    /// debug("~ step $stepId ${steps[stepId].getDescription()}")
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
                        conditionalStepId -> {
                            // debug("~ conditional --------------------------------------")
                            // Текущая инструкция содержит номер, на который надо топать, если следующая дропнет
                            val gotoIfDroppedInstructionId = gotoInstructionId
                            val conditionInstructionId = currentInstructionId + 1

                            val conditionInstruction = instructionsArray[conditionInstructionId]

                            gotoInstructionId = (conditionInstruction shr 32).toInt()
                            val conditionStepId = (conditionInstruction and ((1L shl 32) - 1)).toInt()
                            /// debug("~ conditionInstructionId $conditionInstructionId")
                            /// debug("~ goto if dropped $gotoIfDroppedInstructionId")
                            /// debug("~ goto if not dropped $gotoInstructionId")

                            val conditionStep = stepsArray[conditionStepId]
                            /// debug("~ condition step $conditionStepId ${conditionStep.getDescription()}")
                            if (conditionStep.execute(currentMessage) == StepResult.DROP) {
                                /// debug("~ condition returned DROP")
                                gotoInstructionId = gotoIfDroppedInstructionId
                            }
                        }
                        else -> {
                            // debug("~ lambda")
                            val step = stepsArray[stepId]
                            if (step.execute(currentMessage) == StepResult.DROP) {
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

    private class InputStep(private val input: ConveyorInput) : Step {
        override fun execute(message: Message): StepResult {
            input.addMessage(message)
            return StepResult.CONTINUE
        }

        override fun getDescription()
            = "add message into input"
    }

    private class StepList {
        private val list = mutableListOf<Step>()
        @Volatile var array: Array<Step> = arrayOf()

        init {
            // Номера шагов должны начинаться с 1, чтобы проще было определять отсутствие шага
            add(FakeStep) // id 0

            // Добавляем специальные шаги
            add(FakeStep) // forkStepId 1
            add(FakeStep) // conditionalStepId 2
        }

        fun add(element: Step): Int {
            return synchronized(this) {
                list.add(element)
                array = list.toTypedArray()
                list.size - 1
            }
        }

        operator fun get(id: Int)
            = synchronized(this) { list[id] }
    }

    private class InstructionList {
        private val list = mutableListOf<Long>()
        @Volatile var array: LongArray = longArrayOf()

        init {
            // Номера инструкций должны начинаться с 1, чтобы проще было определять отсутствие инструкции
            add(-1)
        }

        fun getSize()
            = synchronized(this) { list.size }

        fun add(element: Long): Int {
            return synchronized(this) {
                list.add(element)
                updateArray()
                list.size - 1
            }
        }

        operator fun get(id: Int)
            = synchronized(this) { list[id] }

        operator fun set(id: Int, value: Long) {
            synchronized(this) {
                list[id] = value
                updateArray()
            }
        }

        fun toList()
            = list

        private fun updateArray() {
            val optimizedInstructions = list.toLongArray()

            /// println(" @ starting optimizations")
            var isModified = true
            while (isModified) {
                /// println(" @ loop")
                isModified = false

                for (i in 1 until optimizedInstructions.size) {
                    val instruction = optimizedInstructions[i]
                    val gotoId = (instruction shr 32).toInt()
                    val stepId = (instruction and ((1L shl 32) - 1)).toInt()

                    // Последняя инструкция = оптимизировать нечего
                    if (gotoId == 0) {
                        continue
                    }

                    val nextInstruction = optimizedInstructions[gotoId]
                    val nextGotoId = (nextInstruction shr 32).toInt()
                    val nextStepId = (nextInstruction and ((1L shl 32) - 1)).toInt()

                    // Следующая инструкция — просто goto дальше, без шагов.
                    // Можно её пропустить.
                    if (nextStepId == 0) {
                        optimizedInstructions[i] = createInstructionValue(stepId, nextGotoId)
                        isModified = true
                        /// println(" @ noop instruction")
                    }
                }
            }

            array = optimizedInstructions
        }
    }

    companion object {
        /// var nest = 0

        fun createInitialConveyor()
            = Conveyor()

        fun createInstructionValue(stepId: Int, gotoInstructionId: Int): Long {
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
    }
}
