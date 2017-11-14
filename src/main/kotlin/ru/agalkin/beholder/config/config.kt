package ru.agalkin.beholder.config

import ru.agalkin.beholder.config.parser.Config
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.stream.Collectors

fun configFromFile(filename: String): Config {
    return Config(File(filename).readText())
}

fun defaultConfig(): Config {
    val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.conf")
    val configText  = BufferedReader(InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"))
    return Config(configText)
}

