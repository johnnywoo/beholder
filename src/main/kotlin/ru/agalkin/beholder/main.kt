package ru.agalkin.beholder

import sun.misc.Signal
import java.io.File

fun main(args: Array<String>) {
    dumpHelpIfNeeded(args)

    val etcConfigFile = "/etc/beholder/beholder.conf"

    val configFile = when {
        args.size > 1 -> args[1]
        with(File(etcConfigFile)) {isFile && canRead()} -> etcConfigFile
        else -> null
    }

    val app = Beholder(configFile)

    Signal.handle(Signal("HUP")) {
        println("Got SIGHUP")
        // app.reload()
    }

    app.start()
}

fun dumpHelpIfNeeded(args: Array<String>) {
    if (args.size <= 1) {
        return
    }
    if (args[1] == "--help" || args[1] == "-h") {
        System.err.println("Usage: " + args[0] + " [config-file]")
        System.exit(0)
    }
}
