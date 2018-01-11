package ru.agalkin.beholder

import org.apache.commons.cli.*

class Cli(args: Array<String>, onParseError: (ParseException) -> Nothing) {
    val isShortHelp: Boolean
        get() = cliArgs.hasOption("h")

    val isQuiet: Boolean
        get() = cliArgs.hasOption("quiet")

    val logFile: String?
        get() = cliArgs.getOptionValue("log")

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
                .desc("Config test: syntax and minimal validation")
                .build()
        )

        options.addOption(
            Option.builder("q")
                .longOpt("quiet")
                .desc("Do not print internal log into stdout")
                .build()
        )

        options.addOption(
            Option.builder("l")
                .longOpt("log")
                .argName("file")
                .hasArg()
                .desc("Internal log file")
                .build()
        )

        options.addOption(
            Option.builder("h")
                .desc("Show usage")
                .build()
        )

        val cliParser = DefaultParser()
        try {
            cliArgs = cliParser.parse(options, args)
        } catch (e: ParseException) {
            onParseError(e)
        }
    }

    fun printUsage() {
        HelpFormatter().printHelp("beholder", options)
    }
}
