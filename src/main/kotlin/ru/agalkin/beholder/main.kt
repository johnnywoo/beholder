package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.stats.PrometheusMetricsHttpServer
import ru.agalkin.beholder.stats.StatsHolder
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
        println("Docs: https://github.com/johnnywoo/beholder")
        exitProcess(0)
    }

    if (cli.isVersion) {
        var version: String
        try {
            version = readTextFromResource("version.txt").trim()
        } catch (e: Throwable) {
            version = "of unknown version: ${e.message}"
        }
        println("beholder $version")
        exitProcess(0)
    }

    if (cli.isQuiet) {
        InternalLog.setStdout(null)
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

    val configMaker: (Beholder) -> Config

    when {
        // beholder --config="from udp 3231; to stdout"
        cli.configText != null -> {
            InternalLog.info("Using config from CLI arguments")
            configMaker = { Config.fromStringWithLog(it, cli.configText + "\n", "cli-arg") }
        }

        // beholder --config-file=/etc/beholder/beholder.conf
        cli.configFile != null -> {
            val filename = cli.configFile
            filename!!

            val file = File(filename)
            if (!file.isFile || !file.canRead()) {
                InternalLog.err("Cannot read config from $filename")
                exitProcess(1)
            }

            InternalLog.info("Using config from file: $filename")

            configMaker = { Config.fromFile(it, filename) }
        }

        // no file and no config text:
        // use bundled config from resources
        // this is intended primarily for development purposes
        else -> {
            InternalLog.info("Using bundled config from jar resources")

            configMaker = { Config.fromStringWithLog(it, readTextFromResource("default-config.conf"), "config-from-jar") }
        }
    }

    StatsHolder.start()

    val app: Beholder
    try {
        app = Beholder(configMaker)
    } catch (e: ParseException) {
        InternalLog.err("=== Error: invalid config ===")
        InternalLog.err(e.message)
        exitProcess(1)
    }

    if (cli.isTest) {
        // всё что нужно было, мы уже сделали (проверили конфиг)
        exitProcess(0)
    }

    if (cli.isDumpInstructions) {
        app.config.initialConveyor.dumpInstructions()
        exitProcess(0)
    }

    PrometheusMetricsHttpServer.init(app)

    app.start()

    Signal.handle(Signal("HUP")) {
        InternalLog.info("Got SIGHUP")
        InternalLog.info("Beholder is reloading")
        app.reload()
    }
}
