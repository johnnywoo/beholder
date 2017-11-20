package ru.agalkin.beholder

import sun.misc.Signal
import java.io.File

const val ETC_CONFIG_FILE = "/etc/beholder/beholder.conf"

fun main(args: Array<String>) {
    dumpHelpIfNeeded(args)

    val configFile = when {
        args.size > 1 -> args[1]
        with(File(ETC_CONFIG_FILE)) {isFile && canRead()} -> ETC_CONFIG_FILE
        else -> null
    }

    val app = Beholder(configFile)

    Signal.handle(Signal("HUP")) {
        println("Got SIGHUP")
        app.reload()
    }
}

fun dumpHelpIfNeeded(args: Array<String>) {
    if (!args.indices.contains(1)) {
        return
    }
    if (args[1] == "--help" || args[1] == "-h") {
        System.err.println("Usage: " + args[0] + " [config-file]")
        System.err.println("Default config file is $ETC_CONFIG_FILE")
        System.exit(0)
    }
}
