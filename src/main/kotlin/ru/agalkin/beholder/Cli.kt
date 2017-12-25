package ru.agalkin.beholder

import org.apache.commons.cli.*
import ru.agalkin.beholder.commands.*
import ru.agalkin.beholder.config.Config

class Cli(args: Array<String>, onParseError: (ParseException) -> Nothing) {
    val isShortHelp: Boolean
        get() = cliArgs.hasOption("h")

    val isFullHelp: Boolean
        get() = cliArgs.hasOption("help")

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
                .desc("Config test")
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
        }
    }

    fun printUsage() {
        HelpFormatter().printHelp("beholder", options)
    }

    fun printHelp() {
        printUsage()

        println(paint(
            "\n\n" + Config.help +
            "\n\n" + FlowCommand.help +
            "\n\n" + FromCommand.help +
            "\n\n" + ParseCommand.help +
            "\n\n" + SetCommand.help +
            "\n\n" + ToCommand.help
        ))
    }

    private fun paint(text: String): String {
        if (System.console() == null) {
            // non-interactive
            return text
        }

        val escape: (Color) -> String = {
            Regex.escapeReplacement(it.toString())
        }

        return text
            // заголовки делаем жёлтыми (заголовок = две пустые строки подряд и потом текст)
            .replace(
                Regex("((?:^|\n)\n\n)((?:[^\n]+\n)+)"),
                "$1" + escape(Color.YELLOW) + "$2" + escape(Color.NONE)
            )
            // `куски кода` делаем голубыми
            .replace(
                Regex("`([^\n]+?)`"),
                escape(Color.CYAN) + "$1" + escape(Color.NONE)
            )
    }

    private enum class Color(private val number: Int) {
        RED(31),
        GREEN(32),
        YELLOW(33),
        BLUE(34),
        PINK(35),
        CYAN(36),
        GRAY(37),
        NONE(0);

        override fun toString(): String
            = if (number == 0) "\u001B[0m" else "\u001B[0;${number}m"
    }
}
