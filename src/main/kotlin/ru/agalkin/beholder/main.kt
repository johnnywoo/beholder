package ru.agalkin.beholder

import ru.agalkin.beholder.config.parser.ParseException
import sun.misc.Signal
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { _, exception ->
        Thread.setDefaultUncaughtExceptionHandler(null)
        InternalLog.exception(exception)
        exitProcess(1)
    }

    val cli = Cli(args) { parseError ->
        InternalLog.err("Bad arguments: ${parseError.message}")
        exitProcess(1)
    }

    if (cli.isShortHelp) {
        cli.printUsage()
        exitProcess(0)
    }

    if (cli.isQuiet) {
        InternalLog.stopWritingToStdout()
    }

    val logFile = cli.logFile
    if (logFile != null) {
        InternalLog.info("Internal log file: $logFile")
        InternalLog.copyToFile(logFile)
    }

    InternalLog.info("Beholder is starting")

    Runtime.getRuntime().addShutdownHook(object : Thread("shutdown-hook") {
        override fun run() {
            InternalLog.info("Beholder is stopping")
        }
    })

    val configFile: String?
    val configText: String?

    when {
        // beholder --config="flow {from udp 3231; to stdout}"
        cli.configText != null -> {
            configFile = null
            configText = cli.configText + "\n"
            InternalLog.info("Using config from CLI arguments")
        }

        // beholder --config-file=/etc/beholder/beholder.conf
        cli.configFile != null -> {
            val filename = cli.configFile

            val file = File(filename)
            if (!file.isFile || !file.canRead()) {
                InternalLog.err("Cannot read config from $filename")
                exitProcess(1)
            }

            configFile = filename
            configText = null // config reader will read the file by itself

            InternalLog.info("Using config from file: $filename")
        }

        // no file and no config text:
        // use bundled config from resources
        // this is intended primarily for development purposes
        else -> {
            configFile = null
            configText = readTextFromResource("default-config.conf")

            InternalLog.info("Using bundled config from jar resources")
        }
    }

    val app: Beholder
    try {
        app = Beholder(configFile, configText)
    } catch (e: ParseException) {
        InternalLog.err("=== Error: invalid config ===")
        InternalLog.err(e.message)
        exitProcess(1)
    }

    if (cli.isTest) {
        // всё что нужно было, мы уже сделали (проверили конфиг)
        exitProcess(0)
    }

    app.start()

    // we need very little memory compared to most Java programs
    // let's shrink the initial heap
    Runtime.getRuntime().gc()

    Signal.handle(Signal("HUP")) {
        InternalLog.info("Got SIGHUP")
        InternalLog.info("Beholder is reloading")
        app.reload()
    }
}
