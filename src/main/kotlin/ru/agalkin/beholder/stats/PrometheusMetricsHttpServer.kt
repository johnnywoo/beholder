package ru.agalkin.beholder.stats

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.ConfigOption
import java.lang.StringBuilder

object PrometheusMetricsHttpServer {
    private var address: Address? = null

    fun init(app: Beholder) {
        app.afterReloadCallbacks.add {
            synchronized(this) {
                val currentAddress = app.getAddressOption(ConfigOption.PROMETHEUS_METRICS_HTTP_ADDRESS)
                if (currentAddress == null) {
                    if (address != null) {
                        InternalLog.info("Stopping Prometheus metrics HTTP server at $address")
                        stopServer()
                    }
                } else if (currentAddress != address) {
                    if (address != null) {
                        InternalLog.info("Stopping Prometheus metrics HTTP server at $address")
                        stopServer()
                    }
                    InternalLog.info("Starting Prometheus metrics HTTP server at $currentAddress")
                    startServer(currentAddress)
                }
            }
        }
    }

    private var httpServer: HttpServer? = null

    private fun startServer(newAddress: Address) {
        val server = HttpServer.create(newAddress.toSocketAddress(), 0)

        server.createContext("/") {
            if (it != null) {
                respond(it)
            }
        }
        server.start()

        address = newAddress
        httpServer = server
    }

    private fun stopServer() {
        httpServer?.stop(0)
        httpServer = null
    }

    private val statsHolder = Stats.createHolder()

    private val countingStatNames = statsHolder.getCountingStatNames().toSet()

    private val statDescriptions = statsHolder.getDescriptions()

    // Some stats do not make sense unless you reset the data every time stats are being read.
    // We do not reset stats for Prometheus, therefore we will not report these stats here.
    private val ignoredStatNames = countingStatNames.filter {
        it.endsWith("MaxBytes") || it.endsWith("MaxNanos") || it.endsWith("MaxSize")
    }

    private fun respond(exchange: HttpExchange) {
        val sb = StringBuilder()

        loop@ for ((name, value) in statsHolder.getStatValues()) {
            val type = when (name) {
                in ignoredStatNames -> continue@loop
                in countingStatNames -> "counter"
                else -> "gauge"
            }

            val help = statDescriptions[name]
            if (help != null) {
                sb.append("# HELP $name $help\n")
            }
            sb.append("# TYPE $name $type\n")
            sb.append("$name $value\n")
        }

        val response = sb.toString()

        val bytes = response.toByteArray()
        exchange.responseHeaders.add("Content-Type", "text/plain; version=0.0.4")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use {
            it.write(bytes)
        }
    }
}
