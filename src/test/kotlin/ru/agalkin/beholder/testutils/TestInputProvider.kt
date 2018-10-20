package ru.agalkin.beholder.testutils

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider

open class TestInputProvider : ArgumentsProvider {
    private val arguments = mutableListOf<Arguments>()

    fun case(vararg args: Any?)
        = arguments.add(Arguments.of(*args))

    override fun provideArguments(context: ExtensionContext?)
        = arguments.stream()
}
