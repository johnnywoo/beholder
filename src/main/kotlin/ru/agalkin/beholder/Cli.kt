package ru.agalkin.beholder

import org.apache.commons.cli.*
import kotlin.system.exitProcess

class Cli(args: Array<String>, onParseError: (ParseException) -> Unit) {
    val isShortHelp: Boolean
        get() = cliArgs.hasOption("h")

    val isFullHelp: Boolean
        get() = cliArgs.hasOption("help")

    val configText: String?
        get() = cliArgs.getOptionValue("config")

    val configFile: String?
        get() = cliArgs.getOptionValue("config-file")

    val isTest: Boolean
        get() = cliArgs.hasOption("test")

    private val options = Options()
    private val cliArgs: CommandLine

    init {
        options.addOption(
            Option.builder("f")
                .longOpt("config-file")
                .argName("file")
                .hasArg()
                .desc("Use config from a file")
                .build()
        )

        options.addOption(
            Option.builder("c")
                .longOpt("config")
                .argName("text")
                .hasArg()
                .desc("Use config from the argument")
                .build()
        )

        options.addOption(
            Option.builder("t")
                .longOpt("test")
                .desc("Config test")
                .build()
        )

        options.addOption(
            Option.builder("h")
                .desc("Show usage")
                .build()
        )

        options.addOption(
            Option.builder()
                .longOpt("help")
                .desc("Show full help with command descriptions")
                .build()
        )

        val cliParser = DefaultParser()
        try {
            cliArgs = cliParser.parse(options, args)
        } catch (e: ParseException) {
            onParseError(e)
            exitProcess(1) // just in case the callback did not exit
        }
    }

    fun printUsage() {
        HelpFormatter().printHelp("beholder", options)
    }

    fun printHelp() {
        printUsage()
        println(readTextFromResource("help.txt"))
    }
}
