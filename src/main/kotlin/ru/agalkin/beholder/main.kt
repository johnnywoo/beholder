package ru.agalkin.beholder

import ru.agalkin.beholder.config.parser.ParseException
import sun.misc.Signal
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val cli = Cli(args) { parseError ->
        System.err.println("Bad arguments: ${parseError.message}")
        exitProcess(1)
    }

    if (cli.isHelp) {
        cli.printHelp()
        exitProcess(0)
    }

    val configFile: String?
    val configText: String?

    when {
        // beholder --config="flow {from udp 3231; to stdout}"
        cli.configText != null -> {
            configFile = null
            configText = cli.configText + "\n"
            println("Using config from CLI arguments")
        }

        // beholder --config-file=/etc/beholder/beholder.conf
        cli.configFile != null -> {
            val filename = cli.configFile

            val file = File(filename)
            if (!file.isFile || !file.canRead()) {
                System.err.println("Cannot read config from $filename")
                exitProcess(1)
            }

            configFile = filename
            configText = null // config reader will read the file by itself

            println("Using config from file: $filename")
        }

        // no file and no config text:
        // use bundled config from resources
        // this is intended primarily for development purposes
        else -> {
            val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.conf")

            configText = InputStreamReader(inputStream).readText()
            configFile = null

            println("Using bundled config from jar resources")
        }
    }

    val app: Beholder
    try {
        app = Beholder(configFile, configText)
    } catch (e: ParseException) {
        System.err.println("=== Error: invalid config ===")
        System.err.println(e.message)
        exitProcess(1)
    }

    if (cli.isTest) {
        // всё что нужно было, мы уже сделали (проверили конфиг)
        exitProcess(0)
    }

    app.start()

    Signal.handle(Signal("HUP")) {
        println("Got SIGHUP")
        app.reload()
    }
}
